#!/bin/bash

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $ROOT

tag=$1         # sstageg65
numWorkers=$2  # 8
params=$3      # parameter set in deployent_params directory

if [ "${tag}" = "" ]; then
  echo "Must specify a tag name!"
  echo "USAGE:"
  echo "test.sh <tag_name> <number_of_workers> <params>"
  exit 1
fi

if [ "${numWorkers}" = "" ]; then
  echo "Must specify number of workers!"
  echo "USAGE:"
  echo "test.sh <tag_name> <number_of_workers> <params>"
  exit 1
fi


if [ "${params}" = "" ]; then
  echo "Must specify params!"
  echo "USAGE:"
  echo "test.sh <tag_name> <number_of_workers> <params>"
  exit 1
fi


source $ROOT/deployment_params/${params}


#-------------------------------------------
# Check if required dependencies are present
#-------------------------------------------

pip -V >> /dev/null

if [ $? -ne 0 ]; then
  echo "Must have pip installed on system and in classpath"
  exit 1
fi

docker -v >> /dev/null

if [ $? -ne 0 ]; then
  echo "Must have docker installed on system and in classpath"
  exit 1
fi

#${ROOT}/check-aws-access.sh
#export STATUS=$?
#if [[ $STATUS != 0 ]]; then
#  echo "AWS access does not appear to be setup.  Aborting..."
#  exit 1
#fi

#${ROOT}//cleanup-aws-resources.sh ${tag}


missionPhaseNameLower=`echo "${missionPhaseName}" | tr '[:upper:]' '[:lower:]'`
AWS_S3_CONF_DIR=${ROOT}/aws/s3/bucket

echo "Running CWS automation..."
echo "             TAG      : ${tag}"
echo "       # WORKERS      : ${numWorkers}"
echo "  MISSION PHASE       : ${missionPhaseName}"
echo "  MPN (lower)         : ${missionPhaseNameLower}"
echo "  PIPELINE_NAME       : ${pipelineName}"
echo "  VENUE               : ${venue}"
echo "  FSW_VER             : ${fswVer}"
echo "  DEPLOYMENT_VENUE    : ${deploymentVenue}"
echo



# -----------------------------------------
# UPDATE THE CWS TERRAFORM CONFIG
# -----------------------------------------
cp ${ROOT}/aws_provisioning/cws/config.tf.template          ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__TAG__^${tag}^g"                              ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__AWS_ACCESS_KEY__^${AWS_ACCESS_KEY_ID}^g"     ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__AWS_SECRET_KEY__^${AWS_SECRET_ACCESS_KEY}^g" ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__TOKEN__^${AWS_SESSION_TOKEN}^g"              ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__NUM_CWS_WORKERS__^${numWorkers}^g"           ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__VENUE__^${venue}^g"                          ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__SUBNET__^${subnet}^g"                        ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__DB_SG__^${dbSg}^g"                           ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__S3_CONF_BUCKET__^${confBucketName}^g"        ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__CONSOLE_SG__^${consoleSg}^g"                 ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__WORKER_SG__^${workerSg}^g"                   ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__DB_SUBNET_1__^${dbSubnet1}^g"                ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__DB_SUBNET_2__^${dbSubnet2}^g"                ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__CONSOLE_AMI__^${consoleAmi}^g"               ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__WORKER_AMI__^${workerAmi}^g"                 ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__SECURITY_PLAN__^${securityPlan}^g"           ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__IAM_POLICY__^${iamPolicy}^g"                 ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__MISSION_PHASE_NAME__^${missionPhaseName}^g"  ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__PIPELINE_NAME__^${pipelineName}^g"           ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__VENUE__^${venue}^g"                          ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__FSW_VER__^${fswVer}^g"                       ${ROOT}/aws_provisioning/cws/config.tf
sed -ie "s^__DEPLOYMENT_VENUE__^${deploymentVenue}^g"     ${ROOT}/aws_provisioning/cws/config.tf
rm ${ROOT}/aws_provisioning/cws/config.tfe

#
# UPDATE THE UI ADAPTATION PROPERTIES
#
#cp ${AWS_S3_CONF_DIR}/cws-adaptation-ui.properties.template ${AWS_S3_CONF_DIR}/cws-adaptation-ui.properties
#sed -ie "s^__IDS_S3_DATA_BUCKET__^${bucketName}^g" ${AWS_S3_CONF_DIR}/cws-adaptation-ui.properties
#sed -ie "s^__IDS_SQS_URL__^${idsSqsUrl}^g"         ${AWS_S3_CONF_DIR}/cws-adaptation-ui.properties
#sed -ie "s^__LF_SQS_URL__^${landformSqsUrl}^g"     ${AWS_S3_CONF_DIR}/cws-adaptation-ui.properties
#rm ${AWS_S3_CONF_DIR}/cws-adaptation-ui.propertiese

