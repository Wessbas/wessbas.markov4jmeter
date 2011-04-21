##
## Script to plot the workload intensity curve with the R statistical environment (http://www.r-project.org/)
##

numSessions=function(min){
 8*cos((min+4.5)*2)+20
}


# pdf("JPS-varyingThreadNum-numSessions.pdf", width=12, height=4)
curve(numSessions(x),from=0,to=5, main="", xlab="", ylab="",xaxt="n",yaxt="n")
axis(side=2,line=-0.3,tick=FALSE,cex.axis=0.9);axis(side=2,line=0,tick=TRUE,labels=FALSE)
axis(side=1,line=-0.3,tick=FALSE,cex.axis=0.9);axis(side=1,line=0,tick=TRUE,labels=FALSE)
mtext("Experiment time (minutes)", side=1, line=2)
mtext("Active sessions", side=2, line=2)
grid()
# dev.off()
