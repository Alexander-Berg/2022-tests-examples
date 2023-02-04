# -*- coding: utf-8 -*-

import pytest
import datetime
import random

from balance.mapper.common import CurrencyRate
from tests import object_builder as ob
from balance import muzzle_util as ut

NOW = datetime.datetime.now()
CODE_RATE_NOT_FOUND = 1
CODE_SUCCESS = 0


def create_currency(session, **kwargs):
    return ob.CurrencyBuilder(**kwargs).build(session).obj


def create_currency_rate(session, **kwargs):
    return ob.CurrencyRateBuilder(**kwargs).build(session).obj


def test_get_currency_rate_not_found(xmlrpcserver):
    res = xmlrpcserver.GetCurrencyRate('RUR', NOW)
    assert res == [1, 'NOT_FOUND']


@pytest.mark.parametrize('currency', ['KZT', 'USD', 'EUR'])
def test_get_currency_rate_real_rates_last_date(session, xmlrpcserver, currency):
    """некоторые реальные свежие курсы """
    result = xmlrpcserver.GetCurrencyRate(currency, NOW)
    assert (result[0], result[1]) == (CODE_SUCCESS, 'SUCCESS')
    currency_rate = CurrencyRate.get_real_currency_rate_by_date(session, currency, NOW, base_cc='RUB',
                                                                rate_src_id=None, iso_base=False)
    assert result[2] == {'currency': currency,
                         'date': datetime.datetime.combine(currency_rate.rate_dt, datetime.time()),
                         'rate': str(currency_rate._rate)}


@pytest.mark.parametrize('rate_src_id', [1000, None])
@pytest.mark.parametrize('base_cc', ['RUR', 'RUB', None])
def test_get_currency_rate_rub_last_date(xmlrpcserver, base_cc, rate_src_id):
    """у рубля к рублю (валюта по умолчанию) в ЦБ РФ (источник по умолчанию) курс равен 1"""
    result = xmlrpcserver.GetCurrencyRate('RUB', NOW, rate_src_id, base_cc)
    assert (result[0], result[1]) == (CODE_SUCCESS, 'SUCCESS')
    assert result[2] == {'currency': 'RUB',
                         'date': datetime.datetime.combine(NOW, datetime.time()),
                         'rate': '1'}


@pytest.mark.parametrize('base_cc', ['RUR', 'RUB'])
@pytest.mark.parametrize('cc', ['UAH', 'TRY', 'KZT'])
def test_get_currency_rate_non_fix_rates(session, xmlrpcserver, base_cc, cc):
    """Проверяем, что не возвращаем фиксированные курсы + возвращаем название валюты в том регистре, что передали"""
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=cc.lower(), base_cc=base_cc,
                                         rate_src_id=1000, rate=100)
    result = xmlrpcserver.GetCurrencyRate(cc, NOW, 1000, base_cc)
    assert (result[0], result[1]) == (CODE_SUCCESS, 'SUCCESS')
    assert result[2] == {'currency': cc,
                         'date': datetime.datetime.combine(currency_rate.rate_dt, datetime.time()),
                         'rate': str(currency_rate._rate)}


def test_get_currency_rate_iso_code_return(session, xmlrpcserver):
    """Проверяем, что метод принимает iso_code валюты,
     ищет курс по соот-ему char_code валюты, а возвращает то, что принял (iso_code)"""
    currency = create_currency(session)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                                         base_cc='RUR',
                                         rate_src_id=9999, rate=100)
    result = xmlrpcserver.GetCurrencyRate(currency.iso_code, NOW, currency_rate.rate_src_id, 'RUR')
    assert currency.iso_code != currency.char_code
    assert result[2]['currency'] == currency.iso_code


def test_get_currency_rate_default_base_cc(session, xmlrpcserver):
    """базовая валюта по умолчанию - рубли"""
    currency = create_currency(session)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                                         base_cc='RUB', rate_src_id=9999, rate=100)
    result = xmlrpcserver.GetCurrencyRate(currency.iso_code, NOW, 9999, None)
    assert result[:2] == [0, 'SUCCESS']
    assert result[2] == {'currency': currency.iso_code,
                         'date': datetime.datetime.combine(currency_rate.rate_dt, datetime.time()),
                         'rate': str(currency_rate._rate)}


def test_get_currency_rate_default_rate_src_id(session, xmlrpcserver):
    """источник курса по умолчанию - ЦБ РФ"""
    currency = create_currency(session)
    base_currency = create_currency(session)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                                         base_cc=base_currency.char_code, rate_src_id=1000, rate=100)
    result = xmlrpcserver.GetCurrencyRate(currency.iso_code, NOW, None, base_currency.char_code)
    assert result[:2] == [0, 'SUCCESS']
    assert result[2] == {'currency': currency.iso_code,
                         'date': datetime.datetime.combine(currency_rate.rate_dt, datetime.time()),
                         'rate': str(currency_rate._rate)}


