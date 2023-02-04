# -*- coding: utf-8 -*-
import datetime
import pytest
import decimal

from balance.mapper.common import CurrencyRate
from balance import muzzle_util as ut
from balance import exc
from tests import object_builder as ob

NOW = datetime.datetime.now().replace(microsecond=0)


def create_currency(session, **kwargs):
    return ob.CurrencyBuilder(**kwargs).build(session).obj


def create_currency_rate(session, **kwargs):
    return ob.CurrencyRateBuilder(**kwargs).build(session).obj


@pytest.mark.parametrize('rate_src_id, cc', [(1000, 'RUB'),
                                             (1000, 'RUR'),
                                             (None, 'RUR')])
def test_get_currency_rate_by_date_rub(session, rate_src_id, cc):
    result = CurrencyRate.get_currency_rate_by_date(session, cc=cc, rate_src_id=rate_src_id, base_cc=None)
    assert result.cc == cc
    assert result.rate == decimal.Decimal(1)
    assert result.rate_dt == session.now()
    assert result.rate_src_id == 1000
    assert result.base_cc == 'RUR'


def test_get_currency_rate_by_date_force_src_id(session):
    currency = create_currency(session)
    base_currency = create_currency(session)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                                         base_cc=base_currency.char_code, rate_src_id=9999, rate=100)
    result = CurrencyRate.get_currency_rate_by_date(session, cc=currency.char_code, rate_src_id=9999,
                                                    base_cc=base_currency.char_code)
    assert result is currency_rate


def test_get_currency_rate_by_date_fix_rub(session):
    currency = create_currency(session)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                                         base_cc='RUB', rate_src_id=9999, rate=100)
    result = CurrencyRate.get_currency_rate_by_date(session, cc=currency.char_code, rate_src_id=9999,
                                                    base_cc='RUR')
    assert result is currency_rate


def test_get_currency_rate_by_date_default_cc(session):
    """дефолтная валюта, для которой запрашивается курс"""
    currency = create_currency(session)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc='USD',
                                         base_cc=currency.char_code, rate_src_id=9999, rate=100)
    result = CurrencyRate.get_currency_rate_by_date(session, base_cc=currency.char_code, rate_src_id=9999)
    assert result is currency_rate


def test_get_currency_rate_by_date_default_rate_src_id(session):
    """источник по умолчанию 1000"""
    currency = create_currency(session)
    base_currency = create_currency(session)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                                         base_cc=base_currency.char_code, rate_src_id=1000, rate=100)
    result = CurrencyRate.get_currency_rate_by_date(session, base_cc=base_currency.char_code, cc=currency.char_code)
    assert result is currency_rate


@pytest.mark.parametrize('cc', ['UAH', 'KZT', 'TRY'])
def test_get_currency_rate_by_date_fixed_rates(session, cc):
    """фиктивный источник для фиксированных курсов"""
    base_currency = create_currency(session)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=cc,
                                         base_cc=base_currency.char_code, rate_src_id=1111, rate=100)
    result = CurrencyRate.get_currency_rate_by_date(session, base_cc=base_currency.char_code, cc=cc)
    assert result is currency_rate


def test_get_currency_rate_by_date_default_date(session):
    """курс по умолчанию на текущую дату полночь, не возвращаем курс в середине дня"""
    currency = create_currency(session)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW) - datetime.timedelta(seconds=1),
                         cc=currency.char_code, base_cc='RUB', rate_src_id=9999, rate=100)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW) + datetime.timedelta(seconds=1),
                         cc=currency.char_code, base_cc='RUB', rate_src_id=9999, rate=100)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                                         base_cc='RUB', rate_src_id=9999, rate=100)
    result = CurrencyRate.get_currency_rate_by_date(session, cc=currency.char_code, rate_src_id=9999,
                                                    base_cc='RUR')
    assert result is currency_rate


def test_get_currency_rate_by_date_force_date(session):
    currency = create_currency(session)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUB', rate_src_id=9999, rate=100)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW) - datetime.timedelta(days=1),
                                         cc=currency.char_code, base_cc='RUB', rate_src_id=9999, rate=100)
    result = CurrencyRate.get_currency_rate_by_date(session, cc=currency.char_code, rate_src_id=9999,
                                                    base_cc='RUR', dat=ut.trunc_date(NOW) - datetime.timedelta(days=1))
    assert result is currency_rate


def test_get_currency_rate_by_date_no_rate(session):
    currency = create_currency(session)
    base_currency = create_currency(session)
    with pytest.raises(exc.INVALID_MISSED_CURRENCY_RATE) as exc_info:
        CurrencyRate.get_currency_rate_by_date(session, base_cc=base_currency.char_code, cc=currency.char_code)
    assert exc_info.value.msg.startswith('Missed currency {} rate on'.format(currency.char_code))
