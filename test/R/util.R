require(MASS)
require(fBasics)
dyn.load("../../R/utils_C.so")
#symbol.C("convert_event_table")

## mysql utils
mysqlSetTimeVariables=function(con, table, experimentID, expIntervalMin){
dbGetQuery(con,"SET @expStartTimeNs=0");
dbGetQuery(con,"SET @expIntervalStartNs=0");
dbGetQuery(con,"SET @expIntervalStopNs=0");
query=paste("SELECT @expStartTimeNs := MIN(tin) FROM ", table, " WHERE experimentID=",experimentID, " AND operation LIKE \"%ActionServlet%\"", sep="")
dbGetQuery(con,query)
min2ns=60*1000*1000*1000
query=paste("SELECT @expIntervalStartNs:=@expStartTimeNs+", expIntervalMin[1], "*", min2ns,sep="")
dbGetQuery(con,query)
query=paste("SELECT @expIntervalStopNs:=@expStartTimeNs+", expIntervalMin[2], "*", min2ns, sep="")
dbGetQuery(con,query)
rm(query)
}
##

##
sort.data.frame <- function(x, key, ...) {
    if (missing(key)) {
        rn <- rownames(x)
        if (all(rn %in% 1:nrow(x))) rn <- as.numeric(rn)
        x[order(rn, ...), , drop=FALSE]
    } else {
        x[do.call("order", c(x[key], ...)), , drop=FALSE]
    }
}
##

## removes normal and extreme outliers but at most
## a given ratio in [0..1]
## expects field respMs
remove_outliers_right = function (df, max_ratio=max_outlier_ratio){
  df.n=nrow(df)
  max_n_outliers=floor(df.n*max_ratio)
  iqr = IQR(df$respMs)
  q3 = quantile(df$respMs, 0.75)
  q_max_ratio = quantile(df$respMs, (df.n-max_n_outliers)/df.n)
  q_normal = q3 + (1.5*iqr)
  q_extreme = q3 + (3*iqr)
  subset(df, respMs<=max(q_max_ratio, q_normal))
}

## returns data frame with fields 
outlier_info_right = function (data) {
  iqr = IQR(data)
  q3 = quantile(data, 0.75)
  q_normal = q3 + (1.5*iqr)
  q_extreme = q3 + (3*iqr)
  n_outliers=length(data[data>q_normal])
  n_extreme=length(data[data>q_extreme])
  n_normal=n_outliers-n_extreme
  n = length(data)
  data.frame(   iqr,
                n_total=n_outliers, ratio_total=n_outliers/n, 
                q_normal, n_normal, ratio_normal=n_normal/n, 
                q_extreme, n_extreme, ratio_extreme=n_extreme/n)
}

## returns data frame with $idx, $val
runmean.steps = function (x, window_size, by, ...) {
   runningmean = runmean(x, k=window_size, endrule="trim", ...)
   num_steps=floor(length(runningmean)/by)
   idx=seq(from=1, to=by*num_steps, by=by)
   res=data.frame(idx=idx, val=runningmean[idx])
   rm(runningmean)
   res
}

## fancy density plot plot
pearsonModeSkewness <- function(x,dataMean,mode) {
  result = (dataMean - mode)/sd(x)
  return(result)
}

theSkewness <- function(data,dataMean) {
return(centralMoment(data,dataMean,3)/(centralMoment(data,dataMean,2)^(3/2)))
}

centralMoment <- function(data,dataMean,i) {
mean((data-dataMean)^i)
}

