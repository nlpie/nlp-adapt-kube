#!/bin/bash

##### Create Archive for NLP-TAB #####

export DATA_DIRECTORY=/data
export BIOMEDICUS_HOME=/usr/share/biomedicus
export BIOMEDICUS_OUT=$DATA_DIRECTORY/biomedicus_out
export SAMPLE_FILE=$DATA_DIRECTORY/nlptab_manifest.txt
export DATA_IN=$DATA_DIRECTORY/in

$BIOMEDICUS_HOME/bin/runCPE.sh $BIOMEDICUS_HOME/nlpie/PlainTextCPM_nlpie.xml

##### Create Archive for NLP-TAB #####
if [ ! -f $SAMPLE_FILE ]; then
    ls $DATA_IN | sed 's/\.txt/\.txt\.xmi/' > $SAMPLE_FILE
    echo "TypeSystem.xml" >> $SAMPLE_FILE
fi

pushd $BIOMEDICUS_OUT
zip $BIOMEDICUS_OUT -@ < $SAMPLE_FILE
popd

