FROM openjdk:8-jre

ADD ctakes-distribution /usr/share/ctakes

# set UMLS credentials via environment variable
RUN echo 'export ctakes_umlsuser=<USER>\nexport ctakes_umlspw=<PW>\nCTAKES_HOME=/usr/share/ctakes\nDATA_IN=/data/in\nCTAKES_OUT=/data/ctakes_out' >  ~/.bashrc

# ENTRYPOINT ["bin/runClinicalPipeline  -i /data/in  --xmiOut /data/ctakes/out  --user umlsUsername  --pass umlsPassword"]

WORKDIR /data/in

