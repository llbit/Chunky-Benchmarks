# This script is used to generate nice comparison renders (1000 SPP).

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

# Run benchmark.
result <- system2('java', c('-jar', 'bench.jar', 'bench', chunkyjar,
                            '--target', 1000))

if (result == 0) {
  if (!file.exists('renders')) {
    dir.create('renders')
  }
  dir.create(file.path('renders', version))
  for (f in list.files(file.path('temp', 'scenes'))) {
    if (substring(f, nchar(f) - 3) == '.png') {
      system2('cp', c(file.path('temp', 'scenes', f), file.path('renders', version, f)))
    }
  }
}
