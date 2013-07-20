#!/bin/bash

BASE_DIR=$( cd "$( dirname "$0" )" && pwd )

jobs=100

cat $BASE_DIR/jobs/step1-jobs.txt | parallel -j $jobs condor_run
