FROM openjdk:8-jre

#RUN mkdir -p /app
RUN adduser --disabled-password elasticsearch
WORKDIR /home/elasticsearch

ADD elasticsearch-2.1.0 ./

EXPOSE 9200 
EXPOSE 9300

#RUN chown -R elasticsearch:elasticsearch /home/elasticsearch
#ENTRYPOINT [ "su", "-", "elasticsearch", "/home/elasticsearch/elasticsearch-2.1.0/bin/elasticsearch" ]

ENTRYPOINT [ "bin/elasticsearch", "-Des.insecure.allow.root=true", "--network.host", "_non_loopback_" ]
