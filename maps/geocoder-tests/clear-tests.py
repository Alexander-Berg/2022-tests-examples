#!/usr/bin/python

import sys
import getopt
import json

def usage():
    print "\n".join([
        "Json testset clear: remove tests with missed/incorrect expected response",
        "Usage:",
        "(1)     stdin > {program} > stdout".format(program=sys.argv[0]),
        "(2)     {program} -i input_file_name, -o out_file_name".format(program=sys.argv[0])])
    sys.exit(1)

#
#   special logics
#

def clear_testset(testset):
    cleared = []
    for test in testset.get("tests", []):
        expected = test.get("expected")
        if not expected:
            continue
        elif expected.get("allWrong") or expected.get("badText"):
            continue
        else:
            cleared.append(test)
    return {"tests": cleared}

#
#   --  main --
#

if __name__ == '__main__':
    args,_ = getopt.getopt(sys.argv[1:], 'i:o:h')
    args = dict(args)

    if "-h" in args:
        usage()

    input_file_name = args.get("-i")
    output_file_name = args.get("-o")

    input_testset = {}
    if input_file_name:
        with open(input_file_name) as input_file:
            input_testset = json.load(input_file)
    else:
        input_testset = json.load(sys.stdin)

# special logics
#
    testset = clear_testset(input_testset)
#
# special logics

    out_json_text = json.dumps(testset,indent=4, ensure_ascii=False).encode('utf-8')
    if output_file_name:
        with open(output_file_name, "w") as output_file:
            output_file.write(out_json_text)
    else:
        print out_json_text
