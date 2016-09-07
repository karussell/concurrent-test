# Concurrent Tests

A bit playground for making GraphHopper storage thread safe when writing too
(not just read thread safe).

# License

Apache license

# Installation

mvn clean install

# Run benchmarks

java -jar target/benchmarks.jar -i 12 -wi 12 -f 1