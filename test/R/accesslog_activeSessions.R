## begin remove (creates session_table with random data)
#session_ids=letters[1:5]
#exp_min=round((rlnorm(n=20)*10))%%10+1
#duration_ms=rlnorm(n=20)*100
#accesslog=data.frame(session_id=session_ids,exp_min,duration_ms)
#session_table=data.frame(accesslog$session_id,tin=accesslog$exp_min,tout=accesslog$exp_min+(accesslog$duration_ms/(60*1000)))
#names(session_table)=c("session_id", "tin", "tout")
#source("R/util.R")
## end remove

## begin remove (loads 'log/accesslog_filtered.csv')
#accesslog=data.frame()
#accesslog_fn="log/accesslog_filtered.csv"
#source("R/accesslog.R")
## end remove

## begin: create session_table
ms2min_div=60*1000
session_list = split(accesslog,accesslog["session_id"])
session_table=data.frame(unname(t(
        # since we only have a resolution of a second in 
        # exp_min, we substract the duration at tout only
	sapply(session_list, function (x) c(tin=min(x$exp_min),tout=max(x$exp_min-x$duration_ms/ms2min_div)))))
)
names(session_table)=c("tin","tout")
## end: create session_table

## begin: create active_sessions_table
active_sessions_table=sessionTable2activeSessionsTable(session_table)
rm(session_table)
## end: create active_sessions_table


## begin: plot
pdf(activeSessions_fn,height=height,width=width)
par(mfrow=c(1,1))
plotFancyActiveSessionsLineplot(active_sessions_table, main="Active sessions", 
	xlab="Experiment time (minutes)", 
	ylab="Active sessions")
# plotFancyActiveSessionsRegressionplot(active_sessions_table, main="Active Sessions",
# 	xlab="Experiment time (minutes)", ylab="Sessions")
rm(active_sessions_table)
dev.off()
## end: plot