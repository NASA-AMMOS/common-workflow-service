#!/bin/sh
# --------
# create_server_dist.sh
# --------
# Assembles a CWS server archive, comprised of the CWS core libraries and associated executables
# (Tomcat, Logstash, etc...)

pwd
echo " directory for create_server_dist.sh"
ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source ${ROOT}/utils.sh

SERVER_DIST='cws_server.tar.gz'

DIST=${ROOT}/dist
CWS=${DIST}/cws
INSTALL_DIR=${ROOT}/install

print 'Removing previous distribution directory...'
rm -rf ${DIST}

print 'Creating new CWS distribution directory...'
mkdir -p ${CWS}/{bpmn,config/templates,installer,logs,sql/cws}

print 'Unzipping Camunda into distribution...'
unzip ${INSTALL_DIR}/cws_camunda-bpm-tomcat-${CAMUNDA_VER}.zip -x start-camunda.bat start-camunda.sh -d ${CWS} > ${CWS}/logs/camunda_extract.log 2>&1

if [[ $? -gt 0 ]]; then
    print "ERROR: failed to unzip Camunda distribution, check ${CWS}/logs/camunda_extract.log for details."
    exit 1
fi

CWS_TOMCAT_ROOT=${CWS}/server/apache-tomcat-${TOMCAT_VER}
CWS_WEBAPPS_ROOT=${CWS_TOMCAT_ROOT}/webapps

# --------------------------------------------
# CREATE EXPLODED cws-ui webapp directory
print 'Creating exploded cws-ui webapps directory...'
mkdir -p ${CWS_WEBAPPS_ROOT}/cws-ui
unzip ${ROOT}/cws-ui/target/cws-ui.war -d ${CWS_WEBAPPS_ROOT}/cws-ui > ${CWS}/logs/cws-ui-unzip.log 2>&1

if [[ $? -gt 0 ]]; then
    print "ERROR: failed to deploy cws-ui webapp, check ${CWS}/logs/cws-ui-unzip.log for details."
    exit 1
fi

# ----------------------------------------------
# CREATE EXPLODED cws-engine webapp directory
print 'Creating exploded cws-engine webapps directory...'
mkdir -p ${CWS_WEBAPPS_ROOT}/cws-engine
unzip ${ROOT}/cws-engine/target/cws-engine.war -d ${CWS_WEBAPPS_ROOT}/cws-engine > ${CWS}/logs/cws-engine-unzip.log 2>&1

if [[ $? -gt 0 ]]; then
    print "ERROR: failed to deploy cws-engine webapp, check ${CWS}/logs/cws-engine-unzip.log for details."
    exit 1
fi

# ------------------
# LOGSTASH SETUP
print 'Copying Logstash zip into place...'
cp ${INSTALL_DIR}/logging/logstash-${LOGSTASH_VER}.zip ${CWS}/server

# MOVE TEMPLATE CONFIG FILES INTO PLACE
print 'Copying configuration templates...'
CONFIG_TEMPLATES_DIR=${CWS}/config/templates
mkdir -p ${CONFIG_TEMPLATES_DIR}/{cws-engine,cws-ui,tomcat_bin,tomcat_lib,tomcat_conf,camunda_mods,engine-rest_mods,logging}

