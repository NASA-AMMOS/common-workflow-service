#!/bin/bash
# -------
# test.sh
# -------
# Runs the CWS unit and integration test suites. Note that at least one instance of a CWS console and worker must be
# started prior to this script for the integration tests to run correctly.

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source ${ROOT}/utils.sh

#dump_java_apps

#print "Running unit tests..."
#mvn -Dmaven.compiler.debug=true -Dmaven.compiler.debuglevel=lines,vars,source clean test jacoco:report-aggregate

dump_java_apps

print "Running integration tests..."
mvn -Dmaven.compiler.debug=true -Dmaven.compiler.debuglevel=lines,vars,source integration-test verify -DskipTests

#dump_java_apps

print "Finished"
