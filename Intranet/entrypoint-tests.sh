#!/bin/bash

set -e

if [ "$1" == "" ]; then
    py.test -vvv -rsx tasha/tests/
else
    py.test -vvv $1
fi

flake8 --max-line-length=200 tasha/ --exclude=migrations
