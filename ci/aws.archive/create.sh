#!/bin/bash

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $ROOT

#confBucketName="cws-dev"
tag=$1
numWorkers=$2

if [ "${tag}" = "" ]; then
  echo "Must specify a tag name!"
  echo "USAGE:"
  echo "  run.sh <tag_name> <number_of_workers>"
  exit 1
fi

if [ "${numWorkers}" = "" ]; then
  echo "Must specify number of workers!"
  echo "USAGE:"
  echo "  run.sh <tag_name> <number_of_workers>"
  exit 1
fi


#
# UPLOAD NECESSARY CONFIGURATION TO S3 BUCKET
#
#echo "Copying over files to S3 bucket ${confBucketName} ..."
#aws s3 cp ./aws/s3/${confBucketName}/console_conf.template s3://$confBucketName
#aws s3 cp ./aws/s3/${confBucketName}/worker_conf.template s3://$confBucketName
#aws s3 cp ./aws/s3/${confBucketName}/cws-process-initiators.xml s3://$confBucketName
#aws s3 cp ./aws/s3/${confBucketName}/test.bpmn s3://$confBucketName
#aws s3 cp ./aws/s3/${confBucketName}/snippets.java s3://$confBucketName
#aws s3 cp ./aws/s3/${confBucketName}/cws-adaptation.properties s3://$confBucketName
#echo "Done copying files to S3 bucket ${confBucketName} ."

# ------------------------------------------
# CREATE CLOUD FLEET USED FOR TESTING
#
cd cloud_provisioning
echo Y > auto_input.dat
echo Y >> auto_input.dat
echo $tag >> auto_input.dat
echo $numWorkers >> auto_input.dat
echo Y >> auto_input.dat

cat auto_input.dat
echo "------------------------------"
echo "Creating test fleet on AWS... THIS MAY TAKE ABOUT 10 minutes..."
./create_cloud_fleet.sh < ./auto_input.dat
rm ./auto_input.dat
cd ..
echo "Done creating test fleet on AWS."
#--------------------------------------------


CWS_CONSOLE_HOST=`cat ./cloud_provisioning/fleets/$tag/console_hostname.txt`
DB_HOSTNAME=`cat ./cloud_provisioning/fleets/$tag/db_hostname.txt`
echo "CONSOLE HOSTNAME = ${CWS_CONSOLE_HOST}"
echo "DB HOSTNAME      = ${DB_HOSTNAME}"

