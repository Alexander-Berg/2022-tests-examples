# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import datetime

import pytest
import http.client as http
import hamcrest

from balance import mapper
from balance.actions import single_account

from brest.core.tests import security
from brest.core.tests import utils as test_utils
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
from tests import object_builder as ob
from tests.balance_tests.invoices.invoice_common import (
    create_invoice as create_common_invoice,
)

RUR_BANK_PAYSYS_ID = 1003

from yb_snout_api.tests_unit.resources.v1.cart.utils import items_qty_to_decimal, items_to_matchers


@pytest.fixture(autouse=True)
def switch_client(client):
    security.set_passport_client(client)


class TestCreateRequest(TestCaseApiAppBase):
    BASE_API = '/v1/cart/create-request'

    def create_request_api_call(self, items, invoices=None):
        call_params = {
            'service_id': SERVICE_ID,
            'item_ids': ','.join([str(item.id) for item in items]),
        }
        if invoices:
            call_params['invoice_ids'] = ','.join(str(i.id) for i in invoices)

        response = self.test_client.secure_post(
            self.BASE_API,
            data=call_params,
            is_admin=False,
        )
        hamcrest.assert_that(response.status_code, http.OK, 'Response code must be OK')
        response_data = response.get_json()['data']
        items_qty_to_decimal(response_data['items'])
        items_qty_to_decimal(response_data['request']['orders'])
        return response_data

    def test_items(self, cart_with_items):
        """
        Создаём запрос для всех строк корзины кроме первой.
        Возвращаемый запрос должен содержать эти строки.
        После создания запроса в корзине должна остаться только первая строка
        """
        items = cart_with_items.items_query.all()
        item_for_request = items.pop()
        response = self.create_request_api_call([item_for_request])

        hamcrest.assert_that(
            response['request']['orders'],
            hamcrest.contains(*items_to_matchers([item_for_request])),
            'Created request should contains all rows from the cart list except the first.',
        )

        hamcrest.assert_that(
            response['items'],
            hamcrest.contains(*items_to_matchers(items)),
            'All items that aren\'t in the request should be preserved in the cart.',
        )

    @pytest.mark.charge_note_register
    def test_w_invoices(self, cart_with_items):
        session = test_utils.get_test_session()

        items = cart_with_items.items_query.all()
        client = cart_with_items.client
        person = ob.PersonBuilder.construct(session, client=client, type='ur')

        session.config.__dict__['SINGLE_ACCOUNT_MIN_CLIENT_DT'] = datetime.datetime(2000, 1, 1)
        session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = True
        single_account.prepare.process_client(client)

        invoices = [
            create_common_invoice(session, 666, RUR_BANK_PAYSYS_ID, person=person, overdraft=1)
            for _ in range(3)
        ]

        response = self.create_request_api_call(items, invoices)
        request = session.query(mapper.Request).get(response['request']['id'])

        hamcrest.assert_that(
            request,
            hamcrest.has_properties(
                rows=hamcrest.contains_inanyorder(*[
                    hamcrest.has_properties(order=i.order)
                    for i in items
                ]),
                register_rows=hamcrest.contains_inanyorder(*[
                    hamcrest.has_properties(ref_invoice=i)
                    for i in invoices
                ]),
            ),
        )
