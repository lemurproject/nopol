#!/usr/bin/env python
'''Runs nopol on the English portion of Clueweb09.

This program creates the Condor scripts to process the subdirectories of the
disks 1 and 2.

'''

import sys
import os 
import glob
import argparse

def get_dir_pairs(base_out_dir, base_in_dir, in_dirs):    
    for d in in_dirs:
        d_abs = os.path.abspath(d)
        if not d_abs.startswith(base_in_dir):
            print >> sys.stderr, 'Skipping input directory: ', d_abs

        d_relative = os.path.relpath(d_abs, base_in_dir)
        out_d = os.path.join(base_out_dir, d_relative)

        d_id = d_relative.replace('/', '.')
        yield d_id, (d_abs, out_d)


def create_job(exe, logdir, dir_pairs):

    print 'Universe = vanilla'

    tpl = '''
executable = %s
arguments = %s %s
output = %s
error = %s
log = %s
queue
'''


    for id_dir, (in_dir, out_dir) in dir_pairs:
        out_file = os.path.join(logdir, '%s.out' % id_dir)
        err_file = os.path.join(logdir, '%s.err' % id_dir)
        log_file = os.path.join(logdir, '%s.log' % id_dir)

        print tpl % (exe, in_dir, out_dir, out_file, err_file, log_file)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('exe')
    parser.add_argument('output', help='Base output directory')
    parser.add_argument('logdir')
    parser.add_argument('base_input')
    parser.add_argument('input', help='Input directory', nargs='+')
    args = parser.parse_args()

    
    dir_pairs = get_dir_pairs(args.output, args.base_input, args.input)

    create_job(args.exe, args.logdir, dir_pairs)

if __name__ == '__main__':
    main()

