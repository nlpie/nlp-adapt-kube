#!/bin/bash

# --- RUN ---
if [ -f /data/in/export.yml ]; then
    java -jar /usr/share/amicus/amicus.jar /data/in/export.yml
else
<<<<<<< HEAD
    java -jar /usr/share/amicus/amicus.jar /usr/share/amicus/nlpie/merge_concepts.yml
=======
    java -jar /usr/share/amicus.jar /home/ubuntu/amicus/nlpie/merge_concepts.yml
>>>>>>> ed5783e1aa92e15765419f229eb2ab0fceefddfd
fi
