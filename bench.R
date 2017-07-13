# This script runs multiple benchmark iterations.

args <- commandArgs(trailingOnly=T)
if (length(args) > 0) {
  version = args[1]
} else {
  cat('ERROR: No version specified!\n')
  stop()
}

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
