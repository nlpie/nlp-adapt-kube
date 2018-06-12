FROM openjdk:8-jre

ADD public_mm /usr/share/public_mm
ADD apache-uima /usr/share/uima
ADD required/nlpie /usr/share/public_mm/nlpie

RUN echo 'export METAMAP_HOME=/usr/share/public_mm\nDATA_IN=/data/in\nMETAMAP_OUT=/data/metamap_out\n PATH=/usr/share/public_mm/bin:$PATH' >  ~/.bashrc

WORKDIR /data/in

