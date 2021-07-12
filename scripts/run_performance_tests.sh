#!/bin/bash

set -e

wrk -t2 -c100 -d5 -R 18000 -L  http://localhost:9876/
