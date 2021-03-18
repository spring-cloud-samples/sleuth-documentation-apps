#!/bin/bash

set -e

sdk use java 15.0.0.hs-adpt

# Generate project with Sleuth, Wavefront, Web
# Add logback encoder, logz.io integration
# Copy the properties and logback
# Run without wavefront props
# Run with wavefront props from the command line

./mvnw spring-boot:run

http :9876/