cp ${INSTALL_DIR}/tomcat_lib/css-jaas.cfg                     ${CONFIG_TEMPLATES_DIR}/tomcat_lib
cp ${INSTALL_DIR}/tomcat_bin/setenv.sh                        ${CONFIG_TEMPLATES_DIR}/tomcat_bin
cp ${INSTALL_DIR}/tomcat_conf/bpm-platform.xml                ${CONFIG_TEMPLATES_DIR}/tomcat_conf
cp ${INSTALL_DIR}/tomcat_conf/server.xml                      ${CONFIG_TEMPLATES_DIR}/tomcat_conf
cp ${INSTALL_DIR}/tomcat_conf/web.xml                         ${CONFIG_TEMPLATES_DIR}/tomcat_conf
cp ${INSTALL_DIR}/tomcat_conf/ldap_plugin_bean.xml            ${CONFIG_TEMPLATES_DIR}/tomcat_conf
cp ${INSTALL_DIR}/tomcat_conf/ldap_plugin_ref.xml             ${CONFIG_TEMPLATES_DIR}/tomcat_conf
cp ${INSTALL_DIR}/cws-engine/applicationContext.xml           ${CONFIG_TEMPLATES_DIR}/cws-engine
cp ${INSTALL_DIR}/cws-engine/process_start_req_listener.xml   ${CONFIG_TEMPLATES_DIR}/cws-engine
cp ${INSTALL_DIR}/cws-engine/cws-engine.properties            ${CONFIG_TEMPLATES_DIR}/cws-engine
cp ${INSTALL_DIR}/cws-ui/cws-ui.properties                    ${CONFIG_TEMPLATES_DIR}/cws-ui
cp ${INSTALL_DIR}/cws-ui/applicationContext.xml               ${CONFIG_TEMPLATES_DIR}/cws-ui
cp ${INSTALL_DIR}/cws-ui/*.ftl                                ${CONFIG_TEMPLATES_DIR}/cws-ui
cp ${INSTALL_DIR}/cws-ui/sqs_dispatcher_thread_bean.xml       ${CONFIG_TEMPLATES_DIR}/cws-ui
cp ${INSTALL_DIR}/camunda_mods/web.xml                        ${CONFIG_TEMPLATES_DIR}/camunda_mods
cp ${INSTALL_DIR}/engine-rest/web.xml                         ${CONFIG_TEMPLATES_DIR}/engine-rest_mods
cp ${INSTALL_DIR}/logging/cws-logstash.conf                   ${CONFIG_TEMPLATES_DIR}/logging
cp ${INSTALL_DIR}/refresh_cws_token.sh                        ${CONFIG_TEMPLATES_DIR}
cp ${INSTALL_DIR}/stop_cws.sh                                 ${CONFIG_TEMPLATES_DIR}
cp ${INSTALL_DIR}/clean_es_history.sh                         ${CONFIG_TEMPLATES_DIR}

# ______________________________________________
# MOVE KEYSTORE AND TRUSTSTORE FILES INTO TOMCAT
# ==============================================
TOMCAT_LIB_DIR=${CWS_TOMCAT_ROOT}/lib
TOMCAT_BIN_DIR=${CWS_TOMCAT_ROOT}/bin
TOMCAT_CONF_DIR=${CWS_TOMCAT_ROOT}/conf

print 'Installing key and trust store to Tomcat...'
cp ${INSTALL_DIR}/.keystore ${CWS_TOMCAT_ROOT}/conf/.keystore
cp ${INSTALL_DIR}/tomcat_lib/cws_truststore.jks ${TOMCAT_LIB_DIR}

# ___________________________________________________________________
# MAKE TOMCAT ROOT POINT TO cws-ui AND REMOVE DEFAULT TOMCAT ROOT APP
# ===================================================================
print 'Installing cws-ui webapp to Tomcat...'
rm -rf ${CWS_TOMCAT_ROOT}/webapps/ROOT
mkdir ${CWS_TOMCAT_ROOT}/webapps/ROOT
cp ${INSTALL_DIR}/tomcat_root/index.html ${CWS_TOMCAT_ROOT}/webapps/ROOT
cp ${INSTALL_DIR}/tomcat_root/not_authenticated.html ${CWS_TOMCAT_ROOT}/webapps/ROOT

# Remove unused default Tomcat webapp docs
rm -rf ${CWS_TOMCAT_ROOT}/webapps/docs

print 'Installing DB drivers to Tomcat...'
cp ${ROOT}/cws-installer/cws-installer-libs/mysql-connector-java-*.jar ${TOMCAT_LIB_DIR}
cp ${ROOT}/cws-installer/cws-installer-libs/mariadb-java-client-*.jar  ${TOMCAT_LIB_DIR}
cp ${ROOT}/cws-installer/cws-installer-libs/HikariCP-*.jar             ${TOMCAT_LIB_DIR}

print 'Installing core libraries to Tomcat...'
cp ${ROOT}/cws-core/target/cws-core.jar                     ${TOMCAT_LIB_DIR}

rm -f ${TOMCAT_LIB_DIR}/slf4j*.jar
cp ${ROOT}/cws-core/cws-core-libs/slf4j-api-*.jar           ${TOMCAT_LIB_DIR}
cp ${ROOT}/cws-core/cws-core-libs/log4j-slf4j-impl*.jar     ${TOMCAT_LIB_DIR}

cp ${ROOT}/cws-core/cws-core-libs/log4j-*.jar               ${TOMCAT_LIB_DIR}
cp ${ROOT}/cws-core/cws-core-libs/jython*.jar               ${TOMCAT_LIB_DIR}

print 'Installing cws-tasks libraries to Tomcat...'
cp ${ROOT}/cws-tasks/cws-tasks-libs/commons-configuration-*.jar ${TOMCAT_LIB_DIR}

print 'Installing cws-ui libraries to Tomcat...'
CWS_CONSOLE_WEBAPP=${CWS_TOMCAT_ROOT}/webapps/cws-ui
cp ${CWS_CONSOLE_WEBAPP}/WEB-INF/lib/commons-io-*.jar      ${TOMCAT_LIB_DIR}
cp ${CWS_CONSOLE_WEBAPP}/WEB-INF/lib/commons-lang-*.jar    ${TOMCAT_LIB_DIR}
cp ${CWS_CONSOLE_WEBAPP}/WEB-INF/lib/commons-logging-*.jar ${TOMCAT_LIB_DIR}

print 'Removing slf4j lib from cws-ui...'
rm ${CWS_CONSOLE_WEBAPP}/WEB-INF/lib/slf4j*.jar

print 'Installing cws-engine libraries to Tomcat...'
CWS_ENGINE_WEBAPP=${CWS_TOMCAT_ROOT}/webapps/cws-engine
cp ${CWS_ENGINE_WEBAPP}/WEB-INF/lib/jersey-guava-*.jar ${TOMCAT_LIB_DIR}
cp ${CWS_ENGINE_WEBAPP}/WEB-INF/lib/gson-*.jar         ${TOMCAT_LIB_DIR}

print "Modifying Camunda webapp..."
CAMUNDA_WEBAPP=${CWS_TOMCAT_ROOT}/webapps/camunda
cp ${INSTALL_DIR}/camunda_mods/admin/index.html ${CAMUNDA_WEBAPP}/app/admin/index.html
cp ${INSTALL_DIR}/camunda_mods/cockpit/index.html ${CAMUNDA_WEBAPP}/app/cockpit/index.html
cp ${INSTALL_DIR}/camunda_mods/tasklist/index.html ${CAMUNDA_WEBAPP}/app/tasklist/index.html
cp ${INSTALL_DIR}/tomcat_root/not_authenticated.html ${CAMUNDA_WEBAPP}

cp ${INSTALL_DIR}/camunda_mods/tasklist/config.js ${CAMUNDA_WEBAPP}/app/tasklist/scripts/
mkdir ${CAMUNDA_WEBAPP}/app/tasklist/cws
cp ${INSTALL_DIR}/camunda_mods/scripts.js ${CAMUNDA_WEBAPP}/app/tasklist/cws/
cat ${INSTALL_DIR}/camunda_mods/user-styles.css >> ${CAMUNDA_WEBAPP}/app/tasklist/styles/user-styles.css

cp ${INSTALL_DIR}/camunda_mods/cockpit/config.js ${CAMUNDA_WEBAPP}/app/cockpit/scripts/
mkdir ${CAMUNDA_WEBAPP}/app/cockpit/cws
cp ${INSTALL_DIR}/camunda_mods/scripts.js ${CAMUNDA_WEBAPP}/app/cockpit/cws/
cat ${INSTALL_DIR}/camunda_mods/user-styles.css >> ${CAMUNDA_WEBAPP}/app/cockpit/styles/user-styles.css

cp ${INSTALL_DIR}/camunda_mods/admin/config.js ${CAMUNDA_WEBAPP}/app/admin/scripts/
mkdir ${CAMUNDA_WEBAPP}/app/admin/cws
cp ${INSTALL_DIR}/camunda_mods/scripts.js ${CAMUNDA_WEBAPP}/app/admin/cws/
cat ${INSTALL_DIR}/camunda_mods/user-styles.css >> ${CAMUNDA_WEBAPP}/app/admin/styles/user-styles.css

cp ${INSTALL_DIR}/camunda_mods/welcome/config.js ${CAMUNDA_WEBAPP}/app/welcome/scripts/
mkdir ${CAMUNDA_WEBAPP}/app/welcome/cws
cp ${INSTALL_DIR}/camunda_mods/scripts.js ${CAMUNDA_WEBAPP}/app/welcome/cws/
cat ${INSTALL_DIR}/camunda_mods/user-styles.css >> ${CAMUNDA_WEBAPP}/app/welcome/styles/user-styles.css


# COMMENT OUT THE BELOW LINE, WHEN CUTTING A RELEASE
cp ${ROOT}/cws-test/cws-test-libs/*jacoco*runtime*.jar ${TOMCAT_LIB_DIR}



# Inject Java snippets code into core.sql.template
print "Building core.sql..."
cp ${ROOT}/cws-adaptation/src/main/java/jpl/cws/core/code/CustomMethods.java ${DIST}/snippets.java
sed -i.bak "s^'^''^g" ${DIST}/snippets.java
sed -i.bak "s^class CustomMethods^class Snippets^g" ${DIST}/snippets.java
sed -i.bak 's:\\:\\\\:g' ${DIST}/snippets.java

awk 'NR==FNR { a[n++]=$0; next }
/__CUSTOM_METHODS_JAVA__/ { for (i=0;i<n;++i) print a[i]; next }1' ${DIST}/snippets.java ${INSTALL_DIR}/sql/core.sql.template > ${CWS}/sql/cws/core.sql

cp ${INSTALL_DIR}/sql/core.afterstartup.sql.template           ${CWS}/sql/cws/core.afterstartup.sql

rm ${DIST}/snippets.java
rm ${DIST}/snippets.java.bak

print 'Setting up Log4J as the logging backend for Tomcat...'
LOG4J2_LIB=${CWS_TOMCAT_ROOT}/log4j2/lib
LOG4J2_CONF=${CWS_TOMCAT_ROOT}/log4j2/conf
mkdir -p ${LOG4J2_LIB}
mkdir -p ${LOG4J2_CONF}
cp ${ROOT}/cws-core/cws-core-libs/log4j-api-*.jar          ${LOG4J2_LIB}
cp ${ROOT}/cws-core/cws-core-libs/log4j-core-*.jar         ${LOG4J2_LIB}
cp ${ROOT}/cws-core/cws-core-libs/log4j-appserver-*.jar    ${LOG4J2_LIB}
cp ${INSTALL_DIR}/tomcat_lib/log4j2-tomcat.properties      ${LOG4J2_CONF}

print 'Removing default logging.properties from Tomcat...'
rm ${TOMCAT_CONF_DIR}/logging.properties

print 'Copying Installer scripts and libraries...'
cp ${ROOT}/utils.sh                                          ${CWS}
cp ${ROOT}/cws-installer/cws-installer-libs/*                ${CWS}/installer
cp ${ROOT}/cws-installer/target/cws-installer.jar            ${CWS}/installer
cp ${ROOT}/cws-installer/src/main/resources/log4j2.properties ${CWS}/installer

cp ${INSTALL_DIR}/configure.sh                         ${CWS}
cp ${INSTALL_DIR}/installerPresets.properties          ${CWS}/config
cp ${INSTALL_DIR}/example-cws-configuration.properties ${CWS}
cp ${INSTALL_DIR}/start_cws.sh                         ${CWS}
cp ${INSTALL_DIR}/stop_cws.sh                          ${CWS}
cp ${INSTALL_DIR}/refresh_cws_token.sh                 ${CWS}
cp ${INSTALL_DIR}/deploy_proc_def.sh                   ${CWS}
cp ${INSTALL_DIR}/launch_ls.sh                         ${CWS}

print 'Copying Modeller scripts and libraries...'
cp -R ${INSTALL_DIR}/modeler                    ${CWS}

print 'Installing context.xml to Tomcat...'
cp ${INSTALL_DIR}/context.xml ${CWS_TOMCAT_ROOT}/conf/context.xml

print 'Removing old server distribution archive...'
rm -f ${DIST}/${SERVER_DIST}

# Setting COPYFILE_DISABLE to disable Mac tar storing extended file properties in ._* files.
print 'Creating finalized server distribution archive...'
COPYFILE_DISABLE=1 tar --directory=${DIST} -pczf ${DIST}/${SERVER_DIST} cws

print "Removing distribution directory..."
rm -rf ${CWS}

print "Finished"
