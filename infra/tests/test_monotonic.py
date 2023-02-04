# -*- coding: utf-8 -*-
from __future__ import absolute_import

import monotonic


def test_constantly_increasing_time():
    last = monotonic.monotonic()
    for _ in xrange(100):
        current = monotonic.monotonic()
        assert current >= last, "Clock went backwards"
        last = current
