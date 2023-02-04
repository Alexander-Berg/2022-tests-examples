import datetime

import pytest

from tests import object_builder as ob


@pytest.fixture()
def rate_kwargs():
    return {
        'rate_id': 132535235345,
        'src_cc': 'LaL22wP',
        # a naive datetime which will be assigned to MSK TZ
        'dt': datetime.datetime.fromtimestamp(1630000000),
        'iso_currency_from': 'USD',
        'rate_from': 10,
        'iso_currency_to': 'RUB',
        'rate_to': 1
    }


@pytest.fixture()
def iso_currency_rate(session, rate_kwargs):
    yield ob.IsoCurrencyRateBuilder.construct(session, **rate_kwargs)
