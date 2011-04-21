#!/bin/bash

# must be called from experiment root directory

source "bin/plotvars.inc.sh"

PLOT_AS="activeSessions"

# access log analysis
ACCESSLOG_HEADER_CSV="bin/accesslog_header.csv"
ACCESSLOG_CSV="log/accesslog.csv"
ACCESSLOG_FILTERED_CSV="log/accesslog_filtered.csv"
PLOT_BASE="plots/accesslog"
PLOT_SL="session_length_distr"
PLOT_RTDISTR="request_type_distr"
PLOT_RQRESPDENS_PREFIX="request_respt_distr_"
PLOT_RQTYPES_PER_SESSION="request_type_distr_per_session"
PLOT_ACTIVESESSIONS="activeSessions"

if [ ! -z "$(ls ${PLOT_BASE}/)" ]; then
    echo 
    echo "## cleaning plot directory"
    rm ${PLOT_BASE}/*
fi 

#plot accesslog-related stuff
function filter {   
    cp "${ACCESSLOG_HEADER_CSV}" "${ACCESSLOG_FILTERED_CSV}"
    (grep "/jpetstore.*/shop.*.shtml" "${ACCESSLOG_CSV}" \
	| sed s/"\/jpetstore.*\/shop\/"/""/g \
	| sed s/".shtml"/""/g \
	| sed s/"\"?"/"\""/g  \
	| sed s/"\["/""/g \
	| sed s/"\]"/""/g \
	| sed s/"+0.00 "/""/g \
	| sed s/"200.:"/"2008 "/g ) >> "${ACCESSLOG_FILTERED_CSV}" 
}
if [ -f "${ACCESSLOG_CSV}" ]; then
    if filter; then
	PDF_WIDTH=12
	PDF_HEIGHT=8
	R --vanilla <<EOF
accesslog_fn="${ACCESSLOG_FILTERED_CSV}"
sessions_fn="${PLOT_BASE}/${PLOT_SL}.pdf"
requests_fn="${PLOT_BASE}/${PLOT_RTDISTR}.pdf"
requestDuration_fn_prefix="${PLOT_BASE}/${PLOT_RQRESPDENS_PREFIX}"
rq_per_session_fn="${PLOT_BASE}/${PLOT_RQTYPES_PER_SESSION}.pdf"
activeSessions_fn="${PLOT_BASE}/${PLOT_ACTIVESESSIONS}.pdf"

gridcol="${GRIDCOL}"

## read and filter accesslog to variable accesslog
accesslog=data.frame()
removeSessionsLastSec=${REMOVESESSIONLASTSEC}
expIntervalMin=${EXPINTERVALMIN}
source("../../R/accesslog.R")

## some functions
source("../../R/util.R")

## generate Graphs
width=${PDF_WIDTH}/2;height=${PDF_HEIGHT}
source("../../R/accesslog_session_length_distr.R")

width=${PDF_WIDTH};height=${PDF_HEIGHT}
reqstats_table_csv_fn="${PLOT_BASE}/${FN_REQSTATS}.csv"
reqstats_table_html_fn="${PLOT_BASE}/${FN_REQSTATS}.html"
reqstats_table_latex_fn="${PLOT_BASE}/${FN_REQSTATS}.tex"
#source("../../R/accesslog_request_type_distr.R")

max_outlier_ratio=${MAX_OUTLIER_RATIO}
expIntervalMin=${EXPINTERVALMIN}
baw_binWidth=${BAWBINWIDTH}
plot_boxAndWhisker=TRUE
limY=TRUE
plot_density=FALSE
width=${PDF_WIDTH}
height=${PDF_HEIGHT}/2
reqstats_table_csv_fn="${PLOT_BASE}/${PLOT_RQRESPDENS_PREFIX}.csv"
reqstats_table_html_fn="${PLOT_BASE}/${PLOT_RQRESPDENS_PREFIX}.html"
reqstats_table_latex_fn="${PLOT_BASE}/${PLOT_RQRESPDENS_PREFIX}.tex"
source("../../R/accesslog_request_respt_distr.R")

width=${PDF_WIDTH};height=${PDF_HEIGHT}/2
source("../../R/accesslog_request_type_distr_per_session.R")

width=${PDF_WIDTH};height=${PDF_HEIGHT}/2
source("../../R/accesslog_activeSessions.R")
warnings()
rm(accesslog)
EOF
    fi
else
    echo "ERROR: ${ACCESSLOG_CSV} not found"
    exit 1
fi