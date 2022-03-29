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
curl -LO https://downloads.camunda.cloud/release/camunda-modeler/4.1.1/camunda-modeler-4.1.1-linux-x64.tar.gz
tar zxvf *.tar.gz

rm camunda*.tar.gz

# Add elements file for custom templates
cd camunda-modeler-*-linux-x64/resources
mkdir element-templates
cd element-templates

if [[ ! -z `find "../../../" -name "elements.json"` ]]; then
    cp ../../../elements.json .
else
    cp ../../../../../cws-modeler/install/modeler-config/elements.json .
fi

cd ../..
./camunda-modeler &

echo "Done installing Camunda Modeler!"
echo
echo "Running Camunda Modeler Now!"
