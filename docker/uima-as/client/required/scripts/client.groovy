import java.util.concurrent.TimeUnit

import groovy.io.FileType
import groovy.sql.Sql
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.SyncDataflowQueue

// INFO: UIMA Version 2.10.2 UIMA-AS Version 2.10.3
import org.apache.uima.cas.*
import org.apache.uima.adapter.jms.client.BaseUIMAAsynchronousEngine_impl
import org.apache.uima.aae.client.UimaAsynchronousEngine
import org.apache.uima.aae.client.UimaAsBaseCallbackListener
import org.apache.uima.collection.EntityProcessStatus

import edu.umn.biomedicus.uima.adapter.UimaAdapters

def env = System.getenv()
def group = new DefaultPGroup(8)
final def buffer = new SyncDataflowQueue()
final def output = new DataflowQueue()
final def b9 = new DataflowQueue()

def fetchRecords(uri, batch) {
    def sql = Sql.newInstance(uri)
    sql.withStatement { stmt ->
	stmt.setFetchSize(25)
    }
    def rows = sql.rows("select * from txts", *batch)
    sql.close()

    return rows
}

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
    new BiomedicusCallbackListener(output),
    32
)
final def biomedicusArtificer = group.reactor {LinkedHashMap it ->
    def cas = biomedicusPipeline.getCAS()
    def note = it.contents // for toy sqlite schema
    def doc_id = it.doc_id // for toy sqlite schema
    def to_process = cas.createView("Analysis")
    to_process.setDocumentText(note)
    UimaAdapters.createArtifact(cas, null, doc_id)
    reply cas
}

final def rtfPipeline = getUimaPipelineClient(
    env["NLPADAPT_BROKER_URI"],
    "nlpadapt.rtf.outbound",
    new RtfCallbackListener(buffer),
    8
)

final def rtfArtificer = group.reactor {
    def cas = rtfPipeline.getCAS()
    def note = it.contents // for toy sqlite schema
    def doc_id = it.doc_id // for toy sqlite schema
    def to_process = cas.createView("OriginalDocument")
    to_process.setDocumentText(note)
    UimaAdapters.createArtifact(cas, null, doc_id)
    reply cas
}


class RtfCallbackListener extends UimaAsBaseCallbackListener {
    SyncDataflowQueue buffer

    RtfCallbackListener(SyncDataflowQueue buffer){
	this.buffer = buffer
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
	String documentText = aCas.getView("Analysis").getDocumentText()

	def row = ["doc_id": filename, "contents": documentText]

	this.buffer << row
    }
}

class BiomedicusCallbackListener extends UimaAsBaseCallbackListener {
    DataflowQueue output

    BiomedicusCallbackListener(DataflowQueue output){
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

	this.output << filename
    }
}

def multiplier = group.splitter(buffer, [b9])

b9.wheneverBound{
    def bit = it
    group.actor {
	biomedicusArtificer.send bit
	react {
	    biomedicusPipeline.sendCAS(it)
	}
    }
}

output.wheneverBound{
    println "processed ${it}"
}

for(i in fetchRecords(env["NLPADAPT_DATASOURCE_URI"], [0,10000])){
    rtfPipeline.sendCAS(rtfArtificer.sendAndWait(i))
}

// multiplier.terminate()
// group.shutdown()
