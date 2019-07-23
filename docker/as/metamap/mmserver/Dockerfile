FROM openjdk:8-jre

# for liveness probe
RUN apt-get update
RUN apt-get install netcat -y
RUN apt-get install dnsutils -y
RUN apt-get install procps -y

ADD public_mm /usr/share/public_mm
COPY required/scripts /usr/share/public_mm/bin

ENV PATH=/usr/share/public_mm/bin:$PATH

WORKDIR  /usr/share/public_mm

# --- BUILD ---
#RUN chmod 755 /usr/share/public_mm/bin/docker-install.sh
RUN mkdir -p /usr/share/public_mm/WSD_Server/log
RUN mkdir -p /usr/share/public_mm/MedPost-SKR/Tagger_server/log

RUN  /usr/share/public_mm/bin/install.sh
