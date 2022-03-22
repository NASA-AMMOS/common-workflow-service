#!/bin/bash

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo `pwd`

echo "create_cloud_fleet.sh ..."

#
# GENERATE THE SSH KEY
#
#echo "Generating new SSH key..."
#rm -rf ./ssh
#mkdir ./ssh
#ssh-keygen -t rsa -C "insecure-deployer" -P '' -f ssh/insecure-deployer > /dev/null
#echo "Done generating new SSH key."

#
# CLEANUP FROM PREVIOUS RUNS
#
rm -f console_hostname.txt
rm -f worker_hostname.txt
rm -f db_hostname.txt

#
# IF PREVIOUS terraform.tfvars FILE EXISTS,
# PROMPT USER TO USE IT, INSTEAD OF MANUALLY ENTERING VARIABLES
#
#if [ -e "${ROOT}/terraform.tfvars" ]
#then
#  echo "A terraform variables file (terraform.tfvars) already exists."
#  while [[ ! $REPLY =~ ^(y|Y|n|N)$ ]]; do
#  read -p "Use it for configuring this cloud provisioning? (Y/N): " REPLY
#  if [[ $REPLY =~ ^(n|N)$ ]]
#  then
#    rm -f terraform.tfvars
#    touch terraform.tfvars
#
#    # PROMPT USER FOR AWS ACCESS KEY
#    #
#    printf "Enter your AWS access key:\n"
#    read -p ": " AWS_ACCESS_KEY
#    echo "accessKey = \"$AWS_ACCESS_KEY\"" >> ./terraform.tfvars
#
#    # PROMPT USER FOR AWS ACCESS KEY
#    #
#    printf "Enter your AWS secret key:\n"
#    read -p ": " AWS_SECRET_KEY
#    echo "secretKey = \"$AWS_SECRET_KEY\"" >> ./terraform.tfvars
#
#    # PROMPT USER FOR S3 BUCKET
#    #
#    printf "Enter the name of the AWS S3 bucket that holds the CWS binary and configuration to use:\n"
#    read -p ": " S3_BUCKET
#    echo "cwsS3Bucket = \"$S3_BUCKET\"" >> ./terraform.tfvars
#
#    # PROMPT USER FOR CWS ADMIN USER
#    #
#    printf "Enter the JPL username for the initial CWS login account (more users can be added later via the CWS UI):\n"
#    read -p ": " CWS_ADMIN_USER
#    echo "cwsAdminUser = \"$CWS_ADMIN_USER\"" >> ./terraform.tfvars
#  fi
#
#  if [[ $REPLY =~ ^(y|Y)$ ]]
#  then
#    break
#  fi
#
#  printf "  ERROR: Must specify either 'Y' or 'N'.\n\n";
#  done
#else
# rm -f terraform.tfvars
#    touch terraform.tfvars
#
#    # PROMPT USER FOR AWS ACCESS KEY
#    #
#    printf "Enter your AWS access key:\n"
#    read -p ": " AWS_ACCESS_KEY
#    echo "accessKey = \"$AWS_ACCESS_KEY\"" >> ./terraform.tfvars
#
#    # PROMPT USER FOR AWS ACCESS KEY
#    #
#    printf "Enter your AWS secret key:\n"
#    read -p ": " AWS_SECRET_KEY
#    echo "secretKey = \"$AWS_SECRET_KEY\"" >> ./terraform.tfvars
#
#    # PROMPT USER FOR S3 BUCKET
#    #
#    printf "Enter the name of the AWS S3 bucket that holds the CWS binary and configuration to use:\n"
#    read -p ": " S3_BUCKET
#    echo "cwsS3Bucket = \"$S3_BUCKET\"" >> ./terraform.tfvars
#
#    # PROMPT USER FOR CWS ADMIN USER
#    #
#    printf "Enter the JPL username for the initial CWS login account (more users can be added later via the CWS UI):\n"
#    read -p ": " CWS_ADMIN_USER
#    echo "cwsAdminUser = \"$CWS_ADMIN_USER\"" >> ./terraform.tfvars
#fi


echo
echo "+---------------------------+"
echo "| Creating CWS ... |"
echo "+---------------------------+"
echo

#
# PROMPT FOR CONINUATION
#
while [[ ! $REPLY2 =~ ^(y|Y|n|N)$ ]]; do
	read -p "Please review the setup details above.  Proceed with CWS cloud provisioning? (Y/N): " REPLY2
	if [[ $REPLY2 =~ ^(n|N)$ ]]
	then
		exit 1;  # abort installation process
	fi
	
	if [[ $REPLY2 =~ ^(y|Y)$ ]]
	then
		break
	fi

	printf "  ERROR: Must specify either 'Y' or 'N'.\n\n";
done

# PROMPT USER FOR TAG
#
printf "Enter the tag name (NO SPACES OR SPECIAL CHARACTERS IN NAME) to be used for the cloud fleet (machines/instances/services will be tagged with this name):\n"
read -p ": " TAG
echo "TAG IS: $TAG"

# PROMPT USER FOR NUM_WORKERS
#
printf "Enter the number of workers to create:\n"
read -p ": " NUM_WORKERS
echo "NUM_WORKERS IS: $NUM_WORKERS"

#
# PROMPT FOR CONTINUATION
#
REPLY2="X"
while [[ ! $REPLY2 =~ ^(y|Y|n|N)$ ]]; do
	read -p "The directory at ${ROOT}/aws_state/${TAG} will be permanently deleted (if exists).  Proceed (Y/N): " REPLY2
	if [[ $REPLY2 =~ ^(n|N)$ ]]
	then
		exit 1;  # abort installation process
	fi

  if [[ $REPLY2 =~ ^(y|Y)$ ]]
  then
    break
  fi

  printf "  ERROR: Must specify either 'Y' or 'N'.\n\n";
done

#echo "Removing dir ${ROOT}/aws_state/${TAG} ..."

rm -rf ${ROOT}/aws_state/${TAG}
mkdir -p ${ROOT}/aws_state/${TAG}
cd ${ROOT}/aws_state/${TAG}

cp ${ROOT}/config.tf .
#cp ${ROOT}/terraform.tfvars .
cp ${ROOT}/dest_temp ./destroy_cloud_fleet.sh

#
# RUN TERRAFORM APPLY
#
#echo "Calling 'terraform init' ..."
terraform init
#echo "Calling 'terraform apply -auto-approve' ..."
terraform apply -auto-approve

#
# PRINT OUT MACHINE ADDRESSES
#
echo
echo "+---------------------------------------------------------------------------------------------------"
echo "| CWS on AWS! "
echo "+-----------------------"
echo "|   Working directory : `pwd`"
echo "|   Console hostname  : `cat console_hostname.txt`  ( access at  http://`cat console_hostname.txt`:38080 )"
echo "|   Worker hostname(s): "
while read w; do
	echo "|                     : ${w}"
done <worker_hostnames.txt
echo "|   Database hostname : `cat db_hostname.txt`"
echo "|"
echo "+---------------------------------------------------------------------------------------------------"
echo

