#!/bin/bash

#minikube --memory 3000 --cpus 2 start

#eval $(minikube docker-env)

docker login ahc-nlpie-docker.artifactory.umn.edu/biomedicus 
docker images

echo "Begin uploading biomedicus image..."
docker push ahc-nlpie-docker.artifactory.umn.edu/biomedicus 
echo "end biomedicus"

echo "Begin uploading clamp image..."
docker push ahc-nlpie-docker.artifactory.umn.edu/clamp 
echo "end clamp"

echo "Begin uploading ctakes image..."
docker push ahc-nlpie-docker.artifactory.umn.edu/ctakes 
echo "end ctakes"

echo Begin "uploading metamap image..."
docker push ahc-nlpie-docker.artifactory.umn.edu/metamap 
echo "end metamap"

echo "uploading elastic image..."
docker push ahc-nlpie-docker.artifactory.umn.edu/elastic 
echo "end elastic"

echo "Begin uploading nlpab-webapp image..."
docker push ahc-nlpie-docker.artifactory.umn.edu/nlptab 
echo "end nlptab"

echo "Begin uploading amicus image..."
docker push ahc-nlpie-docker.artifactory.umn.edu/amicus 
echo "end amicus"




