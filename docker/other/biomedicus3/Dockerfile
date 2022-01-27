FROM openjdk:11

RUN apt-get update
RUN apt-get -qy install zip
RUN apt-get -qy install jq 
RUN apt-get -qy install netcat
RUN apt-get -qy install build-essential


# Install miniconda
ENV CONDA_DIR /opt/conda
RUN wget --quiet https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh -O ~/miniconda.sh && \
/bin/bash ~/miniconda.sh -b -p /opt/conda

ENV PATH=$CONDA_DIR/bin:$PATH

RUN conda install pytorch torchvision torchaudio cpuonly -c pytorch

COPY biomedicus3_src /home/biomedicus3
COPY required /home/biomedicus3/scripts

WORKDIR /home/biomedicus3
RUN pip install .

RUN chmod 755 /home/biomedicus3/scripts/run-biomedicus-pipeline.sh
RUN chmod 755 /home/biomedicus3/scripts/deploy-biomedicus.sh
RUN chmod 755 /home/biomedicus3/scripts/test.sh

WORKDIR /data
