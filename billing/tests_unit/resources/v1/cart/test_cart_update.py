# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import http.client as http
from decimal import Decimal
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    has_item,
)

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.cart import (
    SERVICE_ID,
    create_cart,
    create_cart_with_items,
    create_client,
    create_orders,
    get_session_passport,
)

from yb_snout_api.tests_unit.resources.v1.cart.utils import items_qty_to_decimal


@pytest.fixture(autouse=True)
def switch_client(client):
    security.set_passport_client(client)


class TestCartUpdateItem(TestCaseApiAppBase):
    BASE_API = '/v1/cart/item/update'
    QTY = Decimal('1.100500')

    def update_item_api_call(self, item_id, new_qty, old_qty):
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'service_id': SERVICE_ID,
                'item_id': item_id,
                'new_qty': new_qty,
                'old_qty': old_qty,
            },
            is_admin=False,
        )
        if response.status_code == http.OK:
            items = response.get_json()['data']['items']
            items_qty_to_decimal(items)
        return response

    def test_update_cart_item(self, cart_with_items):
        """
        Обновляем количество для позиции корзины
        """
        item = cart_with_items.items_query.first()
        new_qty = item.quantity.as_decimal() + self.QTY
        response = self.update_item_api_call(item.id, new_qty, item.quantity.as_decimal())
        assert_that(response.status_code, equal_to(http.OK), 'Response code must be OK')
        assert_that(
            response.get_json()['data']['items'],
            has_item(
                has_entries({
                    'id': item.id,
                    'qty': new_qty,
                })),
            'Cart has item with new qty',
        )

    def test_update_cart_item_wrong_old_qty(self, cart_with_items):
        """
        Обновляем количество для позиции корзины, с указанным не верным параметром old_qty,
        проверяем, что вернулся код ошибки 422(UNPROCESSABLE_ENTITY)
        """
        item = cart_with_items.items_query.first()
        new_qty = item.quantity.as_decimal() + self.QTY
        old_qty = item.quantity.as_decimal() + self.QTY * 2
        response = self.update_item_api_call(item.id, new_qty, old_qty)
        assert_that(
            response.status_code,
            equal_to(http.BAD_REQUEST),
            'Response code should be BAD_REQUEST',
        )

    def test_update_cart_item_wrong_id(self, cart_with_items):
        """
        Обновляем количество для позиции корзины, с указанным не верным параметром item_id,
        проверяем, что вернулся код ошибки 422(UNPROCESSABLE_ENTITY)
        """
        not_existing_id = max(item.id for item in cart_with_items.items_query.all()) + 1
        response = self.update_item_api_call(
            not_existing_id,
            self.QTY * 2,
            self.QTY,
        )
        assert_that(
            response.status_code,
            equal_to(http.BAD_REQUEST),
            'Response code must be BAD_REQUEST',
        )
