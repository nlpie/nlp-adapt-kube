FROM ubuntu:16.04
#FROM debian:jessie

# install and configure ssh for connecting to container
#RUN apt-get update && apt-get install -y openssh-server
#RUN mkdir /var/run/sshd
#RUN echo 'root:screencast' | chpasswd
#RUN sed -i 's/PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config

# SSH login fix. Otherwise user is kicked off after login
#RUN sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd

ENV NOTVISIBLE "in users profile"
RUN echo "export VISIBLE=now" >> /etc/profile


# install misc
RUN apt-get update
RUN apt-get install -y software-properties-common #python-software-properties

RUN apt-get install -y vim

# Java
# Oracle
#RUN add-apt-repository ppa:webupd8team/java
#RUN echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | /usr/bin/debconf-set-selections
#RUN apt-get update
#RUN apt-get install -y oracle-java8-installer

#ENV JAVA_HOME /usr/lib/jvm/java-8-oracle
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64

#RUN echo 'deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main' >> /etc/apt/sources.list && \
#    echo 'deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main' >> /etc/apt/sources.list && \
#    apt-key adv --keyserver keyserver.ubuntu.com --recv-keys C2518248EEA14886 && \
#    apt-get update && \
#    echo oracle-java${JAVA_VER}-installer shared/accepted-oracle-license-v1-1 select true |  /usr/bin/debconf-set-selections && \
#    apt-get install -y --force-yes --no-install-recommends oracle-java${JAVA_VER}-installer oracle-java${JAVA_VER}-set-default && \
#    apt-get clean && \
#    rm -rf /var/cache/oracle-jdk${JAVA_VER}-installer

RUN echo "export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64" >> ~/.bashrc
# OpenJava
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

# ansible 
RUN apt-add-repository ppa:ansible/ansible
RUN apt-get update
RUN apt-get install -y ansible

ADD playbooks /ansible

#VOLUME /ansible
WORKDIR /ansible

RUN ansible-playbook ctakes.yml -c local 

EXPOSE 22
CMD ["/usr/sbin/sshd", "-D"]

#COPY local.conf /etc/fonts/local.conf

RUN echo $JAVA_HOME

WORKDIR /ansible/test/ctakes


ENV CTAKES_HOME /ansible/test/ctakes
ENV ctakes_umlsuser horcle
ENV ctakes_umlspw nEj123456

RUN echo $CTAKES_HOME

#CMD ["bash"]

#ENTRYPOINT [ "java", "-cp", "/ansible/test/ctakes/desc/:/ansible/test/ctakes/resources/:/ansible/test/ctakes/lib/*", "-Dlog4j.configuration=file:/ansible/test/ctakes/config/log4j.xml", "-Xms512M", "-Xmx3g", "org.apache.uima.tools.cvd.CVD" ]

#ENTRYPOINT [ "bin/runctakesCVD.sh",  "-desc", "desc/ctakes-clinical-pipeline/desc/analysis_engine/AggregatePlaintextFastUMLSProcessor.xml" ]

#ENTRYPOINT [ "java", "-cp", "/ansible/test/ctakes/desc/:/ansible/test/ctakes/resources/:/ansible/test/ctakes/lib/*", "-Dlog4j.configuration=file:/ansible/test/ctakes/config/log4j.xml", "-Xms512M", "-Xmx3g", "org.apache.uima.tools.cpm.CpmFrame" ]
#ENTRYPOINT [ "java", "-cp", "/ansible/test/ctakes/desc/:/ansible/test/ctakes/resources/:/ansible/test/ctakes/lib/*", "-Dlog4j.configuration=file:/ansible/test/ctakes/config/log4j.xml", "-Xms512M", "-Xmx3g", "org.apache.uima.tools.cpm.CpmFrame", "-desc", "/ansible/test/ctakes/desc/ctakes-clinical-pipeline/desc/collection_processing_engine/testing123.xml" ]
#ENTRYPOINT [ "java", "-cp", "/ansible/test/ctakes/desc/:/ansible/test/ctakes/resources/:/ansible/test/ctakes/lib/*", "-Dlog4j.configuration=file:/ansible/test/ctakes/config/log4j.xml", "-Xms512M", "-Xmx3g", "org.apache.uima.tools.cpm.CpmFrame", "-desc", "/ansible/test/ctakes/desc/ctakes-clinical-pipeline/desc/collection_processing_engine/testing123.xml" ]
#ENTRYPOINT ["bin/runClinicalPipeline.sh", "-i", "/ansible/test/data/in", "--xmiOut", "/ansible/test/data/out" ]



