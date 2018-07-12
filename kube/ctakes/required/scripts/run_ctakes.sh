##### Run cTAKES #####

source /usr/share/ctakes/umls.sh
export CTAKES_HOME=/usr/share/ctakes
export DATA_DIRECTORY=/data
export DATA_IN=$DATA_DIRECTORY/in
export CTAKES_OUT=$DATA_DIRECTORY/ctakes_out

$CTAKES_HOME/bin/runClinicalPipeline.sh -i $DATA_IN --xmiOut $CTAKES_OUT

##### Create Archive for NLP-TAB #####
cp $CTAKES_HOME/resources/org/apache/ctakes/typesystem/types/TypeSystem.xml $CTAKES_OUT/TypeSystem.xml
if [ ! -f $SAMPLE_FILE ]; then
    ls $DATA_IN | shuf -n $RANDOM_SAMPLE | sed 's/\.txt/\.txt\.xmi/' > $SAMPLE_FILE
    echo "TypeSystem.xml" >> $SAMPLE_FILE
fi
pushd $CTAKES_OUT
zip $CTAKES_OUT -@ < $SAMPLE_FILE
popd

##### Create cTAKES NLP-TAB profile and upload archive #####
CTAKES_META='{"systemName":"cTAKES", "systemDescription":"cTAKES annotation engine", "instance":"default"}'
RESPONSE=$(echo $CTAKES_META | curl -sS -d @- http://192.168.99.100:31345/_nlptab-systemindexmeta)
curl -sS --data-binary @$CTAKES_OUT.zip -H 'Content-Type: application/zip' "http://192.168.99.100:31345/_nlptab-systemindex?instance=default&index=$(echo $RESPONSE | jq -r .index)&useXCas=false"
