#!/bin/bash

nopol=$1
clueweb_home=$2
base_out=$3

if [ -z $base_out ]; then
    echo "Usage: nopol-exe.sh clueweb09_dir outdir"
    exit 1
fi

outdir=$base_out/output
logdir=$base_out/log
jobsdir=$base_out/jobs

mkdir -p {$outdir,$logdir,$jobsdir}


disk=1
for n in `seq 1 5`; do
    python2.7 alelante.py $nopol $outdir $logdir $clueweb_home \
        $clueweb_home/Disk$disk/ClueWeb09_English_$n/* > $jobsdir/clueweb09-ner-disk$disk-eng-$n.job
done


disk=2
for n in `seq 6 10`; do
    python2.7 alelante.py $nopol $outdir $logdir $clueweb_home \
        $clueweb_home/Disk$disk/ClueWeb09_English_$n/* > $jobsdir/clueweb09-ner-disk$disk-eng-$n.job
done

