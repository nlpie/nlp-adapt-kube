FROM openjdk:8-jre

RUN apt-get update
RUN apt-get -qy install zip
RUN apt-get -qy install jq 

ADD clamp /usr/share/clamp
ADD required/nlpie /usr/share/clamp/nlpie
ADD required/scripts /usr/share/clamp/scripts

RUN chmod 755 /usr/share/clamp/scripts/run_clamp.sh

WORKDIR /data/data_in

