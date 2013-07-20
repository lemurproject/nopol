#!/bin/bash

BASE_DIR=$( cd "$( dirname "$0" )" && pwd )

jobs=10

cat $BASE_DIR/jobs/step1-jobs-cw12.txt | parallel -j $jobs condor_run
