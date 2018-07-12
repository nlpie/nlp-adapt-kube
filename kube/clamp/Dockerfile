FROM openjdk:8-jre

ADD ClampCmd_1.4.0 /usr/share/clamp
ADD required/nlpie /usr/share/clamp/nlpie
ADD required/scripts /usr/share/clamp/scripts

RUN chmod 755 /usr/share/clamp/scripts/run_ctakes.sh
RUN chmod 755 /usr/share/clamp/scripts/umls.sh

WORKDIR /data/in

