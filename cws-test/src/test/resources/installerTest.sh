#!/bin/bash

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo $ROOT


cd $ROOT/../../../

# Clean up old dir
rm -rf newCWSDir

mkdir newCWSDir
cd newCWSDir/
mkdir console-only 

cd $ROOT/../../../../dist/

cp cws_server.tar.gz ../cws-test/newCWSDir/console-only/

cd $ROOT/../../../newCWSDir/console-only

tar zxf cws_server.tar.gz
cd cws

#cp $ROOT/configuration.properties ./
cp $ROOT/../../../../dist/console-only/cws/configuration.properties ./
cp $ROOT/configure_with_jacoco.sh ./

echo "CONFIGURING CWS..."
./configure_with_jacoco.sh configuration.properties
