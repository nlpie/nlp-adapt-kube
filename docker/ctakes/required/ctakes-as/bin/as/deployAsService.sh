#!/usr/bin/env bash

"$UIMA_HOME/bin/runUimaClass.sh" org.apache.uima.adapter.jms.service.UIMA_Service \
-saxonURL "file:$UIMA_HOME/saxon/saxon8.jar" -xslt "$UIMA_HOME/bin/dd2spring.xsl" -dd $@
