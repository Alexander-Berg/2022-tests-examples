#!/usr/bin/python

#!/usr/bin/python

import sys
import json

def usage():
    print "\n".join([
        "Count tests in testsets",
        "Usage: {program} {{testset.json}}".format(program=sys.argv[0])])
    sys.exit(1)

#
#   --  main --
#

if __name__ == '__main__':
    if len(sys.argv) < 2:
        usage()

    joined_testset = []
    files_to_join = sys.argv[1:]
    for filename in files_to_join:
        with open(filename) as f:
            json_data = json.load(f)
            joined_testset.extend(json_data["tests"])

    print len(joined_testset)
