#!/bin/bash
# --------
# build.sh
# --------
# Builds and packages an instance of cws_server.tar.gz under the dist directory of a CWS root

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source ${ROOT}/utils.sh

BUILD_PROFILE="core"

print "Cleaning out libs from cws-core/cws-core-libs..."
rm -f ${ROOT}/cws-core/cws-core-libs/*

print "Cleaning out libs from cws-engine-service/cws-core-libs..."
rm -f ${ROOT}/cws-engine-service/cws-core-libs/*

print "Cleaning out libs from cws-installer/cws-installer-libs..."
rm -f ${ROOT}/cws-installer/cws-installer-libs/*

print "Cleaning out libs from cws-tasks/cws-tasks-libs..."
rm -f ${ROOT}/cws-tasks/cws-tasks-libs/*

print "Cleaning out libs from cws-test/cws-test-libs..."
rm -f ${ROOT}/cws-test/cws-test-libs/*

print "Installing CWS libraries..."
mvn -DskipTests -Dskip.integration.tests clean install -P ${BUILD_PROFILE}

if [[ $? -eq 0 ]]; then
    # Build distribution
    print "Creating server distribution..."
    ${ROOT}/create_server_dist.sh ${ROOT}
else
    print "ERROR: There were one or more build errors, aborting."
    exit 1
fi

print "Finished"
