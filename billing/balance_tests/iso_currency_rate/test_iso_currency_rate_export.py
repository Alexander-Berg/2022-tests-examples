import datetime

import pytest

from balance.son_schema import common


@pytest.fixture()
def result(rate_kwargs):
    return {
        "rate_from": str(rate_kwargs['rate_from']),
        "rate_to": str(rate_kwargs['rate_to']),
        "src_cc": rate_kwargs['src_cc'],
        "iso_currency_from": rate_kwargs['iso_currency_from'],
        "iso_currency_to": rate_kwargs['iso_currency_to'],
        "version_id": 0,
        "dt": "2021-08-26T20:46:40+03:00",
        "id": rate_kwargs['rate_id'],
    }


def test_iso_currency_rate_serialization(session, iso_currency_rate, result):
    s = common.IsoCurrencyRateSchema().dump(iso_currency_rate).data
    assert s == result


def test_iso_currency_rate_update(session, iso_currency_rate):
    version = iso_currency_rate.version_id
    iso_currency_rate.dt += datetime.timedelta(days=1)
    session.add(iso_currency_rate)
    session.flush()
    session.refresh(iso_currency_rate)
    assert iso_currency_rate.version_id == version + 1
