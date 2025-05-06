#!/usr/bin/env bash
set -e # Exit immediately if a command exits with a non-zero status.

# Get version from utils.sh
ROOT=$(pwd)
cd ../../.. # Change to project root
source utils.sh # Source utils.sh from project root
ver=$CWS_VER    # use version from utils.sh

# Rebuild cws tar-ball
./build.sh # Execute build.sh from project root

cd $ROOT

CWS_PACKAGE=../../../dist/cws_server.tar.gz

if [ ! -f "$CWS_PACKAGE" ]; then
  echo "Error: Build package not found."
  echo "Need to build CWS package first: run './build.sh' in root dir to build cws_server.tar.gz."
  exit 1
fi

cp "$CWS_PACKAGE" .
cp ../../../cws-core/cws-core-libs/joda-time-2.1.jar .
# Copy the certs directory from the project root into the build context
cp -R ../../../cws-certs .
# Copy utils.sh for setup_test_env.sh to use
cp ../../../utils.sh .

echo "Building CWS docker image.  Version = $ver"

docker build -t nasa-ammos/common-workflow-service:$ver .

rm cws_server.tar.gz
rm joda-time-2.1.jar
# Remove the copied certs directory
rm -rf cws-certs
# Remove the copied utils.sh
rm utils.sh

echo
echo "Done building!"
echo
