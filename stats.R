# Functions for collecting render statistics.

# Get results for benchmarked version.
get.results <- function(version, version.name) {
  df <- read.csv(file.path('data', paste0(version, '.csv')), header=T, sep=' ')
  # Compute the megasamples per second statistic:
  df$sps <- ((df$width * df$height * df$spp) / df$time) / 1000.0
  df$version <- version.name
  df
}

merge.stats <- function(prev, version, version.name=version) {
  if (is.data.frame(prev)) {
    rbind(prev, get.results(version, version.name))
  } else {
    get.results(version, version.name)
  }
}

# To get the data in wide format, use for example:
# library(reshape)
# cc <- cast(df, scene~version, mean, value='sps')
# round(100 * (cc$d1 / cc$d2) - 100, 1) # Print relative difference.
bench.compare <- function(versions) {
  stats <- NA
  for (version in versions) {
    stats <- merge.stats(stats, version)
  }
  stats
}

read.multiple <- function(version, iters) {
  stats <- NA
  for (iter in iters) {
    name <- paste0(version, '-', iter)
    filename <- file.path('data', paste0(name, '.csv'))
    if (file.exists(filename)) {
      stats <- merge.stats(stats, name, version)
    }
  }
  stats
}

# Function to compute T distribution confidence interval for 90% confidence.
ci <- function(x.sd, x.N) {
  se <- x.sd / sqrt(x.N)
  mult <- qt(0.95, x.N-1)
  mult * x.sd
}

# report <- ddply(df, .(scene, version), summarise, mean=mean(sps), sd=sd(sps), N=length(sps))
# report$ci <- ci(report$sd, report$N)
