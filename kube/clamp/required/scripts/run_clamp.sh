##### Run CLAMP #####

source /usr/share/clamp/scripts/umls.sh

export DATA_DIRECTORY=/usr/share/host_data
export DATA_IN=$DATA_DIRECTORY/in

export CLAMP_OUT=$DATA_DIRECTORY/clamp_out

mkdir -p $CLAMP_OUT
$CLAMP_HOME/nlpie/run_nlpie_pipeline.sh

##### Create Archive for NLP-TAB #####
rm $CLAMP_OUT/*.txt
pushd $CLAMP_OUT
for f in *.xmi; do mv -- "$f" "${f%.xmi}.txt.xmi"; done

cp $CLAMP_HOME/nlpie/TypeSystem.xml $CLAMP_OUT/TypeSystem.xml
if [ ! -f $SAMPLE_FILE ]; then
    ls $DATA_IN | shuf -n $RANDOM_SAMPLE | sed 's/\.txt/\.txt\.xmi/' > $SAMPLE_FILE
    echo "TypeSystem.xml" >> $SAMPLE_FILE
fi

zip $CLAMP_OUT -@ < $SAMPLE_FILE
popd

##### Create CLAMP NLP-TAB profile and upload archive #####
CLAMP_META='{"systemName":"CLAMP", "systemDescription":"CLAMP annotation engine", "instance":"default"}'
RESPONSE=$(echo $CLAMP_META | curl -sS -d @- http://192.168.99.100:31345/_nlptab-systemindexmeta)
curl -sS --data-binary @$CLAMP_OUT.zip -H 'Content-Type: application/zip' "http://192.168.99.100:31345/_nlptab-systemindex?instance=default&index=$(echo $RESPONSE | jq -r .index)&useXCas=false"

