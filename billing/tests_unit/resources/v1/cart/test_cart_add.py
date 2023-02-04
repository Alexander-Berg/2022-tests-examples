# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
from decimal import Decimal
import pytest
from hamcrest import (
    assert_that,
    contains_inanyorder,
    equal_to,
    has_entries,
    has_item,
    has_property,
)

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.cart import (
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


class TestCartAddItem(TestCaseApiAppBase):
    BASE_API = '/v1/cart/item/add'
    QTY = Decimal('987.789')

    def add_item_api_call(self, order, qty, old_qty=None, deny_promocode=None, service_promocode=None):
        data = {
            'service_id': order.service_id,
            'service_order_id': order.service_order_id,
            'qty': qty,
            'old_qty': old_qty,
        }
        if deny_promocode is not None:
            data['deny_promocode'] = deny_promocode
        if service_promocode is not None:
            data['service_promocode'] = service_promocode

        response = self.test_client.secure_post(
            self.BASE_API,
            data=data,
            is_admin=False,
        )
        if response.status_code == http.OK:
            items = response.get_json()['data']['items']
            items_qty_to_decimal(items)
        return response

    def test_add_cart_item(self, cart, orders):
        """
        Добавляем позиции в корзину, проверяем, что позиции появляются в корзине
        """
        for order in orders:
            response = self.add_item_api_call(order, self.QTY)
            assert_that(response.status_code, equal_to(http.OK), 'Response code must be OK')
            items = response.get_json()['data']['items']
            assert_that(
                items,
                has_item(
                    has_entries({
                        'service_id': order.service_id,
                        'service_order_id': order.service_order_id,
                        'qty': self.QTY,
                    })),
                'Response must contain created item.',
            )

        cart_expected_items = items_to_matchers(cart.items_query.all())

        assert_that(
            items,
            contains_inanyorder(*cart_expected_items),
            'Items from add query and cart/items query must be equal.',
        )

    def test_add_existing_cart_item(self, cart_with_items):
        """
        Увеличиваем количество для позиции корзины
        """
        test_item = cart_with_items.items_query.first()
        order = test_item.order
        old_qty = test_item.quantity.as_decimal()
        response = self.add_item_api_call(order, self.QTY, old_qty=old_qty)
        assert_that(response.status_code, equal_to(http.OK), 'Response code must be OK')
        items = response.get_json()['data']['items']
        assert_that(
            items,
            has_item(
                has_entries({
                    'service_id': order.service_id,
                    'service_order_id': order.service_order_id,
                    'qty': self.QTY + old_qty,
                })),
            'Response should contain item with expected qty.',
        )

    def test_add_existing_cart_item_wrong_old_qty(self, cart_with_items):
        """
        Увеличиваем количество у позиции корзины, с указанным не верным параметром old_qty,
        проверяем, что вернулся код ошибки 400(BAD_REQUEST)
        """
        test_item = cart_with_items.items_query.first()
        order = test_item.order
        incorrect_old_qty = test_item.quantity.as_decimal() + self.QTY

        response = self.add_item_api_call(order, self.QTY, old_qty=incorrect_old_qty)
        assert_that(
            response.status_code,
            equal_to(http.BAD_REQUEST),
            'Response code should be BAD_REQUEST',
        )

    def test_add_existing_cart_item_none_old_qty(self, cart_with_items):
        """
        Увеличиваем количество у позиции корзины, без параметра old_qty,
        проверяем, что вернулся код ошибки 400(BAD_REQUEST)
        """
        test_item = cart_with_items.items_query.first()
        order = test_item.order

        response = self.add_item_api_call(order, self.QTY, old_qty=None)
        assert_that(
            response.status_code,
            equal_to(http.BAD_REQUEST),
            'Response code should be BAD_REQUEST',
        )

    @pytest.mark.parametrize('flag_value', [False, True])
    def test_add_payload_deny_promocode(self, cart, flag_value, orders):
        response = self.add_item_api_call(orders[0], self.QTY, deny_promocode=flag_value)
        assert_that(response.status_code, equal_to(http.OK), 'Response code must be OK')

        cart_item, = cart.items_query
        assert_that(
            cart_item,
            has_property(
                'payload',
                {'deny_promocode': flag_value},
            ),
        )

    @pytest.mark.parametrize('flag_value', [False, True])
    def test_add_payload_service_promocode(self, cart, flag_value, orders):
        response = self.add_item_api_call(orders[0], self.QTY, service_promocode=flag_value)
        assert_that(response.status_code, equal_to(http.OK), 'Response code must be OK')

        cart_item, = cart.items_query
        assert_that(
            cart_item,
            has_property(
                'payload',
                {'service_promocode': flag_value},
            ),
        )

    @pytest.mark.parametrize('service_promocode', [False, True])
    @pytest.mark.parametrize('deny_promocode', [False, True])
    def test_add_payload_deny_service_promocode(self, cart, orders, service_promocode, deny_promocode):
        response = self.add_item_api_call(
            orders[0],
            self.QTY,
            service_promocode=service_promocode,
            deny_promocode=deny_promocode,
        )
        assert_that(response.status_code, equal_to(http.OK), 'Response code must be OK')

        cart_item, = cart.items_query
        assert_that(
            cart_item,
            has_property(
                'payload',
                {
                    'service_promocode': service_promocode,
                    'deny_promocode': deny_promocode,
                },
            ),
        )
