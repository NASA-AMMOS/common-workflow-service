#!/bin/bash

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source ${ROOT}/../utils.sh

BUILD_PROFILE="core"

print "Cleaning and installing code..."
cd ${ROOT}/..

#echo "Running Maven build from directory:" `pwd`
#mvn -Dmaven.compiler.debug=true -Dmaven.compiler.debuglevel=lines,vars,source -DskipTests -Dskip.integration.tests -s ${ROOT}/settings.xml clean install -P ${BUILD_PROFILE}
mvn -DskipTests -Dskip.integration.tests clean install -P ${BUILD_PROFILE}

# Build distribution
print "Creating server distribution..."
${ROOT}/../create_server_dist.sh

print "Finished"
