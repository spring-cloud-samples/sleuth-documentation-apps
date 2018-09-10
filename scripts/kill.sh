#!/bin/bash

<<<<<<< HEAD
kill `jps | grep "1.0.0.SLEUTH_DOCS.jar" | cut -d " " -f 1`
kill `jps | grep "zipkin.jar" | cut -d " " -f 1`
docker-compose kill || echo "Failed to kill docker compose started apps"
=======
kill `jps | grep "1.0.0.SLEUTH_DOCS.jar" | cut -d " " -f 1` || echo "No apps running"
kill `jps | grep "zipkin.jar" | cut -d " " -f 1` || echo "No zipkin running"
docker ps -a -q | xargs -n 1 -P 8 -I {} docker stop {} || echo "No docker containers running"
>>>>>>> c80fe9e... Please work
