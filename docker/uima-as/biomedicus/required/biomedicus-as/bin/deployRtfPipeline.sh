BIOMEDICUS_HOME=${BIOMEDICUS_HOME:-"/usr/share/biomedicus"}
BIOMEDICUS_JAVA_OPTS="-Xmx8g" $BIOMEDICUS_HOME/bin/as/deployAsService.sh $BIOMEDICUS_HOME/desc/NlpAdaptRtfDeployment.xml
