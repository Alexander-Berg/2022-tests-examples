#!/usr/bin/env bash

while read name taskid; do
    download_tasks_by_ids.py -D $taskid &&
    mv "${taskid}_request.json" "${name}_request.json" &&
    mv "${taskid}_response.json" "${name}_response.json" &&
    mv "${taskid}_distances.json" "${name}_distances.json"
done
