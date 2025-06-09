#!/usr/bin/env bash

ver='2.6.0'    # update this each CWS release

# Rebuild cws tar-ball
ROOT=$(pwd)
cd ../../..
./build.sh

cd $ROOT

CWS_PACKAGE=../../../dist/cws_server.tar.gz

if [ ! -f "$CWS_PACKAGE" ]; then
  echo "Error: Build package not found."
  echo "Need to build CWS package first: run './build.sh' in root dir to build cws_server.tar.gz."
  exit 1
fi

cp "$CWS_PACKAGE" .
cp ../../../cws-core/cws-core-libs/joda-time-2.1.jar .
cp -r ../../../cws-certs .

echo "Building CWS docker image.  Version = $ver"

docker build -t nasa-ammos/common-workflow-service:$ver .

rm cws_server.tar.gz
rm joda-time-2.1.jar
rm -rf cws-certs

echo
echo "Done building!"
echo
