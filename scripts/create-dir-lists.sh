#!/bin/bash

# Creates the directory lists for CW09 and CW12. 
# Each list includes the directories that directly contain the WARC files.

CW09=/bos/tmp8/ClueWeb09

# Record counts
rm -f cw09-counts.txt
for i in 1 2; do
    rec_dir="$CW09/Disk$i/record_counts"
    rec_files="`find $rec_dir -name 'ClueWeb09_English_*'`"
    for f in $rec_files; do
        prefix="Disk$i\/`basename $f | sed s/_counts.txt//`"
        cat $f | sed "s/^\*\./$prefix/" >> cw09-counts.txt
    done
done

# Directories
rm -f cw09-dirlist.txt
for i in 1 2; do
    find $CW09/Disk$i -mindepth 2  -type d >> cw09-dirlist.txt
done


CW12=/bos/tmp16/ClueWeb12_Data

# Directories
find $CW12 -mindepth 3 -type d | grep -v 'OtherData' > cw12-dirlist.txt

# Record counts
find $CW12 -name '*_counts.txt' | cat | sort > cw12-counts.txt

rm -f cw12-counts.txt
for i in 1 2 3 4; do
    rec_dir="$CW12/Disk$i/recordcounts"
    rec_files="`find $rec_dir -name 'ClueWeb12_*_counts.txt'`"
    for f in $rec_files; do
        prefix="Disk$i\/`basename $f | sed s/_counts.txt//`"
        cat $f | sed "s/^\./$prefix/" >> cw12-counts.txt
    done
done


