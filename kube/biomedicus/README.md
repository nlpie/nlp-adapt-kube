# University of Minnesota NLP-ADAPT (Artifact Discovery and Preparation Toolkit)

## Experiments in creating Dockerized containers utilizing:

1. Build Docker image named gms/biomedicus to run BioMedICUS annotation engine:

`docker build -t gms/biomedicus`

NB: Be sure to modify `/path/to/bin/runCPE.sh`and `/path/to/CpeDescriptor.xml` in Dockerfile, accordingly to the type of BioMedICUS build you are using

2. To SSH into container:

`docker run -i -t --entrypoint /bin/bash gms/biomedicus`

3. Run BioMedICUS via container with shared host data:

`docker run -it gms/biomedicus /bin/bash`


_Known Issues_

* Dockerfile must be in the parent directory of where the BioMedICUS distribution is located in order to add this to the container during build of the Docker image (symlinked directories do not work).



