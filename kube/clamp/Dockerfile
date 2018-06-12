FROM openjdk:8-jre

ADD ClampCmd_1.4.0 /usr/share/clamp
ADD required/nlpie /usr/share/clamp/nlpie

RUN echo 'export clamp_umlsuser=<USER>\nexport clamp_umlspw=<PW>\nCLAMP_HOME=/usr/share/clamp\nDATA_IN=/data/in\nCTAKES_OUT=/data/clamp_out' >  ~/.bashrc

WORKDIR /data/in

