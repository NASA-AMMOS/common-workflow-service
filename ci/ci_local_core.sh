#!/bin/bash

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source ${ROOT}/../utils.sh

INTERNET_TYPE=0
LOCAL_HOSTNAME=""

printf "What type of internet connection are you currently using?\n"
printf "    1) Wifi\n"
printf "    2) Wired internet\n"
printf "    3) Off-lab VPN\n"
printf "    4) Manually enter hostname\n"

while [[ ! $INTERNET_TYPE =~ ^(1|2|3|4)$ ]]; do
        read -p "Select a connection type (1/2/3/4): " INTERNET_TYPE
        if [[ $INTERNET_TYPE =~ ^(1|2|3|4)$ ]]
        then
                break  # Skip entire rest of loop.
        fi
        printf "  ERROR: Must specify either 1, 2, 3, or 4.\n\n";
done

# On-lab
if [ $INTERNET_TYPE == "1" ]; then
  LOCAL_HOSTNAME=`/sbin/ifconfig | grep inet | grep broadcast | tr -s [:space:] ' ' | cut -d " " -f3`
fi

if [ $INTERNET_TYPE == "2" ]; then
  LOCAL_HOSTNAME=$(nslookup $(ifconfig | grep inet | tail -1 | cut -d' ' -f2) | grep name | cut -d'=' -f2 | sed 's/.$//' | awk '{$1=$1};1')
fi


# Off-lab
if [ $INTERNET_TYPE == "3" ]; then
#  read -p "Enter your host name: " LOCAL_HOSTNAME
  LOCAL_HOSTNAME=`ifconfig | grep "inet " | tail -1 | cut -d' ' -f2`
fi

# Manually entered hostname / IP
if [ $INTERNET_TYPE == "4" ]; then
  read -p "Enter your hostname: " LOCAL_HOSTNAME
fi

printf "\nHostname set to '%s'\n\n" $LOCAL_HOSTNAME


#
# Update file that stores local hostname for CI
#
echo "$LOCAL_HOSTNAME" > ${ROOT}/../cws-test/src/test/resources/hostname.txt


print "Running ci_local.sh with hostname of: ${LOCAL_HOSTNAME}..."

${ROOT}/../stop_dev.sh | tee -a ${ROOT}/ci_log.txt

bash -c "${ROOT}/ci.sh NA cws_ci localhost 3306 cws_ci user password n camunda ${LOCAL_HOSTNAME} 'email@address' 'Continuous' 'Integration' 'email@address'" | tee -a ${ROOT}/ci_log.txt

cd ${ROOT}/..

bash -c ./test.sh | tee -a ${ROOT}/ci_log.txt

# -----------------------------------
# TESTS ARE NOW COMPLETE.  STOP CWS

# Needed if integration tests are not run...Remove otherwise
${ROOT}/../stop_dev.sh

print "Finished."
