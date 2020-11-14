#!/bin/bash
# ------------
# stop_dev.sh
# ------------
# Stops a running instance of the CWS console and a number of workers. Console and workers are expected to have been
# installed to the dist directory under the CWS root, such as by dev.sh.

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source ${ROOT}/utils.sh

NUM_WORKERS=${1}

if [[ -z "${NUM_WORKERS}" ]]; then
	NUM_WORKERS=4
fi

for ((WORKER_NUM=1; WORKER_NUM <= $NUM_WORKERS; WORKER_NUM++)); do
    WORKER_TAG="worker${WORKER_NUM}"
    STOP_SCRIPT=${ROOT}/dist/${WORKER_TAG}/cws/stop_cws.sh

    if [[ -e ${STOP_SCRIPT} ]]; then
        print "Stopping worker ${WORKER_NUM}"
        ${STOP_SCRIPT}
    fi
done

STOP_SCRIPT=${ROOT}/dist/console-only/cws/stop_cws.sh
if [[ -e ${STOP_SCRIPT} ]]; then
    print "Stopping console"
    ${STOP_SCRIPT}
fi

print "Finished"
