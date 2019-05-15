#!/usr/bin/env bash
CTAKES_HOME=${CTAKES_HOME:-"/usr/share/ctakes"}

export UIMA_HOME=${UIMA_HOME:-"/usr/share/uima"}
export UIMA_CLASSPATH=$UIMA_HOME/lib:$CTAKES_HOME/lib:$CTAKES_HOME/resources

JAVA_TOOL_OPTIONS='-Xms2G -Xmx6G -XX:MinHeapFreeRatio=25 -XX:+UseG1GC' $CTAKES_HOME/bin/as/deployAsService.sh $CTAKES_HOME/desc/NlpAdaptCtakesDeployment.xml
