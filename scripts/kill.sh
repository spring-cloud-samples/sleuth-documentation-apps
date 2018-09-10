#!/bin/bash

kill `jps | grep "1.0.0.SLEUTH_DOCS.jar" | cut -d " " -f 1` || echo "No apps running"
kill `jps | grep "zipkin.jar" | cut -d " " -f 1` || echo "No zipkin running"
docker ps -a -q | xargs -n 1 -P 8 -I {} docker kill {} || echo "No docker containers running"
