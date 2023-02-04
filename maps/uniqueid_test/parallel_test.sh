#!/bin/sh

tests/generate_id 10000 > genfiles/ids1 & tests/generate_id 10000 > genfiles/ids2

if [ `cat genfiles/ids1 genfiles/ids2 | sort -u | wc -l` -ne 20000 ]; then
    echo "Error: non-unique ids detected!" >&2
    exit 1
fi

rm genfiles/ids1 genfiles/ids2

