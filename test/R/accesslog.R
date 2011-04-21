## remove
#accesslog_fn="log/accesslog_filtered.csv"
#removeSessionsLastSec=60
#removeSessionsLastSec=0
#expIntervalMin=c(0,30)
## remove

accesslog_raw <- read.table (file=accesslog_fn, header=TRUE, sep=' ', quote='"\'', dec='.',  na.strings = "NA", nrows = -1, skip =  0, row.names = NULL, fill = FALSE, strip.white = FALSE, blank.lines.skip = TRUE, comment.char="#",stringsAsFactors=FALSE)

## convert time string hh:mm:ss to double representing the minute of day
convertTime = function  (timeStr){
    hms = as.numeric(unlist(strsplit(as.character(timeStr),":")))
    hms[1]*60+hms[2]+(hms[3]/60)
}
accesslog_raw["time_min"]=sapply(accesslog_raw[["time"]],convertTime)

## remove the first minute
# we possibly can even rely on that the first entry contains the minimum
startMin=min(accesslog_raw[["time_min"]]) 

# When willing to remove a warmup phase, the following needs to be used:
# accesslog2=subset(subset(accesslog_raw,time_min>=startMin+expIntervalMin[1] && time_min<startMin+expIntervalMin[2]))
accesslog2 = accesslog_raw

rm(accesslog_raw)
startMin=min(accesslog2[["time_min"]]) 
accesslog2["exp_min"]=accesslog2[["time_min"]]-startMin

# returns session ids of session which sent request during
# the last x seconds of the experiment
lastSec=removeSessionsLastSec
invalid_sessions=as.list(unique(subset(accesslog2,time_min>=max(accesslog2["time_min"])-lastSec/60)[["session_id"]]))
accesslog=subset(accesslog2,sapply(session_id,function (x) all(x!=invalid_sessions)))
rm(accesslog2)
