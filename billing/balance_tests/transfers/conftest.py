# -*- coding: utf-8 -*-

import decimal

import pytest

from balance import mapper
from balance.constants import (
    DIRECT_PRODUCT_ID,
    DIRECT_PRODUCT_RUB_ID,
)

from tests import object_builder as ob

D = decimal.Decimal


@pytest.fixture
def client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture
def product(session):
    return ob.ProductBuilder.construct(session, price=D('12.0'), currency='RUR')


@pytest.fixture
def direct_product(session):
    return ob.Getter(mapper.Product, DIRECT_PRODUCT_ID).build(session).obj


@pytest.fixture
def direct_rub_product(session):
    return ob.Getter(mapper.Product, DIRECT_PRODUCT_RUB_ID).build(session).obj
