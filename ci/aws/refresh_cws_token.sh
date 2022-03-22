#!/bin/bash

CWS_CONSOLE_HOST=$1
CWS_CONSOLE_SSL_PORT='38443'
CWS_AUTH_SCHEME='CAMUNDA'

# PROMPT USER FOR CWS USERNAME
while [ -z $CWS_USERNAME ]; do
	read -p "Enter the CWS username: " CWS_USERNAME
	if [ -z $CWS_USERNAME ]; then
		printf "Must specify a CWS username!\n\n"
	fi
done

# PROMPT USER FOR CWS PASSWORD
unset CWS_PASSWORD
unset charCount
charCount=0;
prompt="Enter the CWS password: "
while IFS= read -p "$prompt" -r -s -n 1 char
do
	if [[ $char == $'\0' ]]
	then
		break
	fi

	# HANDLE BACKSPACE/DELETE
	if [[ $char == $'\177' ]]
	then
		if [[ $charCount -gt 0 ]]
		then
			prompt=$'\10 \10';
			let "charCount -= 1"
			CWS_PASSWORD="${CWS_PASSWORD%?}";
		else
			prompt=$'\10 ';
			let "charCount = 0";
			CWS_PASSWORD="";
		fi
	else
		prompt='*'
		CWS_PASSWORD+="$char";
		let "charCount += 1"
	fi
done
printf "\n";

#
# Authenticate with CWS
#
echo "Attempting authentication with CWS..."
AUTH_RESP=`curl -1 -k -c ./cookies.txt -s https://${CWS_CONSOLE_HOST}:${CWS_CONSOLE_SSL_PORT}/cws-ui/rest/authenticate -u ${CWS_USERNAME}:${CWS_PASSWORD}`
echo "${AUTH_RESP}"

if [[ $AUTH_RESP != *"SUCCESS"* ]]; then
  echo "Invalid credentials.  Please try again."
  exit 1
fi

CWS_TOKEN=`cat ./cookies.txt | grep cwsToken | cut -f7`
echo $CWS_TOKEN > cws_token.txt

echo "+--------------------------------------------------------------------------------------------"
echo "| CWS authorization scheme is :  ${CWS_AUTH_SCHEME}                                          "
echo "+--------------------------------------------------------------------------------------------"
echo "| CWS token is   : '${CWS_TOKEN}'                                                            "
echo "| cookie file is : cookies.txt    (use for curl commands later)                              "
echo "| token file is  : cws_token.txt  (use in BPMN processes when making REST GET or POST calls) "
echo "+--------------------------------------------------------------------------------------------"
echo "| NOTE: If the authentication scheme is 'CAM', you can view the CAM token (cookie key/value) "
echo "|       in the last like of the cookies.txt file.                                            "
echo "|                                                                                            "
echo "| NOTE: The CWS token value above will be blank, unless the auth scheme is 'LDAP'.           "
echo "+--------------------------------------------------------------------------------------------"
echo "| Here are some examples of how to interact with CWS programmatically:                       "
echo "|                                                                                            "
echo "| CAM scheme:                                                                                "
echo "|   curl <CWS_REST_URL> -b \"iPlanetDirectoryPro=<CAM_TOKEN>\"                               "
echo "|   curl <CWS_REST_URL> -b \"<PATH_TO_COOKIES_FILE>\"                                        "
echo "|                                                                                            "
echo "| LDAP scheme:                                                                               "
echo "|   curl <CWS_REST_URL> -b \"cwsToken=<CWS_TOKEN>\"                                          "
echo "|   curl <CWS_REST_URL> -b \"<PATH_TO_COOKIES_FILE>\"                                        "
echo "|                                                                                            "
echo "| ALL schemes:                                                                               "
echo "|   curl <CWS_REST_URL> -u <USERNAME>:<PASSWORD>                                             "
echo "+--------------------------------------------------------------------------------------------"


