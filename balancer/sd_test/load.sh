#!/bin/sh

while true; do wget --tries=1 --header="Balancing: $2" -q -O /dev/null "http://127.0.0.5:$1/version"; sleep 0.2; done
