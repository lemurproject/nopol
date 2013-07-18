#!/usr/bin/env python2.7
'''
Groups the annotations by the WARC file that included the original pages.

This script takes as input several annotation files (in .tsv format, optionally
gzipped), identifies the name of the WARC file that contains the page and, for
each one, creates an output file that contains the annotations included in the
respective WARC file. This allows us later to process the annotations and the
WARC files in parallel.

Usage: group-annotations.py [-h] {cw09,cw12} outdir ann_file [ann_file ...]

positional arguments:
  {cw09,cw12}  Dataset name
  outdir       Output directory
  ann_file     Annotation files in .tsv[.gz] format

The script relies a set of files that describe the datasets (CW09 and CW12) and 
are used to identify the WARC that contains a page. The files are stored in the
directory 'condor-jobs'.

'''

import sys
import os
import gzip
import argparse
import fileinput
import logging

pjoin = os.path.join

BASE_DIR = os.path.dirname(os.path.realpath(__file__))
JOBS_DIR = pjoin(BASE_DIR, 'condor-jobs')

assert os.path.isdir(JOBS_DIR), 'condor-jobs dir not found'

def load_dirlists(fname_root, fname_dirlists):
    '''
    Create a dictionary that maps directory names to their path in CW.

    It reads a list of directories of the dataset and returns a dictionary 
    that has as keys the name of the directory and values the relative
    path of the directory with respect to the root directory of the dataset.

    For example, for CW09 `fname_root` points to a file with the following
    content:

        /bos/tmp8/ClueWeb09

    While `fname_dirlists` will contain several lines with the list of 
    directories, as follows:

        /bos/tmp8/ClueWeb09/Disk1/ClueWeb09_English_1/en0001
        ...
        /bos/tmp8/ClueWeb09/Disk1/ClueWeb09_English_2/en0019

    Then, the returned dictionary will contain the following entries:

        en0001: Disk1/ClueWeb09_English_1/en0001
        en0021: Disk1/ClueWeb09_English_2/en0021

    This dictionary allows us to find the path in which the output file
    must be created, given the directory name encoded in the TREC-ID of a
    record.
    '''
    with open(fname_root) as fp:
        root = fp.read().strip()

    dirlists = {}

    with open(fname_dirlists) as fp:
        for line in fp:
            line = line.strip()
            path = os.path.relpath(line, root)
            path_parts = path.split('/')
            dirlists[path_parts[-1]] = path

    return dirlists

def parse_annotation(line):
    '''
    Parses an annotation and returns a tuple containing the directory name and
    the file number. For example:

        line = 'clueweb12-0202wb-28-00098       Black   10070   10075   5.8514895e-05   /m/019sc'
        parse_annotation(line)
        >> ('0202wb', '28')
    '''
    cols = line.split('\t')
    if len(cols) != 7:
        raise Exception('Error parsing annotation. Columns %d/7' % len(cols))

    name_parts = cols[0].split('-')
    return name_parts[1], name_parts[2]

class WarcOutput(object):
    '''Creates the output files based on the TREC-ID of an annotation.
    '''
    def __init__(self, ds, outdir, dirlists):
        self.ds = ds
        self.outdir = outdir
        self.fp = {}
        self.dirlists = dirlists

    def create_warc_out(self, ann):
        dirname, fileno = ann
        if self.ds == 'cw09':
            warc_fname = '%s.ann.tsv.gz' % fileno
        elif self.ds == 'cw12':
            warc_fname = '%s-%s.ann.tsv.gz' % (dirname, fileno)

        warc_dir = pjoin(self.outdir, self.dirlists[dirname])
        if not os.path.isdir(warc_dir):
            os.makedirs(warc_dir)

        fname = pjoin(warc_dir, warc_fname)
        fp = gzip.open(fname, 'a+')
        self.fp[ann] = fp
        return fp

    def add_ann(self, ann, line):
        fp = self.fp.get(ann)
        if not fp:
            fp = self.create_warc_out(ann)
        fp.write(line)

    def close(self):
        for key in self.fp:
            fp = self.fp[key]
            fp.close()

def main():
    parser = argparse.ArgumentParser(
        description='''
        Groups the annotations by the WARC file that included the original pages.'''
    )
    parser.add_argument('ds', choices=('cw09', 'cw12'), help='Dataset name')
    parser.add_argument('outdir', help='Output directory')
    parser.add_argument('ann_file', nargs='+', help='Annotation files in .tsv[.gz] format')

    args = parser.parse_args()

    logging.basicConfig()

    fname_root = pjoin(JOBS_DIR, '%s-root.txt' % args.ds)
    fname_dirlists = pjoin(JOBS_DIR, '%s-dirlist.txt' % args.ds)
    dirlists = load_dirlists(fname_root, fname_dirlists)
    outfiles = WarcOutput(args.ds, args.outdir, dirlists)

    ann_input = fileinput.FileInput(args.ann_file, openhook=fileinput.hook_compressed)

    for line in ann_input:
        try:
            ann = parse_annotation(line)
            outfiles.add_ann(ann, line)
        except:
            logging.exception('Error processing line: "%s"', line.strip())

    outfiles.close()


if __name__ == '__main__':
    main()
