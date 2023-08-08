#!/bin/sh
# wait until MySQL is really available
maxcounter=300
 
echo DB_HOST = $DB_HOST
echo DB_USER = $DB_USER
echo DB_PW = $DB_PW
echo ES_PROTOCOL = $ES_PROTOCOL
echo ES_HOST = $ES_HOST
echo ES_PORT = $ES_PORT

counter=1
while ! mysql --host="${DB_HOST}" --user="${DB_USER}" --password="${DB_PW}" -e "SHOW DATABASES;" > /dev/null 2>&1; do
    sleep 1
    counter=`expr $counter + 1`
    echo "Retry wait for DB: $counter"
    if [ $counter -gt $maxcounter ]; then
        >&2 echo "We have been waiting for MySQL too long already; failing."
        exit 1
    fi;
done

while ! curl -s "${ES_PROTOCOL}://${ES_HOST}:${ES_PORT}"; do
	sleep 1
	echo "Retry wait for ES"
done

>&2 echo "MariaDb and ES are up..."

# If starting a worker only, wait for console to startup
install_type=$(grep install_type config.properties | cut -d '=' -f2)
if [[ "$install_type" == 3 ]]; then
  echo "Waiting for console to startup..."
  cws_console_host=$(grep cws_console_host config.properties | cut -d '=' -f2)
  cws_console_ssl_port=$(grep cws_console_ssl_port config.properties | cut -d '=' -f2)
  while ! curl -k -s "https://${cws_console_host}:${cws_console_ssl_port}/cws-ui/login" > /dev/null 2>&1; do
    sleep 5
    echo "Retry wait for console"
  done
  echo "Console is running!"
fi

# Start app
>&2 echo "Executing startup.sh..."

./startup.sh

# Start app
>&2 echo "CWS is up!!"

exec "$@"

tail -f cws/server/apache-tomcat-*/logs/cws.log
