#!/bin/bash

set -o errexit

RABBIT_PORT=${RABBIT_PORT:-9672}
DEFAULT_HEALTH_HOST=${DEFAULT_HEALTH_HOST:-localhost}
export SPRING_RABBITMQ_HOST="${DEFAULT_HEALTH_HOST}"
export SPRING_RABBITMQ_PORT="${RABBIT_PORT}"
WITH_RABBIT="${WITH_RABBIT:-yes}"
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
DOWNLOAD_ZIPKIN="${DOWNLOAD_ZIPKIN:-true}"

[[ -z "${MEM_ARGS}" ]] && MEM_ARGS="-Xmx128m -Xss1024k"

mkdir -p build

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

# build apps
./gradlew clean && ./gradlew build --parallel

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

# nohup ${JAVA_HOME}/bin/java ${DEFAULT_ARGS} ${MEM_ARGS} -jar zipkin-server/zipkin-server-*-exec.jar > build/zipkin-server.out &
pushd zipkin-server
mkdir -p build
cd build
if [[ "${DOWNLOAD_ZIPKIN}" == "true" ]]; then
  echo -e "\nDownloading Zipkin Server"
  rm -rf zipkin.jar || echo "No zipkin.jar to remove"
  curl -sSL https://zipkin.io/quickstart.sh | bash -s
else
  echo "Won't download zipkin - the [DOWNLOAD_ZIPKIN] switch is set to false"
fi
popd

echo -e "\nWaiting for 30 seconds for rabbit to work"
sleep 30

echo -e "\nStarting Zipkin Server..."
if [[ "${WITH_RABBIT}" == "yes" ]] ; then
    echo "Will use rabbit to send spans"
    ZIPKIN_ARGS="-DRABBIT_ADDRESSES=${DEFAULT_HEALTH_HOST}:${RABBIT_PORT}"
else
    echo "Will use web to send spans"
    MEM_ARGS="${MEM_ARGS} -Dspring.zipkin.sender.type=WEB"
fi
nohup ${JAVA_PATH_TO_BIN}java ${MEM_ARGS} ${ZIPKIN_ARGS} -jar zipkin-server/build/zipkin.jar > build/zipkin.log &

echo -e "\nStarting the apps..."
nohup ${JAVA_PATH_TO_BIN}java ${MEM_ARGS} -jar service1/build/libs/*.jar --server.port="${SERVICE1_PORT}"  > build/service1.log &
nohup ${JAVA_PATH_TO_BIN}java ${MEM_ARGS} -jar service2/build/libs/*.jar --server.port="${SERVICE2_PORT}"  > build/service2.log &
nohup ${JAVA_PATH_TO_BIN}java ${MEM_ARGS} -jar service3/build/libs/*.jar --server.port="${SERVICE3_PORT}"  > build/service3.log &
nohup ${JAVA_PATH_TO_BIN}java ${MEM_ARGS} -jar service4/build/libs/*.jar --server.port="${SERVICE4_PORT}"  > build/service4.log &

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
