import java.util.concurrent.TimeUnit

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
def group = new DefaultPGroup(4);
def inDataSource = new BasicDataSource();
inDataSource.setUrl(env["NLPADAPT_INPUT_DATASOURCE_URI"]);
def outDataSource = new BasicDataSource();
outDataSource.setUrl(env["NLPADAPT_OUTPUT_DATASOURCE_URI"]);


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
    new BiomedicusCallbackListener(outDataSource),
    16
);

final def biomedicusArtificer = group.reactor {
    def cas = biomedicusPipeline.getCAS()
    def note = it.rtf2plain
    def source_note_id = it.id // for toy sqlite schema
    def to_process = cas.createView("OriginalDocument")
    to_process.setDocumentText(note)
    UimaAdapters.createArtifact(cas, null, source_note_id.toString())
    reply cas
}

class BiomedicusCallbackListener extends UimaAsBaseCallbackListener {
  BasicDataSource output;

  BiomedicusCallbackListener(BasicDataSource output){
	this.output = output
    }

    @Override
    void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {

	Type type = aCas.getTypeSystem().getType("ArtifactID");
	Feature documentId = type.getFeatureByBaseName("artifactID");
	
	// Type conceptType = aCas.getTypeSystem().getType("uima.tcas.Annotation");
	//Feature cui = type.getFeatureByBaseName("cui");
	
	Integer source_note_id = aCas.getView("metadata")
	.getIndexRepository()
	.getAllIndexedFS(type)
	.next()
	.getStringValue(documentId) as Integer;

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
	//XmlCasSerializer.serialize(aCas, System.out)
	//println aCas.getView("Analysis")
	// for(CAS c: aCas.getViewIterator()){
	//   for(FeatureStructure fs: c.getIndexRepository().getAllIndexedFS(c.getJCas().getCasType(TOP.type))){
	//     println fs.getType().getName()
	//   }
	// }
	XmlCasSerializer.serialize(aCas, System.out)
	println "\n***************************************************"
    }
}

def in_db = Sql.newInstance(inDataSource);
in_db.withStatement { stmt ->
  stmt.setFetchSize(25)
}

while(true){
  def query = "SELECT id, rtf2plain FROM source_note WHERE rtf2plain IS NOT NULL AND b9='U' limit 1000";
  in_db.eachRow(query){ row ->
    biomedicusPipeline.sendCAS(biomedicusArtificer.sendAndWait(row));
  }
}
