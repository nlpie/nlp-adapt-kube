#!/bin/bash

#minikube --memory 3000 --cpus 2 start

#eval $(minikube docker-env)

docker login ahc-nlpie-docker.artifactory.umn.edu
docker images

echo "Begin uploading biomedicus image..."
docker pull ahc-nlpie-docker.artifactory.umn.edu/biomedicus 
echo "end biomedicus"

echo "Begin uploading clamp image..."
#docker pull ahc-nlpie-docker.artifactory.umn.edu/clamp 
echo "end clamp"

echo "Begin uploading ctakes image..."
#docker pull ahc-nlpie-docker.artifactory.umn.edu/ctakes 
echo "end ctakes"

echo "Begin uploading amq image..."
docker pull ahc-nlpie-docker.artifactory.umn.edu/amq 
echo "end ctakes"

echo "Begin uploading cpc image..."
docker pull ahc-nlpie-docker.artifactory.umn.edu/cpc
echo "end ctakes"

echo Begin "uploading metamap image..."
docker pull ahc-nlpie-docker.artifactory.umn.edu/mmserver	
docker pull ahc-nlpie-docker.artifactory.umn.edu/mmannotator
docker pull ahc-nlpie-docker.artifactory.umn.edu/wsd
docker pull ahc-nlpie-docker.artifactory.umn.edu/medpost 
echo "end metamap"

echo "Begin uploading amicus image..."
#docker pull ahc-nlpie-docker.artifactory.umn.edu/amicus 
echo "end amicus"




