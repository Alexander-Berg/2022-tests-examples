#!/bin/bash

set -e

export USE_TENSORFLOW=1

if [ "$1" == "" ]; then
    py.test -vv --randomly-seed=1 -p no:warnings uhura/tests/test_*.py
else
    py.test -vv --randomly-seed=1 -p no:warnings $1
fi

flake8 --max-line-length=120 uhura/
flake8 --max-line-length=120 cli/
flake8 --max-line-length=120 ./*.py
