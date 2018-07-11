FROM openjdk:8-jre

RUN apt-get update
RUN apt-get -qy install zip
RUN apt-get -qy install jq 

ADD public_mm /usr/share/public_mm
ADD apache-uima /usr/share/uima
ADD required/nlpie /usr/share/public_mm/nlpie
ADD required/scripts /usr/share/public_mm/scripts

RUN chmod 755 /usr/sharepublic_mm//scripts/run_metamap.sh

RUN echo 'export METAMAP_HOME=/usr/share/public_mm\nDATA_IN=/data/in\nMETAMAP_OUT=/data/metamap_out\n PATH=/usr/share/public_mm/bin:$PATH' >  ~/.bashrc

WORKDIR /data/in
