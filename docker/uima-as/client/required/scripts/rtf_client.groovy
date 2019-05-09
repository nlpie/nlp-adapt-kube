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
  
final def rtfPipeline = getUimaPipelineClient(
    env["NLPADAPT_BROKER_URI"],
    "nlpadapt.rtf.outbound",
    new RtfCallbackListener(outDataSource),
    4
)

final def rtfArtificer = group.reactor {
    def cas = rtfPipeline.getCAS()
    def note = it.contents // for toy sqlite schema
    def doc_id = it.rowid // for toy sqlite schema
    def to_process = cas.createView("OriginalDocument")
    to_process.setDocumentText(note)
    UimaAdapters.createArtifact(cas, null, doc_id.toString())
    reply cas
}


class RtfCallbackListener extends UimaAsBaseCallbackListener {
  BasicDataSource output;

    RtfCallbackListener(BasicDataSource output){
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
	String documentText = aCas.getView("Analysis").getDocumentText()

	def row = [note_id:filename, rtf2plain:documentText, rtf_pipeline:'P']
	def sql = Sql.newInstance(this.output);
	sql.executeUpdate "UPDATE source_note SET rtf2plain=$row.rtf2plain, rtf_pipeline=$row.rtf_pipeline WHERE note_id=$row.note_id"
    }
}

def out_db = Sql.newInstance(outDataSource);
out_db.withStatement { stmt ->
  stmt.setFetchSize(25)
}

def in_db = Sql.newInstance(inDataSource);
in_db.withStatement { stmt ->
  stmt.setFetchSize(25)
}

while(true){
  def note_ids = out_db.rows("SELECT note_id FROM source_note WHERE rtf_pipeline='U' limit 1000")*.note_id;
  def query = "SELECT rowid, doc_id, contents FROM txts WHERE rowid IN (${note_ids.join(',')})".toString();

  in_db.eachRow(query){ row ->
    rtfPipeline.sendCAS(rtfArtificer.sendAndWait(row));
  }
}

// multiplier.terminate()
// group.shutdown()
