#!/bin/bash

javac -cp joda-time-2.1.jar getTime.java
java -cp .:joda-time-2.1.jar getTime

echo "TOMCAT Version: ${TOMCAT_VERSION}"

ls /home/cws_user/cws/server/apache-tomcat-${TOMCAT_VERSION}/logs

# Clear out any previous logs before starting (Note: Previous logs will cause CWS not to start)
rm -rf /home/cws_user/cws/server/apache-tomcat-${TOMCAT_VERSION}/logs/*

cd cws
./configure.sh ../config.properties Y

echo
echo "Done with configure!"
echo

./start_cws.sh
