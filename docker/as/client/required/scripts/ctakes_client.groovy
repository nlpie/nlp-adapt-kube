import java.util.concurrent.TimeUnit
import java.io.ByteArrayOutputStream
import java.io.PrintStream

import groovy.json.JsonOutput
import groovy.io.FileType
import groovy.sql.Sql
import groovy.transform.SourceURI
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.SyncDataflowQueue

import org.apache.ctakes.typesystem.type.structured.DocumentID

// INFO: UIMA Version 2.10.2 UIMA-AS Version 2.10.3
import org.apache.uima.cas.*
import org.apache.uima.jcas.JCas
import org.apache.uima.jcas.cas.TOP
import org.apache.uima.util.XmlCasSerializer
import org.apache.uima.jcas.tcas.Annotation
import org.apache.uima.adapter.jms.client.BaseUIMAAsynchronousEngine_impl
import org.apache.uima.aae.client.UimaAsynchronousEngine
import org.apache.uima.aae.client.UimaAsBaseCallbackListener
import org.apache.uima.collection.EntityProcessStatus
import org.apache.uima.aae.client.UimaASProcessStatus
import org.apache.uima.examples.SourceDocumentInformation
import org.apache.uima.fit.util.CasUtil

import org.apache.commons.dbcp2.BasicDataSource

def env = System.getenv();
def group = new DefaultPGroup(8);

@SourceURI
URI sourceUri;
def scriptDir = new File(sourceUri).parent;

def batchBegin = (env["BATCH_BEGIN"] ?: 0) as Integer;
def batchEnd = (env["BATCH_END"] ?: 9999) as Integer;
def batchOffset = env["BATCH_OFFSET"] ?:  new Random().nextInt(batchEnd - batchBegin);

def dataSource = new BasicDataSource();
dataSource.setPoolPreparedStatements(true);
dataSource.setMaxTotal(5);
dataSource.setUrl(env["DATASOURCE_URI"]);
dataSource.setUsername(env["DATASOURCE_USERNAME"]);
dataSource.setPassword(env["DATASOURCE_PASSWORD"]);
def inputStatement = new File("$scriptDir/ctakes_sql/input.sql").text;
def artifactStatement = new File("$scriptDir/ctakes_sql/artifact.sql").text;

SyncDataflowQueue outputQueue = new SyncDataflowQueue();


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
    env["BROKER_URI"],
    "nlpadapt.ctakes.outbound",
    new CtakesCallbackListener(outputQueue),
    8
);

final def ctakesArtificer = group.reactor {
    def cas = ctakesPipeline.getCAS()
    def jcas = cas.getJCas()
    def doc_id = new DocumentID(jcas)
    
    def source_note_id = it.note_id
    doc_id.setDocumentID(source_note_id.toString())
    doc_id.addToIndexes()
    
    def note = it.rtf2plain ?: ""
    jcas.setDocumentText(note)
    def adapt = jcas.createView("NlpAdapt")

    reply jcas.getCas()
}

class CtakesCallbackListener extends UimaAsBaseCallbackListener {
  SyncDataflowQueue output;

  CtakesCallbackListener(SyncDataflowQueue output){
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
        Type termType = aCas.getTypeSystem().getType("org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation");
        Feature polarity = termType.getFeatureByBaseName("polarity");
        Feature historical = termType.getFeatureByBaseName("historyOf");
        
        Feature cuiArray = termType.getFeatureByBaseName("ontologyConceptArr");
        Type umlsConceptType = aCas.getTypeSystem().getType("org.apache.ctakes.typesystem.type.refsem.UmlsConcept");
        Feature cui = umlsConceptType.getFeatureByBaseName("cui");
        Feature tui = umlsConceptType.getFeatureByBaseName("tui");
        Feature preferred = umlsConceptType.getFeatureByBaseName("preferredText");
        
        def items = CasUtil.select(aCas, termType);
        def filtered_items = []
        items.each{
            item -> 
            def concepts = item.getFeatureValue(cuiArray);
            def cuiTuis = concepts.collect{h -> [cui: h.getStringValue(cui), tui:h.getStringValue(tui), preferredText: h.getStringValue(preferred)]}.groupBy{it.cui}
            cuiTuis.each{ k,v ->
                filtered_items.push([
                    item_type:'C',
                    item:k,
                    note_id:source_note_id,
                    engine_id:4,
                    begin_span:item.getBegin(),
                    end_span:item.getEnd(),
                    negated: item.getIntValue(polarity) < 0 ? 'T' : 'F',
                    historical: item.getIntValue(historical),
                    text:item.getCoveredText(),
                    attributes:JsonOutput.toJson([semanticGroups: v*.tui, preferred: v*.preferredText[0]])
                ])
            }
        }        
        
        if(!aStatus.isException()){     
            def process_results = [note_id:source_note_id, ctakes:'P', items:filtered_items]
            this.output << process_results
        } else {
            ByteArrayOutputStream errors = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(errors);
            for(e in aStatus.getExceptions()){ e.printStackTrace(ps); }
            def process_results = [note_id:source_note_id, ctakes:'E', error:errors]
            this.output << process_results
        }  
    }
}

5.times{
  group.task {
    while(true){
      def data = outputQueue.val;
      def sql = Sql.newInstance(dataSource);
      if(data.ctakes == 'P'){
        sql.withTransaction{
          sql.withBatch(100, artifactStatement){ ps ->
            for(i in data.items){
              ps.addBatch(i)
            }
          }
          sql.executeUpdate "UPDATE dbo.u01 SET ctakes=$data.ctakes WHERE note_id=$data.note_id"
        }
        println "SUCCESS: $data.note_id"
      } else {
        data.error = data.error?.toString().take(3999)
        sql.executeUpdate "UPDATE dbo.u01 SET ctakes=$data.ctakes, error=$data.error WHERE note_id=$data.note_id"
        println "ERROR:   $data.note_id"
      }
      sql.close()
    }
  }
}

while(true){
  def templater = new groovy.text.SimpleTemplateEngine();
  def inputTemplate = templater.createTemplate(inputStatement);
  
  for(batch in (batchBegin + batchOffset)..batchEnd){
    def in_db = Sql.newInstance(dataSource);
    in_db.withStatement { stmt ->
      stmt.setFetchSize(20)
    }
    
    in_db.eachRow(inputTemplate.make(["batch":batch]).toString()){ row ->
        ctakesPipeline.sendCAS(ctakesArtificer.sendAndWait(row));
    }
    
    in_db.close();
  }
  batchOffset = 0;
}
