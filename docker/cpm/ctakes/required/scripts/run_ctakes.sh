#!/bin/bash
##### Run cTAKES #####

export CTAKES_HOME=/usr/share/ctakes
export DATA_DIRECTORY=/data
export DATA_IN=$DATA_DIRECTORY/data_in
export SAMPLE_FILE=$DATA_DIRECTORY/nlptab_manifest.txt
export CTAKES_OUT=$DATA_DIRECTORY/ctakes_out


export ctakes_umlsuser=$ctakes_umlsuser
export ctakes_umlspw=$ctakes_umlspw

mkdir -p $CTAKES_OUT

export JAVA_TOOL_OPTIONS='-Xms2G -Xmx9G -XX:MinHeapFreeRatio=25 -XX:+UseG1GC'

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


