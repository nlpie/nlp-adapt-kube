import java.util.concurrent.TimeUnit
import java.io.ByteArrayOutputStream

import groovy.io.FileType
import groovy.sql.Sql
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.SyncDataflowQueue

// INFO: UIMA Version 2.10.2 UIMA-AS Version 2.10.3
import org.apache.uima.cas.*
import org.apache.uima.jcas.cas.TOP
import org.apache.uima.util.XmlCasSerializer
import org.apache.uima.adapter.jms.client.BaseUIMAAsynchronousEngine_impl
import org.apache.uima.aae.client.UimaAsynchronousEngine
import org.apache.uima.aae.client.UimaAsBaseCallbackListener
import org.apache.uima.collection.EntityProcessStatus
import org.apache.uima.aae.client.UimaASProcessStatus
import org.apache.uima.examples.SourceDocumentInformation

import org.apache.commons.dbcp2.BasicDataSource

import edu.umn.biomedicus.uima.adapter.UimaAdapters
import edu.umn.biomedicus.uima.adapter.CASArtifact
import edu.umn.nlpengine.Artifact
import edu.umn.biomedicus.types.*

def env = System.getenv();
def group = new DefaultPGroup(8);
def dataSource = new BasicDataSource()
dataSource.setPoolPreparedStatements(true)
dataSource.setMaxTotal(5)
dataSource.setUrl(env["NLPADAPT_DATASOURCE_URI"])
dataSource.setUsername(env["NLPADAPT_DATASOURCE_USERNAME"])
dataSource.setPassword(env["NLPADAPT_DATASOURCE_PASSWORD"])

def outputQueue = new DataflowQueue();

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
    env["NLPADAPT_BROKER_URI"],
    "nlpadapt.biomedicus.outbound",
    new BiomedicusCallbackListener(outputQueue),
    16
);

final def biomedicusArtificer = group.reactor {
    def cas = biomedicusPipeline.getCAS()
    def note = it.rtf2plain?.characterStream?.text
    def source_note_id = it.note_id // for toy sqlite schema
    def to_process = cas.createView("OriginalDocument")
    to_process.setDocumentText(note)
    UimaAdapters.createArtifact(cas, null, source_note_id.toString())
    reply cas
}

final def biomedicusDatabaseWrite = group.reactor { data ->
  def sql = Sql.newInstance(dataSource);
  if(data.b9 == 'P'){
    data.xmi = data.xmi?.toString()
    // sql.withTransaction{
    //   sql.withBatch(100, "INSERT INTO nlp_sandbox.u01_detected_item(item_type, item, note_id, engine_id, begin, end, negated, text) VALUES (:item_type, :item, :note_id, :engine_id, :begin, :end, :negated, :text)"){ ps ->
    // 	for(i in data.items){
    // 	  ps.addBatch(i)
    // 	}
    //   }
    // }    
    sql.executeUpdate "UPDATE u01_tmp SET b9=$data.b9, xmi=$data.xmi WHERE note_id=$data.note_id"
    reply "SUCCESS: $data.note_id"
  } else {
    sql.executeUpdate "UPDATE u01_tmp SET b9=$data.b9, error=$data.error WHERE note_id=$data.note_id"
    reply "ERROR:   $data.note_id"
  }
  sql.close()
};

class BiomedicusCallbackListener extends UimaAsBaseCallbackListener {
  DataflowQueue output;

  BiomedicusCallbackListener(DataflowQueue output){
	this.output = output
    }

    @Override
    void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {

	Type type = aCas.getTypeSystem().getType("ArtifactID");
	Feature documentId = type.getFeatureByBaseName("artifactID");
		
	Integer source_note_id = aCas.getView("metadata")
	.getIndexRepository()
	.getAllIndexedFS(type)
	.next()
	.getStringValue(documentId) as Integer;
	
	if(!aStatus.isException()){
	  ByteArrayOutputStream xmi = new ByteArrayOutputStream();
	  XmlCasSerializer.serialize(aCas, xmi)
	  def process_results = [note_id:source_note_id, b9:'P', xmi:xmi]
	  this.output << process_results
	} else {
	  def process_results = [note_id:source_note_id, b9:'E', error:aStatus.getStatusMessage().take(3999)]
	  this.output << process_results
	}
	
    }
}

outputQueue.wheneverBound {
  group.actor{
    biomedicusDatabaseWrite.send outputQueue.val
    react {
      println "$it"
    }
  }
}


while(true){
  def in_db = Sql.newInstance(dataSource);
  in_db.withStatement { stmt ->
    stmt.setFetchSize(50)
  }
  
  in_db.eachRow("SELECT note_id, rtf2plain FROM nlp_sandbox.u01_tmp WHERE rtf2plain IS NOT NULL AND b9 IN ('U', 'R') AND rownum<=1000"){ row ->
    biomedicusPipeline.sendCAS(biomedicusArtificer.sendAndWait(row));
  }
  
  in_db.close()
}
