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

import org.apache.commons.dbcp2.BasicDataSource

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

final def ctakesPipeline = getUimaPipelineClient(
    env["NLPADAPT_BROKER_URI"],
    "nlpadapt.ctakes.outbound",
    new CtakesCallbackListener(outDataSource),
    16
);

final def ctakesArtificer = group.reactor {
    def cas = ctakesPipeline.getCAS()
    def jcas = cas.getJCas()
    def doc_id = new DocumentID(jcas)
    
    def source_note_id = it.id // for toy sqlite schema
    doc_id.setDocumentID(source_note_id.toString())
    doc_id.addToIndexes()
    
    def note = it.rtf2plain
    jcas.setDocumentText(note)

    jcas.createView("NlpAdapt")
    reply cas
}

class CtakesCallbackListener extends UimaAsBaseCallbackListener {
  BasicDataSource output;

  CtakesCallbackListener(BasicDataSource output){
	this.output = output
    }

    @Override
    void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {

	// Type type = aCas.getTypeSystem().getType("DocumentID");
	// Feature documentId = type.getFeatureByBaseName("artifactID");
	
	// Type conceptType = aCas.getTypeSystem().getType("uima.tcas.Annotation");
	//Feature cui = type.getFeatureByBaseName("cui");
	
	// Integer source_note_id = aCas.getView("metadata")
	// .getIndexRepository()
	// .getAllIndexedFS(type)
	// .next()
	// .getStringValue(documentId) as Integer;

	// CASArtifact response = UimaAdapters.getArtifact(aCas.getView("Analysis"), null)
	// def b9 = 'P';
	// def sql = Sql.newInstance(this.output);
	// sql.executeUpdate "UPDATE source_note SET b9=$b9 WHERE id=$source_note_id";
	//println "callback"
	// for(FeatureStructure concept: aCas.getView("Analysis").getIndexRepository().getAllIndexedFS(conceptType)){
	//   for(Feature f: conceptType.getFeatures()){
	//     println concept.getStringValue(f)
	//   }
	// }
	//println aCas.getView("Analysis").getIndexRepository().getAllIndexedFS(conceptType)
	// for(Type t: aCas.getTypeSystem().getTypeIterator()){
	//   println t.getName()
	// }
	XmlCasSerializer.serialize(aCas, System.out)
	//println aCas.getView("Analysis")
	// for(CAS c: aCas.getViewIterator()){
	//   for(FeatureStructure fs: c.getIndexRepository().getAllIndexedFS(c.getJCas().getCasType(TOP.type))){
	//     println fs.getType().getName()
	//   }
	// }
	println "\n***************************************************"
    }
}

def in_db = Sql.newInstance(inDataSource);
in_db.withStatement { stmt ->
  stmt.setFetchSize(25)
}

while(true){
  def query = "SELECT id, rtf2plain FROM source_note WHERE rtf2plain IS NOT NULL AND ctakes='U' limit 1000";
  in_db.eachRow(query){ row ->
    ctakesPipeline.sendCAS(ctakesArtificer.sendAndWait(row));
  }
}
