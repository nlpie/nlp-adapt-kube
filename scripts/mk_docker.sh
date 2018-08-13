#!/bin/bash

#minikube --memory 3000 --cpus 2 start

#eval $(minikube docker-env)

docker images

echo "Begin building biomedicus container..."
cd docker/biomedicus
docker build -t horcle/biomedicus .
echo "end biomedicus"

echo "Begin building clamp container..."
cd ../clamp
docker build -t horcle/clamp .
echo "end clamp"

echo "Begin building ctakes container..."
cd ../ctakes
docker build -t horcle/ctakes .
echo "end ctakes"

echo Begin "building metamap container..."
cd ../metamap
docker build -t horcle/metamap .
echo "end metamap"

echo "building elastic container..."
cd ../elastic_search
docker build -t horcle/elastic .
echo "end elastic"

echo "Begin building nlp-tab-webapp container..."
cd ../nlp-tab
docker build -t horcle/nlptab .
echo "end nlptab"

echo "Begin building amicus container..."
cd ../amicus
docker build -t horcle/amicus .
echo "end amicus"

echo "List images in minikube env"
docker images



