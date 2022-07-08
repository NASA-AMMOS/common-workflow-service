#!/bin/bash

javac -cp joda-time-2.1.jar getTime.java
java -cp .:joda-time-2.1.jar getTime

ls /home/cws_user/cws/server/apache-tomcat-9.0.33/logs

# Clear out any previous logs before starting (Note: Previous logs will cause CWS not to start)
rm -rf /home/cws_user/cws/server/apache-tomcat-9.0.33/logs/*

install_type=$(grep install_type config.properties | cut -d '=' -f2)
if [[ "$install_type" == 3 ]]; then
  echo "Waiting for console to startup..."
  cws_console_host=$(grep cws_console_host config.properties | cut -d '=' -f2)
  cws_console_ssl_port=$(grep cws_console_ssl_port config.properties | cut -d '=' -f2)
  while ! curl -k -s "https://${cws_console_host}:${cws_console_ssl_port}/cws-ui/login" > /dev/null 2>&1; do
    sleep 5
    echo "Retry wait for console"
  done
  echo "Console is now running!"
fi

cd cws
./configure.sh ../config.properties Y

echo
echo "Done with configure!"
echo

./start_cws.sh
