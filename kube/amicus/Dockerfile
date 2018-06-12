FROM openjdk:8-jre

ADD amicus-distribution /usr/share/amicus

RUN echo 'export AMICUS_HOME=/usr/share/amicus\nAMICUS_OUT=/data/amicus_out' >  ~/.bashrc

WORKDIR /data/in

