#!/bin/bash

set -o errexit

RABBIT_PORT=${RABBIT_PORT:-9672}
DEFAULT_HEALTH_HOST=${DEFAULT_HEALTH_HOST:-localhost}
export SPRING_RABBITMQ_HOST="${DEFAULT_HEALTH_HOST}"
export SPRING_RABBITMQ_PORT="${RABBIT_PORT}"
WITH_RABBIT="${WITH_RABBIT:-no}"
WAIT_TIME="${WAIT_TIME:-5}"
RETRIES="${RETRIES:-30}"
SERVICE1_PORT="${SERVICE1_PORT:-8081}"
SERVICE2_PORT="${SERVICE2_PORT:-8082}"
SERVICE3_PORT="${SERVICE3_PORT:-8083}"
SERVICE4_PORT="${SERVICE4_PORT:-8084}"
ZIPKIN_PORT="${ZIPKIN_PORT:-9411}"
JAVA_PATH_TO_BIN="${JAVA_HOME}/bin/"
if [[ -z "${JAVA_HOME}" ]] ; then
    JAVA_PATH_TO_BIN=""
fi

[[ -z "${MEM_ARGS}" ]] && MEM_ARGS="-Xmx128m -Xss1024k"

mkdir -p target

function check_app() {
    READY_FOR_TESTS="no"
    curl_local_health_endpoint $1 && READY_FOR_TESTS="yes" || echo "Failed to reach health endpoint"
    if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
        echo "Failed to start service running at port $1"
        print_logs
        exit 1
    fi
}

# ${RETRIES} number of times will try to curl to /health endpoint to passed port $1 and localhost
function curl_local_health_endpoint() {
    curl_health_endpoint $1 "127.0.0.1"
}

# ${RETRIES} number of times will try to curl to /health endpoint to passed port $1 and host $2
function curl_health_endpoint() {
    local PASSED_HOST="${2:-$HEALTH_HOST}"
    local READY_FOR_TESTS=1
    for i in $( seq 1 "${RETRIES}" ); do
        sleep "${WAIT_TIME}"
        curl --fail -m 5 "${PASSED_HOST}:$1/health" && READY_FOR_TESTS=0 && break || echo "Failed"
        echo "Fail #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds"
    done
    return ${READY_FOR_TESTS}
}

# Kills all docker related elements
function kill_docker() {
    docker ps -a -q | xargs -n 1 -P 8 -I {} docker stop {} || echo "No running docker containers are left"
}

export LOGZ_IO_API_TOKEN="${LOGZ_IO_API_TOKEN:-}"
PROFILES="notests,wavefront"

if [[ "${LOGZ_IO_API_TOKEN}" != "" ]]; then
  echo "Logz io token present - will enable the logzio profile"
  PROFILES="${PROFILES},logzio"
  TOKENS="--spring.profiles.active=logzio"
  rm -rf /tmp/logzio-logback-queue/
else
  echo "Logz io token missing"
  TOKENS="--spring.profiles.active=default"
fi

echo "Building the apps with profiles [${PROFILES}]"

./mvnw clean install -P"${PROFILES}"

if [[ "${WITH_RABBIT}" == "yes" ]] ; then
    # run rabbit
    #kill_docker || echo "Failed to kill"
    docker-compose kill  || echo "Failed to kill"
    docker-compose pull
    docker-compose up -d
fi

if [[ "${JAVA_HOME}" != "" ]]; then
  JAVA_BIN="${JAVA_HOME}/bin/java"
else
  JAVA_BIN="java"
fi

export WAVEFRONT_API_TOKEN="${WAVEFRONT_API_TOKEN:-}"
echo "Will prepend the following runtime arguments [${TOKENS}]"
TOKENS="${TOKENS} --management.metrics.export.wavefront.api-token=${WAVEFRONT_API_TOKEN} --management.metrics.export.wavefront.uri=${WAVEFRONT_URI:-https://demo.wavefront.com}"

mkdir -p build

echo -e "\nStarting the apps..."
nohup ${JAVA_PATH_TO_BIN}java ${MEM_ARGS} -jar service1/target/*.jar --debug --server.port="${SERVICE1_PORT}" ${TOKENS} > build/service1.log 2>&1 &
nohup ${JAVA_PATH_TO_BIN}java ${MEM_ARGS} -jar service2/target/*.jar --debug --server.port="${SERVICE2_PORT}" ${TOKENS}  > build/service2.log 2>&1 &
nohup ${JAVA_PATH_TO_BIN}java ${MEM_ARGS} -jar service3/target/*.jar --debug --server.port="${SERVICE3_PORT}" ${TOKENS}  > build/service3.log 2>&1 &
nohup ${JAVA_PATH_TO_BIN}java ${MEM_ARGS} -jar service4/target/*.jar --debug --server.port="${SERVICE4_PORT}" ${TOKENS}  > build/service4.log 2>&1 &

echo -e "\n\nChecking if Service1 is alive"
check_app ${SERVICE1_PORT}
echo -e "\n\nChecking if Service2 is alive"
check_app ${SERVICE2_PORT}
echo -e "\n\nChecking if Service3 is alive"
check_app ${SERVICE3_PORT}
echo -e "\n\nChecking if Service4 is alive"
check_app ${SERVICE4_PORT}
