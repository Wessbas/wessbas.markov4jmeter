pdf(sessions_fn,height=height,width=width)

session_list = split(accesslog["session_id"],accesslog["session_id"])
frequencies=as.vector(sapply(session_list,nrow))
hist(frequencies, breaks=c((min(frequencies)-0.5):(max(frequencies)+0.5)),right=FALSE,freq=FALSE,main="Session Length Frequencies",xlab="Number of Requests")
grid(col=gridcol)

dev.off()
rm(session_list)
rm(frequencies)