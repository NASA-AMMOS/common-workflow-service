#!/bin/bash
# ------------
# launch_ls.sh
# ------------
# Launches an instance of Logstash.

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source ${ROOT}/utils.sh

LS_VER=$1
export JAVA_HOME="$2"
VERBOSE=$3
export SINCEDB_DIR="${ROOT}/server/logstash-${LS_VER}/lib"
export SINCEDB_PATH="${ROOT}/server/logstash-${LS_VER}/lib"

print "Starting Logstash (version ${LS_VER})...";

chmod -R 700 ${ROOT}/server/logstash-${LS_VER}

# START LOGSTASH
if [[ ${VERBOSE} ]]; then
    ${ROOT}/server/logstash-${LS_VER}/bin/logstash -f ${ROOT}/server/logstash-${LS_VER}/cws-logstash.conf --verbose > ${ROOT}/logs/logstash_cws.log
else
    ${ROOT}/server/logstash-${LS_VER}/bin/logstash -f ${ROOT}/server/logstash-${LS_VER}/cws-logstash.conf
fi

print "Now running $(ps -ef | grep [l]ogstash | grep -v grep | wc -l) Logstash processes"
