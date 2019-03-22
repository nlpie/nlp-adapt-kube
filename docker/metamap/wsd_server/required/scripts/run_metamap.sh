#!/bin/bash

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
REMOTEPORT=1795
TIMEOUT=5

until 
	nc -w $TIMEOUT -z $REMOTEHOST $REMOTEPORT 
do 
	echo sleep && sleep 1;
done 

mmserver &
	

# TODO 
#source ./setup_uima.sh

BASEDIR=/usr/share/public_mm
export BASEDIR

MM_API_UIMA=${BASEDIR}/src/uima
export MM_API_UIMA
JAVAAPI=${BASEDIR}/src/javaapi/dist/MetaMapApi.jar
export JAVAAPI
PROLOGBEANS=${BASEDIR}/src/javaapi/dist/prologbeans.jar
export PROLOGBEANS

# Apache UIMA classes
UIMA_HOME=/usr/share/uima
export UIMA_HOME
UIMA_CLASSPATH=\
${UIMA_HOME}/examples/resources:\
${UIMA_HOME}/lib/uima-core.jar:\
${UIMA_HOME}/lib/uima-document-annotation.jar:\
${UIMA_HOME}/lib/uima-cpe.jar:\
${UIMA_HOME}/lib/uima-tools.jar:\
${UIMA_HOME}/lib/uima-examples.jar:\
${UIMA_HOME}/lib/uima-adapter-soap.jar:\
${UIMA_HOME}/lib/uima-adapter-vinci.jar:\
${UIMA_HOME}/lib/jVinci.jar  
export UIMA_CLASSPATH

# classpath
CLASSPATH=\
${CLASSPATH}:\
${MM_API_UIMA}/classes:\
${MM_API_UIMA}/desc:\
${BASEDIR}/nlpie:\
${MM_API_UIMA}/lib/metamap-api-uima.jar:\
${UIMA_CLASSPATH}:\
${JAVAAPI}:\
${PROLOGBEANS}
export CLASSPATH

PATH=$PATH:${UIMA_HOME}/bin
export PATH
# end TODO

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



