Configuration and library files to run cTAKES as a UIMA-AS service. You will need to make sure CTAKES_HOME is set before invoking a service deployment via the included scripts.

`cp -r required/ctakes-as/* $CTAKES_HOME` will copy the files to their proper homes in a standard cTAKES installation (remember that cTAKES requires a separate dictionary download as well).

`ctakes_umlsuser` and `ctakes_umlspw` will need to be set with UMLS credentials in order to start the pipeline.

A full copy of UIMA AS (2.10.3) will need to be present (usually at `/usr/share/uima` [UIMA_HOME will need to be set if you pick a different location].

