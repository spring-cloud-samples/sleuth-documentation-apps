#!/bin/bash

set -e

DEFAULT_HEALTH_HOST=${DEFAULT_HEALTH_HOST:-localhost}

# build apps
./mvnw clean install -Ptests

mkdir -p build
TESTS_PASSED="no"
grep "Service1: Baggage for \[key\] is \[foo\]" build/service1.log &&
grep "Service2: Baggage for \[key\] is \[foo\]" build/service2.log &&
grep "Service3: Baggage for \[key\] is \[foo\]" build/service3.log &&
grep "Service4: Baggage for \[key\] is \[foo\]" build/service4.log &&
TESTS_PASSED="yes" && echo "Baggage works fine!"

# Check the result of tests execution
if [[ "${TESTS_PASSED}" == "yes" ]] ; then
    echo -e "\n\nBaggage was propagated successfully"
    exit 0
else
    echo -e "\n\nBaggage wasn't propagated"
    exit 1
fi
