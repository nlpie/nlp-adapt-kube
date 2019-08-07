import java.util.concurrent.TimeUnit
import java.io.ByteArrayOutputStream
import java.io.PrintStream

import groovy.io.FileType
import groovy.sql.Sql
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.SyncDataflowQueue

// INFO: UIMA Version 2.10.2 UIMA-AS Version 2.10.3
import org.apache.uima.cas.*
import org.apache.uima.util.XmlCasSerializer
import org.apache.uima.jcas.tcas.Annotation
import org.apache.uima.adapter.jms.client.BaseUIMAAsynchronousEngine_impl
import org.apache.uima.aae.client.UimaAsynchronousEngine
import org.apache.uima.aae.client.UimaAsBaseCallbackListener
import org.apache.uima.collection.EntityProcessStatus
import org.apache.uima.examples.SourceDocumentInformation
import org.apache.uima.fit.util.CasUtil

import org.apache.commons.dbcp2.BasicDataSource

import edu.umn.biomedicus.uima.adapter.UimaAdapters

def env = System.getenv()
def group = new DefaultPGroup(8)
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

final def metamapPipeline = getUimaPipelineClient(
    env["NLPADAPT_BROKER_URI"],
    "nlpadapt.metamap.outbound",
    new MetamapCallbackListener(outputQueue),
    16
);

final def metamapArtificer = group.reactor {
    def cas = metamapPipeline.getCAS()
    def note = """



.National Institutes of Health Super Scale
Interval: Baseline done at 3:33pm



"""
    
    def source_note_id = "THIS IS A TEST"
    def to_process = cas.getView("_InitialView")
    to_process.setDocumentText(note)

    SourceDocumentInformation doc_info = new SourceDocumentInformation(to_process.getJCas());
    doc_info.setUri(source_note_id);
    doc_info.setOffsetInSource(0);
    doc_info.setDocumentSize((int) note.length());
    doc_info.setLastSegment(true);
    doc_info.addToIndexes();

    reply cas
};


class MetamapCallbackListener extends UimaAsBaseCallbackListener {
    DataflowQueue output

    MetamapCallbackListener(DataflowQueue output){
	this.output = output
    }

    @Override
    void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
      Type type = aCas.getTypeSystem().getType("org.apache.uima.examples.SourceDocumentInformation");
      Feature documentId = type.getFeatureByBaseName("uri");
      String source_note_id = aCas.getView("_InitialView")
      .getIndexRepository()
      .getAllIndexedFS(type)
      .next()
      .getStringValue(documentId);

      this.output << source_note_id
    }
}

outputQueue.wheneverBound {
  println it
}


while(true){
  metamapPipeline.sendCAS(metamapArtificer.sendAndWait(""));
  sleep 1000
}

