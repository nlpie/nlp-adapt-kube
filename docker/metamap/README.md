Two options for building MetaMap ->

* `mm_server_annotator`: bundled `mmserver` and `mm annotator`
* `mmserver` `mmannotator`: individualized containers

NB: wsd and medpost-skr both needed for either options

--

Configuration and library files to run MetaMap as a UIMA-AS service. You will need to make sure`METAMAP_HOME` is set before invoking a service deployment via the included scripts. `MMSERVER_HOST` and `MMSERVER_PORT` can be used to control how the UIMA MetaMapApi Annotator connects to mmserver.

`cp -r required/metamap-as/* $METAMAP_HOME/src/uima` will copy the files to their proper homes in a standard MetaMap UIMA installation.

`source` the included version of the `setup_uima.sh` script to setup up all the `PATH` and `CLASSPATH` variables that the UIMA wrapper needs in order to run.

_If you are adapting these configuration files to your own project please beware of the hardcoded directory strings. They assume that the MetaMap installation is in `/usr/share`_
