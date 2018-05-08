FROM openjdk:8-jre

ADD ctakes-distribution /ctakes

RUN echo $JAVA_HOME

# set UMLS credentials via environment variable
RUN echo 'export ctakes_umlsuser=<USER>\nexport ctakes_umlspw=<PW>' >  ~/.bashrc

# ENTRYPOINT ["bin/runClinicalPipeline  -i /data/in  --xmiOut /data/ctakes/out  --user umlsUsername  --pass umlsPassword"]

WORKDIR /data/in

