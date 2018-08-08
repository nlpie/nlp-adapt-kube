FROM openjdk:8-jre

RUN apt-get update
RUN apt-get -qy install zip
RUN apt-get -qy install jq 

ADD biomedicus-distribution-2.0.0-release /usr/share/biomedicus
ADD required/nlpie /usr/share/biomedicus/nlpie
ADD required/scripts /usr/share/biomedicus/scripts

RUN chmod 755 /usr/share/biomedicus/scripts/run_biomedicus.sh

WORKDIR /data/in
