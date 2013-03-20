#!/bin/bash

# Creates the directory lists for CW09 and CW12. 
# Each list includes the directories that directly contain the WARC files.

outdir=$1

if [ -z $outdir ]; then
    echo "Usage: $0 outdir"
    exit 1
fi


CW09=/bos/tmp8/ClueWeb09

echo $CW09 >> $outdir/cw09-root.txt

# Record counts
rm -f $outdir/cw09-counts.txt
for i in 1 2; do
    rec_dir="$CW09/Disk$i/record_counts"
    rec_files="`find $rec_dir -name 'ClueWeb09_English_*'`"
    for f in $rec_files; do
        prefix="Disk$i\/`basename $f | sed s/_counts.txt//`"
        cat $f | sed "s/^\*\./$prefix/" >> $outdir/cw09-counts.txt
    done
done

# Directories
rm -f $outdir/cw09-dirlist.txt
for i in 1 2; do
    find $CW09/Disk$i -mindepth 2  -type d >> $outdir/cw09-dirlist.txt
done


CW12=/bos/tmp16/ClueWeb12_Data

echo $CW12 >> $outdir/cw12-root.txt

# Directories
find $CW12 -mindepth 3 -type d | grep -v 'OtherData' > $outdir/cw12-dirlist.txt

# Record counts

rm -f $outdir/cw12-counts.txt
for i in 1 2 3 4; do
    rec_dir="$CW12/Disk$i/recordcounts"
    rec_files="`find $rec_dir -name 'ClueWeb12_*_counts.txt'`"
    for f in $rec_files; do
        prefix="Disk$i\/`basename $f | sed s/_counts.txt//`"
        cat $f | sed "s/^\./$prefix/" >> $outdir/cw12-counts.txt
    done
done


