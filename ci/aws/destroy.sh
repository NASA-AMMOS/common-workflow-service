#!/bin/bash

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $ROOT

export TF_LOG=TRACE
export TF_LOG_PATH=./terraform.log

read -p "You must re-run awscreds.  Did you do that?  (Y/N): " REPLY
	
if [[ $REPLY =~ $(echo "^(y|Y)$") ]]; then
    echo "Cool!"
elif [[ $REPLY =~ $(echo "^(n|N)$") ]]; then
    echo "Please run awscreds, and source the environment values, then re-run this script."
    exit 1
else
    printf "  ERROR: Must specify either 'Y' or 'N'.\n\n";
    exit 1
fi

tag=$1

if [ "${tag}" = "" ]; then
  echo "Must specify a tag name!"
  echo "USAGE:"
  echo "destroy.sh <tag_name>"
  exit 1
fi

export PROMPT=Y


sleep 10

#
# CWS (DELETE)
#
cd ${ROOT}/aws_provisioning/cws/aws_state/${tag}
terraform init
terraform destroy -auto-approve
echo $?
cd ${ROOT}
rm -rf ${ROOT}/aws_provisioning/cws/aws_state/${tag}
rm ${ROOT}/aws_provisioning/cws/terraform.tfstate*
exit 0

#echo $tag > auto_input.dat
#
#echo "RUNNING destroy_cloud_fleet.sh in `pwd`..."
#./destroy_cloud_fleet.sh Y < auto_input.dat
#rm auto_input.dat

