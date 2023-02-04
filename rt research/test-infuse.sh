#!/bin/sh
if [ -f "$1" ]; then
    ./utils/infuse_web_categs.pl --test=$1
else
    echo "$0 <filename>"
fi
