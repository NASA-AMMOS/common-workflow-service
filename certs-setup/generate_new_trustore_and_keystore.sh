#!/bin/bash

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd $DIR

# Prints the provided string, tagging with the script that called it
function print () {
    echo "[`basename ${0}`] ${1}"
}

# Runs a number of checks to see if Java is properly installed and at a compatible version for CWS
function check_java_requirements () {
    print "Checking Java requirements..."
    if [[ -z ${JAVA_HOME} ]]; then
        print "  ERROR: CWS requires JAVA_HOME be set."
        print "  JAVA_HOME should be set to the JDK on your home system."
        exit 1
    else
        print "  JAVA_HOME set  [OK]"
        print "  JAVA_HOME = ${JAVA_HOME}"
    fi

    JAVA_HOME_VERSION=$("${JAVA_HOME}/bin/java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    print "  JAVA_HOME Java version : ${JAVA_HOME_VERSION}"

    JAVA_PATH_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    print "  PATH      Java version : ${JAVA_PATH_VERSION}  $(which java)"

    if [[ "${JAVA_PATH_VERSION}" == "${JAVA_PATH_VERSION}" ]]; then
        print "  Java versions match      [OK]"
    else
        print "  ERROR: Java versions don't match."
        print "  Please ensure your JAVA_HOME java is on your PATH environment variable."
        exit 1
    fi

    if [[ "${JAVA_PATH_VERSION}" > "1.8" && "${JAVA_PATH_VERSION}" < "1.9" ]]; then
        print "  Java version == 1.8x     [OK]"
    else
        print "  ERROR: Java version is ${JAVA_PATH_VERSION}. CWS only supports Java version 1.8x."
        exit 1
    fi

    JAVAC_EXISTS=`stat ${JAVA_HOME}/bin/javac &> /dev/null;echo $?`

    if [[ "${JAVAC_EXISTS}" == "0" ]]; then
        print "  Java Compiler available  [OK]"
    else
        print "  ERROR: No Java compiler (javac) found."
        print "  Please make sure you are using a JAVA_HOME that is a JDK, NOT a JRE."
        print "  Make sure your path is set correctly."
        print "  For example, if running under bash:"
        print "  (in ~/.bash_profile)"
        print "     export PATH=\$JAVA_HOME\/bin:\$PATH"
        exit 1
    fi

    print "Java requirements met."
}


# ------------------------
# CHECK JAVA REQUIREMENTS
#  JAVA_HOME must be set
#  Java must be 1.8x
# ------------------------
check_java_requirements

rm -rf ${DIR}/temp
mkdir -p ${DIR}/temp
cd ${DIR}/temp


read -p "Enter alias for certificate (e.g. 'cws'): "  ALIAS
read -p "Enter server DNS entry name (e.g. 'localhost'): " DNS

keytool \
    -keystore "keystore.jks" \
    -genkey \
    -keyalg 'rsa' \
    -keysize '2048' \
    -dname "CN=${DNS},OU=NA,O=NA,L=NA,S=NA,C=NA" \
    -storepass "changeit" \
    -keypass "changeit" \
    -alias "${ALIAS}"

keytool \
    -keystore "keystore.jks" \
    -certreq \
    -alias "${ALIAS}" \
    -ext SAN=dns:"${DNS}" \
    -keyalg 'rsa' \
    -keysize '2048' \
    -storepass "changeit" \
    -file "${ALIAS}.csr"

echo

cat ./${ALIAS}.csr


CRT_FILE=`basename *.crt`
echo "CRT_FILE = $CRT_FILE"
openssl verify ${CRT_FILE}



yes | keytool -import \
  -keystore "keystore.jks" \
  -storepass "changeit" \
  -file "${CRT_FILE}" \
  -alias "${ALIAS}"

cp ../cws_truststore.jks .

