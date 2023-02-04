#!/usr/bin/env python

import json
import sys

matrix = json.load(sys.stdin)

count = 0
max_duration = 0
max_distance = 0

for matrix_slice in matrix:
    for loc1, row in enumerate(matrix_slice['durations']):
        for loc2, duration in enumerate(row):
            if not isinstance(duration, (float, int)) or duration > 86400:
                count += 1

            if isinstance(duration, (float, int)):
                max_duration = max(max_duration, duration)

            distance = matrix_slice['distances'][loc1][loc2]
            if isinstance(distance, (float, int)):
                max_distance = max(max_distance, distance)

print ("OK" if count == 0 else "%d errors" % count) + ": max_duration = %g days; max_distance = %g km" % (max_duration/86400.0, max_distance * 1e-3)
