FROM openjdk:8-jre

RUN apt-get update
RUN apt-get -qy install maven

ADD amicus-distribution /usr/share/amicus

COPY required/TypeSystems /usr/share/amicus/TypeSystems

WORKDIR /usr/share/amicus

RUN ./build.sh

WORKDIR /data/in

