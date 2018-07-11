export DATA_DIRECTORY=/data
export METAMAP_OUT=$DATA_DIRECTORY/metamap_out
export SAMPLE_FILE=$DATA_DIRECTORY/nlptab_manifest.txt
export DATA_IN=$DATA_DIRECTORY/in
export METAMAP_HOME=/usr/share/public_mm # /usr/share/public_mm

export JAVA_TOOL_OPTIONS='-Xms2G -Xmx6G -XX:MinHeapFreeRatio=25 -XX:+UseG1GC'

##### Start Metamap Tagger Servers #####
skrmedpostctl start
wsdserverctl start
mmserver &

##### Run UIMA against Metamap taggers #####
source ./setup_uima.sh

runCPE.sh $METAMAP_HOME/nlpie/MetaMapCPM_nlpie.xml

##### Create Archive for NLP-TAB #####
cp $METAMAP_HOME/nlpie/CombinedTypeSystem.xml $METAMAP_OUT/TypeSystem.xml
if [ ! -f $SAMPLE_FILE ]; then
    ls $DATA_IN | sed 's/\.txt/\.txt\.xmi/' > $SAMPLE_FILE
    echo "TypeSystem.xml" >> $SAMPLE_FILE
fi

pushd $METAMAP_OUT
zip $METAMAP_OUT -@ < $SAMPLE_FILE
popd

##### Create Metamap NLP-TAB profile and upload archive #####
METAMAP_META='{"systemName":"MetaMap", "systemDescription":"MetaMap UIMA annotation engine", "instance":"default"}'
RESPONSE=$(echo $METAMAP_META | curl -sS -d @- http://192.168.99.100:31345/_nlptab-systemindexmeta)

echo $RESPONSE

curl -sS --data-binary @$METAMAP_OUT.zip -H 'Content-Type: application/zip' "http://192.168.99.100:31345/_nlptab-systemindex?instance=default&index=$(echo $RESPONSE | jq -r .index)&useXCas=false"


