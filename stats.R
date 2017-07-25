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

benchmark <- function(version) {
  # Clone Chunky.
  bench.dir <- getwd()
  cloned <- FALSE
  if (!file.exists(file.path(bench.dir, 'chunky'))) {
    cloned <- TRUE
    system2('git', c('clone', 'https://github.com/llbit/chunky.git', 'chunky'))
  }
  setwd(file.path(bench.dir, 'chunky'))
  if (!cloned) {
    system2('git', c('fetch'))
  }
  system2('git', c('reset', '--hard', version))
  system2('git', c('clean', '-fdX')) # Remove any unwanted files.
  system2('gradle', 'releaseJar')
  setwd(bench.dir)

  chunkyjar <- file.path(bench.dir, 'chunky', 'build', paste0('chunky-', version, '.jar'))

  # Build the benchmark tool.
  system2('gradle', c('jar'))

  for (iter in 1:10) {
    cat(paste0('Benchmark iteration ', iter, '\n'))

    # Run benchmark iteration.
    system2('java', c('-jar', 'build/libs/chunky-bench.jar', 'bench', chunkyjar,
                      '--target', 300))

    # Collect results
    csvout <- file.path('data', paste0(version, '-', iter, '.csv'))
    system2('java', c('-jar', 'build/libs/chunky-bench.jar', 'stats'), stdout=csvout)
  }
}
