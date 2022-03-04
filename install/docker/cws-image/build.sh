#!/usr/bin/env bash

ver='2.2.0'    # update this each CWS release

echo
echo "ATTENTION: If changes were made to the source code or config files, don't forget to rebuild the cws .tar.gz package file before running this script."
echo

CWS_PACKAGE=../../../dist/cws_server.tar.gz

if [ ! -f "$CWS_PACKAGE" ]; then
  echo "Error: Build package not found."
  echo "Need to build CWS package first: run './build.sh' in root dir to build cws_server.tar.gz."
  exit 1
fi

cp "$CWS_PACKAGE" .
cp ../../../cws-core/cws-core-libs/joda-time-2.1.jar .

echo "Building CWS docker image.  Version = $ver"

docker build -t nasa-ammos/common-workflow-service:$ver .

rm cws_server.tar.gz
rm joda-time-2.1.jar

echo
echo "Done building!"
echo
