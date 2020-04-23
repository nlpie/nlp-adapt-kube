# build image for glove300 embeddings
# to build a particular target, speficy:
# DOCKER_BUILDKIT=1 docker build -t <imagename> --target <target> .

FROM continuumio/anaconda3 AS qumls

RUN apt-get update
RUN apt-get -qy install vim
RUN apt-get -qy install build-essential

COPY QuickUMLS /home/QuickUMLS

WORKDIR /home/QuickUMLS

RUN pip install quickumls
RUN pip install -r requirements.txt

RUN python -m spacy download en

RUN ./setup_simstring.sh 3


