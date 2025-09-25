#!/bin/bash

javac getTime.java
java getTime

ls /home/cws_user/cws/server/apache-tomcat-10.1.43/logs

# Clear out any previous logs before starting (Note: Previous logs will cause CWS not to start)
rm -rf /home/cws_user/cws/server/apache-tomcat-10.1.43/logs/*

cd cws
./configure.sh ../config.properties Y

echo
echo "Done with configure!"
echo

./start_cws.sh
