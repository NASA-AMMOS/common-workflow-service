#!/bin/bash

#Make sure this is sourced
. /root/.bashrc

# Run the dev script with expect to automatically answer prompts
ROOT=`pwd`
source ${ROOT}/utils.sh

DIST=${ROOT}/dist
SERVER_DIST='cws_server.tar.gz'

# Setup ENV, use defaults if not defined
THIS_HOSTNAME=${THIS_HOSTNAME:-cws-dev}
LDAP_USERNAME=${LDAP_USERNAME:-nobody}
DB_TYPE=${DB_TYPE:-maridb}
DB_HOST=${DB_HOST:-db}
DB_PORT=${DB_PORT:-3306}
DB_NAME=${DB_NAME:-cws}
DB_USER=${DB_USER:-root}
DB_PASS=${DB_PASS:-changeme}
ES_PROTOCOL=${ES_PROTOCOL:-http}
ES_HOST=${ES_HOST:-es}
ES_INDEX_PREFIX=${ES_INDEX_PREFIX:-cws}
ES_PORT=${ES_PORT:-9200}
ES_USE_AUTH=${ES_USE_AUTH:-n}
ES_USERNAME=${ES_USERNAME:-elastic}
ES_PASSWORD=${ES_PASSWORD:-password}
ENABLE_CLOUD_AS=${ENABLE_CLOUD_AS:-0}
SECURITY_SCHEME=${SECURITY_SCHEME:-ldap}
NOTIFICATION_EMAILS=${NOTIFICATION_EMAILS:-nobody@jpl.nasa.gov}
ADMIN_FIRSTNAME=${ADMIN_FIRSTNAME:-Docker}
ADMIN_LASTNAME=${ADMIN_LASTNAME:-Testing}
ADMIN_EMAIL=${ADMIN_EMAIL:-nobody@jpl.nasa.gov}
NUM_WORKERS=${NUM_WORKERS:-1}
WORKER_MAX_NUM_RUNNING_PROCS=${WORKER_MAX_NUM_RUNNING_PROCS:-2}
WORKER_ABANDONED_DAYS=${WORKER_ABANDONED_DAYS:-1}
BASE_PORT=${BASE_PORT:-38080}
LOG_FILE=${LOG_FILE:-${DIST}/console-and-worker/cws/logs/catalina.out}

mkdir -p $(dirname $LOG_FILE)

# Stop CWS is it is currently running
./stop_dev.sh

EMAIL_LIST="nobody@jpl.nasa.gov"
ADMIN_FIRST="Docker"
ADMIN_LAST="Testing"
ADMIN_EMAIL="nobody@jpl.nasa.gov"

# -------------------
# CONFIGURE CONSOLE
# -------------------
print "Preparing console installation files..."
mkdir -p ${DIST}/console-and-worker
tar --directory=${DIST}/console-and-worker -zxf ${DIST}/${SERVER_DIST}

# -------------------
# CONFIGURE Keystore
# -------------------
# Assumes you ran cws-certs/generate-certs.sh before build.sh
mkdir ~/.cws/
chmod 700 ~/.cws/
echo "changeit" > ~/.cws/creds
chmod 600 ~/.cws/creds

print "Generating console installation properties..."
auto_conf_data console-and-worker ${ROOT} \
    ${LDAP_USERNAME} \
    ${DB_TYPE} \
    ${DB_HOST} \
    ${DB_PORT} \
    ${DB_NAME} \
    ${DB_USER} \
    ${DB_PASS} \
    ${ES_PROTOCOL} \
    ${ES_HOST} \
    ${ES_INDEX_PREFIX} \
    ${ES_PORT} \
    ${ES_USE_AUTH} \
    ${ES_USERNAME} \
    ${ES_PASSWORD} \
    ${ENABLE_CLOUD_AS} \
    ${SECURITY_SCHEME} \
    ${THIS_HOSTNAME} \
    ${NOTIFICATION_EMAILS} \
    ${ADMIN_FIRSTNAME} \
    ${ADMIN_LASTNAME} \
    ${ADMIN_EMAIL} \
    ${NUM_WORKERS} \
    ${WORKER_MAX_NUM_RUNNING_PROCS} \
    ${WORKER_ABANDONED_DAYS} \
    ${ROOT}/auto_conf_console.dat

if [[ ! -f ${ROOT}/auto_conf_console.dat ]]; then
    print "ERROR: Failed to generate configuration file ${ROOT}/auto_conf_console.dat"
    exit 1
fi

print "Configuring console installation..."
${DIST}/console-and-worker/cws/configure.sh ${ROOT}/auto_conf_console.dat Y

${DIST}/console-and-worker/cws/start_cws.sh -d $BASE_PORT