@pytest.mark.parametrize('base_cc, rate_src_id', [('RUR', 1000), ('USD', 1001), ('TRY', 1002),
                                                  ('UAH', 1003), ('CNY', 1004), ('AMD', 1005),
                                                  ('BYN', 1006), ('KZT', 1007), ('GEL', 1008)])
def test_get_currency_rate_rate_src_id_by_base_cc(session, xmlrpcserver, base_cc, rate_src_id):
    """источник курса в зависимости от базовой валюты, если не был передан явно"""
    currency = create_currency(session)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                                         base_cc=base_cc, rate_src_id=rate_src_id, rate=100)
    result = xmlrpcserver.GetCurrencyRate(currency.iso_code, NOW, None, base_cc)
    assert result[:2] == [0, 'SUCCESS']
    assert result[2] == {'currency': currency.iso_code,
                         'date': datetime.datetime.combine(currency_rate.rate_dt, datetime.time()),
                         'rate': str(currency_rate._rate)}


def test_get_currency_rate_exact_rate_src_id(session, xmlrpcserver):
    """явно указанный источник курса"""
    currency = create_currency(session)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                                         base_cc='RUR',
                                         rate_src_id=9999, rate=100)
    result = xmlrpcserver.GetCurrencyRate(currency.iso_code, NOW, currency_rate.rate_src_id, 'RUR')
    assert result[:2] == [0, 'SUCCESS']
    assert result[2] == {'currency': currency.iso_code,
                         'date': datetime.datetime.combine(currency_rate.rate_dt, datetime.time()),
                         'rate': str(currency_rate._rate)}


@pytest.mark.parametrize('iso_base', [True, False])
def test_get_currency_rate_iso_base(session, xmlrpcserver, iso_base):
    """по умолчанию дб передан char_code базовой валюты (кроме рублей, они сами исправятся),
    но можно передать iso_base флажок и учитываться iso_code базовой валюты"""
    currency = create_currency(session)
    base_currency = create_currency(session)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                                         base_cc=base_currency.char_code, rate_src_id=1000, rate=100)
    result = xmlrpcserver.GetCurrencyRate(currency.iso_code, NOW, None, base_currency.iso_code, iso_base)
    if iso_base:
        assert result[:2] == [0, 'SUCCESS']
        assert result[2] == {'currency': currency.iso_code,
                             'date': datetime.datetime.combine(currency_rate.rate_dt, datetime.time()),
                             'rate': str(currency_rate._rate)}
    else:
        assert result == [1, 'NOT_FOUND']


@pytest.mark.parametrize('currency, expected_rate', [('USD', '315.35'), ('EUR', '343.98'), ('RUB', '5.52')])
@pytest.mark.parametrize('rate_src_id', [1007, None])
@pytest.mark.parametrize('iso_base', [True, False])
def test_get_fixed_currency_rates(session, xmlrpcserver, rate_src_id, expected_rate, currency, iso_base):
    """При запросе некоторых курсов к тенге без указания источника или в 1007 источнике, получим фиксированные курсы,
    потому что реальные курсы указаны с kzt. флаг iso_base позволит в данном случае char code базовой валюты,
    который как раз указан в нижнем регистре"""
    result = xmlrpcserver.GetCurrencyRate(currency, NOW, rate_src_id, 'KZT', iso_base)
    if iso_base:
        if rate_src_id:
            currency_rate = CurrencyRate.get_real_currency_rate_by_date(session, currency, None, base_cc='kzt',
                                                                        rate_src_id=rate_src_id, iso_base=False)
            assert result[2] == {'currency': currency,
                                 'date': datetime.datetime.combine(currency_rate.rate_dt, datetime.time()),
                                 'rate': str(currency_rate._rate)}
        else:
            # в цб рф реальных курсов к тенге нет
            assert result == [1, 'NOT_FOUND']
    else:
        # здесь переданный источник всегда заменяется 1007 по базовой валюте
        assert result[2] == {'currency': currency,
                             'date': datetime.datetime(2017, 5, 4, 0, 0),
                             'rate': expected_rate}


def test_get_currency_rate_force_date(session, xmlrpcserver):
    """Возвращаем самый поздний курс доступный курс, не позднее запрашиваемого, во времени запроса откидываем время
    т.о. всегда возвращаем курс не позднее 00:00 запрашиваемого дня"""
    currency = create_currency(session)
    base_currency = create_currency(session)
    dates = [ut.trunc_date(NOW) - datetime.timedelta(seconds=1),
             ut.trunc_date(NOW),
             ut.trunc_date(NOW) + datetime.timedelta(seconds=1)]
    rates = [create_currency_rate(session, rate_dt=date, cc=currency.char_code,
                                  base_cc=base_currency.char_code, rate_src_id=9999,
                                  rate=random.randint(1, 100)) for date in dates]
    result = xmlrpcserver.GetCurrencyRate(currency.iso_code, NOW, 9999, base_currency.char_code, False)
    assert result[:2] == [0, 'SUCCESS']
    assert result[2] == {'currency': currency.iso_code,
                         'date': datetime.datetime.combine(rates[1].rate_dt, datetime.time()),
                         'rate': str(rates[1]._rate)}
