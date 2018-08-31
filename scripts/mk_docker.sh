#!/bin/bash

#minikube --memory 3000 --cpus 2 start

#eval $(minikube docker-env)

docker images

echo "Begin building biomedicus image..."
cd docker/biomedicus
docker build -t ahc-nlpie-docker.artifactory.umn.edu/biomedicus .
echo "end biomedicus"

echo "Begin building clamp image..."
cd ../clamp
docker build -t ahc-nlpie-docker.artifactory.umn.edu/clamp .
echo "end clamp"

echo "Begin building ctakes image..."
cd ../ctakes
docker build -t ahc-nlpie-docker.artifactory.umn.edu/ctakes .
echo "end ctakes"

echo Begin "building metamap image..."
cd ../metamap
docker build -t ahc-nlpie-docker.artifactory.umn.edu/metamap .
echo "end metamap"

echo "building elastic image..."
cd ../elastic_search
docker build -t ahc-nlpie-docker.artifactory.umn.edu/elastic .
echo "end elastic"

echo "Begin building nlp-tab-webapp image..."
cd ../nlp-tab
docker build -t ahc-nlpie-docker.artifactory.umn.edu/nlptab .
echo "end nlptab"

echo "Begin building amicus image..."
cd ../amicus
docker build -t ahc-nlpie-docker.artifactory.umn.edu/amicus .
echo "end amicus"

echo "List images in minikube env"
docker images



