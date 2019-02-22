#!/usr/bin/env bash
METAMAP_HOME=${METAMAP_HOME:-"/usr/share/public_mm"}
JAVA_TOOL_OPTIONS='-Xms2G -Xmx6G -XX:MinHeapFreeRatio=25 -XX:+UseG1GC' $METAMAP_HOME/src/uima/bin/as/deployAsService.sh $METAMAP_HOME/src/uima/desc/NlpAdaptMetamapDeployment.xml
