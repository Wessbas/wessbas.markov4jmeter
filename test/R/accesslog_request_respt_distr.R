## begin remove (loads 'log/accesslog_filtered.csv')
# accesslog=data.frame()
# accesslog_fn="log/accesslog_filtered.csv"
# source("R/accesslog.R")
# plot_boxAndWhisker=TRUE
# plot_density=TRUE
# par(mfrow=c(3,1))
# baw_binWidth=1
# limY=TRUE
# i=6
#source("../R/util.R")
##  end remove (loads 'log/accesslog_filtered.csv')

## select only the required columns
accesslog_extract=subset(accesslog,exp_min>=expIntervalMin[1] & exp_min<=expIntervalMin[2])
request_list=split(subset(accesslog_extract,select=c(path,time,time_min,exp_min,duration_ms)),accesslog_extract["path"])
rm(accesslog_extract)

reqstats.table=sort.data.frame(data.frame(
   operation=names(request_list),
   minDurationMs=unlist(sapply(request_list, function(x) min(x[["duration_ms"]]),simplify=FALSE),use.names=FALSE),
   medianDurationMs=unlist(sapply(request_list, function(x) median(x[["duration_ms"]]),simplify=FALSE),use.names=FALSE),
   meanDurationMs=unlist(sapply(request_list, function(x) mean(x[["duration_ms"]]),simplify=FALSE),use.names=FALSE),
   stddevDurationMs=unlist(sapply(request_list, function(x) sd(x[["duration_ms"]]),simplify=FALSE),use.names=FALSE),
   maxDurationMs=unlist(sapply(request_list, function(x) max(x[["duration_ms"]]),simplify=FALSE),use.names=FALSE),
   n=unlist(sapply(request_list, nrow,simplify=FALSE),use.names=FALSE)
),key="operation")
write.csv(reqstats.table, file=reqstats_table_csv_fn)
if(require(xtable)){
  reqstats.xtable=xtable(reqstats.table)
  print(reqstats.xtable, type="html", file=reqstats_table_html_fn)  
  print(reqstats.xtable, type="latex", file=reqstats_table_latex_fn)  
}


for (i in 1:length(request_list)) {
   curName=names(request_list[i])

   pdf(paste(requestDuration_fn_prefix,curName,".pdf",sep=""),height=height,width=width)
   par(mfrow=c(1,1))

   curReq=request_list[[curName]]
   quant=quantile(curReq[["duration_ms"]],c(0.98))
   curReqQuant=subset(curReq,duration_ms<=quant)
   rm(quant)
   attach(curReqQuant)

   ## scatter plot
   plotFancyScatterplot(exp_min,duration_ms, main=paste("Scatter Plot of Response Times (", curName, ")", sep=""), 
	ylab="Response time (milliseconds)", xlab="Experiment time (minutes)")

   ## box-and-whysker plot
   if (plot_boxAndWhisker){
     plotFancyBoxplot(expDuration=exp_min,binWidth=baw_binWidth,resptime=duration_ms, limY=limY, 
	main=paste("Box-and-Whisker Plot of Experiment Response Times (",curName,")", sep=""),
	xlab="Experiment time (minutes)", ylab="Response time (ms)")
   }

   ## density plot
   if (plot_density){
     plotFancyDensity(data=duration_ms, main=paste("Density Plot of Response Times (", curName, ")", sep=""), 
     xlab="Response time (milliseconds)")
   }

   dev.off()
 }

rm(request_list)
