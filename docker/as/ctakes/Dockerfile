FROM openjdk:8-jre

ADD apache-ctakes-4.0.1-SNAPSHOT /usr/share/ctakes
ADD apache-uima-as-2.10.3 /usr/share/uima
COPY required/ctakes-as /usr/share/ctakes

WORKDIR /usr/share/ctakes

#CMD ["./deployCtakesPipeline.sh"]
