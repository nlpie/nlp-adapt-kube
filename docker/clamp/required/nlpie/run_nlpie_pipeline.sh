#!/bin/bash

set -x

DATA_DIRECTORY=/data
DATA_IN=$DATA_DIRECTORY/in
CLAMP_HOME=/usr/share/clamp
CLAMP_OUT=$DATA_DIRECTORY/clamp_out
umlsUser=$umlsUser
umlsPass=$umlsPass
input=$DATA_IN
output=$CLAMP_OUT

clampbin="$CLAMP_HOME/bin/clamp-nlp-1.4.0-jar-with-dependencies.jar"
pipeline="$CLAMP_HOME/pipeline/clamp-ner-attribute.pipeline.jar"
umlsIndex="$CLAMP_HOME/resource/umls_index/"

java -DCLAMPLicenceFile="$CLAMP_HOME/CLAMP.LICENSE" -Xmx3g -cp $clampbin edu.uth.clamp.nlp.main.PipelineMain \
    -i $input \
    -o $output \
    -p $pipeline \
    -U $umlsUser \
    -P $umlsPass \
    -I $umlsIndex

set +x


