# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

from decimal import Decimal
import pytest
import allure
from tests import object_builder as ob

from balance import constants as cst, mapper
from balance.actions.cart import Cart

from brest.core.tests import utils as test_utils
from brest.core.typing import PassportId


DEFAULT_QTY = Decimal('100500.100500')
SERVICE_ID = cst.ServiceId.DIRECT


@pytest.fixture(name='passport')
def get_session_passport():
    session = test_utils.get_test_session()
    passport_id = PassportId(session.oper_id)
    passport = session.query(mapper.Passport).getone(passport_id)
    return passport


@pytest.fixture(name='client')
def create_client(passport):
    with allure.step(u"create test client"):
        session = test_utils.get_test_session()
        client = ob.ClientBuilder.construct(session, single_account_number=ob.get_big_number())
        passport.link_to_client(client)
        session.flush()
        return client


@pytest.fixture(name='orders')
def create_orders(client):
    with allure.step(u"create test orders"):
        session = test_utils.get_test_session()
        service1 = ob.Getter(mapper.Service, SERVICE_ID)

        orders = [
            ob.OrderBuilder.construct(session, service=service1, client=client),
            ob.OrderBuilder.construct(session, service=service1, client=client),
        ]
        return orders


@pytest.fixture(name='cart')
def create_cart(client):
    return Cart(client)


@pytest.fixture(name='cart_with_items')
def create_cart_with_items(cart, orders):
    session = test_utils.get_test_session()
    with allure.step(u"create test cart"):
        for order in orders:
            cart.add(order, DEFAULT_QTY)
        session.flush()
    return cart
