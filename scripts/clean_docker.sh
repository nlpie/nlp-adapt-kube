#!/bin/bash

# for MacOs use gxargs: for 'nix use xargs  
# remove exited containers:
docker ps --filter status=dead --filter status=exited -aq | gxargs -r docker rm -v
    
# remove unused images:
docker images --no-trunc | grep '<none>' | awk '{ print $3 }' | gxargs -r docker rmi

# remove unused volumes:
docker volume ls -qf dangling=true | gxargs -r docker volume rm
