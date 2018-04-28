# Chunky Benchmarks

A benchmark harness and collection of benchmark scenes for Chunky.

## Benchmarking

To manually run a benchmark:

    gradle jar
    java -jar bench.jar bench path-to-chunky.jar
    # Wait...
    java -jar bench.jar stats


R scripts for power users make it easier to perform incremental benchmarking.
The script `bench.R` benchmarks a single version, and `latest.R` benchmarks the
latest commit. If the subdirectory `chunky` is already a git repo, then the
scripts pull the latest commits from it, otherwise the default Chunky GitHub
repo is cloned.
