FROM ubuntu:16.04

ENV NOTVISIBLE "in users profile"
RUN echo "export VISIBLE=now" >> /etc/profile


# install misc
RUN apt-get update
RUN apt-get install -y software-properties-common #python-software-properties

RUN apt-get install -y vim

# Java
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64
RUN echo "export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64" >> ~/.bashrc
RUN add-apt-repository ppa:jonathonf/openjdk
RUN apt-get update
RUN apt-get install -y openjdk-8-jdk

# x11 from https://github.com/jessfraz/dockerfiles/blob/master/firefox/Dockerfile
RUN apt-get update && apt-get install -y \
	dirmngr \
	gnupg \
	--no-install-recommends \
	&& apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 0AB215679C571D1C8325275B9BDB3D89CE49EC21 \
	&& apt-get update && apt-get install -y \
	ca-certificates \
	hicolor-icon-theme \
	libasound2 \
	libgl1-mesa-dri \
	libgl1-mesa-glx \
	--no-install-recommends \
	&& rm -rf /var/lib/apt/lists/*

ADD biomedicus /nlp/biomedicus

RUN echo $JAVA_HOME

WORKDIR /nlp/biomedicus

# Modify /path/to/CpeDescriptor.xml in second argument; make sure inout and output data point to virtual mounted data directory
ENTRYPOINT ["/nlp/biomedicus/biomedicus-distribution/target/biomedicus-distribution-1.8.4-release/bin/runCPE.sh", "/nlp/biomedicus/biomedicus-distribution/target/biomedicus-distribution-1.8.4-release/examples/plaintext/PlainTextCPM_Test.xml" ]