#
# UPDATE THE ENGINE ADAPTATION PROPERTIES
#
#cp ${AWS_S3_CONF_DIR}/cws-adaptation-engine.properties.template ${AWS_S3_CONF_DIR}/cws-adaptation-engine.properties
#sed -ie "s^__IDS_S3_DATA_BUCKET__^${bucketName}^g" ${AWS_S3_CONF_DIR}/cws-adaptation-engine.properties
#sed -ie "s^__IDS_SQS_URL__^${idsSqsUrl}^g"         ${AWS_S3_CONF_DIR}/cws-adaptation-engine.properties
#sed -ie "s^__LF_SQS_URL__^${landformSqsUrl}^g"     ${AWS_S3_CONF_DIR}/cws-adaptation-engine.properties
#rm ${AWS_S3_CONF_DIR}/cws-adaptation-engine.propertiese

#
# UPDATE THE INITIATORS CONFIGURATION
#
#cd ${ROOT}
#echo "Updating the cws-process-initiators.xml file..."
#cp ${AWS_S3_CONF_DIR}/cws-process-initiators.xml.template ${AWS_S3_CONF_DIR}/cws-process-initiators.xml
#sed -ie "s^__IDS_S3_DATA_BUCKET__^${bucketName}^g" ${AWS_S3_CONF_DIR}/cws-process-initiators.xml
#sed -ie "s^__PIPELINE_NAME__^${pipelineName}^g"    ${AWS_S3_CONF_DIR}/cws-process-initiators.xml
#sed -ie "s^__PLACES_BASE_URL__^${placesBaseUrl}^g" ${AWS_S3_CONF_DIR}/cws-process-initiators.xml
#sed -ie "s^__OUTREACH_BUCKET_NAME__^${outreachBucketName}^g" ${AWS_S3_CONF_DIR}/cws-process-initiators.xml
#rm ${AWS_S3_CONF_DIR}/cws-process-initiators.xml

#
# UPLOAD NECESSARY CONFIGURATION TO S3 BUCKET
#
sleep 10
echo "Copying over files to S3 bucket ${confBucketName} ..."
#aws s3 cp ${AWS_S3_CONF_DIR}/cws_server.tar.gz s3://$confBucketName
aws s3 cp ${AWS_S3_CONF_DIR}/console_conf.template s3://$confBucketName
aws s3 cp ${AWS_S3_CONF_DIR}/cws-process-initiators.xml s3://$confBucketName
# UPLOAD ALL BPMN FILES...
aws s3 cp ${AWS_S3_CONF_DIR} s3://${confBucketName} --recursive --exclude '*' --include '*.bpmn'
#aws s3 cp ${AWS_S3_CONF_DIR}/snippets.java s3://$confBucketName
aws s3 cp ${AWS_S3_CONF_DIR}/worker_conf.template s3://$confBucketName
aws s3 cp ${AWS_S3_CONF_DIR}/cws-adaptation-ui.properties s3://$confBucketName
aws s3 cp ${AWS_S3_CONF_DIR}/cws-adaptation-engine.properties s3://$confBucketName
#aws s3 cp ~/.netrc s3://$confBucketName
echo "Done copying files to S3 bucket ${confBucketName} ."


# ------------------------------------------
# CREATE CLOUD FLEET USED FOR TESTING
#
cd ${ROOT}/aws_provisioning/cws
echo Y > auto_input.dat
#echo Y >> auto_input.dat
echo $tag >> auto_input.dat
echo $numWorkers >> auto_input.dat
echo Y >> auto_input.dat

cat auto_input.dat
echo "------------------------------"
echo "Creating CWS on AWS... THIS MAY TAKE ABOUT 10 MINUTES..."
./create_cloud_fleet.sh < ./auto_input.dat
rm ./auto_input.dat
cd ..
echo "Done creating CWS on AWS."
#--------------------------------------------

#
# WRITE OUT config bucket name to file
#
mkdir -p ${ROOT}/aws_provisioning/cws/aws_state/${tag}
echo ${confBucketName} > ${ROOT}/aws_provisioning/cws/aws_state/${tag}/.s3ConfBucketName.txt

