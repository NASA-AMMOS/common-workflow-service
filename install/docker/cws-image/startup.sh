#!/bin/bash

javac -cp joda-time-2.1.jar getTime.java
java -cp .:joda-time-2.1.jar getTime

ls /home/cws_user/cws/server/apache-tomcat-9.0.33/logs

# Clear out any previous logs before starting (Note: Previous logs will cause CWS not to start)
rm -rf /home/cws_user/cws/server/apache-tomcat-9.0.33/logs/*

install_type=$(grep install_type config.properties | cut -d '=' -f2)
if [[ "$install_type" == 3 ]]; then
  echo "Installing 'Worker Only'.  Waiting for server to startup..."
  sleep 60
fi

cd cws
./configure.sh ../config.properties Y

echo
echo "Done with configure!"
echo

./start_cws.sh
