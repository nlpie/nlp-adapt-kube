import java.util.concurrent.TimeUnit
import java.io.ByteArrayOutputStream
import java.io.PrintStream

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.json.JsonParserType
import groovy.io.FileType
import groovy.sql.Sql
import groovy.transform.SourceURI
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.dataflow.DataflowQueue

import org.apache.ctakes.typesystem.type.structured.DocumentID

// INFO: UIMA Version 2.10.2 UIMA-AS Version 2.10.3
import org.apache.uima.cas.*
import org.apache.uima.jcas.JCas
import org.apache.uima.util.XmlCasSerializer
import org.apache.uima.adapter.jms.client.BaseUIMAAsynchronousEngine_impl
import org.apache.uima.aae.client.UimaAsynchronousEngine
import org.apache.uima.aae.client.UimaAsBaseCallbackListener
import org.apache.uima.collection.EntityProcessStatus
import org.apache.uima.aae.client.UimaASProcessStatus
import org.apache.uima.examples.SourceDocumentInformation
import org.apache.uima.fit.util.CasUtil

import org.apache.commons.dbcp2.BasicDataSource

def env = System.getenv();
def group = new DefaultPGroup(8);

@SourceURI
URI sourceUri;
def scriptDir = new File(sourceUri).parent;

def batchBegin = (env["BATCH_BEGIN"] ?: 0) as Integer;
def batchEnd = (env["BATCH_END"] ?: 9999) as Integer;
def batchOffset = env["BATCH_OFFSET"] ?:  new Random().nextInt(batchEnd - batchBegin);

def dataSource = new BasicDataSource();
dataSource.setPoolPreparedStatements(true);
dataSource.setMaxTotal(5);
dataSource.setUrl(env["DATASOURCE_URI"]);
dataSource.setUsername(env["DATASOURCE_USERNAME"]);
dataSource.setPassword(env["DATASOURCE_PASSWORD"]);
def inputStatement = new File("$scriptDir/clamp_sql/input.sql").text;
def artifactStatement = new File("$scriptDir/clamp_sql/artifact.sql").text;

DataflowQueue outputQueue = new DataflowQueue();

def getUimaPipelineClient(uri, endpoint, callback, poolsize) {
    def pipeline = new BaseUIMAAsynchronousEngine_impl()
    if(callback != null) pipeline.addStatusCallbackListener(callback)
    def context = [(UimaAsynchronousEngine.ServerUri): uri,
		   (UimaAsynchronousEngine.ENDPOINT): endpoint,
		   (UimaAsynchronousEngine.CasPoolSize): poolsize]
    pipeline.initialize(context)
    return pipeline
}

final def clampPipeline = getUimaPipelineClient(
    env["BROKER_URI"],
    "mySimpleQueueName",
    new ClampCallbackListener(outputQueue),
    8
);

final def clampArtificer = group.reactor {
    def cas = clampPipeline.getCAS()
    def jcas = cas.getJCas()
    def doc_id = new DocumentID(jcas)
    
    def source_note_id = it.note_id
    doc_id.setDocumentID(source_note_id.toString())
    doc_id.addToIndexes()
    
    def note = it.rtf2plain ?: ""
    jcas.setDocumentText(note)

    reply jcas.getCas()
}

final def clampDatabaseWrite = group.reactor { data ->
  def sql = Sql.newInstance(dataSource);
  if(data.clamp == 'P'){
    sql.withTransaction{
      sql.withBatch(100, artifactStatement){ ps ->
    	for(i in data.items){
    	  ps.addBatch(i)
    	}
      }
      sql.executeUpdate "UPDATE dbo.u01 SET clamp=$data.clamp WHERE note_id=$data.note_id"
    }
    reply "SUCCESS: $data.note_id"
  } else {
    data.error = data.error?.toString().take(3999)
    sql.executeUpdate "UPDATE u01_tmp SET clamp=$data.clamp, error=$data.error WHERE note_id=$data.note_id"
    reply "ERROR:   $data.note_id"
  }
  sql.close()
};

class ClampCallbackListener extends UimaAsBaseCallbackListener {
  DataflowQueue output;
  JsonSlurper slurper;

  ClampCallbackListener(DataflowQueue output){
    this.output = output;
    this.slurper = new JsonSlurper(type: JsonParserType.INDEX_OVERLAY);
  }

  @Override
  void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
    Type type = aCas.getTypeSystem().getType("org.apache.ctakes.typesystem.type.structured.DocumentID");
    Feature documentId = type.getFeatureByBaseName("documentID");
    
    Integer source_note_id = aCas.getView("_InitialView")
    .getIndexRepository()
    .getAllIndexedFS(type)
    .next()
    .getStringValue(documentId) as Integer;
    Type termType = aCas.getTypeSystem().getType("edu.uth.clamp.nlp.typesystem.ClampNameEntityUIMA");
    Feature cui = termType.getFeatureByBaseName("cui");
    Feature attribute = termType.getFeatureByBaseName("attribute");
    Feature semanticTag = termType.getFeatureByBaseName("semanticTag");
    Feature assertion = termType.getFeatureByBaseName("assertion");
    def items = CasUtil.select(aCas, termType)
    
    def filtered_items = items.collect{
      def item = it.getStringValue(cui);
      def attributes = [:];
      attributes.putAll(this.slurper.parseText(it.getStringValue(attribute) ? it.getStringValue(attribute) : "{}"));
      attributes.semanticTag = it.getStringValue(semanticTag)
      attributes.assertion = it.getStringValue(assertion)
      [
      item_type:'C',
      item:item,
      note_id:source_note_id,
      engine_id:3,
      begin_span:it.getBegin(),
      end_span:it.getEnd(),
      negated: (it.getStringValue(assertion) == "present") ? 'F' : 'T',
      text:it.getCoveredText(),
      attributes:JsonOutput.toJson(attributes)
      ]
    }.findAll{ it.item != null && it.item != "" };
	
	
    if(!aStatus.isException()){
      def process_results = [note_id:source_note_id, clamp:'P', items:filtered_items]
      this.output << process_results
    } else {
      ByteArrayOutputStream errors = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(errors);
      for(e in aStatus.getExceptions()){ e.printStackTrace(ps); }
      def process_results = [note_id:source_note_id, clamp:'E', error:errors]
      this.output << process_results
    }	
  }
}

outputQueue.wheneverBound {
  group.actor{
    clampDatabaseWrite.send outputQueue.val
    react {
      println "$it"
    }
  }
}


while(true){
  def templater = new groovy.text.SimpleTemplateEngine();
  def inputTemplate = templater.createTemplate(inputStatement);
  
  for(batch in (batchBegin + batchOffset)..batchEnd){
    def in_db = Sql.newInstance(dataSource);
    in_db.withStatement { stmt ->
      stmt.setFetchSize(20)
    }
    
    in_db.eachRow(inputTemplate.make(["batch":batch]).toString()){ row ->
	clampPipeline.sendCAS(clampArtificer.sendAndWait(row));
    }
    
    in_db.close();
  }
  batchOffset = 0;
}
