#!/usr/bin/env bash

export DATA_DIRECTORY=/data
export METAMAP_OUT=$DATA_DIRECTORY/metamap_out
export SAMPLE_FILE=$DATA_DIRECTORY/nlptab_manifest.txt
export DATA_IN=$DATA_DIRECTORY/data_in
export METAMAP_HOME=/usr/share/public_mm # /usr/share/public_mm

export JAVA_TOOL_OPTIONS='-Xms2G -Xmx6G -XX:MinHeapFreeRatio=25 -XX:+UseG1GC'
export UIMA_JVM_OPTS='-Xms128M -Xmx5g' 

# TODO: refactor out tagger and WSD as sidecard contaienrs
# https://github.com/argoproj/argo/tree/master/examples#sidecars
##### Start Metamap Tagger Servers #####
skrmedpostctl start
wsdserverctl start

# need to ensure tagger service has beee started
REMOTEHOST=127.0.0.1
ANNPORT=1795
WSDPORT=5554
TIMEOUT=30

until 
	nc -z $REMOTEHOST $ANNPORT 
do 
	echo sleep && sleep 1;
done 

until 
	nc -z $REMOTEHOST $WSDPORT 
do 
	echo sleep && sleep 1;
done 

mmserver &
	
source /usr/share/public_mm/scripts/setup_uima.sh

mkdir -p $METAMAP_OUT

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



