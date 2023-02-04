# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from sepelib.util.misc import drop_none


def test_drop_none_drops_none_values():
    src = {'a': 1, 'b': 2, 'c': None, 'd': None}
    assert drop_none(src) == {'a': 1, 'b': 2}


def test_drop_none_preserves_not_none_values():
    src = {'a': 1, 'b': 2}
    assert drop_none(src) == {'a': 1, 'b': 2}
