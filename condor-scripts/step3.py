#!/usr/bin/env python

import sys
import os
import argparse

pjoin = os.path.join
basedir = os.path.abspath(pjoin(os.path.dirname(__file__), '..'))

def load_warc_names(fname):
    with open(fname) as fp:
        for line in fp:
            cols = line.strip().split()
            if len(cols) == 2:
                yield cols[0].replace('.warc.gz', '')


def check_dir(path):
    dir_name = os.path.dirname(path)
    if not os.path.isdir(dir_name):
        os.makedirs(dir_name)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('ds', choices=('cw09', 'cw12'))
    parser.add_argument('warc_dir')
    parser.add_argument('ann_dir')
    parser.add_argument('out_dir')
    parser.add_argument('log_dir')

    config_dir = pjoin(basedir, 'scritps/condor-jobs')
    jar_path = pjoin(basedir, 'target/clueweb-annotations-0.2.0-SNAPSHOT-jar-with-dependencies.jar')

    opts = parser.parse_args()
    # Load file counts using config dir and dataset
    fname_counts = pjoin(config_dir, '%s-counts.txt' % opts.ds)
    warcs = load_warc_names(fname_counts)

    # Iterate over the files
    for warc_name in warcs:
        warc_fname = pjoin(opts.warc_dir, warc_name + '.warc.gz')
        ann_fname = pjoin(opts.ann_dir, warc_name + '.ann.tsv')
        out_fname = pjoin(outs.out_dir, warc_name + '.anns.tsv')
        log_fname = pjoin(outs.log_dir, warc_name + '.log')
        
        check_dir(out_fname)
        check_dir(log_fname)

        cmd = "java -cp %(jar)s lemur.cw.ann.DetectEncoding %(warc)s %(ann)s > %(out)s 2> %(log)s"
        
        print cmd % {
            'jar': jar_path,
            'warc': warc_fname,
            'ann': ann_fname,
            'out': out_fname,
            'log': log_fname
        }


if __name__ == '__main__':
    main()
