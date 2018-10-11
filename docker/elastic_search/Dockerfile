FROM openjdk:8-jre
# see https://github.com/docker-library/elasticsearch

RUN adduser --disabled-password elasticsearch
WORKDIR /usr/share/elasticsearch
ENV PATH /usr/share/elasticsearch/bin:$PATH

RUN export ES_JAVA_OPTS='-Xms32m -Xmx32m';
COPY elasticsearch-2.1.0/* ./ 

# RUN apt-get update
# RUN apt-get -y install vim 
RUN	for path in \
		./data \
		./logs \
		./config \
		./config/scripts \
	; do \
		mkdir -p "$path"; \
		chown -R elasticsearch:elasticsearch "$path"; \
	done; 

RUN		mkdir -p /data/elastic \
		mkdir -p /data/logs/elastic

COPY required/config ./config

COPY required/plugins/nlptab ./plugins/nlptab

EXPOSE 9200 
EXPOSE 9300

WORKDIR /data/in

# switches allow to run as root and to publish host outside of container
ENTRYPOINT [ "elasticsearch", "-Des.insecure.allow.root=true", "--network.host", "_non_loopback_" ]
# CMD [ "elasticsearch", "-Des.insecure.allow.root=true", "--network.host", "_eth0:ipv4_" ]

