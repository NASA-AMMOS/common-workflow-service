#!/bin/bash
# ------------
# launch_es.sh
# ------------
# Launches an instance of Elasticsearch.

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source ${ROOT}/utils.sh

ES_VER=$1
export JAVA_HOME="$2"

print "ulimit = `ulimit -u`"
print "Starting up Elasticsearch (version ${ES_VER})..."

${ROOT}/server/elasticsearch-${ES_VER}/bin/elasticsearch

print "Now running $(ps -ef | grep [e]lasticsearch | grep -v grep |wc -l) Elasticsearch processes"
