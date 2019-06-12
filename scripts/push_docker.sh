#!/bin/bash

#minikube --memory 3000 --cpus 2 start

#eval $(minikube docker-env)

docker login ahc-nlpie-docker.artifactory.umn.edu 
docker images

echo "Begin uploading biomedicus image..."
docker push ahc-nlpie-docker.artifactory.umn.edu/biomedicus 
echo "end biomedicus"

echo "Begin uploading clamp image..."
#docker push ahc-nlpie-docker.artifactory.umn.edu/clamp 
echo "end clamp"

echo "Begin uploading ctakes image..."
#docker push ahc-nlpie-docker.artifactory.umn.edu/ctakes 
echo "end ctakes"

echo Begin "uploading metamap image..."
docker push ahc-nlpie-docker.artifactory.umn.edu/mmserver 
docker push ahc-nlpie-docker.artifactory.umn.edu/mmannotator
docker push ahc-nlpie-docker.artifactory.umn.edu/wsd
docker push ahc-nlpie-docker.artifactory.umn.edu/medpost 
echo "end metamap"

echo "uploading client image..."
docker push ahc-nlpie-docker.artifactory.umn.edu/cpc 
echo "end client"

echo "Begin uploading amq image..."
docker push ahc-nlpie-docker.artifactory.umn.edu/amq
echo "end amq"





