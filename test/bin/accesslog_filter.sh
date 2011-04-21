#!/bin/bash

ACCESSLOG_HEADER_CSV="bin/accesslog_header.csv"
ACCESSLOG_CSV="log/accesslog.csv"
ACCESSLOG_FILTERED_CSV="log/accesslog_filtered.csv"

PLOT_BASE="plots"
PLOT_SESSION_LENGTHS_PDF="${PLOT_BASE}/session_length_distribution.pdf"
PLOT_REQUEST_DISTRIBUTION_PDF="${PLOT_BASE}/request_distribution.pdf"

function filter {   
    cp "${ACCESSLOG_HEADER_CSV}" "${ACCESSLOG_FILTERED_CSV}"
    (grep "/jpetstore.*/shop.*.shtml" "${ACCESSLOG_CSV}" \
	| sed s/"\/jpetstore\/shop\/"/""/g \
	| sed s/".shtml"/""/g \
	| sed s/"\"?"/"\""/g  \
	| sed s/"\["/""/g \
	| sed s/"\]"/""/g \
	| sed s/"+0200 "/""/g \
	| sed s/"2007:"/"2007 "/g ) >> "${ACCESSLOG_FILTERED_CSV}" 
}

if filter; then
#    R --vanilla <<EOF
#accesslog <- read.table (file="${ACCESSLOG_FILTERED_CSV}", header=TRUE, sep=' ', quote='"\'', dec='.',  na.strings = "NA", nrows = -1, skip =  0, check.names = TRUE, fill = FALSE, strip.white = FALSE, blank.lines.skip = TRUE, comment.char="#")
#sessions_fn="${PLOT_SESSION_LENGTHS_PDF}"
#requests_fn="${PLOT_REQUEST_DISTRIBUTION_PDF}"
#
#source("R/session_length_distribution.R")
#source("R/request_distribution.R")
#
#EOF
true;
fi

