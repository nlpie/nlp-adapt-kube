FROM openjdk:8-jre

RUN adduser --disabled-password elasticsearch
WORKDIR /home/elasticsearch

ADD elasticsearch-2.1.0 ./

EXPOSE 9200 
EXPOSE 9300

# switches allow to run as root and to publish host outside of container
ENTRYPOINT [ "bin/elasticsearch", "-Des.insecure.allow.root=true", "--network.host", "_non_loopback_" ]
