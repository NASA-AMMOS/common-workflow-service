#!/bin/bash
# --------------------
# refresh_cws_token.sh
# --------------------
# Re-authenticates a given user to the CWS console, creating a fresh token file in the root CWS directory.
# This script is template-based, and is filled in with the CWS host/port and authentication scheme during a CWS install.

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source ${ROOT}/utils.sh

CWS_CONSOLE_HOST='__CWS_CONSOLE_HOST__'
CWS_CONSOLE_SSL_PORT='__CWS_CONSOLE_SSL_PORT__'
CWS_AUTH_SCHEME='__CWS_AUTH_SCHEME__'

# PROMPT USER FOR CWS USERNAME
while [[ -z ${CWS_USERNAME} ]]; do
	read -p "Enter the CWS username: " CWS_USERNAME
	if [[ -z ${CWS_USERNAME} ]]; then
		printf "Must specify a CWS username!\n\n"
	fi
done

# PROMPT USER FOR CWS PASSWORD
unset CWS_PASSWORD
unset charCount
charCount=0
prompt="Enter the CWS password: "
while IFS= read -p "$prompt" -r -s -n 1 char
do
	if [[ ${char} == $'\0' ]]
	then
		break
	fi

	# HANDLE BACKSPACE/DELETE
	if [[ ${char} == $'\177' ]]
	then
		if [[ ${charCount} -gt 0 ]]
		then
			prompt=$'\10 \10'
			let "charCount -= 1"
			CWS_PASSWORD="${CWS_PASSWORD%?}"
		else
			prompt=$'\10 '
			let "charCount = 0"
			CWS_PASSWORD=""
		fi
	else
		prompt='*'
		CWS_PASSWORD+="$char"
		let "charCount += 1"
	fi
done
printf "\n";

# Authenticate with CWS
print "Attempting authentication with CWS..."
AUTH_RESP=`curl -1 -k -c ${ROOT}/cookies.txt -s https://${CWS_CONSOLE_HOST}:${CWS_CONSOLE_SSL_PORT}/cws-ui/rest/authenticate -u ${CWS_USERNAME}:${CWS_PASSWORD}`
print "${AUTH_RESP}"

if [[ ${AUTH_RESP} != *"SUCCESS"* ]]; then
  print "Invalid credentials. Please try again."
  exit 1
fi

CWS_TOKEN=`cat ${ROOT}/cookies.txt | grep cwsToken | cut -f7`
echo ${CWS_TOKEN} > ${ROOT}/cws_token.txt

print "+--------------------------------------------------------------------------------------------"
print "| CWS authorization scheme is :  ${CWS_AUTH_SCHEME}                                          "
print "+--------------------------------------------------------------------------------------------"
print "| CWS token is   : '${CWS_TOKEN}'                                                            "
print "| cookie file is : cookies.txt    (use for curl commands later)                              "
print "| token file is  : cws_token.txt  (use in BPMN processes when making REST GET or POST calls) "
print "+--------------------------------------------------------------------------------------------"
print "| NOTE: The CWS token value above will be blank, unless the auth scheme is 'LDAP'.           "
print "+--------------------------------------------------------------------------------------------"
print "| Here are some examples of how to interact with CWS programmatically:                       "
print "|                                                                                            "
print "| LDAP scheme:                                                                               "
print "|   curl <CWS_REST_URL> -b \"cwsToken=<CWS_TOKEN>\"                                          "
print "|   curl <CWS_REST_URL> -b \"<PATH_TO_COOKIES_FILE>\"                                        "
print "|                                                                                            "
print "| ALL schemes:                                                                               "
print "|   curl <CWS_REST_URL> -u <USERNAME>:<PASSWORD>                                             "
print "+--------------------------------------------------------------------------------------------"
