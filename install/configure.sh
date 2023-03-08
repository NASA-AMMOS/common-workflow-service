#!/bin/bash
# ------------
# configure.sh
# ------------
# Performs pre-installation configuration and setup and invokes the Java CWS installer for the actual install.

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source ${ROOT}/utils.sh

# ================
# SETUP VARIABLES
# ================
CONF_FILE=${1}
PROMPT_VALUE=${2}

export CWS_HOME=${ROOT}
export CWS_TOMCAT_HOME=${ROOT}/server/apache-tomcat-${TOMCAT_VER}
export CWS_INSTALLER_PRESET_FILE=${ROOT}/configuration.properties

# ------------------------
# CHECK JAVA REQUIREMENTS
#  JAVA_HOME must be set
#  Java must be 1.8x
# ------------------------
check_java_requirements

# -----------------------------------------
# SETUP CONFIGURATION PROPERTIES AND MODE
if [[ "${CONF_FILE}" == "" ]]; then
    export CWS_INSTALLER_MODE=interactive

	if [[ -f ${CWS_INSTALLER_PRESET_FILE} ]]; then
	    print "A previous installation configuration was detected (${CWS_INSTALLER_PRESET_FILE})..."
	    print "Would you like to use this configuration as the default for the current installation?"

	    RESPONSE=''
	    while [[ ! ${RESPONSE} =~ ^(y|Y|n|N)$ ]]; do
		    read -p "     (Y/N): "  RESPONSE

		    if [[ ${RESPONSE} =~ ^(y|Y|n|N)$ ]]
		    then
			    break  # Skip entire rest of loop.
		    fi

		    print "  ERROR: Must specify either 'Y' or 'N'.\n\n";
	    done

	    if [[ ${RESPONSE} =~ ^(n|N)$ ]]; then
		    # Use provided defaults as configuration
		    print "Using baseline installation presets"
		    cp ${ROOT}/config/installerPresets.properties ${CWS_INSTALLER_PRESET_FILE}
	    else
		    # Use previous configuration
		    print "Re-using installation configuration from ${CWS_INSTALLER_PRESET_FILE}"
		fi
	else
		# Use provided defaults as configuration
		print "Using baseline installation presets"
		cp ${ROOT}/config/installerPresets.properties ${CWS_INSTALLER_PRESET_FILE}
	fi
else
    export CWS_INSTALLER_MODE=configFile

    CONF_FILE="$(cd "$(dirname "${CONF_FILE}")"; pwd)/$(basename "${CONF_FILE}")"

    if [[ ! -f ${CONF_FILE} ]]; then
        print "ERROR: provided configuration file '${CONF_FILE}' does not exist."
        exit 1
    fi

	print "Using installation presets provided from ${CONF_FILE}..."

    if [[ ! "${CONF_FILE}" -ef "${CWS_INSTALLER_PRESET_FILE}" ]]; then
	    # Merge provided configuration with installer presets (defaults)
	    cat ${CONF_FILE} ${ROOT}/config/installerPresets.properties | awk -F= '!a[$1]++' > ${CWS_INSTALLER_PRESET_FILE}
	fi
fi

# --------------------------------------------
# DETERMINE WHETHER THIS IS A RECONFIGURATION
RECONFIGURE=false
if [[ ! -f ${ROOT}/.installType ]]; then
	# .installType file is not present.
	# This means that CWS has NOT been installed before, and this is the first time.
	print "This is a first time configuration install..."
else
	# .installType file IS present.
	# This means that CWS has been installed before.
	print "Reconfiguring existing install..."
	RECONFIGURE=true
fi

# ------------------------------------------
# IF IN INTERACTIVE MODE,
# OR CONFIG FILE WITH NO AUTO_ACCEPT_CONFIG,
# PROMPT FOR STOPPING CWS FIRST
if [[ "${CWS_INSTALLER_MODE}" == "interactive" ]]; then
	# PROMPT USER FOR STOPPING ANY RUNNING CWS, BEFORE PROCEEDING
	prompt_to_continue "CWS must be stopped (if currently running) before proceeding. Continue? (Y/N): "

	${ROOT}/stop_cws.sh
