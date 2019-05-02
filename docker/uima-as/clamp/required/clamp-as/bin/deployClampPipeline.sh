#!/usr/bin/env bash
CLAMP_HOME=${CLAMP_HOME:-"/usr/share/clamp"}

clampjar=$CLAMP_HOME/bin/clamp-nlp-1.5.1-jar-with-dependencies.jar
pipeline=$CLAMP_HOME/pipeline/clamp-ner.pipeline.jar
umls_index=$CLAMP_HOME/resource/umls_index/
config=$CLAMP_HOME/config/ServiceConfig.groovy

export UIMA_HOME=$CLAMP_HOME/apache-uima-as-2.10.3

java -DCLAMPLicenceFile="$CLAMP_HOME/CLAMP.LICENSE" -cp $clampjar edu.uth.clamp.nlp.uimaas.Service $pipeline $umls_index $umls_user $umls_pass $config

