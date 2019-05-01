#!/bin/bash

#minikube --memory 3000 --cpus 2 start

#eval $(minikube docker-env)

docker images

echo "Begin building biomedicus image..."
cd docker/uima-as/biomedicus
docker build -t ahc-nlpie-docker.artifactory.umn.edu/biomedicus .
echo "end biomedicus"

echo "Begin building clamp image..."
#cd ../clamp
#docker build -t ahc-nlpie-docker.artifactory.umn.edu/clamp .
#echo "end clamp"

echo "Begin building ctakes image..."
#cd ../ctakes
#docker build -t ahc-nlpie-docker.artifactory.umn.edu/ctakes .
#echo "end ctakes"

echo Begin "building mmserver image..."
cd ../metamap/mmserver
docker build -t ahc-nlpie-docker.artifactory.umn.edu/mmserver .
echo "end mmserver"

echo Begin "building mmannotator image..."
cd ../mmannotator
docker build -t ahc-nlpie-docker.artifactory.umn.edu/mmannotator .
echo "end mmannotator"

echo Begin "building wsd image..."
cd ../wsd_server
docker build -t ahc-nlpie-docker.artifactory.umn.edu/wsd .
echo "end wsd"

echo Begin "building medpost image..."
cd ../medpost-skr
docker build -t ahc-nlpie-docker.artifactory.umn.edu/medpost .
echo "end medpost"

echo "building client..."
cd ../../client
docker build -t ahc-nlpie-docker.artifactory.umn.edu/cpc .
echo "end client"

echo "Begin building amq..."
cd ../queue
docker build -t ahc-nlpie-docker.artifactory.umn.edu/amq .
echo "end nlptab"

echo "List images in minikube env"
docker images



