# docker run commands for NLP-ADAPT
# NB: -d runs container in daemon mode
# env variables:  _JAVA_OPTION -> sets proxt hosts; others are UMLS credentials for validating license 
# -v maps remote directory to docker filesystem

docker run -d -it -v /mnt/DataResearch/DataStageData/ed_provider_notes/:/data -v /home/gsilver1/data/QuickUMLS/data/:/data/UMLS ahc-nlpie-docker.artifactory.umn.edu/qumls:1 /home/QuickUMLS/run.sh

docker run -d -it -v /mnt/DataResearch/DataStageData/ed_provider_notes/:/data ahc-nlpie-docker.artifactory.umn.edu/b9:1 /usr/share/biomedicus/scripts/run_biomedicus.sh

docker run --env _JAVA_OPTIONS='-Dhttp.proxyHost=ctsigate.ahc.umn.edu -Dhttp.proxyPort=3128 -Dhttps.proxyHost=ctsigate.ahc.umn.edu -Dhttps.proxyPort=3128' --env  umlsUser='your user name' --env umlsPass='your password' -d -it -v /mnt/DataResearch/DataStageData/ed_provider_notes/:/data ahc-nlpie-docker.artifactory.umn.edu/clmp:2 /usr/share/clamp/scripts/run_clamp.sh

docker run -d -it -v /mnt/DataResearch/gsilver1/note_test/ed_provider_notes/:/data ahc-nlpie-docker.artifactory.umn.edu/mm:4 /usr/share/public_mm/scripts/run_metamap.sh

docker run --env _JAVA_OPTIONS='-Dhttp.proxyHost=ctsigate.ahc.umn.edu -Dhttp.proxyPort=3128 -Dhttps.proxyHost=ctsigate.ahc.umn.edu -Dhttps.proxyPort=3128' --env   ctakes_umlsuser'your user name' --env  ctakes_umlspw=='your password' -d -it -v /mnt/DataResearch/DataStageData/ed_provider_notes/:/data ahc-nlpie-docker.artifactory.umn.edu/ctks:1 /usr/share/ctakes/scripts/run_ctakes.sh
