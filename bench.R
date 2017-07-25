# This script runs multiple benchmark iterations.

args <- commandArgs(trailingOnly=T)
if (length(args) > 0) {
  version = args[1]
} else {
  cat('ERROR: No version specified!\n')
  stop()
}

benchmark(version)
