# -*- coding: utf-8 -*-
import pytz

from intranet.forms.micro.core.src import utils


def test_datetime_now():
    first = utils.datetime_now()
    second = utils.datetime_now()
    assert first < second
    assert first.tzinfo is pytz.UTC
