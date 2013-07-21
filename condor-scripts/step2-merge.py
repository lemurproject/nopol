#!/bin/bash

import sys
import os
import argparse

pjoin = os.path.join

def load_warc_names(fname):
    with open(fname) as fp:
        for line in fp:
            cols = line.strip().split()
            if len(cols) == 2:
                yield cols[0].replace('.warc.gz', '')

def get_ann_files(warc_fname, sub_dirs):
    for dir_name in sub_dirs:
        ann_name = pjoin(dir_name, warc_fname + '.tsv')
        if os.path.isfile(ann_name):
            yield ann_name

def get_command(outdir, warc_fname, ann_files):
    outfile = pjoin(outdir, warc_fname + '.tsv.gz')
    return 'cat %(files)s | sort | gzip > %(outfile)' % {
        'files': ' '.join(ann_files),
        'outfile': outfile
    }

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('config_dir')
    parser.add_argument('ds', choices=('cw09', 'cw12'))
    parser.add_argument('indir')
    parser.add_argument('outdir')

    opts = parser.parse_args()
    # Load file counts using config dir and dataset
    fname_counts = pjoin(opts.config_dir, '%s-counts.txt' % opts.ds)
    warcs = load_warc_names(fname_counts)

    # Get the subdirectories in the input dir
    sub_dirs = [pjoin(opts.indir, d) for d in os.path.listdir(opts.indir)]

    # Iterate over the files, finding all the directories that contain the file
    for warc_fname in warcs:
        ann_files = get_ann_files(warc_fname, sub_dirs)
        print get_command(opts.outdir, warc_fname, ann_files)


if __name__ == '__main__':
    main()
