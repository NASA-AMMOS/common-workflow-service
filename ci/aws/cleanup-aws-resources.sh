#!/bin/bash

tag=$1

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

${ROOT}/check-aws-access.sh
export STATUS=$?
if [[ $STATUS != 0 ]]; then
  echo "AWS access does not appear to be setup.  Aborting..."
  exit 1
fi

if [ "${tag}" = "" ]; then
  read -p "What is the tag name?: " tag
  echo "tag is ${tag}"
fi

echo ${tag}

EC2_INSTANCES=`aws ec2 describe-instances --filters "Name=tag:Name,Values=m20-*-ids-pipeline-*${tag}*" "Name=key-name,Values=m2020-ids-dev" --output text --query 'Reservations[*].Instances[*].InstanceId'`
echo ${EC2_INSTANCES}
if [ "${EC2_INSTANCES}" = "" ]; then
  echo "no EC2 instances found that match m20-*-ids-pipeline-*${tag}*"
   
else
  aws ec2 describe-instances --instance-ids $EC2_INSTANCES --query 'Reservations[].Instances[].[InstanceId,InstanceType,Tags[?Key==`Name`]| [0].Value]' --output table
fi

confBucketName="m20-ids-g-conf-${tag}"
bucketName="m20-ids-g-data-${tag}"
snsName="m20-ids-g-sns-${tag}"
sqsQueueName="m20-ids-g-sqs-ids-${tag}"
sqsLandformName="m20-ids-g-sqs-landform-${tag}"
idsSqsUrl="https://sqs.us-gov-west-1.amazonaws.com/${AWS_ACCOUNT}/${sqsQueueName}"
landformSqsUrl="https://sqs.us-gov-west-1.amazonaws.com/${AWS_ACCOUNT}/${sqsLandformName}"
CWS_RDS_ID="cws-db-${tag}"
CWS_RDS_PG="rds-pg-${tag}"
EDRGEN_RDS_ID="edrgen-db-${tag}"
EDRGEN_RDS_PG="rds-edrgen-pg-${tag}"


#echo "Checking for existence of ${bucketName} ..."
export S3_DATA_EXISTS=`aws s3 ls | grep ${bucketName} | wc -l | awk '{$1=$1};1'`
if [ "${S3_DATA_EXISTS}" = "1" ]; then
  echo "S3 data bucket   = ${bucketName}"
else
  echo "S3 data bucket   = NOT FOUND"
fi

#echo "Checking for existence of ${confBucketName} ..."
export S3_CONF_EXISTS=`aws s3 ls | grep ${confBucketName} | wc -l | awk '{$1=$1};1'`
if [ "${S3_CONF_EXISTS}" = "1" ]; then
  echo "S3 conf bucket   = ${confBucketName}"
else
  echo "S3 conf bucket   = NOT FOUND"
fi

#echo "Checking for existence of RDS ${CWS_RDS_ID} ..."
export CWS_RDS_EXISTS=`aws rds describe-db-instances --db-instance-identifier ${CWS_RDS_ID} --output text --query 'DBInstances[*].DBInstanceIdentifier' | wc -l | awk '{$1=$1};1'`
if [[ "${CWS_RDS_EXISTS}" = "1" ]]; then
  echo "RDS CWS DB       = ${CWS_RDS_ID}"
else
  echo "RDS CWS DB       = NOT FOUND"
fi

#echo "Checking for existence of RDS ${EDRGEN_RDS_ID} ..."
export EDRGEN_RDS_EXISTS=`aws rds describe-db-instances --db-instance-identifier ${EDRGEN_RDS_ID} --output text --query 'DBInstances[*].DBInstanceIdentifier' | wc -l | awk '{$1=$1};1'`
if [ "${EDRGEN_RDS_EXISTS}" = "1" ]; then
  echo "RDS EDRGEN DB    = ${EDRGEN_RDS_ID}"
else
  echo "RDS EDRGEN DB    = NOT FOUND"
fi

#echo "Checking SQS queue ${sqsQueueName} ..."
export SQS_EXISTS=`aws sqs list-queues | grep ${sqsQueueName} | wc -l | awk '{$1=$1};1'`
if [ "${SQS_EXISTS}" = "1" ]; then
  echo "SQS queue        = ${sqsQueueName}"
else
  echo "SQS queue        = NOT FOUND"
fi

 
#echo "--------------------------------------------------------"
#echo "S3 conf bucket   = ${confBucketName}"
#echo "snsName          = ${snsName}"
#echo "sqsQueueName     = ${sqsQueueName}"
#echo "sqsLandformName  = ${sqsLandformName}"
#echo "idsSqsUrl        = ${idsSqsUrl}"
#echo "landformSqsUrl   = ${landformSqsUrl}"
echo "--------------------------------------------------------"

