#!/bin/sh -ue

me=open-files-${1:-sleep}

send () {
	echo "PASSIVE-CHECK:$me;$1;$2"
        exit 0
}


c=0;  for i in $(ps axuw | fgrep ${1:-sleep} | awk '{print $2}'); do c=$((c+$(ls -1U /proc/$i/fd/ 2>/dev/null | wc -l))) ; done ;


send 0 "open files for (${1:-sleep}) -- $c"
