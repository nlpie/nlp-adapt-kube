#!/bin/bash

export REPO_NAME="ahc-nlpie-docker"
export ARTIFACTORY_HOST=artifactory.umn.edu

# https://github.com/docker/for-mac/issues/1540#issuecomment-304280880
#docker login ${REPO_NAME}.${ARTIFACTORY_HOST}

docker push ${REPO_NAME}.${ARTIFACTORY_HOST}/biomedicus
docker push ${REPO_NAME}.${ARTIFACTORY_HOST}/ctakes
docker push ${REPO_NAME}.${ARTIFACTORY_HOST}/clamp
docker push ${REPO_NAME}.${ARTIFACTORY_HOST}/metamap
docker push ${REPO_NAME}.${ARTIFACTORY_HOST}/nlptab
docker push ${REPO_NAME}.${ARTIFACTORY_HOST}/elastic
docker push ${REPO_NAME}.${ARTIFACTORY_HOST}/amicus

# to use private repo in K8s: https://blog.cloudhelix.io/using-a-private-docker-registry-with-kubernetes-f8d5f6b8f646

