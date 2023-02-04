# encoding: utf8

import pytest

from balance.utils import date_format as df
from datetime import datetime


@pytest.mark.parametrize(
    'lang, string',
    [
        ['ru',  u'25 февраля 2007 г.'],
        ['en', u'February 25 2007'],
        ['ua', u'25 лютого 2007 р.']
    ]
)
def test_date_format(lang, string):
    dt = datetime(2007, 02, 25)
    assert df.written_date(dt, lang) == string
