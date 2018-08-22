#!/bin/bash

# --- RUN ---
if [ -f /data/in/export.yml ]; then
    java -jar /usr/share/amicus/amicus.jar /data/in/export.yml
else
    java -jar /usr/share/amicus/amicus.jar /usr/share/amicus/nlpie/merge_concepts.yml
fi
