#!/bin/bash
# -------------------
# upgrade_2.3_to_2.4.sh
# -------------------
# Upgrade CWS v2.3 infrastructure/database to CWS v2.4
ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

read -p "Would you like to upgrade the CWS Database from CWS v2.3 to CWS v2.4?  (Y/N): " UPGRADE_DB
while [[ ! ${UPGRADE_DB} =~ $(echo "^(y|Y|n|N)$") ]]; do
  echo "  ERROR: Must specify either 'Y' or 'N'.";
  read -p "Continue? (Y/N): " UPGRADE_DB
done

if [[ ${UPGRADE_DB} =~ $(echo "^(y|Y)$") ]]
then
  echo " "
  echo "[Enter Your Database Configuration]"
  read -p 'DB Host: ' DB_HOST
  read -p 'DB Name: ' DB_NAME
  read -p 'DB Port: ' DB_PORT
  read -p 'DB Username: ' DB_USERNAME
  read -sp 'DB Password: ' DB_PASSWORD
  echo " "
  echo " "
  echo "Your database Configuration is:"
  echo "   DB HOST:       ${DB_HOST}"
  echo "   DB NAME:       ${DB_NAME}"
  echo "   DB PORT:       ${DB_PORT}"
  echo "   DB USERNAME:   ${DB_USERNAME}"
  echo "   DB PASSWORD:   [PASSWORD]"
  echo " "

  while [[ ! ${CREDS_DB} =~ $(echo "^(y|Y|n|N)$") ]]; do
    read -p "Do you want to continue with the database upgrade? (specify either 'Y' or 'N'):  " CREDS_DB
  done
fi

if [[ ${CREDS_DB} =~ $(echo "^(y|Y)$") ]]
then
  echo " "
  echo "[mysql]" > ${ROOT}/myupgrade.cnf
  echo "host=${DB_HOST}" >> ${ROOT}/myupgrade.cnf
  echo "user=\"${DB_USERNAME}\"" >> ${ROOT}/myupgrade.cnf
  echo "password=\"${DB_PASSWORD}\"" >> ${ROOT}/myupgrade.cnf
  echo "port=\"${DB_PORT}\"" >> ${ROOT}/myupgrade.cnf
  chmod 644 ${ROOT}/myupgrade.cnf

  echo "Checking number of CORES on machine..."

  if [[ "$OSTYPE" =~ ^darwin ]]; then
    CORE_NUMBER=`sysctl -n hw.ncpu`
    echo "  CORE: " ${CORE_NUMBER}
  fi
  if [[ "$OSTYPE" =~ ^linux ]]; then
    CORE_NUMBER_=`lscpu -b -p=Core,Socket | grep -v '^#' | sort -u | wc -l`
    echo "  CORE: " ${CORE_NUMBER}
  fi

  echo "Checking whether database ${DB_NAME} already exists..."
  RES=`mysql --defaults-file=${ROOT}/myupgrade.cnf -e "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '${DB_NAME}'"`

  if [[ $? -gt 0 ]]; then
    echo "ERROR: Problem checking for database. "
    echo "  Please check your database configuration, and try again."
    rm -f ${ROOT}/myupgrade.cnf
    rm -rf ${ROOT}/upgrade_core_db.sql
    exit 1
  fi

  FOUND=`echo $RES | grep ${DB_NAME} | wc -l`

  if [[ ${FOUND} -eq 1 ]]; then
      echo "  Database ${DB_NAME} exists."
      echo " "
      echo "Updating tables in CWS CORE DB..."

cat > ${ROOT}/upgrade_core_db.sql <<- EOF
DELETE FROM cws_log_usage WHERE worker_id=(SELECT id FROM cws_worker WHERE name='worker0000');
DELETE FROM cws_worker_proc_def WHERE worker_id=(SELECT id FROM cws_worker WHERE name='worker0000');
DELETE FROM cws_worker WHERE name='worker0000';
DELETE FROM cws_log_usage;
DELETE FROM cws_worker_proc_def;
DELETE FROM cws_worker;
ALTER TABLE cws_worker ADD max_num_running_procs int(11) DEFAULT ${CORE_NUMBER} AFTER job_executor_max_pool_size;
EOF

      mysql --defaults-file=${ROOT}/myupgrade.cnf ${DB_NAME} < ${ROOT}/upgrade_core_db.sql

      if [[ $? -gt 0 ]]; then
          echo "ERROR: Problem updating tables. Database Column may already exist."
          echo "  Please check your database upgrade sql template '${ROOT}/upgrade_core_db.sql', and try again."
          rm -rf ${ROOT}/myupgrade.cnf
          rm -rf ${ROOT}/upgrade_core_db.sql
          exit 1
      fi
      rm -rf ${ROOT}/myupgrade.cnf
      rm -rf ${ROOT}/upgrade_core_db.sql
		  echo "   Upgrade to CWS Database was made."
  else
      echo "   Database ${DB_NAME} DOES NOT exists. Please check your database configuration"
  fi
else
		echo "   Upgrade to CWS Database was NOT made."
	  rm -rf ${ROOT}/myupgrade.cnf
    rm -rf ${ROOT}/upgrade_core_db.sql
fi
