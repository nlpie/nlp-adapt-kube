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
def dataSource = new BasicDataSource();
def time = new Date();
dataSource.setPoolPreparedStatements(true);
dataSource.setMaxTotal(5);
dataSource.setUrl(env["NLPADAPT_DATASOURCE_URI"]);
dataSource.setUsername(env["NLPADAPT_DATASOURCE_USERNAME"]);
dataSource.setPassword(env["NLPADAPT_DATASOURCE_PASSWORD"]);

def outputQueue = new DataflowQueue();

/* compile patterns as globals */
def patterns = [
  [ pat:Pattern.compile(/(\d+\/\d+)-/), mat: '$1'],
  [ pat:Pattern.compile(/(\s+|^|\\n)\.(\D+)/), mat: '$1$2'],
  [ pat:Pattern.compile(/^\cM/), mat: ""],
  [ pat:Pattern.compile(/\p{Cntrl}&&[^\cJ\cM\cI]/), mat: ""],
  [ pat:Pattern.compile(/\P{ASCII}/), mat: ""],
  [ pat:Pattern.compile(/(\s+)\.+(\s*)/), mat: '$1$2'],
  [ pat:Pattern.compile(/^\.$/, Pattern.MULTILINE), mat: ""],
  [ pat:Pattern.compile(/\|/), mat: " "]
]

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
    env["NLPADAPT_BROKER_URI"],
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
      sql.withBatch("UPDATE nlp_sandbox.u01_tmp SET rtf2plain=:rtf2plain, rtf_pipeline=:rtf_pipeline, edited=:edited, error=:error, unedited=:unedited WHERE note_id=:note_id"){ stmt ->
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
    if(outputQueue.length() && TimeCategory.minus(new Date(), time).toMilliseconds() > 5000){
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
  def in_db = Sql.newInstance(dataSource);
  in_db.withStatement { stmt ->
    stmt.setFetchSize(50)
  }
  in_db.eachRow("SELECT rh.content, u.* FROM nlp_sandbox.u01_tmp u INNER JOIN LZ_FV_HL7.hl7_note_hist_reduced_final r on r.note_id=u.note_id INNER JOIN NOTES.rtf_historical rh ON rh.hl7_note_historical_id=r.hl7_note_id WHERE u.rtf_pipeline IN ('U', 'R') AND rownum<=1000"){ row ->
    rtfPipeline.sendCAS(rtfArtificer.sendAndWait(row));
  }
  
  in_db.close();
}

// multiplier.terminate()
// group.shutdown()
