FROM openjdk:8-jre

RUN apt-get update
RUN apt-get -qy install zip
RUN apt-get -qy install jq 

ADD public_mm /usr/share/public_mm
ADD apache-uima /usr/share/uima
ADD required/nlpie /usr/share/public_mm/nlpie
ADD required/scripts /usr/share/public_mm/scripts

ENV PATH=/usr/share/public_mm/bin:$PATH

WORKDIR  /usr/share/public_mm

# --- BUILD ---
RUN  /usr/share/public_mm/bin/install.sh

RUN chmod 755 /usr/share/public_mm/scripts/run_metamap.sh
RUN chmod 755 /usr/share/public_mm/scripts/setup_uima.sh

WORKDIR /data/in
