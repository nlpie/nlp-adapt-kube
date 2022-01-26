biomedicus deploy --download-data &

REMOTEHOST=127.0.0.1
DEPLOYPORT=50100
TIMEOUT=30

until 
	nc -z $REMOTEHOST $DEPLOYPORT 
do 
	echo sleep && sleep 1;
done 

biomedicus run --include-label-text /data/data_in /data/data_out
