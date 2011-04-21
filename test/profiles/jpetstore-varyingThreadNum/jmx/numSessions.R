numUsers=function(x){
 10*cos((x+4.5)/1.5)+20
}

#par(mfrow=c(2,1))

#pdf("numUsers-exp26.pdf", width=6, height=4)
curve(numUsers(x),from=0,to=10, main="", xlab="Experiment time in minutes", ylab="User count")
grid()
#axis(1, 0:24,0:24)
#dev.off()

#x=seq(0,24,by=0.25)
#plot(x,numUsers(x+6))
#axis(1, 0:24, 0:24)
