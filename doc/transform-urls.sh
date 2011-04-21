#!/bin/bash
BASEPATH="tutorial/"
OUTPATH="out/"
NODE_ORIG=(tutorial node1 node2 node3 node4 node5 node6 node7 node8 node9 node10 footnode)
NODE_REPL=(33147 33148 33149 33150 33151 33152 33153 33154 33155 33156 33157 34311)


REPLACEMENT="sed s/tutorial.html/${NODE_REPL[0]}.html/g"
for ((a=1; a <= 11 ; a++)); do 
    REPLACEMENT="${REPLACEMENT} | sed s/${NODE_ORIG[${a}]}\.html/${NODE_REPL[${a}]}\.html/g"
done
echo ${REPLACEMENT}

CMD="cat ${BASEPATH}/${NODE_ORIG[0]}.html | ${REPLACEMENT}"
eval ${CMD} > ${OUTPATH}/${NODE_REPL}.html
for ((a=1; a <= 11 ; a++)); do 
    CMD="cat ${BASEPATH}/${NODE_ORIG[${a}]}.html | ${REPLACEMENT}"
    eval ${CMD} > ${OUTPATH}/${NODE_REPL[${a}]}.html
done

echo "Output written to ${OUTPATH}"