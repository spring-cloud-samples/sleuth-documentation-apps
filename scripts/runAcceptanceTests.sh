#!/usr/bin/env bash

set -o errexit

function print_logs() {
    echo -e "\n\nSOMETHING WENT WRONG :( :( \n\n"
    echo -e "\n\nPRINTING LOGS FROM ALL APPS\n\n"
    tail -n +1 -- /build/*.log
}

function fail_with_message() {
    echo -e $1
    print_logs
    exit 1
}

# Kill the running apps
./scripts/kill.sh && echo "Killed some running apps" || echo "No apps were running"

# Next run the `./runApps.sh` script to initialize Zipkin and the apps (check the `README` of `sleuth-documentation-apps` for Docker setup info)
./scripts/start_with_zipkin_server.sh

echo -e "\n\nReady to curl first request"

./scripts/curl_start.sh || fail_with_message "Failed to send the request"

echo -e "\n\nReady to curl a request that will cause an exception"

./scripts/curl_exception.sh && fail_with_message "\n\nShould have failed the request but didn't :/" || echo -e "\n\nSent a request and got an exception!"

echo -e "\n\nRunning acceptance tests"
./scripts/run_acceptance_tests.sh

./scripts/kill.sh
