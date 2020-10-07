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
import org.apache.uima.jcas.JCas
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
def inputStatement = new File("$scriptDir/ctakes_sql/input.sql").text;
def artifactStatement = new File("$scriptDir/ctakes_sql/artifact.sql").text;

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

final def ctakesPipeline = getUimaPipelineClient(
    env["BROKER_URI"],
    "nlpadapt.ctakes.outbound",
    new CtakesCallbackListener(outputQueue),
    8
);

final def ctakesArtificer = group.reactor {
    def cas = ctakesPipeline.getCAS()
    def jcas = cas.getJCas()
    def doc_id = new DocumentID(jcas)
    
    def source_note_id = it.note_id
    doc_id.setDocumentID(source_note_id.toString())
    doc_id.addToIndexes()
    
    def note = it.rtf2plain?.characterStream?.text
    jcas.setDocumentText(note)
    def adapt = jcas.createView("NlpAdapt")

    reply jcas.getCas()
}

class CtakesCallbackListener extends UimaAsBaseCallbackListener {
  SyncDataflowQueue output;

  CtakesCallbackListener(SyncDataflowQueue output){
	this.output = output
    }
  
  @Override
  void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
    
    def items = [];

    def filtered_items = items.findAll{ it != null && it.item != null && it.item != ""};
    
    if(!aStatus.isException()){
      def process_results = [note_id:source_note_id, ctakes:'P', items:filtered_items]
      this.output << process_results
    } else {
      ByteArrayOutputStream errors = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(errors);
      for(e in aStatus.getExceptions()){ e.printStackTrace(ps); }
      def process_results = [note_id:source_note_id, ctakes:'E', error:errors]
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
	ctakesPipeline.sendCAS(ctakesArtificer.sendAndWait(row));
    }
    
    in_db.close();
  }
  batchOffset = 0;
}
