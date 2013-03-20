#!/usr/bin/env python
'''
A script to generate Condor scripts that execute commands in parallel
on multiple files or directories.
'''

import sys
import os 
import glob
import fileinput
import itertools as it
import shutil
import argparse

def read_arg_list(fname):
    '''Reads a list of arguments passed as a file'''
    if not fname:
        return []
    rtn = []
    with open(fname) as fp:
        return [line.strip() for line in fp if line.strip()]


def get_dir_pairs(base_out_dir, base_in_dir, in_dirs):    
    for d in in_dirs:
        d_abs = os.path.abspath(d)
        if not d_abs.startswith(base_in_dir):
            print >> sys.stderr, 'Directory does not exist: %s' % d_abs

        d_relative = os.path.relpath(d_abs, base_in_dir)
        out_d = os.path.join(base_out_dir, d_relative)

        d_id = d_relative.replace('/', '.')
        yield d_id, (d_abs, out_d)


def load_file_names(files):
    for fname in files:
        fname = fname.strip()
        if fname and os.path.isfile(fname):
            yield os.path.abspath(fname)


def create_job_dirs(exe, logdir, dir_pairs):
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


def create_job_files(exe, logdir, fnames):
    print 'Universe = vanilla'
    tpl = '''
executable = %s
arguments = %s
output = %s
error = %s
log = %s
queue
'''
    for id_file, fname in enumerate(fnames):
        out_file = os.path.join(logdir, '%s.out' % id_file)
        err_file = os.path.join(logdir, '%s.err' % id_file)
        log_file = os.path.join(logdir, '%s.log' % id_file)

        print tpl % (exe, fname, out_file, err_file, log_file)


def grouper(n, iterable):
    "Collect data into fixed-length chunks or blocks"
    # grouper(3, 'ABCDEFG') --> ABC DEF G
    args = [iter(iterable)] * n

    for g in it.izip_longest(*args):
        yield it.ifilter(None, g)

class Job(object):

    job_tpl = '''
universe   = vanilla
executable = %(exec)s
arguments  = %(args)s
%(logs)s
queue
    '''

    logs_tpl = '''
log        = %(basename)s.log
output     = %(basename)s.out
error      = %(basename)s.err
'''

    def __init__(self, command, args):
        self.command = command
        self.args = args

    def jobdef(self, basename=None):

        logs = ''
        if basename:
            logs = self.logs_tpl % {
                'basename': basename 
            }

        rtn = self.job_tpl % {
            'exec': self.command,
            'args': ' '.join(self.args),
            'logs': logs
        }

        return rtn

    def to_file(self, name):
        base = os.path.basename(name)
        with open(name, 'w+') as fp:
            fp.write(self.jobdef(base))


