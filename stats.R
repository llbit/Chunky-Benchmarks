# Functions for collecting render statistics.

# Get results for benchmarked version.
get.results <- function(version) {
  df <- read.csv(paste0('data/', version, '.csv'), header=T, sep=' ')
  # Compute the megasamples per second statistic:
  df$sps <- ((df$width * df$height * df$spp) / df$time) / 1000.0
  df$version <- version
  df
}

merge.stats <- function(prev, version) {
  if (is.data.frame(prev)) {
    rbind(prev, get.results(version))
  } else {
    get.results(version)
  }
}

bench.compare <- function(versions) {
  stats <- NA
  for (version in versions) {
    stats <- merge.stats(stats, version)
  }
  stats
}
