#!/usr/bin/python
# coding: utf-8

import sys
import getopt
import json

def usage():
    print "\n".join([
        "Json testset update: append cgi parameter to each request",
        "Usage:",
        "(1)     stdin > {program} -a cgi:value > stdout".format(program=sys.argv[0]),
        "(2)     {program} -a cgi:value -i input_file_name, -o out_file_name".format(program=sys.argv[0])])
    sys.exit(1)

#
#   special logics
#


def translate(testset):
    for test in testset.get("tests", []):
        expected = json.dumps(test["expected"], ensure_ascii=False)
        test["expected"] = json.loads(expected\
            .replace(u"Ile-de-France", u"Île-de-France")\
            .replace(u"Île-de-France, Paris", u"Paris")
        )

    return testset

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
    testset = translate(input_testset)
#
# special logics

    out_json_text = json.dumps(testset, indent=4, ensure_ascii=False).encode('utf-8')
    if output_file_name:
        with open(output_file_name, "w") as output_file:
            output_file.write(out_json_text)
    else:
        print out_json_text
