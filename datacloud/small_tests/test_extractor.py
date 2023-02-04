# -*- coding: utf-8 -*-
from datacloud.features.phone_range import extractor
import numpy as np


e = extractor.extract_phone_range_features


def test_extract_none_vectors():
    actual = e({'region_id': None, 'change_region': None, 'operator': None})
    expected = np.array([-1, -1, -1], dtype=float)
    assert np.array_equal(actual, expected)


def test_extract_vectors_1():
    actual = e({'region_id': 10995, 'change_region': False, 'operator': 'ПАО \"МегаФон\"'})
    expected = np.array([10995, 0, 1353285624], dtype=float)
    assert np.array_equal(actual, expected)


def test_extract_vectors_2():
    actual = e({'region_id': 0, 'change_region': True, 'operator': ''})
    expected = np.array([0, 1, 0], dtype=float)
    assert np.array_equal(actual, expected)
