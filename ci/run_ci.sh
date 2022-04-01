#!/bin/bash

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

HOSTNAME=localhost

# Used in cws-test
#echo "$HOSTNAME" > ${ROOT}/../cws-test/src/test/resources/hostname.txt

SECURITY="CAMUNDA"

# Stop CWS is it is currently running
#./stop_dev.sh

# DB config
DB_TYPE=
DB_HOST=
DB_NAME= # needs to match the db you set up beforehand
DB_USER= # needs to match the user you set up beforehand
DB_PASS= # could also be specified with environment vars
DB_PORT= # mariadb default

USER=
CLOUD=n # Enable cloudwatch monitoring

EMAIL_LIST=""

ADMIN_FIRST=""
ADMIN_LAST=""
ADMIN_EMAIL=""



ES_HOST=""
ES_PORT=9200
ES_USE_AUTH=n
ES_USERNAME="na"
ES_PASSWORD="na"

# Num of workers to start. 1 is the minimum.
NUM_WORKERS=1

# Default value is 1. Maximum is 20. Specifies the number of days until the
# abandoned workers in the cws_workers database table are cleaned out.
WORKER_ABANDONED_DAYS=1

# Run the dev script
./ci.sh `pwd` ${USER} ${DB_TYPE} ${DB_HOST} ${DB_PORT} ${DB_NAME} ${DB_USER} ${DB_PASS} ${ES_HOST} ${ES_PORT} ${ES_USE_AUTH} ${ES_USERNAME} ${ES_PASSWORD} ${CLOUD} ${SECURITY} ${HOSTNAME} ${NOTIFICATION_EMAILS} ${ADMIN_FIRST} ${ADMIN_LAST} ${ADMIN_EMAIL} ${NUM_WORKERS} ${WORKER_ABANDONED_DAYS}
