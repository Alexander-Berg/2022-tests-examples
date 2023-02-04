#!/bin/bash

# In fact, we don't need it here, because it is set in toolsbase Dockerfile,
# but explicit is better than implicit.
PYTHONPATH="/utils:/yblib:${PYTHONPATH}"

export PYTHONPATH

exec py.test $TEST_ROOT_PATH $ADDITIONAL_PARAMS --ignore=$PWD/autodasha --teamcity -p no:cacheprovider \
    --forked -n $CPU_NUM # --cov-report=html:../../artifacts/tests/cov_html --cov .. --cov-config .coveragerc
