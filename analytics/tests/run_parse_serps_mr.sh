#!/usr/bin/env bash
cd ..
PYTHONPATH=.:../../yweb/scripts/datascripts/common:$PYTHONPATH
DEF_MR_EXEC=mapreduce-yt
DEF_MR_SERVER=hahn
YT_PROXY=hahn
MR_USER=rearrange
python -u parse_serps_mr.py --input_dir tests/out.2 -i //tmp/serp_anatomy/tests/r1_html -o //tmp/serp_anatomy/tests/r1 -d tests/r1_out
