#accesslog <- read.table (file="log/accesslog_filtered.csv", header=TRUE, sep=' ', quote='"\'', dec='.',  na.strings = "NA", nrows = -1, skip =  0, check.names = TRUE, fill = FALSE, strip.white = FALSE, blank.lines.skip = TRUE, comment.char="#",stringsAsFactors=FALSE)

rq_type_list = split(accesslog["session_id"],accesslog["path"])

req_type_frequencies=data.frame(c(),c())

for (i in 1:length(rq_type_list)) {
   curName=names(rq_type_list[i])
   curSessionList=rq_type_list[[curName]]
   ## we most likely have the same bug here!
   freq_vec=as.vector(sapply(split(curSessionList,curSessionList["session_id"]),nrow))
   op_freq=data.frame(curName,freq_vec)
   names(op_freq)=c("request", "freq")
   req_type_frequencies=rbind(req_type_frequencies,op_freq)
}
pdf(rq_per_session_fn,height=height,width=width)
boxplot(freq~request, req_type_frequencies, main="Request Invocations per Session", ylab="Invocations per session", las=3) #xlab="Request type",
dev.off()
