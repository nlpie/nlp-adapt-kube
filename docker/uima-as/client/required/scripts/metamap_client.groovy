import java.util.concurrent.TimeUnit

import groovy.io.FileType
import groovy.sql.Sql
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.SyncDataflowQueue

// INFO: UIMA Version 2.10.2 UIMA-AS Version 2.10.3
import org.apache.uima.cas.*
import org.apache.uima.util.XmlCasSerializer
import org.apache.uima.adapter.jms.client.BaseUIMAAsynchronousEngine_impl
import org.apache.uima.aae.client.UimaAsynchronousEngine
import org.apache.uima.aae.client.UimaAsBaseCallbackListener
import org.apache.uima.collection.EntityProcessStatus
import org.apache.uima.examples.SourceDocumentInformation;

import org.apache.commons.dbcp2.BasicDataSource

import edu.umn.biomedicus.uima.adapter.UimaAdapters

def env = System.getenv()
def group = new DefaultPGroup(8)
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

final def metamapPipeline = getUimaPipelineClient(
    env["NLPADAPT_BROKER_URI"],
    "nlpadapt.metamap.outbound",
    new MetamapCallbackListener(outDataSource),
    16
);

final def metamapArtificer = group.reactor {
    def cas = metamapPipeline.getCAS()
    def note = it.rtf2plain
    def source_note_id = it.id
    def to_process = cas.getView("_InitialView")
    to_process.setDocumentText(note)

    SourceDocumentInformation doc_info = new SourceDocumentInformation(to_process.getJCas());
    doc_info.setUri(source_note_id.toString());
    doc_info.setOffsetInSource(0);
    doc_info.setDocumentSize((int) note.length());
    doc_info.setLastSegment(true);
    doc_info.addToIndexes();

    reply cas
}

class MetamapCallbackListener extends UimaAsBaseCallbackListener {
    BasicDataSource output

    MetamapCallbackListener(BasicDataSource output){
	this.output = output
    }

    @Override
    void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {

	Type type = aCas.getTypeSystem().getType("org.apache.uima.examples.SourceDocumentInformation");
	Feature documentId = type.getFeatureByBaseName("uri")
	String filename = aCas.getView("_InitialView")
	    .getIndexRepository()
	    .getAllIndexedFS(type)
	    .next()
	    .getStringValue(documentId)

	XmlCasSerializer.serialize(aCas, System.out)
	println "\n***************************************************"
    }
}

def in_db = Sql.newInstance(outDataSource);
in_db.withStatement { stmt ->
  stmt.setFetchSize(25)
}

while(true){
  def query = "SELECT id, rtf2plain FROM source_note WHERE rtf2plain IS NOT NULL AND mm='U' limit 1000";
  in_db.eachRow(query){ row ->
    metamapPipeline.sendCAS(metamapArtificer.sendAndWait(row));
  }
}

