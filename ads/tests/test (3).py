from __future__ import print_function

import sys

import pytest
from ads.bsyeti.exp_stats.py_lib import exp_stats


@pytest.mark.skipif(True, reason="This is not a test")
def test():
    """
    This is not a test, because CHYT do not mocked.
    This just for local debug.
    """
    arguments = "./bin --exp 6644 --fields clicks,cost_d --range 20190810..20190815".split()
    print(exp_stats(arguments), file=sys.stderr)
