# -*- coding: utf-8 -*-

import pytest
import datetime
import random

from balance.mapper.common import CurrencyRate
from tests import object_builder as ob
from balance import muzzle_util as ut

NOW = datetime.datetime.now()
TWO_DAYS_AGO = NOW - datetime.timedelta(days=2)
YESTERDAY = NOW - datetime.timedelta(days=1)
CODE_RATE_NOT_FOUND = 1
CODE_SUCCESS = 0


def create_currency(session, **kwargs):
    return ob.CurrencyBuilder(**kwargs).build(session).obj


def create_currency_rate(session, **kwargs):
    return ob.CurrencyRateBuilder(**kwargs).build(session).obj


def test_get_currency_rate_by_period_from_dt_is_none(xmlrpcserver):
    result = xmlrpcserver.GetCurrencyRateByPeriod('RUB', None   , NOW)
    assert result == [1, 'NOT_FOUND']


@pytest.mark.parametrize('rate_src_id', [1000, None])
@pytest.mark.parametrize('base_cc', ['RUR', 'RUB', None])
def test_get_currency_rate_by_period_rub_last_date(session, xmlrpcserver, base_cc, rate_src_id):
    """у рубля к рублю (валюта по умолчанию) в ЦБ РФ (источник по умолчанию) курс равен 1"""
    result = xmlrpcserver.GetCurrencyRateByPeriod('RUB', TWO_DAYS_AGO, NOW)
    assert (result[0], result[1]) == (CODE_SUCCESS, 'SUCCESS')
    assert result[2] == [{'currency': 'RUB',
                          'date': datetime.datetime.combine(TWO_DAYS_AGO, datetime.time()),
                          'rate': '1'},
                         {'currency': 'RUB',
                          'date': datetime.datetime.combine(YESTERDAY, datetime.time()),
                          'rate': '1'},
                         {'currency': 'RUB',
                          'date': datetime.datetime.combine(NOW, datetime.time()),
                          'rate': '1'}]


def test_get_currency_rate_iso_code_return(session, xmlrpcserver):
    """Проверяем, что в метод передаем char_code валюты, а возвращается ее iso_code"""
    currency = create_currency(session)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                                         base_cc='RUR',
                                         rate_src_id=9999, rate=100)
    result = xmlrpcserver.GetCurrencyRateByPeriod(currency.iso_code, YESTERDAY, NOW, currency_rate.rate_src_id, 'RUR')
    assert currency.iso_code != currency.char_code
    for rate in result[2]:
        assert rate['currency'] == currency.iso_code


def test_get_currency_rate_default_base_cc(session, xmlrpcserver):
    """Валюта по умолчанию - рубли"""
    currency = create_currency(session)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                                         base_cc='RUB', rate_src_id=9999, rate=100)
    result = xmlrpcserver.GetCurrencyRateByPeriod(currency.iso_code, YESTERDAY, NOW, 9999, None)
    for rate in result[2]:
        assert rate == {'currency': currency.iso_code,
                        'date': datetime.datetime.combine(currency_rate.rate_dt, datetime.time()),
                        'rate': str(currency_rate._rate)}


def test_get_currency_rate_default_rate_src_id(session, xmlrpcserver):
    """источник курса по умолчанию - ЦБ РФ"""
    currency = create_currency(session)
    base_currency = create_currency(session)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                                         base_cc=base_currency.char_code, rate_src_id=1000, rate=100)
    result = xmlrpcserver.GetCurrencyRateByPeriod(currency.iso_code, YESTERDAY, NOW, None, base_currency.char_code)
    for rate in result[2]:
        assert rate == {'currency': currency.iso_code,
                        'date': datetime.datetime.combine(currency_rate.rate_dt, datetime.time()),
                        'rate': str(currency_rate._rate)}


def test_get_currency_rate_by_period_empty_result(xmlrpcserver):
    result = xmlrpcserver.GetCurrencyRateByPeriod('USD', YESTERDAY, NOW, 1007, 'KZT')
    assert (result[0], result[1]) == (CODE_SUCCESS, 'SUCCESS')
    assert result[2] == []
