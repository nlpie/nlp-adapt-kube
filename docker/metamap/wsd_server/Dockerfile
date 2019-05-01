FROM openjdk:8-jre

RUN apt-get update
RUN apt-get -qy install zip
RUN apt-get -qy install jq
RUN apt-get -qy install netcat
RUN apt-get -qy install vim

RUN mkdir /usr/share/public_mm
ADD public_mm/WSD_Server /usr/share/public_mm/WSD_Server
ADD public_mm/bin /usr/share/public_mm/bin
#ADD required/nlpie /usr/share/public_mm/nlpie
ADD required/scripts /usr/share/public_mm/scripts

ENV PATH=/usr/share/public_mm/bin:$PATH

WORKDIR  /usr/share/public_mm

# --- BUILD ---
RUN  /usr/share/public_mm/bin/install.sh

RUN chmod 755 /usr/share/public_mm/bin/wsd_foreground.in
#RUN chmod 755 /usr/share/public_mm/scripts/setup_uima.sh

WORKDIR /data

CMD wsd_foreground start
