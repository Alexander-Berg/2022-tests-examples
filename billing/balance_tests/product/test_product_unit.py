# -*- coding: utf-8 -*-
import pytest
import mock
from decimal import Decimal as D

from balance import constants as cst, mapper
from tests import object_builder as ob


TEST_UNIT_NAME = u'тестовое название'
TEST_UNIT_NAME_ENG = u'test name'

TEST_PRODUCT_TYPE_CC = 'New_type'
TEST_PRODUCT_TYPE_NAME = u'abc'

TEST_PRECISION = 6


def get_mock_tanker(lang):
    if lang == cst.LOCALE_RU:
        return {
            'ID_Product_unit_bucks': u'у.е.',
            'ID_Product_unit_units': u'штуки',
            'ID_RUB': u'руб.',
            'ID_RUB_full': u'Российский рубль',
            'ID_USD': u'USD',
            'ID_USD_full': u'Доллар США',
        }
    else:
        return {
            'ID_Product_unit_bucks': u'прям',
            'ID_Product_unit_units': u'прум',
            'ID_RUB': u'rub.',
            'ID_RUB_full': u'Russian ruble',
            'ID_USD': u'USD',
            'ID_USD_full': u'USA Dollar',
        }


def get_unit(session, unit_id, type_rate=1):
    if unit_id is not None:
        return ob.Getter(mapper.ProductUnit, unit_id)
    product_type = mapper.ProductType(
        id=ob.get_big_number(),
        cc=TEST_PRODUCT_TYPE_CC,
        name=TEST_PRODUCT_TYPE_NAME,
        precision=TEST_PRECISION,
    )
    unit = mapper.ProductUnit(
        id=ob.get_big_number(),
        name=TEST_UNIT_NAME,
        englishname=TEST_UNIT_NAME_ENG,
        type_rate=type_rate,
        precision=TEST_PRECISION,
        product_type_id=product_type.id,
    )
    session.add(product_type)
    session.add(unit)
    session.flush()
    return unit


class TestProductMapper(object):

    @pytest.mark.parametrize(
        'unit_id, lang, unit_name, unit_full_name, type_rate',
        [
            (cst.AUCTION_UNIT_ID, 'ru', u'у.е.', u'у.е.', None),
            (cst.AUCTION_UNIT_ID, 'non_ru', u'прям', u'прям', None),
            (cst.DEFAULTS_1000_UNIT_ID, 'ru', u'штуки × 1000', u'штуки × 1000', None),
            (cst.DEFAULTS_1000_UNIT_ID, 'non_ru', u'прум × 1000', u'прум × 1000', None),
            (cst.RUB_UNIT_ID, 'ru', u'руб.', u'Российский рубль', None),
            (cst.RUB_UNIT_ID, 'non_ru', u'rub.', u'Russian ruble', None),
            (cst.USD_UNIT_ID, 'ru', u'USD', u'Доллар США', None),
            (cst.USD_UNIT_ID, 'non_ru', u'USD', u'USA Dollar', None),
            (None, 'ru', TEST_UNIT_NAME, TEST_UNIT_NAME, 1),
            (None, 'en', TEST_UNIT_NAME_ENG, TEST_UNIT_NAME_ENG, 1),
            (None, 'ru', TEST_UNIT_NAME, TEST_UNIT_NAME, 666),
            (None, 'en', TEST_UNIT_NAME_ENG, TEST_UNIT_NAME_ENG, 666),
        ],
    )
    @mock.patch('balance.multilang_support.PhraseManager', get_mock_tanker)
    def test_get_local_unit_name(self, session, unit_id, lang, unit_name, unit_full_name, type_rate):
        product = ob.ProductBuilder(
            unit=get_unit(session, unit_id, type_rate),
        ).build(session).obj
        assert product.get_local_unit_names(lang) == (unit_name, unit_full_name)

    @pytest.mark.parametrize(
        'unit_id, normalize, qty, printable_val',
        [
            (None, False, D('12.12300007'), '12.123000'),
            (None, True, D('12.123000007'), '12.123'),
            (cst.RUB_UNIT_ID, False, D('12.1000007'), '12.10'),
            (cst.RUB_UNIT_ID, True, D('12.1000007'), '12.10'),
            (cst.RUB_UNIT_ID, True, D('12.126666'), '12.13'),
            (cst.RUB_UNIT_ID, False, D('12.1266667'), '12.13'),
        ],
    )
    def test_get_printable_qty(self, session, unit_id, normalize, qty, printable_val):
        product = ob.ProductBuilder(
            unit=get_unit(session, unit_id),
        ).build(session).obj
        assert product.get_printable_qty(qty, normalize) == printable_val
