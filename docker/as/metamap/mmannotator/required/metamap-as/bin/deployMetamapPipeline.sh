#!/usr/bin/env bash
#!/usr/bin/env bash

source /usr/share/public_mm/src/uima/bin/setup_uima.sh

METAMAP_HOME=${METAMAP_HOME:-"/usr/share/public_mm"}
JAVA_TOOL_OPTIONS='-Xms2G -Xmx6G -XX:MinHeapFreeRatio=25 -XX:+UseG1GC' $METAMAP_HOME/src/uima/bin/as/deployAsService.sh $METAMAP_HOME/src/uima/desc/NlpAdaptMetamapDeployment.xml
