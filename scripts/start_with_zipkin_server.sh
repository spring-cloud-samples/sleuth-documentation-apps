#!/bin/bash

set -e

RABBIT_PORT=${RABBIT_PORT:-9672}
DEFAULT_HEALTH_HOST=${DEFAULT_HEALTH_HOST:-localhost}
export SPRING_RABBITMQ_HOST="${DEFAULT_HEALTH_HOST}"
export SPRING_RABBITMQ_PORT="${RABBIT_PORT}"
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

mkdir -p build

function check_app() {
    READY_FOR_TESTS="no"
    curl_local_health_endpoint $1 && READY_FOR_TESTS="yes"
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
        curl -m 5 "${PASSED_HOST}:$1/health" && READY_FOR_TESTS=0 && break
        echo "Fail #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds"
    done
    return ${READY_FOR_TESTS}
}

# build apps
./gradlew clean && ./gradlew build --parallel

# run zipkin stuff
docker-compose kill
docker-compose pull
docker-compose up -d

echo -e "\n\nWaiting for 10 seconds for rabbit to start"
sleep 10

if [[ "${JAVA_HOME}" != "" ]]; then
  JAVA_BIN="${JAVA_HOME}/bin/java"
else
  JAVA_BIN="java"
fi

echo -e "\nDownloading Zipkin Server"
# nohup ${JAVA_HOME}/bin/java ${DEFAULT_ARGS} ${MEM_ARGS} -jar zipkin-server/zipkin-server-*-exec.jar > build/zipkin-server.out &
pushd zipkin-server
mkdir -p build
cd build
[ -f "zipkin.jar" ] && echo "Zipkin server already downloaded" || curl -sSL https://zipkin.io/quickstart.sh | bash -s
popd

echo -e "\nStarting Zipkin Server..."
nohup ${JAVA_PATH_TO_BIN}java ${MEM_ARGS} -DRABBIT_ADDRESSES=${DEFAULT_HEALTH_HOST}:${RABBIT_PORT} -jar zipkin-server/build/zipkin.jar > build/zipkin.log &

echo -e "\nStarting the apps..."
nohup ${JAVA_PATH_TO_BIN}java ${MEM_ARGS} -jar service1/build/libs/*.jar > build/service1.log &
nohup ${JAVA_PATH_TO_BIN}java ${MEM_ARGS} -jar service2/build/libs/*.jar > build/service2.log &
nohup ${JAVA_PATH_TO_BIN}java ${MEM_ARGS} -jar service3/build/libs/*.jar > build/service3.log &
nohup ${JAVA_PATH_TO_BIN}java ${MEM_ARGS} -jar service4/build/libs/*.jar > build/service4.log &

echo -e "\n\nChecking if Zipkin is alive"
check_app ${ZIPKIN_PORT}
echo -e "\n\nChecking if Service1 is alive"
check_app ${SERVICE1_PORT}
echo -e "\n\nChecking if Service2 is alive"
check_app ${SERVICE2_PORT}
echo -e "\n\nChecking if Service3 is alive"
check_app ${SERVICE3_PORT}
echo -e "\n\nChecking if Service4 is alive"
check_app ${SERVICE4_PORT}
