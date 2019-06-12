Configuration and library files to run CLAMP as a UIMA-AS service. You will need to make sure CLAMP_HOME is set before invoking a service deployment via the included scripts.

`cp -r required/clamp-as/* $CLAMP_HOME` will copy the files to their proper homes in a standard CLAMP UIMA-AS installation (special copy requested from UTH).

`umls_user` and `umls_pass` will need to be set with UMLS credentials in order to start the pipeline.
