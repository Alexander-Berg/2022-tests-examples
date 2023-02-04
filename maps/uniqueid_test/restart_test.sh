#!/bin/sh

uid1=`tests/generate_id`
sleep 1
uid2=`tests/generate_id`

if [ "$uid1" = "$uid2" ]; then
    echo "Error: non-unique ids detected!" >&2
    exit 1
fi
