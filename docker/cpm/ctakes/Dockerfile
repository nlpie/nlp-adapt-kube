FROM openjdk:8-jre

RUN apt-get update
RUN apt-get -qy install zip
RUN apt-get -qy install jq 
RUN apt-get update
RUN apt-get -qy install vim

#ADD ctakes-distribution /usr/share/ctakes
ADD apache-ctakes-4.0.1-SNAPSHOT /usr/share/ctakes
ADD required/nlpie /usr/share/ctakes/nlpie
ADD required/scripts /usr/share/ctakes/scripts
ADD required/resources/org/apache/ctakes/clinical/pipeline /usr/share/ctakes/resources/org/apache/ctakes/clinical/pipeline

RUN chmod 755 /usr/share/ctakes/scripts/run_ctakes.sh
RUN chmod 755 /usr/share/ctakes/scripts/umls.sh

WORKDIR /data

