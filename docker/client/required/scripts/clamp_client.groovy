import java.util.concurrent.TimeUnit
import java.io.ByteArrayOutputStream

import groovy.io.FileType
import groovy.sql.Sql
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.SyncDataflowQueue

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

import org.apache.commons.dbcp2.BasicDataSource

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

final def clampPipeline = getUimaPipelineClient(
    env["NLPADAPT_BROKER_URI"],
    "mySimpleQueueName",
    new ClampCallbackListener(outputQueue),
    16
);

final def clampArtificer = group.reactor {
    def cas = clampPipeline.getCAS()
    def jcas = cas.getJCas()
    def doc_id = new DocumentID(jcas)
    
    def source_note_id = it.note_id
    doc_id.setDocumentID(source_note_id.toString())
    doc_id.addToIndexes()
    
    def note = it.rtf2plain?.characterStream?.text
    jcas.setDocumentText(note)

    reply jcas.getCas()
}

final def clampDatabaseWrite = group.reactor { data ->
  def sql = Sql.newInstance(dataSource);
  if(data.clamp == 'P'){
    data.xmi = data.xmi?.toString()
    // sql.withTransaction{
    //   sql.withBatch(100, "INSERT INTO nlp_sandbox.u01_detected_item(item_type, item, note_id, engine_id, begin, end, negated, text) VALUES (:item_type, :item, :note_id, :engine_id, :begin, :end, :negated, :text)"){ ps ->
    // 	for(i in data.items){
    // 	  ps.addBatch(i)
    // 	}
    //   }
    // }    
    sql.executeUpdate "UPDATE u01_tmp SET clamp=$data.clamp, xmi=$data.xmi WHERE note_id=$data.note_id"
    reply "SUCCESS: $data.note_id"
  } else {
    sql.executeUpdate "UPDATE u01_tmp SET clamp=$data.clamp, error=$data.error WHERE note_id=$data.note_id"
    reply "ERROR:   $data.note_id"
  }
  sql.close()
};

class ClampCallbackListener extends UimaAsBaseCallbackListener {
  DataflowQueue output;

  ClampCallbackListener(DataflowQueue output){
	this.output = output
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
	
	if(!aStatus.isException()){
	  ByteArrayOutputStream xmi = new ByteArrayOutputStream();
	  XmlCasSerializer.serialize(aCas, xmi)
	  def process_results = [note_id:source_note_id, clamp:'P', xmi:xmi]
	  this.output << process_results
	} else {
	  def process_results = [note_id:source_note_id, clamp:'E', error:aStatus.getStatusMessage().take(3999)]
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
  def in_db = Sql.newInstance(dataSource);
  in_db.withStatement { stmt ->
    stmt.setFetchSize(50)
  }
  
  in_db.eachRow("SELECT note_id, rtf2plain FROM nlp_sandbox.u01_tmp WHERE rtf2plain IS NOT NULL AND clamp IN ('U', 'R') AND rownum<=1000"){ row ->
    clampPipeline.sendCAS(clampArtificer.sendAndWait(row));
  }
  
  in_db.close()
}
