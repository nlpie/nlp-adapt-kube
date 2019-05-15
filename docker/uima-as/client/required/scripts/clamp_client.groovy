import java.util.concurrent.TimeUnit

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
import org.apache.uima.examples.SourceDocumentInformation;

import org.apache.commons.dbcp2.BasicDataSource

import edu.umn.biomedicus.uima.adapter.UimaAdapters

def env = System.getenv()
def group = new DefaultPGroup(4)
def inDataSource = new BasicDataSource()
inDataSource.setUrl(env["NLPADAPT_INPUT_DATASOURCE_URI"])
def outDataSource = new BasicDataSource()
outDataSource.setUrl(env["NLPADAPT_OUTPUT_DATASOURCE_URI"])

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
    new ClampCallbackListener(outDataSource),
    16
);

final def clampArtificer = group.reactor {
    def cas = clampPipeline.getCAS()
    def jcas = cas.getJCas()
    def doc_id = new DocumentID(cas)
    
    def source_note_id = it.id // for toy sqlite schema
    doc_id.setDocumentID(source_note_id.toString())
    doc_id.addToIndexes()
    
    def note = it.rtf2plain
    jcas.setDocumentText(note)

    reply cas
}

class ClampCallbackListener extends UimaAsBaseCallbackListener {
  BasicDataSource output;

  ClampCallbackListener(BasicDataSource output){
	this.output = output
    }

    @Override
    void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
	XmlCasSerializer.serialize(aCas, System.out)
	println "\n***************************************************"
    }
}

def in_db = Sql.newInstance(inDataSource);
in_db.withStatement { stmt ->
  stmt.setFetchSize(25)
}

while(true){
  def query = "SELECT id, rtf2plain FROM source_note WHERE rtf2plain IS NOT NULL AND clamp='U' limit 1000";
  in_db.eachRow(query){ row ->
    clampPipeline.sendCAS(clampArtificer.sendAndWait(row));
  }
}
