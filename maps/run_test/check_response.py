#!/usr/bin/python
import sys

def is_ok(line):
    return "HTTP 200" in line

num_queries = len(open(sys.argv[1]).readlines())
responses = open(sys.argv[2]).readlines()
num_oks = sum(1 for line in responses if is_ok(line))

if num_queries != num_oks:
    for line in responses:
        if not is_ok(line) and "HTTP" in line:
            sys.stderr.write(line + "\n")
    sys.stderr.write("Only {} of {} queries exited successfully\n".format(
        num_oks, num_queries))
    exit(1)
