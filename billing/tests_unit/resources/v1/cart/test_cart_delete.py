# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import http.client as http
from hamcrest import (
    assert_that,
    contains,
    equal_to,
    empty,
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

from yb_snout_api.tests_unit.resources.v1.cart.utils import items_qty_to_decimal, items_to_matchers


@pytest.fixture(autouse=True)
def switch_client(client):
    security.set_passport_client(client)


class TestCartDeleteItem(TestCaseApiAppBase):
    BASE_API = '/v1/cart/item/delete'

    def delete_item_api_call(self, items):
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'service_id': SERVICE_ID,
                'item_ids': ','.join([str(item.id) for item in items]),
            },
            is_admin=False,
        )
        if response.status_code == http.OK:
            items = response.get_json()['data']['items']
            items_qty_to_decimal(items)
        return response

    def test_delete_cart_item(self, cart_with_items):
        """
        Удаляем одну позицию из корзины, в корзине остаются все строчки кроме удалённой
        """
        items = cart_with_items.items_query.all()
        item_to_delete = items.pop()
        response = self.delete_item_api_call([item_to_delete])
        assert_that(response.status_code, equal_to(http.OK), 'Response code must be OK')

        cart_expected_items = items_to_matchers(items)
        assert_that(
            response.get_json()['data']['items'],
            contains(*cart_expected_items),
            'Should contains all items except deleted.',
        )

    def test_delete_all_cart_items(self, cart_with_items):
        """
        Удаляем все позиции из корзины, корзина пустая
        """
        items = cart_with_items.items_query.all()
        response = self.delete_item_api_call(items)
        assert_that(response.status_code, equal_to(http.OK), 'Response code must be OK')
        assert_that(
            response.get_json()['data']['items'],
            empty(),
            'Cart should be empty.',
        )
