FROM openjdk:8-jre

ADD biomedicus-distribution /usr/share/biomedicus


# Modify /path/to/CpeDescriptor.xml in second argument; make sure inout and output data point to virtual mounted data directory
# ENTRYPOINT ["/nlp/biomedicus/biomedicus-distribution/target/biomedicus-distribution-1.8.4-release/bin/runCPE.sh", "/nlp/biomedicus/biomedicus-distribution/target/biomedicus-distribution-1.8.4-release/examples/plaintext/PlainTextCPM_Test.xml" ]

WORKDIR /data/in

