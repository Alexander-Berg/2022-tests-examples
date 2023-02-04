#!/bin/sh
set -euo pipefail

cd ru/auto/buildserver
python3 -m unittest discover