class JobDag(object):
    dag_tpl = '''
# Jobs
%(jobs)s

# Dependencies
%(deps)s

    '''

    def __init__(self, groups):
        self.jobs = []
        self.groups = []
        self.jfinal = Job('/bin/echo', ['Finished.'])
        self.jfinal_fname = 'final.condor'
        n_j = 0
        for gid, g in enumerate(groups, 1):
            group = set()
            for job_fname in g:
                job_id = 'J%s_%s' % (gid, n_j)
                self.jobs.append((job_id, job_fname))
                group.add(job_id)
                n_j += 1

            jstart = Job('/bin/echo', ['"group started %s"' % gid])
            jend = Job('/bin/echo', ['"group finished %s"' % gid])

            jstart_fname = 'g%s-start.condor' % gid
            jend_fname = 'g%s-end.condor' % gid

            self.groups.append( (gid, (jstart, jstart_fname), (jend, jend_fname), group) )


    def jobdef(self):
        jobs = []
        for (jid, jfname) in self.jobs:
            child_fname = os.path.basename(jfname)
            jobs.append('Job %s %s' % (jid, child_fname))

        deps = []
        for gid, jstart, jend, group in self.groups:
            jstart_job, jstart_fname = jstart
            jend_job, jend_fname = jend
            
            jstart_id = 'start_%s' % gid
            jend_id = 'end_%s' % gid

            jobs.append('Job %s %s' % (jstart_id, jstart_fname))
            jobs.append('Job %s %s' % (jend_id, jend_fname))

            deps.append('PARENT %s CHILD %s' % (jstart_id, ' '.join(group)))
            deps.append('PARENT %s CHILD %s' % (' '.join(group), jend_id))

            j_start_next_id = 'start_%s' % (gid + 1)
            deps.append('PARENT %s CHILD %s' % (jend_id, j_start_next_id))

        jfinal_id = 'start_%s' % (gid + 1)
        jobs.append('Job %s %s' % (jfinal_id, self.jfinal_fname))

        rtn = self.dag_tpl % {
            'jobs': '\n'.join(jobs),
            'deps': '\n'.join(deps)
        }
        return rtn

    def to_file(self, outdir):

        opath = lambda d: os.path.join(outdir, d)

        for (jid, jfname) in self.jobs:
            child_fname = opath(os.path.basename(jfname))
            shutil.copyfile(jfname, child_fname)

        for gid, jstart, jend, group in self.groups:
            jstart_job, jstart_fname = jstart
            jend_job, jend_fname = jend

            jstart_job.to_file(opath(jstart_fname))
            jend_job.to_file(opath(jend_fname))

        self.jfinal.to_file(opath(self.jfinal_fname))

        fname = opath('dag.condor') 
        with open(fname, 'w+') as fp:
            fp.write(self.jobdef())

        return fname


def create_dag(n_parallel, outdir, jobs):
    job_groups = grouper(n_parallel, jobs)

    dag = JobDag(job_groups)
    dag.to_file(outdir)

def main():
    parser = argparse.ArgumentParser()

    subparsers = parser.add_subparsers()

    subp = subparsers.add_parser('files')
    subp.set_defaults(action='files')
    subp.add_argument('exe')
    subp.add_argument('logdir')
    subp.add_argument('files')

    subp_d = subparsers.add_parser('dirs', 
        help='Runs a program on multiple directories within a parent directory',
        description='''
Runs a program on multiple directories within a parent input directory and
creates the output with the same directory structure on an output directory.

The program should expect two arguments: input-dir and output-dir.

The output directory is chosen as follows. The path corresponding to 
'base_input' is removed from every input directory; the resulting path
is appended to 'base_output'

Then, the program will be called once for each input directory, with the 
corresponding output directory.
        '''
    )
    subp_d.set_defaults(action='dirs')

    subp_d.add_argument('exe', help='Path to executable to run')
    subp_d.add_argument('base_output', help='Base output directory')
    subp_d.add_argument('logdir')
    subp_d.add_argument('base_input', help='Base input directory')
    subp_d.add_argument('--input_file', help='Read the input directories from file')
    subp_d.add_argument('input', help='Input directory', nargs='*')

    subp = subparsers.add_parser('dag', 
        help='Creates a DAG job from multiple (independent) job files')
    subp.set_defaults(action='dag')
    subp.add_argument('-p', '--parallel', type=int, 
        help='Number of child nodes per step', default=10)
    subp.add_argument('outdir')
    subp.add_argument('job', nargs='+')

    args = parser.parse_args()

    if args.action == 'dirs':
        # Run the job on multiple directories
        input_dirs = []
        input_dirs.extend(args.input)
        input_dirs.extend(read_arg_list(args.input_file))

        if not input_dirs:
            subp_d.error('No input directories provided')

        dir_pairs = get_dir_pairs(args.base_output, args.base_input, input_dirs)
        create_job_dirs(args.exe, args.logdir, dir_pairs)
    elif args.action == 'files':
        # Run multiple commands, one per file
        lines = fileinput.input(args.files, openhook=fileinput.hook_compressed)
        fnames = load_file_names(lines)
        create_job_files(args.exe, args.logdir, fnames)
    elif args.action == 'dag':
        create_dag(args.parallel, args.outdir, args.job)

if __name__ == '__main__':
    main()

