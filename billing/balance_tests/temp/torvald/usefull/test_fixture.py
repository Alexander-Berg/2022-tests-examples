# -*- coding: utf-8 -*-

import pytest


@pytest.fixture(params=[1, 2])
def fix(request):
    return request.param


@pytest.mark.parametrize('value', [1, 2])
def test_1(fix, value):
    assert fix == value