else
	AUTO_ACCEPT_CONFIG=`grep auto_accept_config ${CWS_INSTALLER_PRESET_FILE} | grep -v "^#" | cut -d"=" -f 2`

	if [[ "${AUTO_ACCEPT_CONFIG}" == "y" ]]; then
		# AUTOMATICALLY STOP CWS, WITHOUT PROMPTING
		print "AUTO_ACCEPT_CONFIG = Y, so automatically stopping CWS..."
		${ROOT}/stop_cws.sh
	else
		# PROMPT USER FOR STOPPING ANY RUNNING CWS, BEFORE PROCEEDING
		prompt_to_continue "CWS must be stopped (if currently running) before proceeding. Continue? (Y/N): "

	    ${ROOT}/stop_cws.sh
	fi
fi

print "Staring CWS Installer..."
if [[ "$RECONFIGURE" = true ]]; then
	${JAVA_HOME}/bin/java -classpath "${ROOT}/installer/*" jpl.cws.task.CwsInstaller --reconfigure
else
	${JAVA_HOME}/bin/java -classpath "${ROOT}/installer/*" jpl.cws.task.CwsInstaller
fi

if [[ $? -gt 0 ]]; then
    echo ""
    print "ERROR: Problems detected during CWS Installer, aborting installation."
    exit 1
fi

sleep 1

CWS_INSTALL_TYPE=`grep install_type ${CWS_INSTALLER_PRESET_FILE} | grep -v "^#" | cut -d"=" -f 2`

