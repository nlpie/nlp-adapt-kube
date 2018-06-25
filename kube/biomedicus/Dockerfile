FROM openjdk:8-jre

RUN apt-get update
RUN apt-get -qy install zip

# ADD biomedicus-distribution /usr/share/biomedicus
ADD biomedicus-distribution-2.0.0-release /usr/share/biomedicus
ADD required/nlpie /usr/share/biomedicus/nlpie

RUN echo 'export BIOMEDICUS_HOME=/usr/share/biomedicus\nDATA_IN=/data/in\nBIOMEDICUS_OUT=/data/biomedicus_out' >  ~/.bashrc

WORKDIR /data/in

# Modify /path/to/CpeDescriptor.xml in second argument; make sure inout and output data point to virtual mounted data directory
# ENTRYPOINT ["/usr/share/biomedicus/bin/runCPE.sh", "/usr/share/biomedicus/bin/biomedicus/nlpie/PlainTextCPM_nlpie.xml" ]


