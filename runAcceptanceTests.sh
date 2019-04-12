#!/bin/bash

set -o errexit

# ======================================= FUNCTIONS START =======================================

CLOUD_DOMAIN=${DOMAIN:-run.pivotal.io}
CLOUD_TARGET=api.${DOMAIN}
CLOUD_PREFIX="docssleuth"
CLOUD_FOUNDRY_STACK="cflinuxfs3"

function login(){
    cf api | grep ${CLOUD_TARGET} || cf api ${CLOUD_TARGET} --skip-ssl-validation
    cf apps | grep OK || cf login
}

function app_domain(){
    D=`cf apps | grep $1 | tr -s ' ' | cut -d' ' -f 6 | cut -d, -f1`
    echo $D
}

function deploy_app(){
    deploy_app_with_name $1 $1
}

function deploy_app_with_name(){
    APP_DIR=$1
    APP_NAME=$2
    cd $APP_DIR
    cf push $APP_NAME --no-start -s "${CLOUD_FOUNDRY_STACK}"
    APPLICATION_DOMAIN=`app_domain $APP_NAME`
    echo determined that application_domain for $APP_NAME is $APPLICATION_DOMAIN.
    cf env $APP_NAME | grep APPLICATION_DOMAIN || cf set-env $APP_NAME APPLICATION_DOMAIN $APPLICATION_DOMAIN
    cf restart $APP_NAME
    cd ..
}

function deploy_service(){
    N=$1
    D=`app_domain $N`
    JSON='{"uri":"http://'$D'"}'
    cf create-user-provided-service $N -p $JSON
}

function reset(){
    app_name=$1
    echo "going to remove ${app_name} if it exists"
    cf apps | grep $app_name && cf d -f $app_name
    echo "deleted ${app_name}"
}

# ======================================= FUNCTIONS END =======================================


# ======================================= BUILD START =======================================
root=`pwd`
./gradlew clean --parallel
#echo -e "\n\nPrinting dependencies"
#./gradlew allDeps
echo -e "\n\nBuilding builds in parallel"
./gradlew build --parallel --refresh-dependencies

# ======================================= BUILD END   =======================================


# ======================================= DEPLOY START =======================================

echo -e "\nDeploying infrastructure apps\n\n"

READY_FOR_TESTS="no"
# create RabbitMQ
APP_NAME="${CLOUD_PREFIX}-rabbitmq"
echo "Booting RabbitMQ with name [${APP_NAME}]"
cf s | grep ${APP_NAME} && echo "found ${APP_NAME}" && READY_FOR_TESTS="yes" ||
    cf cs cloudamqp lemur ${APP_NAME} && echo "Started RabbitMQ" && READY_FOR_TESTS="yes"

if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
    echo "RabbitMQ failed to start..."
    exit 1
fi

# ====================================================
# Boot zipkin-stuff
READY_FOR_TESTS="no"
# create MySQL DB
APP_NAME="${CLOUD_PREFIX}-mysql"
echo -e "\n\nBooting up MySQL with name [${APP_NAME}]"
cf s | grep ${APP_NAME} && echo "found ${APP_NAME}" && READY_FOR_TESTS="yes" ||
    cf cs cleardb spark ${APP_NAME} && echo "Started ${APP_NAME}" && READY_FOR_TESTS="yes"

if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
    echo "MySQL failed to start..."
    exit 1
fi

# ====================================================
cd $root

echo -e "\n\nDeploying Zipkin Server"
zq=zipkin-server
ZQ_APP_NAME="${CLOUD_PREFIX}-$zq"
cd $root/$zq
reset $ZQ_APP_NAME
cf d -f $ZQ_APP_NAME
cd $root/zipkin-server
mkdir -p build
READY_FOR_TESTS="no"
DOWNLOAD_ZIPKIN="${DOWNLOAD_ZIPKIN:-true}"

pushd build
if [[ "${DOWNLOAD_ZIPKIN}" == "true" ]]; then
  echo -e "\nDownloading Zipkin Server"
  rm -rf zipkin.jar || echo "No zipkin.jar to remove"
  curl -sSL https://zipkin.io/quickstart.sh | bash -s
else
  echo "Won't download zipkin - the [DOWNLOAD_ZIPKIN] switch is set to false"
fi
popd

cf push -s "${CLOUD_FOUNDRY_STACK}" && READY_FOR_TESTS="yes"

if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
    echo "Zipkin Server failed to start..."
    exit 1
fi
cd $root

# ====================================================

cd $root
echo -e "\n\nStarting brewery apps..."
deploy_app_with_name "service1" "${CLOUD_PREFIX}-service1"
deploy_app_with_name "service2" "${CLOUD_PREFIX}-service2"
deploy_app_with_name "service3" "${CLOUD_PREFIX}-service3"
deploy_app_with_name "service4" "${CLOUD_PREFIX}-service4"


# ====================================================

SERVICE1_HOST=`app_domain ${CLOUD_PREFIX}-service1`
ZIPKIN_SERVER_HOST=`app_domain ${CLOUD_PREFIX}-zipkin-server`
echo -e "Service1 host is [${SERVICE1_HOST}]"
echo -e "Zikpin server host is [${ZIPKIN_SERVER_HOST}]"

# ======================================= DEPLOY END =======================================

# ======================================= TEST START =======================================

echo -e "Running acceptance tests"

cd $root
export LOCAL_URL="http://${ZIPKIN_SERVER_HOST}"
export SERVICE1_ADDRESS="${SERVICE1_HOST}"
./gradlew :acceptance-tests:acceptanceTests -DLOCAL_URL="http://${ZIPKIN_SERVER_HOST}" -Dservice1.address="${SERVICE1_HOST}" -Dzipkin.query.port=80 --stacktrace --no-daemon --configure-on-demand

# ======================================= TEST END   =======================================
