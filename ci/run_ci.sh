#!/bin/bash

HOSTNAME="localhost"

printf "\nHostname set to '%s'\n\n" $HOSTNAME

# Used in cws-test
echo "$HOSTNAME" > ../cws-test/src/test/resources/hostname.txt

SECURITY=${1} # parameter is passed as an env through workflow file

# Stop CWS is it is currently running
#./stop_dev.sh

# DB config
DB_TYPE=mariadb
DB_HOST=127.0.0.1
DB_NAME=cws_dev # needs to match the db you set up beforehand
DB_USER=root # needs to match the user you set up beforehand
DB_PASS=adminpw # could also be specified with environment vars
DB_PORT=3306 # mariadb default

USER=cwsci
CLOUD=n # Enable cloudwatch monitoring

EMAIL_LIST="/"

ADMIN_FIRST="/"
ADMIN_LAST="/"
ADMIN_EMAIL="/"

ES_PROTOCOL="HTTP"
ES_HOST="http://localhost"
ES_INDEX_PREFIX="cws-index"
ES_PORT=9200
ES_USE_AUTH=n
ES_USERNAME="na"
ES_PASSWORD="na"

# Num of workers to start. 1 is the minimum.
NUM_WORKERS=${2}  # parameter is passed as an env through workflow file

# Default value is 1. Maximum is 20. Specifies the number of days until the
# abandoned workers in the cws_workers database table are cleaned out.
WORKER_ABANDONED_DAYS=1

# Run the ci script
./ci.sh `pwd` ${USER} ${DB_TYPE} ${DB_HOST} ${DB_PORT} ${DB_NAME} ${DB_USER} ${DB_PASS} ${ES_PROTOCOL} ${ES_HOST} ${ES_INDEX_PREFIX} ${ES_PORT} ${ES_USE_AUTH} ${ES_USERNAME} ${ES_PASSWORD} ${CLOUD} ${SECURITY} ${HOSTNAME} ${EMAIL_LIST} ${ADMIN_FIRST} ${ADMIN_LAST} ${ADMIN_EMAIL} ${NUM_WORKERS} ${WORKER_ABANDONED_DAYS}