plotFancyDensityMultBw=function(data, main="", xlab="", ylab="") {
  dens=density(data,bw="nrd0")
  xlab=paste(xlab,"
N=", dens$n,sep="")

  plot(dens,main=main,xlab=xlab,col=1);
  grid(col=gridcol)
  rug(data)
  lines(density(data, bw="nrd"), col = 2)
  lines(density(data, bw="ucv"), col = 3)
  lines(density(data, bw="bcv"), col = 4)
#lines(density(entry[["respMs"]], bw="SJ-ste"), col = 5)
  lines(density(data, bw="SJ-dpi"), col = 5)

  legend("topright",
       legend = c("bw:nrd0", "bw:nrd", "bw:ucv", "bw:bcv", "bw:SJ-ste", "bw:SJ-dpi", "log-normal"),
       col = 1:6, lty = 1,box.lty=0)
}

plotFancyDensity=function(data, main="", xlab="", ylab="", ...) {
  dens=density(data,n=1024)
  xlab=paste(xlab,"
N=", dens$n, ", Bandwidth=",format(mean(dens$bw),digits=4),sep="")

  dataMean=mean(data)
  dataMeanY=approx(dens$x,dens$y,xout=dataMean)$y[1]
  dataMedian=median(data)
  dataMedianY=approx(dens$x,dens$y,xout=dataMedian)$y[1]
  densModeY=max(dens$y)
  densMode=approx(dens$y,dens$x,xout=densModeY)$y[1]
  moskewness = skewness(data) #theSkewness(data,dataMean);

  plot(dens,main=main,xlab=xlab, ...);
  grid(col=gridcol)
  rug(data)
  points(dataMean,dataMeanY,pch=3,lwd=1,type="h",lty="dashed",col="red");
  points(dataMean,dataMeanY,pch=1,lwd=2,col="red");
  points(dataMedian,dataMedianY,pch=4,lwd=1,type="h",lty="dashed",col="blue");
  points(dataMedian,dataMedianY,pch=2,lwd=2,col="blue");
  points(densMode,densModeY,pch=5,lwd=1,type="h",lty="dashed",col="darkgreen");
  points(densMode,densModeY,pch=3,lwd=2,col="darkgreen");

  t1 = paste("Mean ",format(dataMean,digits=4))
  t2 = paste("Median ",format(dataMedian,digits=4))
  t3 = paste("Approx. Mode ",format(densMode,digits=4))
  t4 = paste("Skewness ",format(moskewness,digits=4))
  #t5 = paste("Pearson Mode Skewness ",format(pearsonModeSkewness(data,dataMean,densMode),digits=4))
  t5 = paste("Kurtosis ",format(kurtosis(data),digits=4))

  if (moskewness < 0) { 
     legendpos = "topleft";	
  }
  else {
     legendpos = "topright";	
  }
  legend(legendpos,c(t1,t2,t3,t4,t5),pch=c(1,2,3,0,0),col=c("red","blue","darkgreen","white","white"),bty = "n")
}

plotFancyDensityGiven=function(dens, minimum, maximum, dataQ1, dataQ3, dataMean, dataMedian, densMode, skewness, kurt,
                               main="", xlab="", ylab="", ...) {
  xlab=paste(xlab,"
N=", dens$n, ", Bandwidth=",format(mean(dens$bw),digits=4),sep="")

#   dataMeanY=approx(dens$x,dens$y,xout=dataMean)$y[1]
#   dataQ1Y=approx(dens$x,dens$y,xout=dataQ1)$y[1]
#   dataMedianY=approx(dens$x,dens$y,xout=dataMedian)$y[1]
#   dataQ3Y=approx(dens$x,dens$y,xout=dataQ3)$y[1]
  densModeY=approx(dens$x,dens$y,xout=densMode)$y[1]

  plot(dens,main=main,xlab=xlab, lwd=1.5);
  grid(col=gridcol)

  abline(v=minimum, lwd=1,lty="dashed",col="black")
  abline(v=maximum, lwd=1,lty="dashed",col="black")
  abline(v=dataMean, lwd=1,lty="dashed",col="red")
  abline(v=dataQ1, lwd=1,lty="dashed",col="blue")
  abline(v=dataMedian, lwd=1,lty="dashed",col="blue")
  abline(v=dataQ3, lwd=1,lty="dashed",col="blue")
  points(densMode,densModeY, lwd=1,type="h",lty="dashed",col="green");
  points(densMode,densModeY,pch=1,lwd=2,col="green");

#   points(dataMean,dataMeanY,pch=3,lwd=1.5,type="h",lty="dashed",col="red");
#   points(dataMean,dataMeanY,pch=1,lwd=2,col="red");
#   points(dataQ1,dataQ1Y,pch=4,lwd=1.5,type="h",lty="dashed",col="blue");
#   points(dataQ1,dataQ1Y,pch=1,lwd=2,col="blue");
#   points(dataMedian,dataMedianY,pch=4,lwd=1.5,type="h",lty="dashed",col="blue");
#   points(dataMedian,dataMedianY,pch=1,lwd=2,col="blue");
#   points(dataQ3,dataQ3Y,pch=4,lwd=1.5,type="h",lty="dashed",col="blue");
#   points(dataQ3,dataQ3Y,pch=1,lwd=2,col="blue");
#   points(densMode,densModeY,pch=5,lwd=1.5,type="h",lty="dashed",col="green");
#   points(densMode,densModeY,pch=1,lwd=2,col="green");

  t0 = paste("min, max (", format(minimum,digits=4), ", ", format(maximum,digits=4), ")", sep="")
  t1 = paste("mean (",format(dataMean,digits=4), ")", sep="")
  t2 = paste("quartiles (",format(dataQ1,digits=4),", ",format(dataMedian,digits=4), ", ", format(dataQ3,digits=4), ")", sep="")
  t3 = paste("approx. mode (",format(densMode,digits=4), ")", sep="")
  t4 = paste("skewness (",format(skewness,digits=4), ")", sep="")
  #t5 = paste("Pearson Mode Skewness ",format(pearsonModeSkewness(data,dataMean,densMode),digits=4))
  t5 = paste("kurtosis (",format(kurt,digits=4), ")", sep="")

  if (skewness < 0) { 
     legendpos = "topleft";	
  }
  else {
     legendpos = "topright";	
  }
  legend(legendpos,c(t0,t1,t2,t3,t4,t5),pch=c(NA,NA,NA,NA,1,NA,NA),col=c("black","red","blue","green","white","white"),lwd=c("solid"),bty = "n")
}

plotFancyXYCurve = function( x,y=NULL, add=FALSE, grid=TRUE, ...){
  if (add){
    points(x=x, y=y, ...)
  }else{
    plot(x=x, y=y, ...)
  }
  lines(spline(x=x, y=y), ...)
  if(grid) grid(col=gridcol)
}

plotFancyXYLine = function(x, y=NULL, add=FALSE, grid=TRUE, type="b", ...){
  if (add){
    points(x=x, y=y, type=type, ...)
  }else{
    plot(x=x, y=y, type=type, ...)
  }
  if(grid) grid(col=gridcol)
}

## fancy bin boxplot
## bin_width is relative to the exp_duration
plotFancyBoxplot=function (expDuration, binWidth, resptime,limY=FALSE,main="",xlab="",ylab=""){
   expDurationBins=floor(expDuration/binWidth)*binWidth+binWidth
   df=data.frame(expDurationBins,resptime)
   attach(df) 
   rm(expDurationBins)
   avgs=unique(ave(resptime,expDurationBins))
   boxplots=boxplot(resptime~expDurationBins,df,log="",plot=FALSE) # dirty! need this only for upper hinge
   if(limY){
     boxplots=boxplot(resptime~expDurationBins,df,border="black",medcol="blue",log="",
	plot=TRUE,main=main,xlab=xlab,ylab=ylab#,outline=FALSE#,range=0
	,ylim=c(min(resptime),max(c(boxplots$stats[4,],max(avgs))))
     )
     grid(col=gridcol)
   }else{
     boxplots=boxplot(resptime~expDurationBins,df,border="black",medcol="blue",log="",
	plot=TRUE,main=main,xlab=xlab,ylab=ylab,outline=TRUE)
     grid(col=gridcol)
   }
   medians=boxplots$stats[3,]
   lines(avgs,lty="solid", col="red", lwd=1.5)
   lines(medians,lty="solid", col="blue", lwd=1.5)
   legend("topleft",c("Mean","Median"),lty=c("solid","solid"),col=c("red","blue"),box.lty=0)
}

## fancy scatter plot
scatter.smooth.lcol=function (x, y = NULL, span = 2/3, degree = 1, family = c("symmetric", 
    "gaussian"), xlab = NULL, ylab = NULL, ylim = range(y, prediction$y, 
    na.rm = TRUE), evaluation = 50, lcol="black", ...) 
{
    xlabel <- if (!missing(x)) 
        deparse(substitute(x))
    ylabel <- if (!missing(y)) 
        deparse(substitute(y))
    xy <- xy.coords(x, y, xlabel, ylabel)
    x <- xy$x
    y <- xy$y
    xlab <- if (is.null(xlab)) 
        xy$xlab
    else xlab
    ylab <- if (is.null(ylab)) 
        xy$ylab
    else ylab
    prediction <- loess.smooth(x, y, span, degree, family, evaluation)
    plot(x, y, ylim = ylim, xlab = xlab, ylab = ylab, ...)
    lines(prediction,col=lcol,lwd=1.5)
    invisible()
}

plotFancyScatterplot=function (expDuration, resptime, main="", xlab="", ylab=""){
   scatter.smooth.lcol(expDuration,resptime,main=main,xlab=paste(xlab,"
N=",length(resptime),sep=""),ylab=ylab,family="gaussian", lcol="red")
   grid(col=gridcol)
   legend("topleft",c("Local regression"),lty=c("solid"),col="red", box.lty=0)
}

## fancy throughput plot

plotFancyThroughputplot=function (expDuration, binWidth, reqList, main="", xlab="", ylab=""){
   expDurationBins=floor((expDuration)/binWidth)*binWidth+binWidth
   df=data.frame(expDurationBins, reqList)  
   bin_list = split(df,df["expDurationBins"])
   frequencies=as.vector(sapply(bin_list,nrow))
   times=as.integer(names(bin_list))
   plot(times,frequencies,type="h",main=main,xlab=xlab,ylab=ylab,xaxt="n",col="gray")
   grid(col=gridcol)
   lines(times,frequencies)
   axis(1, at=seq(min(times),max(times),binWidth))
}


## transforms a 'session table' into an 'active session table'
## session_table must contain the fields 'tin' and 'tout'
## the result will contain the field 'event_list' and 'active_sessions'
## if a t_i occurs multiple times, the maximum number of active sessions
## for t is set.
#session_table=data.frame(tin=round(rlnorm(10)*20), tout=round(rlnorm(10)*50)+150)
sessionTable2activeSessionsTable=function (session_table){
  event_table=sort.data.frame(rbind.data.frame(data.frame(event_list=session_table$tin,active_sessions=1),
                                               data.frame(event_list=session_table$tout,active_sessions=-1)
                                   ), key="event_list")
  t_list=unique(event_table$event_list)
  num_list=rep(0,length(t_list))
#  n[1] = 1
#  cur_t = event_table$event_list[1]
#  cur_t_idx=1 # invalid but will be incremented in the first iteration
#  for (i in 2:length(event_table$active_sessions)){ # event_table$active_sessions[1]=1
#    if (event_table$event_list[i] > cur_t) {
#      cur_t=event_table$event_list[i]
#      cur_t_idx=cur_t_idx+1
#    }
#    event_table$active_sessions[i]=event_table$active_sessions[i-1]+event_table$active_sessions[i]
#    if(event_table$active_sessions[i] > n[cur_t_idx]) n[cur_t_idx] = event_table$active_sessions[i]
#  }

  ret = .C("convert_event_table", event_list_t=as.double(event_table$event_list), event_list_action=as.integer(event_table$active_sessions), n=nrow(event_table),
                                  t_list=as.double(t_list), num_list=as.integer(num_list), m=length(num_list))
  rm(event_table); rm(t_list); rm(num_list)
  data.frame(event_list=ret$t_list, active_sessions=ret$num_list)
  # This is the old and stupid way of doing it:
  #attach(session_table)
  #event_list=sort(unique(c(tin,tout)))
  #active_sessions=sapply(event_list,function(x) nrow(subset(session_table,x>=tin&x<tout)))
  #active_sessions_table=data.frame(event_list,active_sessions)
  #rm(event_list)
  #rm(active_sessions)
  #active_sessions_table
}

## fancy active sessions plot (lines)
plotFancyActiveSessionsLineplot=function (active_sessions_table, main="", xlab="", ylab=""){
  attach(active_sessions_table)
  plot(event_list, active_sessions,type="s", xaxt="n", main=main, xlab=xlab, ylab=ylab)
  grid(col=gridcol)
  axis(1, at=seq(floor(min(event_list)),ceiling(max(event_list))))
  detach(active_sessions_table)
}

## fancy active sessions plot (scatter with smooth regression line)
## TODO: probably this function has a bad performance since we draw a scatter plot with white
##       points ;-) 
##       It should be sufficient to only plot the regression line with lines(loess.smooth(...)) 
##       but we get an error concerning xlim and ylim 
plotFancyActiveSessionsRegressionplot=function (active_sessions_table, main="", xlab="", ylab=""){
  attach(active_sessions_table)
  scatter.smooth(event_list, active_sessions,xaxt="n", main=main, xlab=xlab, ylab=ylab,col="white",family="gaussian")
  grid(col=gridcol)
  axis(1, at=seq(floor(min(event_list)),max(ceiling(event_list))))
  legend("topleft",c("Local regression"),lty=c("solid"),box.lty=0)
  detach(active_sessions_table)
}

## execute maximum likely hood optimization for 3-parameter log-normal distribution parameters
## returns a data frame row with columns meanlog, sdlog, shift
fitL3norm = function(data){
  data.unique.sort=sort(unique(data))
#  data.shiftEst=data.unique.sort[1]-(data.unique.sort[2]-data.unique.sort[1])
  data.shiftEst=data.unique.sort[1]-(data.unique.sort[10]-data.unique.sort[1])
  data.shiftedlog=log(data-data.shiftEst)
  data.shiftedlog.mean=mean(data.shiftedlog)
  data.shiftedlog.sd=sd(data.shiftedlog)
  rm(data.shiftedlog)
  rm(data.unique.sort)

  fittedL3norm=fitdistr(data, dl3norm, 
                          start=list(meanlog=data.shiftedlog.mean, 
                                      sdlog=data.shiftedlog.sd, 
                                      shift=data.shiftEst), 
                          verbose=FALSE
#                   , method=c("BFGS")
#                          ,lower(0,0.001,-Inf), upper(Inf,Inf,min(data))
               )
  #"BFGS": most results but sometimes quits with non-finite finite-difference value [3]
  # "L-BFGS-B": often non-finite finite-difference value [3]
  #"SANN" : very slow!
  #"CG": doesn't find much
  fittedL3norm
}

## Aitchison1957 6.22 Cohen's Least Sample Value Method p. 56 (t estimated by quantile ... )
fitL3normCohenSave = function(data){
  data.min=min(data)
  data.min.shifted=data-data.min
  data.min.shifted.log=log(data.min.shifted+0.05)
  data.min.log.mean=mean(data.min.shifted.log)
  data.min.log.sd=sd(data.min.shifted.log)
  data.min.shifted.pmin=ecdf(data.min.shifted)(0)

  data.shiftEst=data.min-exp(data.min.log.mean+qnorm(data.min.shifted.pmin)*data.min.log.sd)
  data.min.shifted=data-data.shiftEst
  # ;-) shouldn't we use data.min.shifted in the following?
  data.min.log.mean=mean(data.min.shifted.log)
  data.min.log.sd=sd(data.min.shifted.log)

  tryCatch(
    fitdistr(data, dl3norm, start=list(meanlog=data.min.log.mean, sdlog=data.min.log.sd, shift=data.shiftEst)),
    error= function (e) {
            list(estimate=data.frame(meanlog=data.min.log.mean, sdlog=data.min.log.sd, shift=data.shiftEst),
            sd=data.frame(meanlog=NA, sdlog=NA, shift=NA))
           }
  )
}

plotDistributionFittings=function(data,l3norm.approx,lnorm.approx,norm.approx){
  l3norm.approx.estimate=l3norm.approx[["estimate"]]
  lnorm.approx.estimate=lnorm.approx[["estimate"]]
  norm.approx.estimate=norm.approx[["estimate"]]

  ## sample data against 3-parameter log-normal distribution with optimized parameters
  if(!any(is.na(as.vector(unlist(l3norm.approx.estimate))))){
    plotFancyDensity(data=data, main=paste("Density Plot of Response Times and
3-Parameter Log-Normal Distribution Model", sep=""), 
                         xlab="Response time (milliseconds)",col="darkgray")
    curve(dl3norm(x, l3norm.approx.estimate[["meanlog"]], 
                         l3norm.approx.estimate[["sdlog"]], 
                         l3norm.approx.estimate[["shift"]]),add=TRUE)
    qqplot(rl3norm(length(data),meanlog=l3norm.approx.estimate[["meanlog"]], 
                               sdlog=l3norm.approx.estimate[["sdlog"]], 
                               shift=l3norm.approx.estimate[["shift"]]),
             data,
             main="QQ Plot of Sample Data and
3-Parameter Log-Normal Distribution",
             ylab="Sample response time (ms)", 
             xlab=substitute(Lambda*"("*tau*"="*shift*", "*mu*"="*meanlog*", "*sigma*"="*sdlog*")",
                      list(shift=round(l3norm.approx.estimate[["shift"]],digits=3),
                            meanlog=round(l3norm.approx.estimate[["meanlog"]],digits=3),
                            sdlog=round(l3norm.approx.estimate[["sdlog"]],digits=3)))
    ); grid(col=gridcol); abline(0,1) 
  }

  ## sample data against 2-parameter log-normal distribution 
  plotFancyDensity(data=data, main=paste("Density Plot of Response Times and
2-Parameter Log-Normal Distribution Model
", sep=""), xlab="Response time (milliseconds)",col="darkgray")
  curve(dlnorm(x, lnorm.approx.estimate[["meanlog"]], 
                       lnorm.approx.estimate[["sdlog"]]),add=TRUE)
  qqplot(rlnorm(length(data),meanlog=lnorm.approx.estimate[["meanlog"]], 
                               sdlog=lnorm.approx.estimate[["sdlog"]]),
           data,
           main="QQ Plot of Sample Data and
2-Parameter Log-Normal Distribution",
           ylab="Sample response time (ms)", 
           xlab=substitute(Lambda*"("*mu*"="*meanlog*", "*sigma*"="*sdlog*")",
                      list(meanlog=round(lnorm.approx.estimate[["meanlog"]],digits=3),
                            sdlog=round(lnorm.approx.estimate[["sdlog"]],digits=3)))
  ); grid(col=gridcol); abline(0,1) 

  ## sample data against normal distribution with sample mean and sd
  plotFancyDensity(data=data, main=paste("Density Plot of Response Times ", 
                                               "and
Normal Distribution Model (Sample Mean)", sep=""), 
                                   xlab="Response time (milliseconds)",col="darkgray")
  curve(dnorm(x, norm.approx.estimate[["mean"]], norm.approx.estimate[["sd"]]),add=TRUE)
  qqplot(rnorm(length(data), norm.approx.estimate[["mean"]], norm.approx.estimate[["sd"]]),
           data,
           main="QQ Plot of Sample Data and
Normal Distribution (Sample mean)",
           ylab="Sample response time (ms)", 
           xlab=substitute(N*"("*mu*"="*mean*", "*sigma*"="*sd*")",
                              list(mean=round(norm.approx.estimate[["mean"]],digits=3),
                                    sd=round(norm.approx.estimate[["sd"]],digits=3)))
  ); grid(col=gridcol); abline(0,1) 
}

## we should optimize this one ;-)
## Usage Example: 
## ecdf_unserialized=unserialize(hexStr2RawVect(ecdf_hex))
## density_unserialized=unserialize(hexStr2RawVect(density_hex))
hexStr2RawVect = function (str){
  as.raw(paste("0x",unlist(strsplit(gsub("[[:space:]]$","",gsub("([[:xdigit:]][[:xdigit:]])","\\1 ",str,extended=TRUE))," ")),sep=""))
}

opStatsToTable = function(experimenttable,experimentid, expinterval, 
                          n_threads, n_traces, workload, operation, 
                          q_removed, n_removed, routlier_info,
                          CI,basicStats,approxMode,l3norm.approx, lnorm.approx, 
                          l3norm.skTestResult, lnorm.skTestResult, norm.skTestResult,
                          data.density, data.ecdf){
  l3norm.approx.estimation=l3norm.approx[["estimate"]]
  l3norm.approx.sd=l3norm.approx[["sd"]]

  lnorm.approx.estimation=lnorm.approx[["estimate"]]
  lnorm.approx.sd=lnorm.approx[["sd"]]

  if (is.na(l3norm.approx.estimation[["meanlog"]]) ||  is.na(l3norm.approx.estimation[["sdlog"]]) || is.na(l3norm.approx.estimation[["shift"]]))
    l3norm.approx.estimation=data.frame(meanlog="null", sdlog="null", shift="null")
  if (is.na(l3norm.approx.sd[["meanlog"]]) ||  is.na(l3norm.approx.sd[["sdlog"]]) || is.na(l3norm.approx.sd[["shift"]]))
    l3norm.approx.sd=data.frame(meanlog="null", sdlog="null", shift="null")

  if (is.na(lnorm.approx.estimation[["meanlog"]]) ||  is.na(lnorm.approx.estimation[["sdlog"]]))
    lnorm.approx.estimation=data.frame(meanlog="null", sdlog="null", shift="null")
  if (is.na(lnorm.approx.sd[["meanlog"]]) ||  is.na(lnorm.approx.sd[["sdlog"]]))
    lnorm.approx.sd=data.frame(meanlog="null", sdlog="null", shift="null")

  if (is.na(l3norm.skTestResult[["D"]]) ||  is.na(l3norm.skTestResult[["D"]]))
    l3norm.skTestResult=data.frame(D="null", p.value="null")
  if (is.na(lnorm.skTestResult[["D"]]) ||  is.na(lnorm.skTestResult[["D"]]))
    lnorm.skTestResult=data.frame(D="null", p.value="null")
  if (is.na(norm.skTestResult[["D"]]) ||  is.na(norm.skTestResult[["D"]]))
    norm.skTestResult=data.frame(D="null", p.value="null")

  query=paste(
    "INSERT INTO ", opstats_table ," (",
      "`date`,",
      "`experimenttable`,",
      "`experimentid`,",
      "`startMin`,",
      "`stopMin`,",
      "`workload`,",
      "`threads`,",
      "`throughputMin`,",
      "`operation`,",
      "`n`,",
      "`q_removed`,", 
      "`n_removed`,",
      "`rnoutliers_q`,",
      "`rnoutliers_n`,",
      "`rnoutliers_r`,",
      "`rxoutliers_q`,",
      "`rxoutliers_n`,",
      "`rxoutliers_r`,",
      "`CI`,",
      "`Variance`,",
      "`Stddev`,",
      "`Skewness`,",
      "`Kurtosis`,",
      "`min`,",
      "`1st Quartile`,",
      "`Mode`,",
      "`Median`,",
      "`LCL Mean`,",
      "`Mean`,",
      "`SE Mean`,",
      "`UCL Mean`,",
      "`3rd Quartile`,",
      "`max`,",
      "`l3norm meanLog`,",
      "`SE l3norm meanLog`,",
      "`l3norm sdLog`,",
      "`SE l3norm sdLog`,",
      "`l3norm shift`,",
      "`SE l3norm shift`,",
      "`lnorm meanLog`,",
      "`SE lnorm meanLog`,",
      "`lnorm sdLog`,",
      "`SE lnorm sdLog`,",
      "`l3norm S-K D`,",
      "`l3norm S-K p`,",
      "`lnorm S-K D`,",
      "`lnorm S-K p`,",
      "`norm S-K D`,",
      "`norm S-K p`,",
      "`ecdf_raw`,",
      "`density_raw`",
    ")",
   " VALUES (",
      "NOW()",",",
      "\"",experimenttable,"\"",",",
      experimentid,",",
      expinterval[1],",",
      expinterval[2],",",
      workload,",",
      n_threads,",",
      n_traces/(expinterval[2]-expinterval[1]),",",
      "\"",operation,"\",",
      basicStats["nobs",],",",
      q_removed,",",
      n_removed,",",
      routlier_info["q_normal"],",",
      routlier_info["n_normal"],",",
      routlier_info["ratio_normal"],",",
      routlier_info["q_extreme"],",",
      routlier_info["n_extreme"],",",
      routlier_info["ratio_extreme"],",",
      CI,",",
      basicStats["Variance",],",",
      basicStats["Stdev",],",",
      basicStats["Skewness",],",",
      basicStats["Kurtosis",],",",
      basicStats["Minimum",],",",
      basicStats["1. Quartile",],",",
      approxMode,",",
      basicStats["Median",],",",
      basicStats["LCL Mean",],",",
      basicStats["Mean",],",",
      basicStats["SE Mean",],",",
      basicStats["UCL Mean",],",",
      basicStats["3. Quartile",],",",
      basicStats["Maximum",],",",
      l3norm.approx.estimation[["meanlog"]],",",
      l3norm.approx.sd[["meanlog"]],",",
      l3norm.approx.estimation[["sdlog"]],",",
      l3norm.approx.sd[["sdlog"]],",",
      l3norm.approx.estimation[["shift"]],",",
      l3norm.approx.sd[["sdlog"]],",",
      lnorm.approx.estimation[["meanlog"]],",",
      lnorm.approx.sd[["meanlog"]],",",
      lnorm.approx.estimation[["sdlog"]],",",
      lnorm.approx.sd[["sdlog"]],",",
      l3norm.skTestResult[["D"]],",",
      l3norm.skTestResult[["p.value"]],",",
      lnorm.skTestResult[["D"]],",",
      lnorm.skTestResult[["p.value"]],",",
      norm.skTestResult[["D"]],",",
      norm.skTestResult[["p.value"]],",",
      "0x",paste(serialize(data.ecdf,connection=NULL),collapse=""),",",
      "0x",paste(serialize(data.density,connection=NULL),collapse=""),"",
   ")", sep=""
  )
#  print.noquote(query)
  con=dbConnect(m, group="performanceData")
  dbSendQuery(con,query)
  dbDisconnect(con)
}

## prints stats to plot
opStatsToPlot = function(operation, CI, basicStats, l3norm.approx, lnorm.approx, 
                         l3norm.skTestResult, lnorm.skTestResult, norm.skTestResult){
  basicStats.stats=row.names(basicStats)
  basicStats.numStats=nrow(basicStats)
  plot(0,0,type="n",xlab="", ylab="", main=paste("Basic Statistics (CI=",CI,")
",operation), xaxt = "n", yaxt="n",xlim=c(0,6),ylim=c(basicStats.numStats,0))
  for (i in 1:basicStats.numStats){
    text(0,i, basicStats.stats[i], adj=c(0,0))
    text(1,i, basicStats[basicStats.stats[i],], adj=c(0,0))
  }
  text(2,1, "l3norm Approximation:", adj=c(0,0)); 
  text(2,2, "shift", adj=c(0,0)); 
  text(3,2, round(l3norm.approx[["estimate"]][["shift"]],digits=4), adj=c(0,0))
  text(2,3, "meanlog", adj=c(0,0)); 
  text(3,3, round(l3norm.approx[["estimate"]][["meanlog"]],digits=4), adj=c(0,0))
  text(2,4, "sdlog", adj=c(0,0)); 
  text(3,4, round(l3norm.approx[["estimate"]][["sdlog"]],digits=4), adj=c(0,0))

  text(2,6, "lnorm Approximation:", adj=c(0,0)); 
  text(2,7, "meanlog", adj=c(0,0)); 
  text(3,7, round(lnorm.approx[["estimate"]][["meanlog"]],digits=4), adj=c(0,0))
  text(2,8, "sdlog", adj=c(0,0)); 
  text(3,8, round(lnorm.approx[["estimate"]][["sdlog"]],digits=4), adj=c(0,0))

  text(4,1, "S-K Test Results:", adj=c(0,0)); 
  text(4,2, "l3norm D", adj=c(0,0)); 
  text(5,2, round(l3norm.skTestResult[["D"]],digits=4), adj=c(0,0))
  text(4,3, "l3norm p-value", adj=c(0,0)); 
  text(5,3, round(l3norm.skTestResult[["p.value"]],digits=4), adj=c(0,0))
  text(4,4, "lnorm D", adj=c(0,0)); 
  text(5,4, round(lnorm.skTestResult[["D"]],digits=4), adj=c(0,0))
  text(4,5, "lnorm p-value", adj=c(0,0)); 
  text(5,5, round(lnorm.skTestResult[["p.value"]],digits=4), adj=c(0,0))
  text(4,6, "norm D", adj=c(0,0)); 
  text(5,6, round(norm.skTestResult[["D"]],digits=4), adj=c(0,0))
  text(4,7, "norm p-value", adj=c(0,0)); 
  text(5,7, round(norm.skTestResult[["p.value"]],digits=4), adj=c(0,0))
}

l3normSKTest = function (data, params){
  ksTestRes=ks.test(respMs,"pl3norm",shift=params[["shift"]], meanlog=params[["meanlog"]], sdlog=params[["sdlog"]])
  data.frame(D=ksTestRes[["statistic"]],p.value=ksTestRes[["p.value"]])
}

lnormSKTest = function (data, params){
  ksTestRes=ks.test(respMs,"plnorm",meanlog=params[["meanlog"]], sdlog=params[["sdlog"]])
  data.frame(D=ksTestRes[["statistic"]],p.value=ksTestRes[["p.value"]])
}

normSKTest = function (data, params){
  ksTestRes=ks.test(respMs,"pnorm",mean=params[["mean"]], sd=params[["sd"]])
  data.frame(D=ksTestRes[["statistic"]],p.value=ksTestRes[["p.value"]])
}

## tests
##
# generate random grouped data
#A=sort(rep(seq(0,39,by=1), 1))
#B=sapply(A,sqrt)+A
#C=B*abs(rnorm(40))
#Z=data.frame(A,C)
#names(Z)=c("min","resp")
#plotFancyBoxplot(expDuration=Z[["min"]], binWidth=5, resptime=Z[["resp"]],
#	main="Box-and-Whisker Plot of Experiment Response Times",limY=TRUE,
#	xlab="Experiment time (minutes)",ylab="Response time (ms)")
#plotFancyThroughputplot(expDuration=Z[["min"]], binWidth=1, reqList=Z[["resp"]],
#	main="Throughput",
#	xlab="Experiment time (minutes)",ylab="Requests")

## test active sessions
#session_id=letters[1:10]
#tin=round((rlnorm(n=10)*10))%%10+1
#tout=round((rlnorm(n=10)*10))%%10+11
#session_table=data.frame(session_id,tin,tout)
#plotFancyActiveSessions(session_table, main="Active sessions", 
#	xlab="Experiment time (minutes)", 
#	ylab="Sessions")

## test plotDistributionFittings
#require(MASS)
#source("../R/l3norm.R")
#data=rl3norm(n=1000)
#gridcol="darkgray"
#par(mfrow=c(3,2))
#plotDistributionFittings(data)