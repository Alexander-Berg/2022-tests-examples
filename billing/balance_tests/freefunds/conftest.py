# -*- coding: utf-8 -*-

import pytest

from balance import mapper

from tests import object_builder as ob

from .common import (
    create_order,
    create_invoice,
)


@pytest.fixture(autouse=True)
def config(session):
    session.config.__dict__['CONSUMPTION_NEGATIVE_REVERSE_ALLOWED'] = False


@pytest.fixture
def client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture
def order_qty(session, client):
    return create_order(session, client, mapper.DIRECT_PRODUCT_ID)


@pytest.fixture
def order_rur(session, client):
    return create_order(session, client, mapper.DIRECT_PRODUCT_RUB_ID)


@pytest.fixture(name='invoice')
def create_invoice_fixture(session, client, order_rur):
    return create_invoice(session, client, order_rur)
