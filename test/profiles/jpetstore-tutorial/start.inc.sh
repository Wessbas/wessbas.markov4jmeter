#!/bin/bash

source env.properties

# uncomment to disable the respective logging
#TIMESTAMPS_LOG="/tmp/timestamps.jtl"
#ARRIVALCTRL_LOG="/tmp/activeSessions.csv"
ERROR_LOG="log/error.log"
ASSERTIONS_ERRORLOG="log/assertionErrors.log"
ACCESSLOGHEADER="log/access_log.header"

MAINTESTPLAN="jmx/TestPlan.jmx"
#MAINTESTPLAN="jmx/TestPlan-nonMarkov4JMeter.jmx"


# Dumps info about thread group in JMX file passed as $1
#
function dumpJMXInfo {
    if [ -z "$1" ] || [ ! -f "$1" ]; then
	echo "JMX file missing or invalid: '$1'"
	return 1
    fi

    PREFIX="ThreadGroup"
    NUM_TREADS=$(grep "${PREFIX}.num_threads" "$1" | head -n1 | grep -o -E "[[:digit:]]+")
    RAMPUP=$(grep "${PREFIX}.ramp_time" "$1" | head -n1 | grep -o -E "[[:digit:]]+")
    LOOPCOUNT=$(grep "LoopController.loops" "$1" | head -n 1 | grep -o -E "\-*[[:digit:]]+")
    SCHEDULER=$(grep "${PREFIX}.scheduler" "$1" | head -n1 | grep -o -E "false|true")
    DURATION=$(grep "${PREFIX}.duration" "$1" | head -n1 | grep -o -E "[[:digit:]]+")
    DELAY=$(grep "${PREFIX}.delay" "$1" | head -n1 | grep -o -E "[[:digit:]]+")

    #DOMAIN=$(grep "HTTPSampler.domain" "$1" | head -n 1 | grep -o -E ">[[:alnum:]]+<" | grep -o -E "[[:alnum:]]+")
    DOMAIN=${SRVHOST}

    echo "File:          | $1"
    echo "---------------------------------"
    echo "Num_threads:   | ${NUM_TREADS}"
    echo "ramp_time:     | ${RAMPUP} seconds"
    echo "loop_count     | ${LOOPCOUNT}"
    echo "scheduler:     | ${SCHEDULER}"
    if [ "${SCHEDULER}" == "true" ]; then
	echo "duration:      | ${DURATION} seconds"
	echo "delay:         | ${DELAY} seconds"
    fi
    echo "domain:        | ${DOMAIN}"
    return 0
}

#
# Copies tomcat accesslog.
# Depending whether local or remote, cp oder scp is used
# Function always returns success value 0
function mvAccesslog {
    if ! ssh "${SRVHOST}" "test -f ${TOMCATACCESSLOG}"; then
	echo "WARNING: '${SRVHOST}:${TOMCATACCESSLOG}' doesn't exist"
	return 0
    fi
    scp "${SSH_USER}@${SRVHOST}:${TOMCATACCESSLOG}" "log/accesslog.csv" && ssh "${SRVHOST}" "rm ${TOMCATACCESSLOG}"
}

## check whether access log exists
function accesslogExists {
    ssh "${SSH_USER}@${SRVHOST}" "test -s ${TOMCATACCESSLOG}"
}

## Check whether accesslogging enabled
function isAccessloggingEnabled {
    if ssh "${SRVHOST}" "grep --before-context=3 \"%s %b %S %D\" ${SERVER_XML} | grep \"<\!--\"  > /dev/null &&  grep --after-context=2 \"%s %b %S %D\" ${SERVER_XML} | grep \"\-\->\"  > /dev/null"; then
	return 1
    else
	return 0
    fi
}

while accesslogExists; do
    echo    "WARNING: Access log '${TOMCATACCESSLOG}' on ${SRVHOST} exists "
    echo -n "         and should be removed ... Press <ENTER>"
    read
done

echo 

echo 
if isAccessloggingEnabled; then
    echo    "INFO: Tomcat access logging is ENABLED"
else
    echo    "INFO: Tomcat access logging is DISABLED"
fi
echo

echo -n "WARNING: Make sure you restarted the server ... Press <ENTER>"
read

if [ -f "${TIMESTAMPS_LOG}" ]; then
    rm "${TIMESTAMPS_LOG}"
fi

if [ -f "${ARRIVALCTRL_LOG}" ]; then
    rm "${ARRIVALCTRL_LOG}"
fi

if [ ! -z "$(ls log/)" ]; then
    echo 
    echo "## cleaning log directory"
    rm log/*
fi

echo
echo "## Starting test at $(date) ..."
if ! dumpJMXInfo "${MAINTESTPLAN}"; then
    exit 1
fi
echo
CMD="${JMETER_BIN} -n -t ${MAINTESTPLAN} -JtimestampFile=${TIMESTAMPS_LOG} "\
"-Jarrivalctrl.log=${ARRIVALCTRL_LOG} -Jerrorlog=${ERROR_LOG}  "\
"-Jassertionerrors=${ASSERTIONS_ERRORLOG} "\
"-JSRVHOST=${SRVHOST} -JSRVPORT=${SRVPORT} -JSRVBASEPATH=${SRVBASEPATH}"
echo "# Executing '${CMD}'"
if ! ${CMD}; then
    echo "'${CMD}' failed"
    exit 1
fi

if test -f "${TIMESTAMPS_LOG}"; then
    mv "${TIMESTAMPS_LOG}" log/
fi

if test -f "${ARRIVALCTRL_LOG}"; then
    mv "${ARRIVALCTRL_LOG}" log/
fi

if test -f "jmeter.log";  then
    mv "jmeter.log" log/
    if grep "ERROR" "log/jmeter.log"; then
	echo "ERROR: 'log/jmeter.log' contains errors"
	exit 1
    fi
fi

if ! mvAccesslog; then
    echo "WARNING: failed to copy '${SRVHOST}:${TOMCATACCESSLOG}'"
fi

if test -s "${ASSERTIONS_ERRORLOG}"; then
    echo "WARNING: '${ASSERTIONS_ERRORLOG}' contains entries"
    #exit 1 we do not quit since assertion error leads to log error as well
fi
if test -s "${ERROR_LOG}"; then
    echo "ERROR: '${ERROR_LOG}' contains entries"
    exit 1
fi
echo "## .. finished test at $(date))"
echo
