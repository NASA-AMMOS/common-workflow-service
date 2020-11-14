#!/bin/bash

echo
echo "Camunda Modeler Installer for Mac OS"
echo

if [ ! -d "/Applications" ]; then
  echo "Error: Cannot find '/Applications' directory.  Is this a Mac OS machine?"
  echo "Exiting..."
  exit 1
fi

echo "Installing in '/Applications' directory"
echo
echo "*** Warning! ***  This will remove your old Camunda Modeler (if exists) and install the latest version..."
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

rm -rf temp
mkdir temp
cd temp

echo
echo "Downloading camunda modeler now..."
curl -LO https://downloads.camunda.cloud/release/camunda-modeler/4.1.1/camunda-modeler-4.1.1-mac.zip

unzip *.zip

rm camunda*.zip

rm -rf /Applications/camunda-modeler*
rm -rf /Applications/Camunda\ Modeler.app
mv * /Applications/

#echo
#echo "Admin privileges needed to continue..."
#sudo spctl --master-disable
#xattr -dr com.apple.quarantine /Applications/camunda-modeler/Camunda\ Modeler.app/
#sudo spctl --master-enable

# Add elements file for custom templates
cp ../../../cws-modeler/install/modeler-config/elements.json .
rm -f ~/Library/Application\ Support/camunda-modeler/resources/element-templates/elements.json
mkdir -p ~/Library/Application\ Support/camunda-modeler/resources/element-templates/
mv elements.json ~/Library/Application\ Support/camunda-modeler/resources/element-templates/

open /Applications/Camunda\ Modeler.app

cd ..
rmdir temp

echo "Done installing new modeler!"
echo
echo "Launching modeler now..."
