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

# --- Generate Certificates ---
CERT_SCRIPT="/opt/cws-certs/generate-certs-testing.sh"
if [ -f "${CERT_SCRIPT}" ]; then
  >&2 echo "Running certificate generation script: ${CERT_SCRIPT}"
  "${CERT_SCRIPT}"
  CERT_GEN_EXIT_CODE=$?
  if [ ${CERT_GEN_EXIT_CODE} -ne 0 ]; then
    >&2 echo "ERROR: Certificate generation script failed with exit code ${CERT_GEN_EXIT_CODE}. Exiting entrypoint."
    exit ${CERT_GEN_EXIT_CODE}
  fi
  >&2 echo "Certificate generation script completed successfully."
else
  >&2 echo "ERROR: Certificate generation script ${CERT_SCRIPT} not found. Exiting entrypoint."
  exit 1
fi
# --- End Certificate Generation ---

mkdir ~/.cws
echo "changeit" >> ~/.cws/creds
chmod 700 ~/.cws/
chmod 600 ~/.cws/creds

# Start app
>&2 echo "Executing startup.sh..."

./startup.sh
STARTUP_EXIT_CODE=$? # Capture exit code

if [ ${STARTUP_EXIT_CODE} -ne 0 ]; then
  >&2 echo "ERROR: startup.sh failed with exit code ${STARTUP_EXIT_CODE}. Exiting entrypoint."
  exit ${STARTUP_EXIT_CODE} # Exit entrypoint script with the same error code
fi

# Only proceed if startup.sh was successful
>&2 echo "CWS startup script completed successfully."

# The tail command will keep the container running and show logs.
# If startup.sh succeeded, Tomcat should be running and the log file should exist.
tail -f cws/server/apache-tomcat-*/logs/cws.log
