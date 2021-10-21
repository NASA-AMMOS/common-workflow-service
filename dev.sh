#!/bin/bash
# --------
# dev.sh
# --------
# Builds, deploys, configures and runs a CWS console/worker setup locally for development.

ROOT=${1}
LDAP_USERNAME=${2}
DB_TYPE=${3}
DB_HOST=${4}
DB_PORT=${5}
DB_NAME=${6}
DB_USER=${7}
DB_PASS=${8}
ES_HOST=${9}
ES_PORT=${10}
ES_USE_UNSECURED=${11}
ES_USE_AUTH=${12}
ES_USERNAME=${13}
ES_PASSWORD=${14}
ENABLE_CLOUD_AS=${15}
SECURITY_SCHEME=${16}
THIS_HOSTNAME=${17}
NOTIFICATION_EMAILS=${18}
ADMIN_FIRSTNAME=${19}
ADMIN_LASTNAME=${20}
ADMIN_EMAIL=${21}
NUM_WORKERS=${22}

source ${ROOT}/utils.sh

# Only for the Mac users
[[ `uname -s` != "Darwin" ]] && return

print 'Building CWS libraries and creating distribution archive'
${ROOT}/build.sh

DIST=${ROOT}/dist
SERVER_DIST='cws_server.tar.gz'

# -------------------
# CONFIGURE CONSOLE
# -------------------
print "Preparing console installation files..."
mkdir -p ${DIST}/console-only
tar --directory=${DIST}/console-only -zxf ${DIST}/${SERVER_DIST}

print "Generating console installation properties..."
auto_conf_data console-only "$@" ${ROOT}/auto_conf_console.dat

print "Configuring console installation..."
${DIST}/console-only/cws/configure.sh ${ROOT}/auto_conf_console.dat Y

if [[ $? -gt 0 ]]; then
	prompt_to_continue "Error during configuration (see above)  Continue? (y/n): "
fi

# -----------------------------------------
# COPY IN DEVELOPMENT BPMN FILES
# used for development and testing purposes
# -----------------------------------------
cp ${ROOT}/install/dev/bpmn/*.bpmn ${DIST}/console-only/cws/bpmn

print "Done configuring console installation."

# --------------
# START CONSOLE
# --------------
LOG_FILE="server/apache-tomcat-${TOMCAT_VER}/logs/catalina.out"
BASE_PORT=8000

tab ${DIST}/console-only/cws "./start_cws.sh -d $BASE_PORT; tail -f $LOG_FILE"

print "Waiting for console startup..."
sleep 100

# -----------------
# CONFIGURE WORKERS
# -----------------
if [[ -z "$NUM_WORKERS" ]]; then
	NUM_WORKERS=1
fi

for ((WORKER_NUM=1; WORKER_NUM <= $NUM_WORKERS; WORKER_NUM++)); do
	print "Configuring worker $WORKER_NUM installation...";
	WORKER_TAG="worker${WORKER_NUM}"
	mkdir -p ${DIST}/${WORKER_TAG}
	tar --directory=${DIST}/${WORKER_TAG} -zxf ${DIST}/${SERVER_DIST}

	auto_conf_data ${WORKER_TAG} "$@" ${ROOT}/auto_conf.dat
	${DIST}/${WORKER_TAG}/cws/configure.sh ${ROOT}/auto_conf.dat

	if [[ $? -gt 0 ]]; then
		prompt_to_continue "Error during configuration (see above)  Continue? (y/n): "
	fi
done

# -------------
# START WORKERS
# -------------
for ((WORKER_NUM=1; WORKER_NUM <= $NUM_WORKERS; WORKER_NUM++)); do
	sleep 5

	print "Starting worker ${WORKER_NUM}..."
	WORKER_TAG="worker${WORKER_NUM}"
	WORKER_PORT=$((BASE_PORT + WORKER_NUM))
	tab ${DIST}/${WORKER_TAG}/cws "./start_cws.sh -d $WORKER_PORT; tail -f $LOG_FILE"
done

rm -f ${ROOT}/auto_conf.dat
rm -f ${ROOT}/auto_conf_console.dat

print "Finished"
