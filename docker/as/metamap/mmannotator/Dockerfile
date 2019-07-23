FROM openjdk:8-jre

# for liveness probe
RUN apt-get update
RUN apt-get install netcat -y
RUN apt-get install dnsutils -y
RUN apt-get install procps -y

ADD public_mm /usr/share/public_mm
ADD apache-uima /usr/share/uima
COPY required/metamap-as /usr/share/public_mm/src/uima

ENV PATH=/usr/share/public_mm/bin:$PATH

WORKDIR  /usr/share/public_mm

RUN chmod 755 /usr/share/public_mm/src/uima/bin

# --- BUILD ---
RUN  /usr/share/public_mm/bin/install.sh

