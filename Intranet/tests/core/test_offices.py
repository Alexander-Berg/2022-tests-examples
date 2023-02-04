# coding: utf-8

from __future__ import unicode_literals

import pytest

from easymeeting.core import offices


@pytest.mark.parametrize('office_id_a, office_id_b', [
    (2, 2),
    (1, 1),
    (1, 147),
    (1, 148),
])
def test_is_nearly_positive(office_id_a, office_id_b):
    assert offices.is_nearly(office_id_a, office_id_b)


@pytest.mark.parametrize('office_id_a, office_id_b', [
    (2, 3),
    (1, 2),
    (2, 147),
    (2, None),
])
def test_is_nearly_negative(office_id_a, office_id_b):
    assert not offices.is_nearly(office_id_a, office_id_b)
