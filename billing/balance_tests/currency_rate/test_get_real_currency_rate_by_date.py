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


@pytest.mark.parametrize('rate_src_id', [None, 9999])
@pytest.mark.parametrize('cc, base_cc, expected_rate_src_id', [('UAH', 'RUR', 1111),
                                                               ('KZT', 'RUR', 1111),
                                                               ('TRY', 'RUR', 1111),
                                                               ('TRY', 'RUB', 1111),
                                                               ('UAH', None, 1000),
                                                               ('KZT', None, 1000),
                                                               ('TRY', None, 1000)])
def test_currency_rate_create(session, cc, base_cc, rate_src_id, expected_rate_src_id):
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=cc, base_cc=base_cc,
                                         rate_src_id=rate_src_id, rate=100)
    assert currency_rate.base_cc == 'RUR'
    assert currency_rate.selling_rate is None
    assert currency_rate.cc == cc
    assert currency_rate.rate_dt == ut.trunc_date(NOW)
    if base_cc == 'RUR':
        if cc in ('UAH', 'KZT', 'TRY'):
            assert currency_rate.rate_src_id == 1111
    else:
        assert currency_rate.rate_src_id == rate_src_id or expected_rate_src_id


@pytest.mark.parametrize('rate_src_id', [1000, None])
@pytest.mark.parametrize('base_cc', ['RUR', 'RUB', None])
def test_get_real_currency_rate_by_date_rub(session, rate_src_id, base_cc):
    """Рубль к рублю в дефолтном источнике или ЦБ РФ с фиксом на iso_code"""
    result = CurrencyRate.get_real_currency_rate_by_date(session, iso_cc='RUB', rate_src_id=rate_src_id,
                                                         base_cc=base_cc)
    assert result.cc == 'RUR'
    assert result.rate == decimal.Decimal(1)
    assert result.rate_dt == NOW.date()


@pytest.mark.parametrize('base_cc', ['RUR', 'RUB'])
def test_get_real_currency_rate_by_date_default_base_cc(session, base_cc):
    """базовая валюта по умолчанию - рубли"""
    currency = create_currency(session)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                                         base_cc=base_cc, rate_src_id=9999, rate=100)
    result = CurrencyRate.get_real_currency_rate_by_date(session, iso_cc=currency.iso_code, rate_src_id=9999)
    assert result is currency_rate


def test_get_real_currency_rate_by_date_default_rate_src_id(session):
    """источник курса по умолчанию - ЦБ РФ"""
    currency = create_currency(session)
    base_currency = create_currency(session)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                                         base_cc=base_currency.char_code, rate_src_id=1000, rate=100)
    result = CurrencyRate.get_real_currency_rate_by_date(session, iso_cc=currency.iso_code,
                                                         base_cc=base_currency.char_code)
    assert result is currency_rate


@pytest.mark.parametrize('base_cc, rate_src_id', [('RUR', 1000), ('USD', 1001), ('TRY', 1002),
                                                  ('UAH', 1003), ('CNY', 1004), ('AMD', 1005),
                                                  ('BYN', 1006), ('KZT', 1007), ('GEL', 1008)])
def test_get_real_currency_rate_by_date_rate_src_id_by_base_cc(session, base_cc, rate_src_id):
    """источник курса в зависимости от базовой валюты, если не был передан явно"""
    currency = create_currency(session)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                                         base_cc=base_cc, rate_src_id=rate_src_id, rate=100)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc=base_cc, rate_src_id=9999, rate=100)
    result = CurrencyRate.get_real_currency_rate_by_date(session, iso_cc=currency.iso_code,
                                                         base_cc=base_cc)
    assert result is currency_rate


def test_get_real_currency_rate_by_date_exact_rate_src_id(session):
    """явно указанный источник курса"""
    currency = create_currency(session)
    base_currency = create_currency(session)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                                         base_cc=base_currency.char_code,
                                         rate_src_id=9999, rate=100)
    result = CurrencyRate.get_real_currency_rate_by_date(session, iso_cc=currency.iso_code,
                                                         base_cc=base_currency.char_code,
                                                         rate_src_id=currency_rate.rate_src_id)
    assert result is currency_rate


def test_get_real_currency_rate_by_date_with_iso_base(session):
    """iso_base - флаг, указывающий в каком формате был передана базовая валюта"""
    currency = create_currency(session)
    base_currency = create_currency(session)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                                         base_cc=base_currency.char_code, iso_base=base_currency.iso_code,
                                         rate_src_id=1000, rate=100)
    result = CurrencyRate.get_real_currency_rate_by_date(session, iso_cc=currency.iso_code, iso_base=True,
                                                         base_cc=base_currency.iso_code)
    assert result is currency_rate


def test_get_real_currency_rate_by_date_fix_iso(session):
    """исправляем RUB на RUR"""
    currency = create_currency(session)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code, base_cc='RUR',
                                         rate_src_id=1000, rate=100)
    result = CurrencyRate.get_real_currency_rate_by_date(session, iso_cc=currency.iso_code, base_cc='RUB')
    assert result is currency_rate


@pytest.mark.parametrize('base_cc', ['RUR', 'RUB'])
@pytest.mark.parametrize('cc', ['UAH', 'TRY', 'KZT'])
def test_get_real_currency_rate_by_date_non_fix_src(session, cc, base_cc):
    """При запросе реальных курсов ищем по маленьким валютам"""
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=cc.lower(), base_cc=base_cc,
                                         rate_src_id=1000, rate=100)
    result = CurrencyRate.get_real_currency_rate_by_date(session, iso_cc=cc, base_cc=base_cc)
    assert result is currency_rate


def test_get_real_currency_rate_by_date_filter_by_date(session):
    """Возвращаем самый поздний курс доступный курс, не позднее запрашиваемого"""
    currency = create_currency(session)
    base_currency = create_currency(session)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                                         base_cc=base_currency.char_code, rate_src_id=1000, rate=100)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW) - datetime.timedelta(hours=1),
                         cc=currency.char_code, base_cc=base_currency.char_code, rate_src_id=1000, rate=100)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW) + datetime.timedelta(hours=1),
                         cc=currency.char_code, base_cc=base_currency.char_code, rate_src_id=1000, rate=100)
    result = CurrencyRate.get_real_currency_rate_by_date(session, iso_cc=currency.iso_code,
                                                         base_cc=base_currency.char_code)
    assert result is currency_rate


def test_get_real_currency_rate_no_rate(session):
    currency = create_currency(session)
    with pytest.raises(exc.INVALID_MISSED_CURRENCY_RATE) as exc_info:
        CurrencyRate.get_real_currency_rate_by_date(session, iso_cc=currency.iso_code, base_cc='RUB',
                                                    dat=ut.trunc_date(NOW))
    assert exc_info.value.msg == 'Missed currency {} rate on {}'.format(currency.iso_code, ut.trunc_date(NOW))
