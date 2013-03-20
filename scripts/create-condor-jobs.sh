#!/bin/bash
#
# Creates the condor jobs needed to process all the files

set -e

dir=$(cd $(dirname $0); pwd)

jobsdir=$1
outdir=$2
EXE=$3

if [[ ! -d $jobsdir ]] || [[ ! -d $outdir ]]; then
    echo "Usage jobsdir outdir [executable]";
    echo ""
    echo "  jobsdir: A directory with the following files: "
    echo "           cw09-root.txt"
    echo "           cw09-dirlist.txt"
    echo "           cw12-root.txt"
    echo "           cw12-dirlist.txt"
    exit 1
fi

if [[ -z $EXE ]]; then
    EXE=$dir/../process-warc.sh
fi

# Number of directories to process on each job
N_STEPS=5

# Number of jobs to run in parallel
N_PAR=10

#

create_jobs() {
    ds=$1
    echo $ds

    dirs=$jobsdir/$ds-dirlist.txt
    counts=$jobsdir/$ds-counts.txt
    # Root for the input directory (ie. CW09|12 root directory) 
    dsroot="`cat $jobsdir/$ds-root.txt`"

    # Output directory
    ds_out=$outdir/$ds
    mkdir -p $ds_out

    job_ds_dir=$jobsdir/$ds
    mkdir -p $job_ds_dir

    if [[ ! -f $dirs ]]; then
        echo "$dirs file does not exist"
        exit 1
    fi

    logdir=$job_ds_dir/log
    mkdir -p $logdir

    # Split the directory list
    mkdir -p $job_ds_dir/steps
    split -l $N_STEPS $dirs $job_ds_dir/steps/dir-

    for step in `ls -1 $job_ds_dir/steps/dir-*`; do
        step_name="$ds-`basename $step`"
        $dir/alelante.py dirs $EXE $ds_out $logdir $dsroot \
            --input_file=$step > $job_ds_dir/steps/job-$step_name.condor
    done

    mkdir -p $job_ds_dir/dag
    $dir/alelante.py dag -p $N_PAR $job_ds_dir/dag $job_ds_dir/steps/job*.condor

    echo "Done. DAG created in $job_ds_dir/dag"

}

create_jobs cw09

create_jobs cw12

