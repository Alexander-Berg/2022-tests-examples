# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest

from core.utils import language


@pytest.fixture
def russian_text():
    return 'Русский текст'

@pytest.fixture
def latin_text():
    return 'Latin text'


def test_is_cyrillic_only(russian_text, latin_text):
    assert language.is_cyrillic_only(russian_text) is True
    assert language.is_cyrillic_only(latin_text) is False
