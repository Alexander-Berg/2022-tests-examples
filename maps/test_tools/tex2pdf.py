#!/usr/bin/python
#encoding: utf8

import subprocess as sp
import os
import glob
import argparse

def run_pdflatex(suitename, tex_dir, pdf_dir):
    logfile = open(pdf_dir + '/' + suitename + '.pdflatex.log', 'w')
    process = sp.Popen(
        ['pdflatex',
        '-output-directory', pdf_dir,
        '-halt-on-error',
        tex_dir + '/' + suitename + '.tex'],
        stdout=logfile,
        stderr=logfile)
    def waiter():
        process.wait()
        logfile.close()
        if process.returncode:
            raise sp.CalledProcessError(process.returncode, 'pdflatex')

    return process, waiter

def clear_logs(suitename, pdf_dir):
    rm_proc = sp.Popen(
        ['rm', '-f',
         pdf_dir + '/' + suitename +'.pdflatex.log',
         pdf_dir + '/' + suitename +'.log',
         pdf_dir + '/' + suitename +'.aux',
         pdf_dir + '/' + suitename +'.out',
         pdf_dir + '/' + suitename +'.toc'])
    rm_proc.wait()

def clear_all_logs(tex_dir, pdf_dir):
    for file in glob.glob(tex_dir + '/*.tex'):
        suitename, ext = os.path.splitext(os.path.basename(file))
        clear_logs(suitename, pdf_dir)


def print_all(tex_dir, pdf_dir):
    waiters = []
    for file in glob.glob(tex_dir + '/*.tex'):
        suitename, ext = os.path.splitext(os.path.basename(file))
        process, waiter = run_pdflatex(suitename, tex_dir, pdf_dir)
        waiters.append(waiter)
    for wait in waiters:
        wait()

def print_suite(tex_dir, pdf_dir, suite):
    process, wait = run_pdflatex(suite, tex_dir, pdf_dir)
    wait()

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--tex-dir', help='tex files directory')
    parser.add_argument('--pdf-dir', help='pdf output directory')
    parser.add_argument('--suite', help='print particular suite')

    args = parser.parse_args()

    if args.suite is None:
        print_all(args.tex_dir, args.pdf_dir)
        print_all(args.tex_dir, args.pdf_dir)
        clear_all_logs(args.tex_dir, args.pdf_dir)
    else:
        print_suite(args.tex_dir, args.pdf_dir, args.suite)
        print_suite(args.tex_dir, args.pdf_dir, args.suite)
        clear_logs(args.suite, args.pdf_dir)