CWS_CONSOLE_HOST=`cat ${ROOT}/aws_provisioning/cws/aws_state/${tag}/console_hostname.txt`
DB_HOSTNAME=`cat ${ROOT}/aws_provisioning/cws/aws_state/${tag}/db_hostname.txt`
DB_INSTANCE_ID=`cat ${ROOT}/aws_provisioning/cws/aws_state/${tag}/db_instance_id.txt`
S3_CONF_BUCKET=`cat ${ROOT}/aws_provisioning/cws/aws_state/${tag}/.s3ConfBucketName.txt`
echo "CONSOLE HOSTNAME   = ${CWS_CONSOLE_HOST}"
echo "DB HOSTNAME        = ${DB_HOSTNAME}"
echo "S3 CONF BUCKET     = ${S3_CONF_BUCKET}"



#
# GET FIRST WORKER ID FROM DATABASE
#
echo "Getting worker ID from database..."
export WORKER_ID=`mysql -s -N -u cws -h ${DB_HOSTNAME} -pmyawscw5 -e 'select id from cws_worker where name="worker0001"' cws | head -1`
while [[ $(echo ${WORKER_ID} | wc -c) -lt 2 ]]
do
  echo "WORKER_ID not available yet..."
  sleep 10
  export WORKER_ID=`mysql -s -N -u cws -h ${DB_HOSTNAME} -pmyawscw5 -e 'select id from cws_worker where name="worker0001"' cws | head -1`
done
echo "WORKER ID        = ${WORKER_ID}"

#
# REFRESH CREDENTIALS FOR CWS
#
cd $ROOT
echo "Authenticating with CWS..."
echo cws > ./creds.txt
echo changeme >> ./creds.txt

cd $ROOT
./refresh_cws_token.sh ${CWS_CONSOLE_HOST} < ./creds.txt
rm ./creds.txt

echo
echo "Updating initiators XML..."
curl --silent --output /dev/null --show-error -k -b cookies.txt -X POST https://${CWS_CONSOLE_HOST}:38443/cws-ui/rest/initiators/loadInitiatorsContextXml -u cws-:changeme

IS_FIRST_WORKER=1

for dest in $(<./aws_provisioning/cws/aws_state/${tag}/worker_hostnames.txt); do
  echo "Updating initiators for $dest ..."

  PREFIX=`echo $dest | tr "." "_"`

  #
  # WORKER ID FROM DATABASE
  #
  echo "Getting worker ID from database..."
  export WORKER_ID=`mysql -s -N -u cws -h ${DB_HOSTNAME} -pmyawscw5 -e "select id from cws_worker where id like \"${PREFIX}%\"" cws | head -1`
  echo "WORKER ID        = ${WORKER_ID}  (for ${dest})"

  if (( IS_FIRST_WORKER )); then
    echo "  Enabling coreg_inputs proc def for ${WORKER_ID} (FIRST WORKER)..."
    curl --silent --output /dev/null --show-error -k -b cookies.txt -X POST https://${CWS_CONSOLE_HOST}:38443/cws-ui/rest/worker/${WORKER_ID}/coreg_inputs/updateWorkerProcDefEnabled/true
    echo "  Enabling coreg_inputs initiator..."
    curl --silent --output /dev/null --show-error -k -b cookies.txt -X POST https://${CWS_CONSOLE_HOST}:38443/cws-ui/rest/initiators/coreg_inputs/enabled -u cws:changeme --data "enabled=true"
  fi

  for initiatorName in \
    external_pwd \
    simple_sleep_30; \
  do
    echo "  Enabling $initiatorName proc def for ${WORKER_ID}"
    curl --silent --output /dev/null --show-error -k -b cookies.txt -X POST https://${CWS_CONSOLE_HOST}:38443/cws-ui/rest/worker/${WORKER_ID}/${initiatorName}/updateWorkerProcDefEnabled/true
    echo "  Enabling $initiatorName initiator..."
    curl --silent --output /dev/null --show-error -k -b cookies.txt -X POST https://${CWS_CONSOLE_HOST}:38443/cws-ui/rest/initiators/${initiatorName}/enabled -u cws:changeme --data "enabled=true"
  done

  IS_FIRST_WORKER=0
done

echo "Done enabling proc defs and initiators."

cp ./aws_provisioning/cws/aws_state/${tag}/worker_hostnames.txt ./all_hostnames.txt
cat ./aws_provisioning/cws/aws_state/${tag}/console_hostname.txt >> ./all_hostnames.txt

cat ./all_hostnames.txt

exit 0


