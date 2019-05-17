import java.util.concurrent.TimeUnit

import groovy.io.FileType
import groovy.sql.Sql
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
def group = new DefaultPGroup(8);
def dataSource = new BasicDataSource();
dataSource.setUrl(env["NLPADAPT_DATASOURCE_URI"]);
dataSource.setUsername(env["NLPADAPT_DATASOURCE_USERNAME"])
dataSource.setPassword(env["NLPADAPT_DATASOURCE_PASSWORD"])

def outputQueue = new DataflowQueue()

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
    4
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

final def rtfDatabaseWrite = group.reactor {
  def sql = Sql.newInstance(dataSource);
  if(it.rtf_pipeline == 'P'){
    // Do QUARANTINE here
    sql.executeUpdate "UPDATE u01_tmp SET rtf2plain=$it.rtf2plain, rtf_pipeline=$it.rtf_pipeline WHERE note_id=$it.note_id"
    reply "SUCCESS: $it.note_id"
  } else {
    sql.executeUpdate "UPDATE u01_tmp SET rtf_pipeline=$it.rtf_pipeline, error=$it.error WHERE note_id=$it.note_id"
    reply "ERROR:   $it.note_id"
  }
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
	def row = [note_id:filename, rtf2plain:documentText, rtf_pipeline:'P']
	
	this.output << row
      } else {
	def row = [note_id:filename, rtf_pipeline:'E' ]
	this.output << row
      }
    }
}

outputQueue.wheneverBound {
  group.actor{
    rtfDatabaseWrite.send outputQueue.val
    react {
      println "$it"
    }
  }
}

def in_db = Sql.newInstance(dataSource);
in_db.withStatement { stmt ->
  stmt.setFetchSize(25)
}

while(true){
  
  def query = "SELECT rh.content, u.* FROM u01_tmp u INNER JOIN LZ_FV_HL7.hl7_note_hist_reduced_all r on r.note_id=u.note_id INNER JOIN NOTES.rtf_historical rh ON rh.hl7_note_historical_id=r.hl7_note_id WHERE u.rtf_pipeline IN ('U', 'R') AND rownum<=1000"

  in_db.eachRow(query){ row ->
    rtfPipeline.sendCAS(rtfArtificer.sendAndWait(row));
  }
}

// multiplier.terminate()
// group.shutdown()
