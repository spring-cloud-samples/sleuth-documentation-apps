#!/bin/bash

kill `jps | grep "1.0.0.SLEUTH_DOCS.jar" | cut -d " " -f 1`
kill `jps | grep "zipkin.jar" | cut -d " " -f 1`
docker-compose kill || echo "Failed to kill docker compose started apps"