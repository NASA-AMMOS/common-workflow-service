#!/bin/bash
# -------------------
# upgrade_2.3_to_2.4.sh
# -------------------
# Upgrade CWS-2.3 infrastructure/database to CWS-2.4
ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"



read -p "Would you like to upgrade the CWS DB Schema for CWSv2.3 to CWSv2.4     (Y/N): " UPGRADE_DB
while [[ ! ${UPGRADE_DB} =~ $(echo "^(y|Y|n|N)$") ]]; do
  print "  ERROR: Must specify either 'Y' or 'N'.";
  read -p "Continue? (Y/N): " UPGRADE_DB
done



if [[ ${UPGRADE_DB} =~ $(echo "^(y|Y)$") ]]
then


read -p 'DB HOST: ' DB_HOST
read -p 'DB Name: ' DB_NAME
read -p 'DB Port: ' DB_PORT
read -p 'DB Username: ' DB_USERNAME
read -sp 'DB Password: ' DB_PASSWORD

echo " "



# ----------------------------------------
echo " "
echo "Database configuration is:"
echo "  DB HOST:   ${DB_HOST}"
echo "  DB NAME:   ${DB_NAME}"
echo "  DB PORT:   ${DB_PORT}"
echo "  DB USERNAME:   ${DB_USERNAME}"
echo "  DB PASSWORD:   ${DB_PASSWORD}"

echo " the ROOT is ${ROOT}"

echo " "

#mysql --defaults-file=my.cnf ${dbname} < sql/cws/upgrade.sql


echo "[mysql]" > ${ROOT}/myupgrade.cnf
echo "host=${DB_HOST}" >> ${ROOT}/myupgrade.cnf
echo "user=\"${DB_USERNAME}\"" >> ${ROOT}/myupgrade.cnf
echo "password=\"${DB_PASSWORD}\"" >> ${ROOT}/myupgrade.cnf
chmod 644 ${ROOT}/myupgrade.cnf

pwd
ls

cat ${ROOT}/myupgrade.cnf

echo "Checking whether database ${DB_NAME} already exists..."
RES=`mysql --defaults-file=${ROOT}/myupgrade.cnf -e "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '${DB_NAME}'"`

if [[ $? -gt 0 ]]; then
  echo "ERROR: Problem checking for database. "
  echo "  Please check your database configuration, and try again."
  rm -f ${ROOT}/myupgrade.cnf
  exit 1
fi
echo "  Database ${DB_NAME} exists."



echo "Upgrading tables in CWS CORE DB..."
mysql --defaults-file=${ROOT}/myupgrade.cnf ${DB_NAME} < ${ROOT}/upgrade_core_db.sql

if [[ $? -gt 0 ]]; then
    echo "ERROR: Problem creating upgrading tables."
    echo "  Please check your database upgrade script '${ROOT}/upgrade_core_db.sql', and try again."
    rm -rf ${ROOT}/myupgrade.cnf
    exit 1
fi


else
		echo "  Upgrade to CWS DB was NOT made."
fi
