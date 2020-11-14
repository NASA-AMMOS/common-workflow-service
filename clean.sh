#!/bin/bash

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source ${ROOT}/utils.sh

print 'Running Maven clean target'
mvn clean

print "Removing existing distribution"
rm -rf ${ROOT}/dist/*

print "Finished"
