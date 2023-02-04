# -*- coding: utf-8 -*-

import pytest
from balance.completions_fetcher.configurable_partner_completion import BaseIterator, CSVIterator, \
    JSONIterator, RawIterator


@pytest.mark.parametrize('expected_class', [
    CSVIterator, JSONIterator, RawIterator
])
def test_children(expected_class):
    actual_class = BaseIterator.children.get(expected_class.id)
    assert actual_class == expected_class, 'Iterator class is different than expected'
