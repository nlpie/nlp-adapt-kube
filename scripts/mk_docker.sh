#!/bin/bash

#minikube --memory 3000 --cpus 2 start

#eval $(minikube docker-env)

docker images

echo "building biomedicus container"
cd kube/biomedicus
docker build -t horcle/biomedicus .

echo "building clamp container"
cd ../clamp
docker build -t horcle/clamp .

echo "building ctakes container"
cd ../ctakes
docker build -t horcle/ctakes .

echo "building metamap container"
cd ../metamap
docker build -t horcle/metamap .

echo "building elastic container"
cd ../elastic_search
docker build -t horcle/elastic .

echo "building nlp-tab-webapp container"
cd ../nlp-tab
docker build -t horcle/nlptab .

echo "images in minikube env"
docker images



