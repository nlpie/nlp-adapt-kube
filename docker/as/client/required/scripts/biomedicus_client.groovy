import java.util.concurrent.TimeUnit
import java.io.ByteArrayOutputStream
import java.io.PrintStream

import groovy.json.JsonOutput
import groovy.io.FileType
import groovy.sql.Sql
import groovy.transform.SourceURI
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.SyncDataflowQueue

// INFO: UIMA Version 2.10.2 UIMA-AS Version 2.10.3
import org.apache.uima.cas.*
import org.apache.uima.jcas.cas.TOP
import org.apache.uima.util.XmlCasSerializer
import org.apache.uima.jcas.tcas.Annotation
import org.apache.uima.adapter.jms.client.BaseUIMAAsynchronousEngine_impl
import org.apache.uima.aae.client.UimaAsynchronousEngine
import org.apache.uima.aae.client.UimaAsBaseCallbackListener
import org.apache.uima.collection.EntityProcessStatus
import org.apache.uima.aae.client.UimaASProcessStatus
import org.apache.uima.examples.SourceDocumentInformation
import org.apache.uima.fit.util.CasUtil

import org.apache.commons.dbcp2.BasicDataSource

import edu.umn.biomedicus.uima.adapter.UimaAdapters
import edu.umn.biomedicus.uima.adapter.CASArtifact
import edu.umn.nlpengine.Artifact
import edu.umn.biomedicus.types.*

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
def inputStatement = new File("$scriptDir/biomedicus_sql/input.sql").text;
def artifactStatement = new File("$scriptDir/biomedicus_sql/artifact.sql").text;

SyncDataflowQueue outputQueue = new SyncDataflowQueue();


def getUimaPipelineClient(uri, endpoint, callback, poolsize) {
    def pipeline = new BaseUIMAAsynchronousEngine_impl()
    if(callback != null) pipeline.addStatusCallbackListener(callback)
    def context = [(UimaAsynchronousEngine.ServerUri): uri,
		   (UimaAsynchronousEngine.ENDPOINT): endpoint,
		   (UimaAsynchronousEngine.CasPoolSize): poolsize]
    pipeline.initialize(context)
    return pipeline
}

final def biomedicusPipeline = getUimaPipelineClient(
    env["BROKER_URI"],
    "nlpadapt.biomedicus.outbound",
    new BiomedicusCallbackListener(outputQueue),
    8
);

final def biomedicusArtificer = group.reactor {
    def cas = biomedicusPipeline.getCAS()
    def note = it.rtf2plain ?: ""
    def source_note_id = it.note_id
    def to_process = cas.createView("OriginalDocument")
    to_process.setDocumentText(note)
    UimaAdapters.createArtifact(cas, null, source_note_id.toString())
    reply cas
}

class BiomedicusCallbackListener extends UimaAsBaseCallbackListener {
  SyncDataflowQueue output;

  BiomedicusCallbackListener(SyncDataflowQueue output){
	this.output = output
    }

  List getNegations(CAS aCas) {
    Type negatedType = aCas.getTypeSystem().getType("biomedicus.v2.Negated");
    
    def negated = CasUtil.select(aCas, negatedType);
    def negated_items = negated.collect{ n ->
      [n.getBegin(), n.getEnd()]
    }

    return negated_items
  }

  List getHistorical(CAS aCas) {
    Type historicalType = aCas.getTypeSystem().getType("biomedicus.v2.Historical");
    
    def historical = CasUtil.select(aCas, historicalType);
    def historical_items = historical.collect{ h ->
      [h.getBegin(), h.getEnd()]
    }

    return historical_items
  }
  
  @Override
  void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {

    CAS analysis = aCas.getView("Analysis");
      
    Type type = aCas.getTypeSystem().getType("ArtifactID");
    Feature documentId = type.getFeatureByBaseName("artifactID");
      
    Integer source_note_id = aCas.getView("metadata")
    .getIndexRepository()
    .getAllIndexedFS(type)
    .next()
    .getStringValue(documentId) as Integer;
    Type termType = aCas.getTypeSystem().getType("biomedicus.v2.UmlsConcept");
    Feature cui = termType.getFeatureByBaseName("cui");
    Feature tui = termType.getFeatureByBaseName("tui");
    Feature source = termType.getFeatureByBaseName("source");
    Feature confidence = termType.getFeatureByBaseName("confidence");
    
    Type acronymType = aCas.getTypeSystem().getType("biomedicus.v2.Acronym");
    Feature expansion = acronymType.getFeatureByBaseName("text");
    
    def items = [];
    def unique = [];
    def negated = getNegations(analysis);
    def historical = getHistorical(analysis);
    def concepts = CasUtil.select(analysis, termType);
    items.addAll(
      concepts.collect{ c ->
	if(!([c.getBegin(), c.getEnd(), c.getStringValue(cui)] in unique)){
	  unique.add([c.getBegin(), c.getEnd(), c.getStringValue(cui)]);
	  
	  [
	  item_type:'C',
	  item:c.getStringValue(cui),
	  note_id:source_note_id,
	  engine_id:1,
	  begin_span:c.getBegin(),
	  end_span:c.getEnd(),
	  negated: [c.getBegin(), c.getEnd()] in negated ? 'T' : 'F',
	  historical: [c.getBegin(), c.getEnd()] in historical ? 'T' : 'F',
	  text: c.getCoveredText(),
	  semantic_type: c.getStringValue(tui)
	  ]
	}
      });
    def acronyms = CasUtil.select(analysis, acronymType);
    items.addAll(
      acronyms.collect{ a ->
    	[
    	item_type:'A',
    	item:a.getStringValue(expansion),
    	note_id:source_note_id,
    	engine_id:1,
    	begin_span:a.getBegin(),
    	end_span:a.getEnd(),
    	negated: null,
    	text: a.getCoveredText(),
	semantic_type: null
    	]
      });

    def filtered_items = items.findAll{ it != null && it.item != null && it.item != "" };
    
    if(!aStatus.isException()){
      def process_results = [note_id:source_note_id, b9:'P', items:filtered_items]
      this.output << process_results
    } else {
      ByteArrayOutputStream errors = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(errors);
      for(e in aStatus.getExceptions()){ e.printStackTrace(ps); }
      def process_results = [note_id:source_note_id, b9:'E', error:errors]
      this.output << process_results
    }  
  }
}

5.times{
  group.task {
    while(true){
      def data = outputQueue.val;
      def sql = Sql.newInstance(dataSource);
      if(data.b9 == 'P'){
	sql.withTransaction{
	  sql.withBatch(100, artifactStatement){ ps ->
	    for(i in data.items){
	      ps.addBatch(i)
	    }
	  }
	  sql.executeUpdate "UPDATE dbo.u01 SET b9=$data.b9 WHERE note_id=$data.note_id"
	}
	println "SUCCESS: $data.note_id"
      } else {
	data.error = data.error?.toString().take(3999)
	sql.executeUpdate "UPDATE dbo.u01 SET b9=$data.b9, error=$data.error WHERE note_id=$data.note_id"
	println "ERROR:   $data.note_id"
      }
      sql.close()
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
	biomedicusPipeline.sendCAS(biomedicusArtificer.sendAndWait(row));
    }
    
    in_db.close();
  }
  batchOffset = 0;
}
