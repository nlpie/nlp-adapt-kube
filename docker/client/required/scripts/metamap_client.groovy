import java.util.concurrent.TimeUnit

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
    def note = it.rtf2plain?.characterStream?.text
    def source_note_id = it.note_id // oracle test environment
    def to_process = cas.getView("_InitialView")
    to_process.setDocumentText(note)

    SourceDocumentInformation doc_info = new SourceDocumentInformation(to_process.getJCas());
    doc_info.setUri(source_note_id.toString());
    doc_info.setOffsetInSource(0);
    doc_info.setDocumentSize((int) note.length());
    doc_info.setLastSegment(true);
    doc_info.addToIndexes();

    reply cas
};

final def metamapDatabaseWrite = group.reactor { data ->
  def sql = Sql.newInstance(dataSource);
  if(data.mm == 'P'){
    sql.withTransaction{
      sql.withBatch(100, "INSERT INTO nlp_sandbox.u01_detected_item(item_type, item, note_id, engine_id, begin, end, negated, text) VALUES (:item_type, :item, :note_id, :engine_id, :begin, :end, :negated, :text)"){ ps ->
	for(i in data.items){
	  ps.addBatch(i)
	}
      }
    }    
    sql.executeUpdate "UPDATE u01_tmp SET mm=$data.mm WHERE note_id=$data.note_id"
    reply "SUCCESS: $data.note_id"
  } else {
    sql.executeUpdate "UPDATE u01_tmp SET mm=$data.mm, error=$data.error WHERE note_id=$data.note_id"
    reply "ERROR:   $data.note_id"
  }
  sql.close()
};


class MetamapCallbackListener extends UimaAsBaseCallbackListener {
    DataflowQueue output

    MetamapCallbackListener(DataflowQueue output){
	this.output = output
    }

  Map getNegations(CAS aCas) {
    Type negatedType = aCas.getTypeSystem().getType("org.metamap.uima.ts.Negation");
    Feature negTrigger = negatedType.getFeatureByBaseName("negTrigger");
    Feature cuiConcepts = negatedType.getFeatureByBaseName("cuiConcepts");
    Feature ncSpans = negatedType.getFeatureByBaseName("ncSpans");
    
    Type cuiConceptType = aCas.getTypeSystem().getType("org.metamap.uima.ts.CuiConcept");
    Feature negExCui = cuiConceptType.getFeatureByBaseName("negExCui");
  
    def negated = CasUtil.select(aCas, negatedType);
    def negated_items = negated.collectEntries{
      def bit = it
      it.getFeatureValue(cuiConcepts).collectEntries{
      [(it.getStringValue(negExCui)): bit.getFeatureValue(ncSpans).collect{it as Annotation}.collect{
	  [it.getBegin(), it.getEnd()]
	}]
      }
    }
    return negated_items
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
      Type termType = aCas.getTypeSystem().getType("org.metamap.uima.ts.Candidate");
      Feature cui = termType.getFeatureByBaseName("cui");
      Feature text = termType.getFeatureByBaseName("concept");
      
      def items = CasUtil.select(aCas, termType);
      items = items.collect{it as Annotation}
      def negated = getNegations(aCas);
      def filtered_items = items.collect{
	def item = it.getStringValue(cui);
      	[
	item_type:'C',
      	item:item,
	note_id:source_note_id,
	engine_id:2,
      	begin:it.getBegin(),
      	end:it.getEnd(),
	negated: (negated.containsKey(item) && [it.getBegin(), it.getEnd()] in negated[item]) ? 'T' : 'F',
      	text:it.getStringValue(text)
      	]
      }
      
      if(!aStatus.isException()){
      	def process_results = [note_id:source_note_id, mm:'P', items:filtered_items]
      	this.output << process_results
      } else {
      	def process_results = [note_id:source_note_id, mm:'E', error:aStatus.getStatusMessage().take(3999)]
      	this.output << process_results
      }
    }
}

outputQueue.wheneverBound {
  group.actor{
    metamapDatabaseWrite.send outputQueue.val
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
  
  in_db.eachRow("SELECT note_id, rtf2plain FROM nlp_sandbox.u01_tmp WHERE rtf2plain IS NOT NULL AND mm IN ('U', 'R') AND rownum<=1000"){ row ->
    metamapPipeline.sendCAS(metamapArtificer.sendAndWait(row));
  }

  in_db.close()
}

