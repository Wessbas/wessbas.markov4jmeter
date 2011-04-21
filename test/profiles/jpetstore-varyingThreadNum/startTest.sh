#!/bin/bash

# wrapper script to tee stderr and stdout to file

OUT_LOG="log/out.log" 

./start.inc.sh 2>&1 | tee out.log
mv out.log ${OUT_LOG}