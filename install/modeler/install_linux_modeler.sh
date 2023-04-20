#!/bin/bash

echo
echo "Camunda Modeler Installer for Linux"
echo
echo "This will install the Camunda Modeler in this directory:"
pwd
echo

# Confirm Install
while [[ ! $REPLY =~ ^(y|Y|n|N)$ ]]; do
  read -p "Press Y to continue or N to abort. (Y/N): " REPLY
  if [[ $REPLY =~ ^(n|N)$ ]]
  then
    echo Aborting...
    exit 1
  fi

  if [[ $REPLY =~ ^(y|Y)$ ]]
  then
    break
  fi

  printf "  ERROR: Must specify either 'Y' or 'N'.\n\n";
  done

echo
echo "Downloading camunda modeler now..."
curl -LO https://downloads.camunda.cloud/release/camunda-modeler/5.7.0/camunda-modeler-5.7.0-linux-x64.tar.gz
tar zxvf *.tar.gz

rm camunda*.tar.gz

# Add elements file for custom templates
cd camunda-modeler-*-linux-x64/resources
mkdir element-templates
cd element-templates

pwd
cp ../../../elements.json .

cd ../..
./camunda-modeler &

echo "Done installing Camunda Modeler!"
echo
echo "Running Camunda Modeler Now!"