read -p "The above resources will be cleared out and deleted.  THIS CAN NOT BE UNDONE!  Continue?  (Y/N): " REPLY

if [[ $REPLY =~ $(echo "^(y|Y)$") ]]; then
    echo "Cleaning up resources..."
elif [[ $REPLY =~ $(echo "^(n|N)$") ]]; then
    exit 1
else
    printf "  ERROR: Must specify either 'Y' or 'N'.\n\n";
    exit 1
fi

#
# CLEANUP EC2 INSTANCES
#
if [ "${EC2_INSTANCES}" = "" ]; then
  echo "EC2 resources deleted already."
else
  echo "Deleting EC2 instances.. $EC2_INSTANCES"
  read -p "Continue?  (Y/N): " REPLY
  if [[ $REPLY =~ $(echo "^(y|Y)$") ]]; then
    aws ec2 terminate-instances --instance-ids $EC2_INSTANCES
  fi
fi

#
# CLEANUP S3 DATA BUCKET
#
if [ "${S3_DATA_EXISTS}" = "1" ]; then
    echo "Emptying s3://${bucketName} ..."
    aws s3 rm s3://${bucketName} --recursive
    echo "Deleting s3://${bucketName} ..."
    aws s3 rb s3://${bucketName} --force
else
  echo "S3 DATA resources deleted already."
fi

#
# CLEANUP S3 CONF BUCKET
#
if [ "${S3_CONF_EXISTS}" = "1" ]; then
    echo "Emptying s3://${confBucketName} ..."
    aws s3 rm s3://${confBucketName} --recursive
    echo "Deleting s3://${confBucketName} ..."
    aws s3 rb s3://${confBucketName} --force
else
  echo "S3 CONF resources deleted already."
fi


#
# CLEANUP RDS CWS DB and param group
#
if [ "${CWS_RDS_EXISTS}" = "1" ]; then
  echo "Deleting RDS CWS DB [${CWS_RDS_ID}] ..."
  aws rds delete-db-instance --db-instance-identifier ${CWS_RDS_ID} --skip-final-snapshot > /dev/null
  echo "Waiting for RDS instance [${CWS_RDS_ID}] to finish deleting... (this may take several minutes)"
  aws rds wait db-instance-deleted --db-instance-identifier ${CWS_RDS_ID}
  echo "RDS CWS DB [${CWS_RDS_ID}] DELETED."
  echo "Deleting RDS CWS param group [${CWS_RDS_PG}] ..."
  aws rds delete-db-parameter-group --db-parameter-group-name ${CWS_RDS_PG}
  echo "RDS CWS param group [${CWS_RDS_PG}] DELETED."
else
  echo "RDS CWS resources deleted already."
fi

echo

#
# CLEANUP RDS EDRGEN DB and param group
#
if [ "${EDRGEN_RDS_EXISTS}" = "1" ]; then
  echo "Deleting RDS EDRGEN DB [${EDRGEN_RDS_ID}] ..."
  aws rds delete-db-instance --db-instance-identifier ${EDRGEN_RDS_ID} --skip-final-snapshot > /dev/null
  echo "Waiting for RDS instance [${EDRGEN_RDS_ID}] to finish deleting... (this may take several minutes)"
  aws rds wait db-instance-deleted --db-instance-identifier ${EDRGEN_RDS_ID}
  echo "RDS EDRGEN DB [${EDRGEN_RDS_ID}] DELETED."
  echo "Deleting RDS EDRGEN param group [${EDRGEN_RDS_PG}] ..."
  aws rds delete-db-parameter-group --db-parameter-group-name ${EDRGEN_RDS_PG}
  echo "RDS EDRGEN param group [${EDRGEN_RDS_PG}] DELETED."
else
  echo "RDS EDRGEN resources deleted already."
fi


#
# CLEANUP SQS attributes
#
if [ "${SQS_EXISTS}" = "1" ]; then
  echo "SQS queue ${sqsQueueName} exists."
  aws sqs get-queue-attributes --queue-url `aws sqs get-queue-url --queue-name ${sqsQueueName} --output text` --attribute-names All

  echo "Purging SQS queue ${sqsQueueName} ..."
  aws sqs purge-queue --queue-url `aws sqs get-queue-url --queue-name ${sqsQueueName} --output text`
  echo "SQS queue ${sqsQueueName} purged."

  echo "Deleting SQS queue ${sqsQueueName} ..."
  aws sqs delete-queue --queue-url `aws sqs get-queue-url --queue-name ${sqsQueueName} --output text`
  echo "SQS queue ${sqsQueueName} deleted."
else
  echo "SQS CWS resources deleted already."
fi
