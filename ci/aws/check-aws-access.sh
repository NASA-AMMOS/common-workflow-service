#!/bin/bash

if [[ -z "${AWS_ACCOUNT}" ]]; then
  echo "Error:  AWS_ACCOUNT env variable is not set.  Please run 'awscreds' or 'credss -a' before continuing."
  exit 1
else
  aws s3 ls > /dev/null
  export STATUS=$?
  if [[ $STATUS != 0 ]]; then
    echo "Error: AWS access does not appear to be setup.  Please run 'awscreds' or 'credss -a' before continuing."
    exit 1
  fi
fi


