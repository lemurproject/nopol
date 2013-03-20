#!/bin/bash
#
# Creates the condor jobs needed to process all the files

set -e

dir=$(cd $(dirname $0); pwd)

EXE=$dir/dummy-exe.sh

OUT_DIR=$dir/dummy-out
mkdir -p $OUT_DIR

# Number of directories to process on each job
N_STEPS=5

# Number of jobs to run in parallel
N_PAR=5

outdir=$dir/condor-jobs

mkdir -p $outdir

#$dir/create-dir-lists.sh $outdir

#

create_jobs() {
    ds=$1
    echo $ds

    dirs=$outdir/$ds-dirlist.txt
    counts=$outdir/$ds-counts.txt
    dsroot="`cat $outdir/$ds-root.txt`"

    ds_out=$OUT_DIR/$ds
    mkdir -p $ds_out

    jobdir=$outdir/$ds
    mkdir -p $jobdir

    if [[ ! -f $dirs ]]; then
        echo "$dirs file does not exist"
        exit 1
    fi

    logdir=$jobdir/log
    mkdir -p $logdir

    # Split the directory list
    mkdir -p $jobdir/steps
    split -l $N_STEPS $dirs $jobdir/steps/dir-

    for step in `ls -1 $jobdir/steps/dir-*`; do
        step_name="$ds-`basename $step`"
        python $dir/alelante.py dirs $EXE $ds_out $logdir $dsroot \
            --input_file=$step > $jobdir/steps/job-$step_name.condor
    done

    mkdir -p $jobdir/dag
    python $dir/alelante.py dag -p $N_PAR $jobdir/dag $jobdir/steps/job*.condor

    echo "Done. DAG created in $jobdir/dag"

}



create_jobs cw09

