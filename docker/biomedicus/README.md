# University of Minnesota NLP-ADAPT (Artifact Discovery and Preparation Toolkit)

## Experiments in creating Dockerized containers utilizing:

1. Drop biomedicus build file in `<path to nlp-adapt-kube>/docker/biomedicus`

2. Build Docker image named gms/biomedicus to run BioMedICUS annotation engine:

`cd <path to nlp-adapt-kube>`

`cd docker/biomedicus`

`docker build -t gms/biomedicus .` (NB: need `.`,  gms/biomedicus is the tag/name of the image)

3. To SSH into container:

`docker run -i -t --entrypoint /bin/bash gms/biomedicus`

4. Run BioMedICUS via container with shared host data:

`docker run -it -v <host_dir_path>:/data gms/biomedicus /bin/bash`


_Known Issues_

* Dockerfile must be in the parent directory of where the BioMedICUS distribution is located in order to add this to the container during build of the Docker image (symlinked directories do not work).



