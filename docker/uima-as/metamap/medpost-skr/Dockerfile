FROM openjdk:8-jre

RUN mkdir /usr/share/public_mm
ADD public_mm/MedPost-SKR /usr/share/public_mm/MedPost-SKR
ADD public_mm/bin /usr/share/public_mm/bin
COPY required/scripts /usr/share/public_mm/bin

ENV PATH=/usr/share/public_mm/bin:$PATH

WORKDIR  /usr/share/public_mm

# --- BUILD ---
RUN  /usr/share/public_mm/bin/install.sh

RUN chmod 755 /usr/share/public_mm/bin/skrmedpost_foreground.in

