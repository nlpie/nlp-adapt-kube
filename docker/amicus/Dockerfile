FROM openjdk:8-jre

RUN apt-get update
RUN apt-get -qy install openjdk-8-jdk maven

ADD amicus-distribution /usr/share/amicus
ADD required/scripts /usr/share/amicus/scripts
COPY required/TypeSystems /usr/share/amicus/TypeSystems

WORKDIR /usr/share/amicus

# --- BUILD ---
RUN ./build.sh

RUN chmod 755 /usr/share/amicus/scripts/run_amicus.sh

WORKDIR /data/in

