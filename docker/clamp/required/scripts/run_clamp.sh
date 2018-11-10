#!/bin/bash
##### Run CLAMP #####
:w

export DATA_DIRECTORY=/data
export DATA_IN=$DATA_DIRECTORY/data_in
export CLAMP_HOME=/usr/share/clamp
export SAMPLE_FILE=$DATA_DIRECTORY/nlptab_manifest.txt
export CLAMP_OUT=$DATA_DIRECTORY/clamp_out

mkdir -p $CLAMP_OUT
$CLAMP_HOME/nlpie/run_nlpie_pipeline.sh

##### Create Archive for NLP-TAB #####
rm -f $CLAMP_OUT/*.txt
pushd $CLAMP_OUT
for f in *.xmi; do mv -- "$f" "${f%.xmi}.txt.xmi"; done

cp $CLAMP_HOME/nlpie/TypeSystem.xml $CLAMP_OUT/TypeSystem.xml
if [ ! -f $SAMPLE_FILE ]; then
    ls $DATA_IN | shuf -n $RANDOM_SAMPLE | sed 's/\.txt/\.txt\.xmi/' > $SAMPLE_FILE
    echo "TypeSystem.xml" >> $SAMPLE_FILE
fi

zip $CLAMP_OUT -@ < $SAMPLE_FILE
popd

