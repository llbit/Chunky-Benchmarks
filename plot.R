library(methods)
library(ggplot2)
library(plyr)

source('stats.R')

args <- commandArgs(trailingOnly=T)
if (length(args) > 0) {
  versions <- args
} else {
  versions <- list()
  for (f in list.files('data')) {
    if (nchar(f) > 7) {
      if (substring(f, nchar(f) - 6) == '-10.csv') {
        versions <- c(versions, substr(f, 1, nchar(f) - 7))
      }
    }
  }
  cat(paste0('Found versions: ', paste(versions, collapse=', '), '\n'))
}

data <- NA
for (version in versions) {
  cat(paste0('Reading version ', version, '\n'))
  df <- read.multiple(version, 1:10)
  if (is.data.frame(data)) {
    data <- rbind(data, df)
  } else {
    data <- df
  }
}

report <- ddply(data, .(scene, version), summarise, mean=mean(sps), sd=sd(sps), N=length(sps))
report$ci <- ci(report$sd, report$N)
print(report)

svg('report.svg')
plot <- ggplot(data, aes(scene, sps, color=version))
plot <- plot + geom_boxplot()
plot <- plot + theme(axis.text.x=element_text(angle=45, hjust=1))
print(plot)
invisible(dev.off())

