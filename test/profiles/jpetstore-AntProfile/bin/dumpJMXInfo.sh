#!/bin/bash

USAGE="HONK!"

echo "Executing '$0 $@'"

while getopts ":f:t:h:d:r:" OPTIONS; do
    case ${OPTIONS} in
	f ) JMXFILE=$OPTARG;;
	t ) NUM_THREADS=$OPTARG;;
	h ) DOMAIN=$OPTARG;;
	d ) DURATION=$OPTARG;;
	r ) RAMPUP=$OPTARG;;
	h ) echo $usage;;
	\? ) echo $usage
	exit 1;;
	* ) echo $usage
	exit 1;;
	
    esac
done

if [ -z "${JMXFILE}" ] || [ ! -f "${JMXFILE}" ]; then
    echo "JMX file missing or invalid: '${JMXFILE}'"
    exit 1
fi

PREFIX="ThreadGroup"
if [ -z "${NUM_THREADS}" ]; then
    NUM_THREADS=$(grep "${PREFIX}.num_threads" "${JMXFILE}" | head -n1 | grep -o -E "[[:digit:]]+")
fi
if [ -z "${RAMPUP}" ]; then
    RAMPUP=$(grep "${PREFIX}.ramp_time" "${JMXFILE}" | head -n1 | grep -o -E "[[:digit:]]+")
fi
LOOPCOUNT=$(grep "LoopController.loops" "${JMXFILE}" | head -n 1 | grep -o -E "\-*[[:digit:]]+")
SCHEDULER=$(grep "${PREFIX}.scheduler" "${JMXFILE}" | head -n1 | grep -o -E "false|true")
if [ -z "${DURATION}" ]; then
    DURATION=$(grep "${PREFIX}.duration" "${JMXFILE}" | head -n1 | grep -o -E "[[:digit:]]+")
fi
DELAY=$(grep "${PREFIX}.delay" "${JMXFILE}" | head -n1 | grep -o -E "[[:digit:]]+")
if [ -z "${DOMAIN}" ]; then
    DOMAIN=$(grep "HTTPSampler.domain" "${JMXFILE}" | head -n 1 | grep -o -E ">[[:alnum:]]+<" | grep -o -E "[[:alnum:]]+")
fi

echo "File:          | ${JMXFILE}"
echo "---------------------------------"
echo "Num_threads:   | ${NUM_THREADS}"
echo "ramp_time:     | ${RAMPUP} seconds"
echo "loop_count     | ${LOOPCOUNT}"
echo "scheduler:     | ${SCHEDULER}"
if [ "${SCHEDULER}" == "true" ]; then
    echo "duration:      | ${DURATION} seconds"
    echo "delay:         | ${DELAY} seconds"
fi
echo "domain:        | ${DOMAIN}"
