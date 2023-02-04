# -*- coding: utf-8 -*-

import pytest
import datetime
from decimal import Decimal

from butils.decimal_unit import DecimalUnit as DU

from tests import object_builder as ob

NOW = datetime.datetime.now()


def create_product(session, **kwargs):
    return ob.ProductBuilder(create_taxes=False, reference_price_currency=None, create_price=False, **kwargs).build(
        session).obj


def create_product_group(session, **kwargs):
    return ob.ProductGroupBuilder(name='product_group', **kwargs).build(session).obj


def create_product_season_coeff(session, **kwargs):
    return ob.ProdSeasonCoeffBuilder(**kwargs).build(session).obj


@pytest.mark.parametrize('coeff_dt, is_actual_on_now', [(NOW - datetime.timedelta(1), False),
                                                        (NOW, True),
                                                        (NOW + datetime.timedelta(2), False)])
def test_season_coeff_dates(session, coeff_dt, is_actual_on_now):
    product = create_product(session)
    create_product_season_coeff(session, target_id=product.id, dt=NOW, finish_dt=NOW + datetime.timedelta(1), coeff=Decimal('2.3'))
    if is_actual_on_now:
        assert product.season_coeff(coeff_dt) == DU('2.3')
    else:
        assert product.season_coeff(coeff_dt) == DU('1')


@pytest.mark.parametrize('coeff_value, expected_value', [(5, DU('5')),
                                                         (10, DU('0.1'))])
def test_season_coeff_value(session, coeff_value, expected_value):
    product = create_product(session)
    create_product_season_coeff(session, target_id=product.id, dt=NOW, finish_dt=NOW + datetime.timedelta(1),
                                coeff=coeff_value)
    assert product.season_coeff(NOW) == expected_value


def test_season_coeff_from_product_group(session):
    product_group = create_product_group(session)
    product = create_product(session, product_group=product_group)
    create_product_season_coeff(session, target_id=product_group.id, dt=NOW, finish_dt=NOW + datetime.timedelta(1),
                                coeff=2)
    assert product.season_coeff(NOW) == DU('2')
