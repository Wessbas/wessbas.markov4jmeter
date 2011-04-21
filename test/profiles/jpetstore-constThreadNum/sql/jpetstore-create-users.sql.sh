#!/bin/bash

# prints sql commands to stdout creating users in the jpetstore database
#
# $1 number of users to generate
# $2 username prefix (optional)
#
# Usernames will be of format ${1}xx

if [ -z "$1" ]; then
    (\
	echo "Usage: $(basename $0) <num> [<name_prefix>]"
	echo 
	echo "Example: "
	echo "         $(basename $0) user_ 10"
	echo " generates users user_0 ... user_9"
	echo ) >&2
    exit 1
fi

PREFIX="$2"
NUM="$1"

echo "use jpetstore;"

for ((i=0; i < ${NUM} ; i++)); do
    CURNAME="${PREFIX}${i}"
    echo    "INSERT INTO signon VALUES('${CURNAME}','${CURNAME}');"
    echo    "INSERT INTO profile VALUES('${CURNAME}','english','DOGS',1,1);"
    echo -n "INSERT INTO account VALUES('${CURNAME}','JPS-${CURNAME}@se.Informatik.Uni-Oldenburg.DE','firstname_${CURNAME}', "
    echo -n "'lastname_${CURNAME}', 'OK', '114-118 Ammerlaender Heerstr. Room A2-2-219a Desk ${i}', '', 'Oldenburg', "
    echo    "'N', '26129', 'GER',  '+49 441 798-999-${i}');"
done