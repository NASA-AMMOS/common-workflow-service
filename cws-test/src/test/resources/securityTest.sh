#!/bin/bash

# PROMPT USER FOR CWS CONSOLE HOST
while [ -z $CWS_CONSOLE_HOST ]; do
	read -p "Enter the CWS console host: " CWS_CONSOLE_HOST
	if [ -z $CWS_CONSOLE_HOST ]; then
		printf "Must specify a CWS hostname!\n\n"
	fi
done

# PROMPT USER FOR CWS CONSOLE SSL PORT
while [ -z $CWS_CONSOLE_SSL_PORT ]; do
	read -p "Enter the CWS console host port: " CWS_CONSOLE_SSL_PORT
	if [ -z $CWS_CONSOLE_SSL_PORT ]; then
		printf "Must specify a CWS hostname port!\n\n"
	fi
done

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

testNumber=1
output=""

run_test()
{
	local url=$1
	local expectedResultCode=$2
	local expectedOutput=$3
	
	echo "Running test $testNumber"
	echo "Contacting:  https://${CWS_CONSOLE_HOST}:${CWS_CONSOLE_SSL_PORT}/${url} ..."
	
	responseCode=$(curl -k -1 --write-out %{http_code} --silent --output /dev/null https://${CWS_CONSOLE_HOST}:${CWS_CONSOLE_SSL_PORT}/${url} -b cookies.txt)
	output=$(curl -k -1 --silent https://${CWS_CONSOLE_HOST}:${CWS_CONSOLE_SSL_PORT}/${url} -b cookies.txt)
	
	if [ "$responseCode" != $expectedResultCode ]; then
	   echo "Test #$testNumber failed return code check."
	   echo "Output is $output"
	   echo "Return code is ${responseCode}, but expected $expectedResultCode"
	   exit $testNumber
	else
		echo "Test #$testNumber Return Code Successful..."
	fi
	
	((testNumber++))
}

must_contain()
{
	let testNum=$testNumber-1

	echo "$output" | grep -q "$1"
	
	if [ $? -ne 0 ]; then
		echo "Test #$testNum Failed output check"
		echo "Output did not contain $1"
		exit $testNum
	else
		echo "Test #$testNum finished successfully!"
	fi
}

# other security scheme testing, loop through all of them


# This needs to be started in Bamboo first

#rm cookies.txt

#No AUTH tests
# These should redirect to login
run_test "cws-ui/" 302
run_test "cws-ui/home" 302
run_test "cws-ui/configuration" 302
# These should redirect to 403 forbidden page
run_test "engine-rest/execution/count" 404
run_test "camunda/app/cockpit/default/" 403

echo "Generating cookies.txt for authentication tests..."
# Generate cookies.txt for authentication tests
AUTH_RESP=`curl -k -1 -c ./cookies.txt -s https://${CWS_CONSOLE_HOST}:${CWS_CONSOLE_SSL_PORT}/cws-ui/rest/authenticate -u ${CWS_USERNAME}:${CWS_PASSWORD}`
echo "AUTH_RESP = $AUTH_RESP"

#AUTH tests
run_test "cws-ui/configuration" 200
must_contain "CWS Version"

run_test "engine-rest/execution/count" 200
must_contain "\"count\":"

run_test "camunda/app/cockpit/default/" 301
must_contain "<title>Camunda Cockpit</title>"

# Remove cookies.txt file
rm cookies.txt
