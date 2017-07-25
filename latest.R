# Benchmark the latest available version, unless it has already been benchmarked.

source('stats.R')

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
latest <- system2('git', c('rev-list', 'origin/master', '--max-count=1'), stdout=TRUE)
version <- system2('git', c('describe', latest), stdout=TRUE)
setwd(bench.dir)
if (file.exists(file.path(bench.dir, 'data', paste0(version, '-10.csv')))) {
  cat(paste0('Error: version ', version, ' has already been benchmarked.\n'))
  stop()
}

cat(paste0('Benchmarking version ', version, '...\n'))
benchmark(version)
