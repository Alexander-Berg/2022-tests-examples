# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future.builtins import str as text
from future import standard_library
standard_library.install_aliases()

import datetime

import pytest
import http.client as http
from hamcrest import assert_that, equal_to, is_

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


@pytest.fixture(autouse=True)
def switch_client(client):
    security.set_passport_client(client)


class TestCheckRequest(TestCaseApiAppBase):
    BASE_API = '/v1/cart/check-request'

    def create_request_api_call(self, items, invoices=None):
        call_params = {
            'service_id': SERVICE_ID,
            'item_ids': ','.join([text(item.id) for item in items]),
        }
        if invoices:
            call_params['invoice_ids'] = ','.join(str(i.id) for i in invoices)

        response = self.test_client.get(
            self.BASE_API,
            params=call_params,
            is_admin=False,
        )
        assert_that(response.status_code, equal_to(http.OK), 'Response code must be OK')
        response_data = response.get_json()['data']
        return response_data

    def test_items(self, cart_with_items):
        items = cart_with_items.items_query.all()
        item_for_request = items.pop()
        response = self.create_request_api_call([item_for_request])

        assert_that(
            response['available_payment'],
            is_(True),
        )

    @pytest.mark.charge_note_register
    @pytest.mark.parametrize('is_ok', [False, True])
    def test_w_invoices(self, cart_with_items, is_ok):
        session = test_utils.get_test_session()

        items = cart_with_items.items_query.all()
        client = cart_with_items.client
        person = ob.PersonBuilder.construct(session, client=client, type='ur')

        session.config.__dict__['SINGLE_ACCOUNT_MIN_CLIENT_DT'] = datetime.datetime(2000, 1, 1)
        session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = is_ok
        single_account.prepare.process_client(client)

        invoices = [
            create_common_invoice(session, 666, RUR_BANK_PAYSYS_ID, person=person, overdraft=1)
            for _ in range(3)
        ]

        response = self.create_request_api_call(items, invoices)
        assert_that(
            response['available_payment'],
            is_(is_ok),
        )
