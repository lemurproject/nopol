#!/bin/bash

DIR=$(cd $(dirname "$0"); pwd)

java=/usr/java/latest/bin/java

indir=$1
outdir=$2

if [[ ! -d $indir ]] || [[ -z $outdir ]]; then
    echo "Usage: $0 indir outdir"
    exit 1
fi

outdir="`echo $outdir | sed 's/\/$//'`"

if [[ ! -d $outdir ]]; then
    mkdir -p $outdir
fi

$java -cp $DIR/target/nopol-assembly-0.1.jar lemur.nopol.Nopol dir $indir $outdir
