FROM openjdk:8-jre

RUN apt-get update
RUN apt-get -qy install zip
RUN apt-get -qy install jq 

ADD ctakes-distribution /usr/share/ctakes
ADD required/scripts /usr/share/biomedicus/scripts

RUN chmod 755 /usr/share/biomedicus/scripts/run_ctakes.sh
RUN chmod 755 /usr/share/biomedicus/scripts/umls.sh

WORKDIR /data/in

