##### Create Archive for NLP-TAB #####

export DATA_DIRECTORY=~/workspace/test
export BIOMEDICUS_OUT=$DATA_DIRECTORY/biomedicus_out
export SAMPLE_FILE=$DATA_DIRECTORY/nlptab_manifest.txt
export DATA_IN=$DATA_DIRECTORY/data_in

##### Create Archive for NLP-TAB #####
if [ ! -f $SAMPLE_FILE ]; then
    ls $DATA_IN | sed 's/\.txt/\.txt\.xmi/' > $SAMPLE_FILE
    echo "TypeSystem.xml" >> $SAMPLE_FILE
fi

pushd $BIOMEDICUS_OUT
zip $BIOMEDICUS_OUT -@ < $SAMPLE_FILE
popd

##### Create BiomedICUS NLP-TAB profile and upload archive #####
BIOMEDICUS_META='{"systemName":"BiomedICUS", "systemDescription":"BiomedICUS annotation engine", "instance":"default"}'
RESPONSE=$(echo $BIOMEDICUS_META | curl -sS -d @- http://192.168.99.100:31345/_nlptab-systemindexmeta)
echo $RESPONSE
curl -sS --data-binary @$BIOMEDICUS_OUT.zip -H 'Content-Type: application/zip' "http://192.168.99.100:31345/_nlptab-systemindex?instance=default&index=$(echo $RESPONSE | jq -r .index)&useXCas=false"
