#!/bin/bash

kill `jps | grep "1.0.0.SLEUTH_DOCS.jar" | cut -d " " -f 1` || echo "No apps running"
kill `jps | grep "zipkin.jar" | cut -d " " -f 1` || echo "No zipkin running"
