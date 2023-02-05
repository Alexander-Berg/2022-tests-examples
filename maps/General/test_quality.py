#!/usr/bin/env python

import subprocess
import sys
import os
import json


TEST_DATA_DIR = "genfiles/test_data"


def main():
    files = os.listdir(TEST_DATA_DIR)
    exec_path = os.path.join(os.path.dirname(sys.argv[0]), "../false_route_lost_detector")
    false_route_lost_as_true = 0
    true_route_lost_as_false = 0
    ignored = 0
    total = 0
    for file in files:
        nth_false_route_lost_expected = [int(ch) for ch in file.split("_")[-1]]
        mapkitsim = file.split("_")[-2]
        file_path = os.path.join(TEST_DATA_DIR, file)
        with open(file_path, "rt") as input_file:
            with open(os.path.join("genfiles", file + ".log"), "wt") as err:
                proc = subprocess.Popen([exec_path], stdin=input_file, stdout=subprocess.PIPE, stderr=err)
                lines = [line for line in proc.stdout]
                for num in range(len(lines)):
                    expected = nth_false_route_lost_expected[num]
                    line = lines[num]
                    metrika = json.loads(line)
                    actual = metrika["FalseRouteLost"]
                    ignore = not metrika["HasPing"] or num + 1 == len(lines) or metrika["NotUsed"]
                    if not ignore:
                        if expected == 1 and actual == 0: true_route_lost_as_false += 1
                        if expected == 0 and actual == 1: false_route_lost_as_true += 1
                        total += 1
                        if (actual != expected): print file, num, expected, metrika
                    else:
                        ignored += 1

    print "false_route_lost_as_true", false_route_lost_as_true
    print "true_route_lost_as_false", true_route_lost_as_false
    print "ignored", ignored
    print "total", total
    q = 100 * (total - false_route_lost_as_true - true_route_lost_as_false) / total
    print "quality", q
    if q < 87:
        print "Error: classificator degradation"
        sys.exit(1)


if __name__ == "__main__":
    main()