# FOR CONSOLE-ONLY or CONSOLE-AND-WORKER INSTALLATIONS,
if [[ "${CWS_INSTALL_TYPE}" = "1" ]] || [[ "${CWS_INSTALL_TYPE}" = "2" ]]; then
	DB_HOST=`grep database_host ${CWS_INSTALLER_PRESET_FILE} | grep -v "^#" | cut -d"=" -f 2`
	DB_NAME=`grep database_name ${CWS_INSTALLER_PRESET_FILE} | grep -v "^#" | cut -d"=" -f 2`
	DB_USER=`grep database_user ${CWS_INSTALLER_PRESET_FILE} | grep -v "^#" | cut -d"=" -f 2`
	DB_PASS=`grep database_password ${CWS_INSTALLER_PRESET_FILE} | grep -v "^#" | cut -d"=" -f 2`

	echo "[mysql]" > ${ROOT}/config/my.cnf
	echo "host=${DB_HOST}" >> ${ROOT}/config/my.cnf
	echo "user=\"${DB_USER}\"" >> ${ROOT}/config/my.cnf
	echo "password=\"${DB_PASS}\"" >> ${ROOT}/config/my.cnf
	chmod 644 ${ROOT}/config/my.cnf

	# ----------------------------------------
	# RUN SQL TO CREATE DATABASE IF NECESSARY.
	print "Your database configuration is:"
	print "  DB HOST:   ${DB_HOST}"
	print "  DB NAME:   ${DB_NAME}"
	print "  DB USER:   ${DB_USER}"
	print "This script will now create the database necessary for CWS to function."

	while [[ ! ${REPLY} =~ $(echo "^(y|Y|n|N)$") ]]; do
		if [[ "${PROMPT_VALUE}" == "" ]]; then
			read -p "Continue? (Y/N): " REPLY
		else
			REPLY=${PROMPT_VALUE}
		fi

		if [[ $REPLY =~ $(echo "^(y|Y)$") ]]
		then
			print "Checking whether database ${DB_NAME} already exists..."
			RES=`mysql --defaults-file=${ROOT}/config/my.cnf -e "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '${DB_NAME}'"`

			if [[ $? -gt 0 ]]; then
				print "ERROR: Problem checking for database. "
				print "  Please check your database configuration, and try again."
				rm -f ${ROOT}/config/my.cnf
				exit 1
			fi

			FOUND=`echo $RES | grep ${DB_NAME} | wc -l`
			if [[ ${FOUND} -eq 1 ]]; then
				print "  Database already exists."
				print "  ** It is recommended that you start your installation with a clean database (no tables) **"
				print "     Do you want this script to drop and re-create the database for you,"
				print "     so that you have a clean install?"

				if [[ "${PROMPT_VALUE}" == "Y" ]]; then
					RECREATE_DB=${PROMPT_VALUE}
				else
					read -p "     (Y/N): " RECREATE_DB
					while [[ ! ${RECREATE_DB} =~ $(echo "^(y|Y|n|N)$") ]]; do
						print "  ERROR: Must specify either 'Y' or 'N'.";
						read -p "Continue? (Y/N): " RECREATE_DB
					done
				fi

				if [[ ${RECREATE_DB} =~ $(echo "^(y|Y)$") ]]
				then
					print "  Dropping database ${DB_NAME}..."
					mysql --defaults-file=${ROOT}/config/my.cnf -e "DROP DATABASE ${DB_NAME}"

					if [[ $? -gt 0 ]]; then
						print "ERROR: Problem dropping database."
						print "  Please check your database configuration, and try again."
						rm -rf ${ROOT}/config/my.cnf
						exit 1
					fi

					rm -f ${ROOT}/.databaseCreated
					rm -f ${ROOT}/.databaseTablesCreated
					rm -f ${ROOT}/.adaptationTablesCreated

					print "  Creating database ${DB_NAME}..."
					mysql --defaults-file=${ROOT}/config/my.cnf -e "CREATE DATABASE ${DB_NAME}"

					if [[ $? -gt 0 ]]; then
						print "ERROR: Problem creating database."
						print "  Please check your database configuration, and try again."
						rm -rf ${ROOT}/config/my.cnf
						exit 1
					fi
				else
					print "  Leaving DB as is."
				fi
			else
				# CREATE DATABASE
				print "  Database doesn't already exist."
				print "  Creating database: ${DB_NAME}..."
				mysql --defaults-file=${ROOT}/config/my.cnf -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME}"

				if [[ $? -gt 0 ]]; then
					print "ERROR: Problem creating database."
					print "  Please check your database configuration, and try again."
					rm ${ROOT}/config/my.cnf
					exit 1
				fi
			fi

			# Record the fact that database is now created
			touch ${ROOT}/.databaseCreated

			print "Creating core tables..."
            mysql --defaults-file=${ROOT}/config/my.cnf ${DB_NAME} < ${ROOT}/sql/cws/core.sql
            if [[ $? -gt 0 ]]; then
                print "ERROR: Problem creating core tables."
                print "  Please check your database configuration, and try again."
                rm -rf ${ROOT}/config/my.cnf
                exit 1
            fi

            # Record the fact that tables have been created
            touch ${ROOT}/.databaseTablesCreated


            # Create any adaptation tables, if provided
            if [[ -f ${ROOT}/sql/cws/adaptation.sql && -f ${ROOT}/sql/cws/adaptation_core.sql && -f ${ROOT}/sql/cws/adaptation_external.sql ]]; then
			          print "Checking for Adaptation DB Settings..."
                ADAPT_USE_SHARED_DB=`grep adaptation_use_shared_db ${CWS_INSTALLER_PRESET_FILE} | grep -v "^#" | cut -d"=" -f 2`

                if [[ "${ADAPT_USE_SHARED_DB}" == "y" ]]; then
                  	ADAPT_DB_HOST=`grep adaptation_db_host ${CWS_INSTALLER_PRESET_FILE} | grep -v "^#" | cut -d"=" -f 2`
                  	ADAPT_DB_NAME=`grep adaptation_db_name ${CWS_INSTALLER_PRESET_FILE} | grep -v "^#" | cut -d"=" -f 2`
                  	ADAPT_DB_USER=`grep adaptation_db_username ${CWS_INSTALLER_PRESET_FILE} | grep -v "^#" | cut -d"=" -f 2`
                  	ADAPT_DB_PASS=`grep adaptation_db_password ${CWS_INSTALLER_PRESET_FILE} | grep -v "^#" | cut -d"=" -f 2`

                  	echo "[mysql]" > ${ROOT}/config/myadapt.cnf
                  	echo "host=${ADAPT_DB_HOST}" >> ${ROOT}/config/myadapt.cnf
                  	echo "user=\"${ADAPT_DB_USER}\"" >> ${ROOT}/config/myadapt.cnf
                  	echo "password=\"${ADAPT_DB_PASS}\"" >> ${ROOT}/config/myadapt.cnf
                  	chmod 644 ${ROOT}/config/myadapt.cnf

                  	cat ${ROOT}/config/myadapt.cnf

                  	# ----------------------------------------
                  	print "Your adaptation database configuration is:"
                  	print "  DB HOST:   ${ADAPT_DB_HOST}"
                  	print "  DB NAME:   ${ADAPT_DB_NAME}"
                  	print "  DB USER:   ${ADAPT_DB_USER}"
                  	print "This script will now create the database necessary for CWS to function."


              			print "Checking whether database ${ADAPT_DB_NAME} already exists..."
              			RES=`mysql --defaults-file=${ROOT}/config/myadapt.cnf -e "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '${ADAPT_DB_NAME}'"`

              			if [[ $? -gt 0 ]]; then
              				print "ERROR: Problem checking for database. "
              				print "  Please check your database configuration, and try again."
              				rm -f ${ROOT}/config/myadapt.cnf
              				exit 1
              			fi
              			print "  Database ${ADAPT_DB_NAME} exists."

                    print "Creating adaptation tables in External DB..."
                    mysql --defaults-file=${ROOT}/config/my.cnf ${DB_NAME} < ${ROOT}/sql/cws/adaptation_core.sql
                    mysql --defaults-file=${ROOT}/config/myadapt.cnf ${ADAPT_DB_NAME} < ${ROOT}/sql/cws/adaptation_external.sql
                    if [[ $? -gt 0 ]]; then
                        print "ERROR: Problem creating adaptation tables."
                        print "  Please check your database configuration and/or adaptation script '${ROOT}/sql/cws/adaptation_external.sql', and try again."
                        rm -rf ${ROOT}/config/myadapt.cnf
                        exit 1
                    fi
				        else
                    print "Creating adaptation tables in Default Core DB..."
                    if [[ -f ${ROOT}/sql/cws/adaptation.sql ]]; then
                        mysql --defaults-file=${ROOT}/config/my.cnf ${DB_NAME} < ${ROOT}/sql/cws/adaptation.sql
                        if [[ $? -gt 0 ]]; then
                            print "ERROR: Problem creating adaptation tables."
                            print "  Please check your database configuration and/or adaptation script '${ROOT}/sql/cws/adaptation.sql', and try again."
                            rm -rf ${ROOT}/config/my.cnf
                            exit 1
                        fi
                    fi
				        fi

                # Record the fact that we applied a database adaptation
                touch ${ROOT}/.adaptationTablesCreated
            fi

		elif [[ $REPLY =~ $(echo "^(n|N)$") ]]; then
			print "ERROR: You have skipped setting up the database."
			print "  DATABASE EXISTENCE IS NECESSARY TO PROCEED WITH CONFIGURATION."
			print "  Aborting configuration..."
			rm ${ROOT}/config/my.cnf
			exit 1
		else
		    print "ERROR: Must specify either 'Y' or 'N'.";
		fi
	done
fi

rm -f ${ROOT}/config/my.cnf
rm -f ${ROOT}/config/myadapt.cnf

print "Finished"
