#!/bin/bash

# =========================================================================
# SETUP VARIABLES
# =========================================================================
ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "-------------ROOT PATH: $ROOT"

source ${ROOT}/utils.sh

CONF_FILE=$1
PROMPT_VALUE=$2

# ================
# SETUP VARIABLES
# ================
export CWS_HOME=${ROOT}
export CWS_INSTALLER_PRESET_FILE=${ROOT}/configuration.properties

cd $ROOT


# ------------------------------------
# CHECK JAVA REQUIREMENTS
#  JAVA_HOME must be set
#  Java must be 1.8x
# ------------------------------------
echo "--------------------------------------------------------------"
echo "Checking Java requirements..."
if [ -z ${JAVA_HOME} ]; then
	printf "  ERROR: CWS requires JAVA_HOME be set. JAVA_HOME should be set to the JDK on your home system.\n\n";
	exit 1
else
	echo "  JAVA_HOME set  [OK]"
	echo "  JAVA_HOME = ${JAVA_HOME}"
fi

java_home_version=$("${JAVA_HOME}/bin/java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo "  JAVA_HOME Java version : ${java_home_version}"
java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
which_java=`which java`
echo "  PATH      Java version : ${java_version}  ($which_java)"

if [[ "$java_home_version" == "$java_version" ]]; then
	echo "  Java versions match      [OK]"
else
	echo "  Java versions don't match.  Please ensure your JAVA_HOME java is on your PATH environment variable."
	exit 1
fi

if [[ "$java_version" > "17" && "$java_version" < "18" ]]; then
	echo "  Java version == 17x      [OK]"
else
	echo " +-------+----------------------------------------------------"
	echo " | ERROR |                                                    "
	echo " +-------+                                                    "
	echo " | Java version is less than 17.  Must run with Java 17x    "
	echo " | Aborting program...                                        "
	echo "--------------------------------------------------------------"
	exit 1
fi

javac_exists=`stat $JAVA_HOME/bin/javac &> /dev/null;echo $?`
if [[ "$javac_exists" == "0" ]]; then
	echo "  Java Compiler available  [OK]"
else
	echo " +-------+----------------------------------------------------"
	echo " | ERROR |                                                    "
	echo " +-------+                                                    "
	echo " | No Java compiler (javac) found.  Please make sure you      "
	echo " | are using a JAVA_HOME that is as JDK, NOT a JRE.           "
	echo " | Make sure your path is set correctly.                      "
	echo " | For example, if running under bash:                        "
	echo " | (in ~/.bash_profile)                                       "
	echo " |     export PATH=\$JAVA_HOME\/bin:\$PATH                    "
	echo " |                                                            "
	echo " | Aborting program...                                        "
	echo "--------------------------------------------------------------"
	exit 1
fi

echo "Java requirements met."
echo "--------------------------------------------------------------"
echo

# -----------------------------------------
# SETUP CONFIGURATION PROPERTIES AND MODE
#
if [ "${CONF_FILE}" == "" ]; then
	if [ -f ${CWS_INSTALLER_PRESET_FILE} ]; then
		# use previous configuration
		export CWS_INSTALLER_MODE=interactive
	else
		#
		# Use provided defaults as configuration
		#
		cp ${ROOT}/config/installerPresets.properties ${CWS_INSTALLER_PRESET_FILE}
		export CWS_INSTALLER_MODE=interactive
	fi
elif [ "${CONF_FILE}" -ef "${CWS_INSTALLER_PRESET_FILE}" ]; then
	echo "re-using ${CONF_FILE}..."
	mv $CONF_FILE ${ROOT}/.tmp_cws_config
	CONF_FILE=${ROOT}/.tmp_cws_config
	#
	# Merge provided configuration with installer presets (defaults)
	#
	cat ${CONF_FILE} ${ROOT}/config/installerPresets.properties | awk -F= '!a[$1]++' > ${CWS_INSTALLER_PRESET_FILE}
	export CWS_INSTALLER_MODE=configFile
	rm ${CONF_FILE}
else
	#
	# Merge provided configuration with installer presets (defaults)
	#
	cat ${CONF_FILE} ${ROOT}/config/installerPresets.properties | awk -F= '!a[$1]++' > ${CWS_INSTALLER_PRESET_FILE}
	export CWS_INSTALLER_MODE=configFile
fi

CWS_INSTALL_TYPE=`grep install_type ${CWS_INSTALLER_PRESET_FILE} | grep -v "^#" | cut -d"=" -f 2`

# -----------------------------------------
# VERIFY USER IS NOT CHANGING INSTALL TYPE
#
if [ "${CWS_INSTALL_TYPE}" != "" ]; then
	echo "Desired install type is ${CWS_INSTALL_TYPE}."
	if [ -f ${ROOT}/.installType ]; then
		PREV_INSTALL_TYPE=`cat ${ROOT}/.installType`
		echo "CWS was previously installed with type ${PREV_INSTALL_TYPE}"
		if [ "${PREV_INSTALL_TYPE}" != "${CWS_INSTALL_TYPE}" ]; then
			echo "---------                                                                   "
			echo "WARNING!                                                                    "
			echo "---------                                                                   "
			echo "  It looks like you have previously configured this CWS installation with a "
			echo "  different install type (${PREV_INSTALL_TYPE}).                            "
			echo "  Re-configurations are only allowed if you keep the same installation type."
			echo "  If you want to change the installation type, then please blow away your   "
			echo "  entire CWS installation directory, and re-extract the CWS tar file.       "
			echo "                                                                            "
			echo "  NOTE 1:  If you blow away the installation directory, then any log        "
			echo "           information accumulated so far will be gone forever.             "
			echo "                                                                            "
			echo "  NOTE 2:  If you completely start over, it\'s also advisable to drop and   "
			echo "           recreate your CWS database as well.                              "
			echo "                                                                            "
			echo "Aborting configuration program...                                           "
			echo "                                                                            "
			exit 1
		fi
	fi
fi


# --------------------------------------------
# DETERMINE WHETHER THIS IS A RECONFIGURATION
#
RECONFIGURE=false
if [ ! -f ${ROOT}/.installType ]; then
	# .installType file is not present.
	# This means that CWS has NOT been installed before, and this is the first time.
	echo "This is the first time configuration CWS..."
else
	# .installType file IS present.
	# This means that CWS has been installed before.
	echo "Reconfiguring existing install..."
	echo
	RECONFIGURE=true
fi


# -------------------------------------------------------------
# IF IN INTERACTIVE MODE, 
# OR CONFIG FILE WITH NO AUTO_ACCEPT_CONFIG,
# PROMPT FOR STOPPING CWS FIRST
#

AUTO_ACCEPT_CONFIG=`grep auto_accept_config ${CWS_INSTALLER_PRESET_FILE} | grep -v "^#" | cut -d"=" -f 2`
echo "AUTO_ACCEPT_CONFIG = ${AUTO_ACCEPT_CONFIG}"
	
echo "CWS_INSTALLER_PRESET_FILE = $CWS_INSTALLER_PRESET_FILE"


#
# FOR CONSOLE-ONLY or CONSOLE-AND-WORKER INSTALLATIONS, 
# 
if [ "${CWS_INSTALL_TYPE}" = "1" ] || [ "${CWS_INSTALL_TYPE}" = "2" ]; then
	DB_HOST=`grep database_host ${CWS_INSTALLER_PRESET_FILE} | grep -v "^#" | cut -d"=" -f 2`
	DB_NAME=`grep database_name ${CWS_INSTALLER_PRESET_FILE} | grep -v "^#" | cut -d"=" -f 2`
	DB_USER=`grep database_user ${CWS_INSTALLER_PRESET_FILE} | grep -v "^#" | cut -d"=" -f 2`
	DB_PASS=`grep database_password ${CWS_INSTALLER_PRESET_FILE} | grep -v "^#" | cut -d"=" -f 2`
	echo "DB HOST          is ${DB_HOST}"
	echo "DB NAME          is ${DB_NAME}"
	echo "DB USER          is ${DB_USER}"
	echo "[mysql]" > ${ROOT}/config/my.cnf
	echo "host=${DB_HOST}" >> ${ROOT}/config/my.cnf
	echo "user=\"${DB_USER}\"" >> ${ROOT}/config/my.cnf
	echo "password=\"${DB_PASS}\"" >> ${ROOT}/config/my.cnf
	chmod 644 ${ROOT}/config/my.cnf
	
	
	# -----------------------------------------
	# RUN SQL TO CREATE DATABASE IF NECESSARY.
	#
	echo
	echo "Your database configuration is:"
	echo "  DB HOST:   ${DB_HOST}"
	echo "  DB NAME:   ${DB_NAME}"
	echo "  DB USER:   ${DB_USER}"
	echo
	echo "This script will now create the database, necessary for CWS to function."
	echo
	
	RES=`mysql --defaults-file=${ROOT}/config/my.cnf -e "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '${DB_NAME}'"`
		while [[ $? -gt 0 ]]; do
			echo "Problem checking for database. Please check your database configuration, and try again."
			cat ${ROOT}/config/my.cnf
			rm ${ROOT}/config/my.cnf
			exit 1
		done
			FOUND=`echo $RES | grep ${DB_NAME} | wc -l`
			if [ ${FOUND} -eq 1 ]; then
				echo "  Database already exists."
				echo "  ** It is recommended that you start your installation with a clean database (no tables) **"
				echo "     ------------------------------------------------------------------------------------"
				echo "     Do you want this script to drop and re-create the database for you,"
				echo "     so that you have a clean install?"
				#
				# CREATE DATABASE
				#
				echo
				echo "  Database doesn't already exist."
				echo
				echo "Creating database: ${DB_NAME}..."
				mysql --defaults-file=${ROOT}/config/my.cnf -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME}"
				while [[ $? -gt 0 ]]; do
					echo "Problem creating database. Please check your database configuration, and try again."
					rm ${ROOT}/config/my.cnf
					rm -f ${ROOT}/.databaseCreated
					exit 1
				done
			else
				#
				# CREATE DATABASE
				#
				echo
				echo "  Database doesn't already exist."
				echo
				echo "Creating database: ${DB_NAME}..."
				mysql --defaults-file=${ROOT}/config/my.cnf -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME}"
				while [[ $? -gt 0 ]]; do
					echo "Problem creating database. Please check your database configuration, and try again."
					rm ${ROOT}/config/my.cnf
					rm -f ${ROOT}/.databaseCreated
					exit 1
				done
		fi
			
			#
			# Record the fact that database is now created
			#
			touch ${ROOT}/.databaseCreated
fi

rm -f ${ROOT}/config/my.cnf

sleep 1

if [ "$RECONFIGURE" = true ]; then
	${JAVA_HOME}/bin/java -classpath "./installer/*" -javaagent:./server/apache-tomcat-${TOMCAT_VER}/lib/org.jacoco.agent-0.8.7-runtime.jar=destfile=./installer-jacoco.exec,append=false jpl.cws.task.CwsInstaller --reconfigure
else
	${JAVA_HOME}/bin/java -classpath "./installer/*" -javaagent:./server/apache-tomcat-${TOMCAT_VER}/lib/org.jacoco.agent-0.8.7-runtime.jar=destfile=./installer-jacoco.exec,append=false jpl.cws.task.CwsInstaller
fi
