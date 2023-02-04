# -*- coding: utf-8 -*-
import datetime
import pytest
from decimal import Decimal

from balance.mapper.common import CurrencyRate
from tests import object_builder as ob
from balance import muzzle_util as ut


def create_currency(session, **kwargs):
    return ob.CurrencyBuilder(**kwargs).build(session).obj


def create_currency_rate(session, **kwargs):
    return ob.CurrencyRateBuilder(**kwargs).build(session).obj


NOW = datetime.datetime.now().replace(microsecond=0)


@pytest.mark.parametrize('from_currency, to_currency', [(None, None), ('ABC', 'ABC')])
def test_get_cross_rate_none(session, from_currency, to_currency):
    result = CurrencyRate.get_cross_rate(session=session, dt=None, from_currency=from_currency, to_currency=to_currency)
    assert result == 1


def test_get_cross_rate_none_to_rate(session):
    from_currency = create_currency(session)
    to_currency = create_currency(session)

    result = CurrencyRate.get_cross_rate(session=session, dt=None, from_currency=from_currency.char_code,
                                         to_currency=to_currency.char_code, from_currency_rate=Decimal('2'),
                                         to_currency_rate=Decimal('2'))
    assert result == 1


@pytest.mark.parametrize('from_currency_rate', [None, Decimal('10.5')])
@pytest.mark.parametrize('to_currency_rate', [None, Decimal('11.5')])
def test_get_cross_rate_from_rates(session, from_currency_rate, to_currency_rate):
    from_currency = create_currency(session)
    from_currency_rate = from_currency_rate
    from_currency_real_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=from_currency.char_code,
                                                   base_cc='RUR', rate_src_id=1000, rate=100)
    to_currency = create_currency(session)
    to_currency_rate = to_currency_rate
    to_currency_real_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=to_currency.char_code,
                                                 base_cc='RUR', rate_src_id=1000, rate=Decimal('67.5'))

    result = CurrencyRate.get_cross_rate(dt=None, session=session, from_currency=from_currency.iso_code,
                                         to_currency=to_currency.iso_code, from_currency_rate=from_currency_rate,
                                         to_currency_rate=to_currency_rate)
    from_currency_rate_value = from_currency_rate or from_currency_real_rate.rate
    to_currency_rate_value = to_currency_rate or to_currency_real_rate.rate
    assert result == from_currency_rate_value / to_currency_rate_value
