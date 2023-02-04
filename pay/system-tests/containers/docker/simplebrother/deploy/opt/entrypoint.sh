#!/bin/bash

cp -rv src/simple_monitorings src/simplebrother_balance_bo src/debian src/setup.py src/README.md /code

pip install -e code

. /etc/oraprofile

exec "$@"