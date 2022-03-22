#!/bin/bash

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source ${ROOT}/../utils.sh

LDAP_USERNAME=$2
DB_HOST=$3
DB_PORT=$4
DB_NAME=$5
DB_USER=$6
DB_PASS=$7
ENABLE_CLOUD_AS=$8
SECURITY_SCHEME=${9}
THIS_HOSTNAME=${10}
NOTIFICATION_EMAILS=${11}
ADMIN_FIRSTNAME=${12}
ADMIN_LASTNAME=${13}
ADMIN_EMAIL=${14}

# Create a file that stores local hostname
echo "$THIS_HOSTNAME" > ${ROOT}/../cws-test/src/test/resources/hostname.txt

# --------------------------------------------
# BUILD CODE AND CREATE DISTRIBUTION ARCHIVE
# --------------------------------------------
${ROOT}/ci_build.sh

DIST=${ROOT}/../dist
SERVER_DIST=cws_server.tar.gz

# -------------------
# CONFIGURE CONSOLE
# -------------------
print "Preparing console installation files..."
mkdir ${DIST}/console-only
tar --directory=${DIST}/console-only -zxf ${DIST}/${SERVER_DIST}

print "Configuring console installation......"
cp ${ROOT}/ci_console.conf.template ${ROOT}/ci_console.conf
cp ${ROOT}/ci_worker.conf.template ${ROOT}/ci_worker.conf
sed -ie "s^__CONSOLE_HOST__^${THIS_HOSTNAME}^g" ${ROOT}/ci_console.conf
sed -ie "s^__WORKER_HOST__^${THIS_HOSTNAME}^g"  ${ROOT}/ci_worker.conf
sed -ie "s^__CONSOLE_HOST__^${THIS_HOSTNAME}^g" ${ROOT}/ci_worker.conf
rm -f ${ROOT}/ci_worker.confe
rm -f ${ROOT}/ci_console.confe

${DIST}/console-only/cws/configure.sh ${ROOT}/ci_console.conf Y

if [[ $? -gt 0 ]]; then
	print "FAILED CONFIGURATION OF CONSOLE!"
	exit 1
fi

print "Done configuring console installation."

# -------------
# START CONSOLE
# -------------
${DIST}/console-only/cws/start_cws.sh -d 8000

# AMQ must be started before configuring workers
print "Waiting for console startup..."
sleep 120

# -------------------
# CONFIGURE WORKER 1
# -------------------
print "Preparing worker 1 installation files..."
mkdir ${DIST}/worker1
tar --directory=${DIST}/worker1 -zxf ${DIST}/${SERVER_DIST}

echo "Configuring worker 1 installation..."
${DIST}/worker1/cws/configure.sh ${ROOT}/ci_worker.conf Y

if [[ $? -gt 0 ]]; then
	echo "FAILED CONFIGURATION OF WORKER 1!"
	exit 1
fi

# ---------------
# START WORKER 1
# ---------------
${DIST}/worker1/cws/start_cws.sh -d 8001

print "Finished."
