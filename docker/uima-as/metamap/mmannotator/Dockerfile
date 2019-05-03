FROM openjdk:8-jre

ADD public_mm /usr/share/public_mm
ADD apache-uima /usr/share/uima
#ADD required/nlpie /usr/share/public_mm/nlpie
#COPY required/scripts /usr/share/public_mm/bin
ADD required/metamap-as /usr/share/public_mm/src/uima

ENV PATH=/usr/share/public_mm/bin:$PATH

WORKDIR  /usr/share/public_mm

RUN chmod 755 /usr/share/public_mm/src/uima/bin

# --- BUILD ---
RUN  /usr/share/public_mm/bin/install.sh

WORKDIR /data

#CMD mmserver
