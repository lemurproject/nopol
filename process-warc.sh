#!/bin/bash
#
# Processes the WARC files on a directory.
#
DIR=$(cd $(dirname "$0"); pwd)

java="`which java`"

if [[ ! -x "$java" ]]; then
    java=/usr/java/latest/bin/java
fi

indir=$1
outdir=$2

if [[ ! -d $indir ]] || [[ -z $outdir ]]; then
    echo "Usage: $0 indir outdir"
    exit 1
fi

# Remove any trailing slashes and create the output directory if it's needed.
outdir="`echo $outdir | sed 's/\/$//'`"
if [[ ! -d $outdir ]]; then
    mkdir -p $outdir
fi

$java -cp $DIR/target/nopol-assembly-0.2.jar lemur.nopol.ProrcessWarc dir $indir $outdir

