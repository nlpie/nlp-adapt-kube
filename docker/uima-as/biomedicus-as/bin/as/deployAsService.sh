DIR="$(cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd)"

"$DIR/../runClass.sh" org.apache.uima.adapter.jms.service.UIMA_Service \
-saxonURL "file:$DIR/saxon8.jar" -xslt "$DIR/dd2spring.xsl" -dd $@
