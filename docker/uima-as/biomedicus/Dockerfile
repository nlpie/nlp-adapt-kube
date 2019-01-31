FROM openjdk:8-jre

RUN apt-get update
RUN apt-get -qy install zip
RUN apt-get -qy install jq 

ADD biomedicus-distribution-2.1.0 /usr/share/biomedicus

#RUN chmod 755 /usr/share/biomedicus/scripts/run_biomedicus.sh

