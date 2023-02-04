#!/usr/bin/env python3
# WARNING: this script may merge incompatible locations into a group.
# Run solver with -v on the original file to see which locations
# are labelled as [NOT MERGED] and delete such groups by hand.
import json
import os
import sys
from collections import defaultdict


REQUEST = '_request.json'
LG_REQUEST = '_lg_request.json'

input_path = sys.argv[1]
assert input_path.endswith(REQUEST)
with open(input_path) as req:
    task = json.load(req)

task['options']['merge_multiorders'] = False
location_groups = defaultdict(list)
for location in task['locations']:
    point = (location['point']['lat'], location['point']['lon'])
    location_groups[point].append(location['id'])
task['options']['location_groups'] = []
for group in location_groups.values():
    if len(group) > 1:
        task['options']['location_groups'].append({'location_ids': group})

output_path = input_path[:-len(REQUEST)] + LG_REQUEST
with open(output_path, 'w') as req:
    json.dump(task, req, indent=4)
