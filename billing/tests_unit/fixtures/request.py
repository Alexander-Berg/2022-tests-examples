# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import allure
import tests.object_builder as ob

from brest.core.tests import utils as test_utils
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.order import create_order, DEFAULT_ORDER_QTY


def create_custom_request(orders_qty_map):
    with allure.step('create request'):
        session = test_utils.get_test_session()
        rows = [ob.BasketItemBuilder(order=order, quantity=qty) for order, qty in orders_qty_map.items()]
        basket = ob.BasketBuilder(rows=rows)
        request = ob.RequestBuilder(basket=basket)
    return request.build(session).obj


@pytest.fixture(name='request_')
def create_request(order=None, client=None, firm_id=None):
    session = test_utils.get_test_session()
    client = client or create_client()
    order = order or create_order(client=client)
    return ob.RequestBuilder.construct(
        session,
        firm_id=firm_id,
        basket=ob.BasketBuilder(
            client=client,
            rows=[ob.BasketItemBuilder(order=order, quantity=1)],
        ),
    )
