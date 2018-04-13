#!/bin/bash

set -e

SERVICE1_PORT="${SERVICE1_PORT:-8081}"
SERVICE2_PORT="${SERVICE2_PORT:-8082}"
SERVICE3_PORT="${SERVICE3_PORT:-8083}"
SERVICE4_PORT="${SERVICE4_PORT:-8084}"

# build apps
./gradlew clean build --parallel

ROOT_FOLDER=${ROOT_FOLDER:-.}
if [[ "${JAVA_HOME}" != "" ]]; then
  JAVA_BIN="${JAVA_HOME}/bin/java"
else
  JAVA_BIN="java"
fi

nohup ${JAVA_BIN} -jar "${ROOT_FOLDER}/service1/build/libs/*.jar" --server.port="${SERVICE1_PORT}" > build/service1.log &
nohup ${JAVA_BIN} -jar "${ROOT_FOLDER}/service2/build/libs/*.jar" --server.port="${SERVICE2_PORT}" > build/service2.log &
nohup ${JAVA_BIN} -jar "${ROOT_FOLDER}/service3/build/libs/*.jar" --server.port="${SERVICE3_PORT}" > build/service3.log &
nohup ${JAVA_BIN} -jar "${ROOT_FOLDER}/service4/build/libs/*.jar" --server.port="${SERVICE4_PORT}" > build/service4.log &
