#!/usr/bin/env bash

set -o errexit

export CURRENT_DIR="$( pwd )"
export KILL_AT_END="${KILL_AT_END:-yes}"
# since 8081-8084 are often taken will bump those to 9081-9084
export DEFAULT_HEALTH_HOST=${DEFAULT_HEALTH_HOST:-localhost}
export SERVICE1_PORT="${SERVICE1_PORT:-9081}"
export SERVICE1_ADDRESS="${SERVICE1_ADDRESS:-${DEFAULT_HEALTH_HOST}:${SERVICE1_PORT}}"
export SERVICE2_PORT="${SERVICE2_PORT:-9082}"
export SERVICE2_ADDRESS="${SERVICE2_ADDRESS:-${DEFAULT_HEALTH_HOST}:${SERVICE2_PORT}}"
export SERVICE3_PORT="${SERVICE3_PORT:-9083}"
export SERVICE3_ADDRESS="${SERVICE3_ADDRESS:-${DEFAULT_HEALTH_HOST}:${SERVICE3_PORT}}"
export SERVICE4_PORT="${SERVICE4_PORT:-9084}"
export SERVICE4_ADDRESS="${SERVICE4_ADDRESS:-${DEFAULT_HEALTH_HOST}:${SERVICE4_PORT}}"
export REPORTING_SYSTEM="${REPORTING_SYSTEM:-zipkin}"

echo -e "\n\nRunning apps on addresses:\nservice1: [${SERVICE1_ADDRESS}]\nservice2: [${SERVICE2_ADDRESS}]\nservice3: [${SERVICE3_ADDRESS}]\nservice4: [${SERVICE4_ADDRESS}]\n\n"

function print_logs() {
    echo -e "\n\nSOMETHING WENT WRONG :( :( \n\n"
    echo -e "\n\nPRINTING LOGS FROM ALL APPS\n\n"
    tail -n +1 -- "${CURRENT_DIR}"/build/*.log
}

function fail_with_message() {
    echo -e $1
    print_logs
    exit 1
}

export -f print_logs
export -f fail_with_message

if [[ "${KILL_AT_END}" == "yes" ]] ; then
    trap "{ ./scripts/kill.sh;docker ps -a -q | xargs -n 1 -P 8 -I {} docker rm --force {} || echo 'No docker containers running'; }" EXIT
fi

# Kill the running apps
./scripts/kill.sh

echo "Running reporting system [${REPORTING_SYSTEM}]"

if [[ "${REPORTING_SYSTEM}" == "zipkin" ]]; then
  # Next run the `./runApps.sh` script to initialize Zipkin and the apps (check the `README` of `sleuth-documentation-apps` for Docker setup info)
  ./scripts/start_with_zipkin_server.sh
elif [[ "${REPORTING_SYSTEM}" == "wavefront" ]]; then
  ./scripts/start_with_wavefront.sh
else
  fail_with_message "No matching reporting system"
fi

echo -e "\n\nReady to curl first request"

./scripts/curl_start.sh || fail_with_message "Failed to send the request"

echo -e "\n\nReady to curl a request that will cause an exception"

./scripts/curl_exception.sh && fail_with_message "\n\nShould have failed the request but didn't :/" || echo -e "\n\nSent a request and got an exception!"

echo -e "\n\nRunning acceptance tests"
./scripts/run_acceptance_tests.sh
