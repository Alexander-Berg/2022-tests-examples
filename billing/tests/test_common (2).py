# -*- coding: utf-8 -*-

import pytest

from billing.library.python.common_utils.common import get_adjacent_element_indexes


@pytest.mark.parametrize(
    ['elements', 'value', 'result'],
    [
        ([], 1, (None, None, None)),

        ([2], 1, (None, None, 0)),
        ([2], 2, (None, 0, None)),
        ([2], 3, (0, None, None)),

        ([2, 4], 1, (None, None, 0)),
        ([2, 4], 2, (None, 0, 1)),
        ([2, 4], 3, (0, None, 1)),
        ([2, 4], 4, (0, 1, None)),
        ([2, 4], 5, (1, None, None)),

        ([4, 6, 8], 3, (None, None, 0)),
        ([4, 6, 8], 4, (None, 0, 1)),
        ([4, 6, 8], 5, (0, None, 1)),
        ([4, 6, 8], 6, (0, 1, 2)),
        ([4, 6, 8], 7, (1, None, 2)),
        ([4, 6, 8], 8, (1, 2, None)),
        ([4, 6, 8], 9, (2, None, None)),

        ([4, 6, 8, 10], 6, (0, 1, 2)),
        ([4, 6, 8, 10], 7, (1, None, 2)),
        ([4, 6, 8, 10], 8, (1, 2, 3)),
    ]
)
def test_get_adjacent_element_indexes(elements, value, result):
    assert get_adjacent_element_indexes(elements, value) == result
