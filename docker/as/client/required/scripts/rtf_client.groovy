import java.util.concurrent.TimeUnit
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.text.Normalizer
import java.util.regex.*

import groovy.io.FileType
import groovy.sql.Sql
import groovy.time.*
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.dataflow.DataflowQueue

// INFO: UIMA Version 2.10.2 UIMA-AS Version 2.10.3
import org.apache.uima.cas.*
import org.apache.uima.adapter.jms.client.BaseUIMAAsynchronousEngine_impl
import org.apache.uima.aae.client.UimaAsynchronousEngine
import org.apache.uima.aae.client.UimaAsBaseCallbackListener
import org.apache.uima.collection.EntityProcessStatus
import org.apache.uima.examples.SourceDocumentInformation;

import org.apache.commons.dbcp2.BasicDataSource

import edu.umn.biomedicus.uima.adapter.UimaAdapters


def env = System.getenv();
def group = new DefaultPGroup(4);
def time = new Date();

def batchBegin = env["BATCH_BEGIN"] ?: 0;
def batchEnd = env["BATCH_END"] ?: 9999;

def rtfDataSource = new BasicDataSource();
rtfDataSource.setPoolPreparedStatements(true);
rtfDataSource.setMaxTotal(5);
rtfDataSource.setUrl(env["RTF_DATASOURCE_URI"]);
rtfDataSource.setUsername(env["RTF_DATASOURCE_USERNAME"]);
rtfDataSource.setPassword(env["RTF_DATASOURCE_PASSWORD"]);
def rtfStatement = new File("rtf_sql/rtf_text.sql").text;

def dataSource = new BasicDataSource();
dataSource.setPoolPreparedStatements(true);
dataSource.setMaxTotal(5);
dataSource.setUrl(env["DATASOURCE_URI"]);
dataSource.setUsername(env["DATASOURCE_USERNAME"]);
dataSource.setPassword(env["DATASOURCE_PASSWORD"]);
def inputStatement = new File("rtf_sql/input.sql").text;
def outputStatement = new File("rtf_sql/output.sql").text;

def outputQueue = new DataflowQueue();

/* compile patterns as globals */
def patterns = [
  [ pat:~/(\s+|^|\\n)(\.)(\D+)/, mat: {global, x, y, z -> "$x" + " ".multiply(y.size()) + "$z" }],
  [ pat:~/^\cM/, mat: ""],
  [ pat:~/\p{Cntrl}&&[^\cJ\cM\cI]/, mat: ""],
  [ pat:~/\P{ASCII}/, mat: {x -> " ".multiply(x.size())}],
  [ pat:~/(\s+)(\.+)((?!\d)\s*)/, mat: {global, x, y, z -> "$x" + " ".multiply(y.size()) + "$z" }],
  [ pat:Pattern.compile(/^\.$/, Pattern.MULTILINE), mat: {global -> " ".multiply(global.size())}],
  [ pat:~/\|/, mat: {global -> " ".multiply(global.size())}]
];

/*******************************/

def getUimaPipelineClient(uri, endpoint, callback, poolsize) {
    def pipeline = new BaseUIMAAsynchronousEngine_impl()
    if(callback != null) pipeline.addStatusCallbackListener(callback)
    def context = [(UimaAsynchronousEngine.ServerUri): uri,
		   (UimaAsynchronousEngine.ENDPOINT): endpoint,
		   (UimaAsynchronousEngine.CasPoolSize): poolsize]
    pipeline.initialize(context)
    return pipeline
}
  
final def rtfPipeline = getUimaPipelineClient(
    env["BROKER_URI"],
    "nlpadapt.rtf.outbound",
    new RtfCallbackListener(outputQueue),
    8
);

final def rtfArtificer = group.reactor {
    def cas = rtfPipeline.getCAS()
    def note = it.content?.characterStream?.text
    def doc_id = it.note_id
    def to_process = cas.createView("OriginalDocument")
    to_process.setDocumentText(note)
    UimaAdapters.createArtifact(cas, null, doc_id.toString())
    reply cas
}

def rtfDatabaseWrite = group.reactor { output ->
    def sql = Sql.newInstance(dataSource);
    sql.withTransaction {
      sql.withBatch(outputStatement){ stmt ->
	for( data in output ){
	  if(data.rtf_pipeline == 'P'){
	    def norm = Normalizer.normalize(data.rtf2plain, Normalizer.Form.NFD);

	    for ( repl in patterns ) {
	      while(norm =~ repl.pat){
		norm = norm.replaceAll(repl.pat, repl.mat);
	      }
	    }
	    
	    if(data.rtf2plain == norm){
	      data.edited = 'N'
	    } else {
	      data.edited = 'Y'
	      data.unedited = data.rtf2plain
	      data.rtf2plain = norm
	    }
	    stmt.addBatch(data)
	    println "SUCCESS: $data.note_id"
	  } else {
	    data.error = data.error?.toString().take(3999)
	    stmt.addBatch(data);
	    println "ERROR:   $data.note_id"
	  }
	}
      }
    }
    sql.close();
    reply "${new Date()}: PROCESSED BATCH"
}

class RtfCallbackListener extends UimaAsBaseCallbackListener {
  DataflowQueue output;

    RtfCallbackListener(DataflowQueue output){
	this.output = output
    }
  
    @Override
    void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
      Type type = aCas.getTypeSystem().getType("ArtifactID");
	Feature documentId = type.getFeatureByBaseName("artifactID");
	String filename = aCas.getView("metadata")
            .getIndexRepository()
            .getAllIndexedFS(type)
            .next()
	    .getStringValue(documentId)
	
      if(!aStatus.isException()){
	String documentText = aCas.getView("Analysis").getDocumentText()
	def row = [note_id:filename, rtf2plain:documentText, rtf_pipeline:'P', error:null, unedited:null, edited:'N']
	
	this.output << row
      } else {
	ByteArrayOutputStream errors = new ByteArrayOutputStream();
	PrintStream ps = new PrintStream(errors);
	for(e in aStatus.getExceptions()){ e.printStackTrace(ps); }
	def row = [note_id:filename, rtf_pipeline:'E', error:errors, rtf2plain:null, unedited:null, edited:'N']
	this.output << row
      }
    }
}

group.task {
  while(true){
    if(outputQueue.length() && TimeCategory.minus(new Date(), time).toMilliseconds() > 1000){
      time = new Date();
      
      def output = [];
      while(outputQueue.length()) output << outputQueue.val
      group.actor{
	rtfDatabaseWrite.send output
	react {
	  println "$it"
	}
      }
    }
    Thread.sleep 10
  }
}

while(true){
  def templater = new groovy.text.SimpleTemplateEngine();
  def inputTemplate = templater.createTemplate(inputStatement);
  def rtfTemplate = templater.createTemplate(rtfStatement);
  
  for(batch in batchBegin..batchEnd){
    def rtf_db = Sql.newInstance(rtfDataSource);
    def db = Sql.newInstance(dataSource);

    def note_ids = db.rows(inputTemplate.make(["batch":batch]).toString())*.note_id*.toString();
    rtf_db.eachRow(rtfTemplate.make(["note_ids": note_ids]).toString()){ row ->
      rtfPipeline.sendCAS(rtfArtificer.sendAndWait(row));
    }
    
    db.close();
    rtf_db.close();
  }
}

// multiplier.terminate()
// group.shutdown()
