#!/bin/bash
# -------------------
# clean_es_history.sh
# -------------------
# Cleans history entries from Elasticsearch which are older than a specified number of days.
# This script is template-based, and is filled in with the ES host/port and days to live during a CWS install.

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source ${ROOT}/utils.sh

ES_HOST='__ES_HOST__'
ES_PORT='__ES_PORT__'
DAYS_TO_LIVE=__CWS_HISTORY_DAYS_TO_LIVE__

# Zero padded days using %d instead of %e
DAYSAGO=$([[ "$(uname)" = Linux ]] && date --date="$DAYS_TO_LIVE days ago" +"%Y%m%d" || date -v-"$DAYS_TO_LIVE"d +"%Y%m%d")

#echo days ago = $DAYSAGO

ALLLINES=$(curl -s http://${ES_HOST}:${ES_PORT}/_cat/indices?v | egrep logstash)

#echo ALLLINES = $ALLLINES

print "The following indices will be deleted:"

echo "$ALLLINES" | while read LINE
do
  FORMATEDLINE=$(echo $LINE | awk '{ print $3 }' | awk -F'-' '{ print $2 }' | sed 's/\.//g')
  if [[ ! -z "$FORMATEDLINE" ]] && [[ "$FORMATEDLINE" -lt "$DAYSAGO" ]]
  then
    TODELETE=$(echo $LINE | awk '{ print $3 }')
    echo "http://$ES_HOST:$ES_PORT/$TODELETE"
  fi
done

print "Will delete in 10 seconds...  Press Ctrl+C to abort."
sleep 10

echo "$ALLLINES" | while read LINE
  do
    FORMATEDLINE=$(echo $LINE | awk '{ print $3 }' | awk -F'-' '{ print $2 }' | sed 's/\.//g')
    if [[ ! -z "$FORMATEDLINE" ]] && [[ "$FORMATEDLINE" -lt "$DAYSAGO" ]]
    then
      TODELETE=$(echo $LINE | awk '{ print $3 }')
      curl -X DELETE http://${ES_HOST}:${ES_PORT}/${TODELETE}
      sleep 1
    fi
  done
