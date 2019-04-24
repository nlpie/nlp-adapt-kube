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
import org.apache.uima.examples.SourceDocumentInformation;

import org.apache.commons.dbcp2.BasicDataSource

import edu.umn.biomedicus.uima.adapter.UimaAdapters

def env = System.getenv()
def group = new DefaultPGroup(8)
def inDataSource = new BasicDataSource()
inDataSource.setUrl(env["NLPADAPT_INPUT_DATASOURCE_URI"])
def outDataSource = new BasicDataSource()
outDataSource.setUrl(env["NLPADAPT_OUTPUT_DATASOURCE_URI"])

final def buffer = new SyncDataflowQueue()
final def output = new DataflowQueue()
final def b9 = new DataflowQueue()
final def mm = new DataflowQueue()

// try with resources
def fetchRecords(datasource, batch) {
    def sql = Sql.newInstance(datasource)
    sql.withStatement { stmt ->
	stmt.setFetchSize(25)
    }
    def rows = sql.rows("select * from txts", *batch) // with each does not prefetch all results
    sql.close()

    return rows
}

def writeRecords(datasource, records) {
    def sql = Sql.newInstance(datasource)
    sql.withStatement { stmt ->
	stmt.setFetchSize(25)
    }
    // write out something

    sql.close()
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
    16
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

final def metamapPipeline = getUimaPipelineClient(
    env["NLPADAPT_BROKER_URI"],
    "nlpadapt.metamap.outbound",
    new MetamapCallbackListener(output),
    32
)
final def metamapArtificer = group.reactor {LinkedHashMap it ->
    def cas = metamapPipeline.getCAS()
    def note = it.contents // for toy sqlite schema
    def doc_id = it.doc_id // for toy sqlite schema
    def to_process = cas.getView("_InitialView")
    to_process.setDocumentText(note)

    SourceDocumentInformation doc_info = new SourceDocumentInformation(to_process.getJCas());
    doc_info.setUri(doc_id);
    doc_info.setOffsetInSource(0);
    doc_info.setDocumentSize((int) note.length());
    doc_info.setLastSegment(true);
    doc_info.addToIndexes();

    reply cas
}

final def rtfPipeline = getUimaPipelineClient(
    env["NLPADAPT_BROKER_URI"],
    "nlpadapt.rtf.outbound",
    new RtfCallbackListener(buffer),
    4
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

	this.output << "BioMedICUS ${filename}"
    }
}

class MetamapCallbackListener extends UimaAsBaseCallbackListener {
    DataflowQueue output

    MetamapCallbackListener(DataflowQueue output){
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

	this.output << "MetaMap ${filename}"
    }
}

def multiplier = group.splitter(buffer, [b9, mm])

b9.wheneverBound{
    def bit = it
    group.actor {
	biomedicusArtificer.send bit
	react {
	    biomedicusPipeline.sendCAS(it)
	}
    }
}

mm.wheneverBound{
    def bit = it
    group.actor {
	metamapArtificer.send bit
	react {
	    metamapPipeline.sendCAS(it)
	}
    }
}

output.wheneverBound{
    println "processed ${it}"
}

for (int j in 1..10){
  for(i in fetchRecords(inDataSource, [0,10000])){
    rtfPipeline.sendCAS(rtfArtificer.sendAndWait(i))
  }
  println "***************** BATCH ${j} *****************"
}

// multiplier.terminate()
// group.shutdown()
