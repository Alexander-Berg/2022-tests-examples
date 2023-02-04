#!/bin/bash

BM_USE_PATCH='' DEBUG=0 ./test-yml2directinf.pl --local | sed "s/^[0-9: \t-]*\[[0-9]*\]\t//g" > yml2directinf-canonized-test.txt
