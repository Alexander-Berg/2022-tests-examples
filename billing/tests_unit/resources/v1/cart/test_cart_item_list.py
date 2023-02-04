# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library
from future.builtins import filter

standard_library.install_aliases()

import datetime

import http.client as http
import pytest
import hamcrest

from balance.actions import single_account
from balance import mapper
from balance.constants import (
    TransferMode,
    ContractTypeId,
    ServiceId,
)

from brest.core.tests import security
from brest.core.tests import utils as test_utils
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.cart import (
    DEFAULT_QTY,
    SERVICE_ID,
    create_cart,
    create_cart_with_items,
    create_client,
    create_orders,
    get_session_passport,
)
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.base import DOMAIN
from tests import object_builder as ob
from tests.balance_tests.invoices.invoice_common import (
    create_invoice as create_common_invoice,
)

from yb_snout_api.tests_unit.resources.v1.cart.utils import items_qty_to_decimal

RUR_BANK_PAYSYS_ID = 1003


@pytest.fixture(autouse=True)
def switch_client(client):
    security.set_passport_client(client)


class TestCartItemList(TestCaseApiAppBase):
    BASE_API = '/v1/cart/item/list'

    @pytest.fixture(autouse=True)
    def mock_config(self):
        session = self.get_test_session()
        session.config.__dict__['CART_SHOW_PAYABLE_INVOICES'] = 1

    def cart_data_api_call(self, params=None):
        params = {'service_id': SERVICE_ID} if params is None else params
        response = self.test_client.get(
            self.BASE_API,
            params=params,
            headers={'HOST': DOMAIN},
            is_admin=False,
        )
        hamcrest.assert_that(response.status_code, http.OK, 'Response code must be OK')
        data = response.get_json()['data']
        items_qty_to_decimal(data['items'])
        return data

    @pytest.mark.usefixtures('client')
    def test_get_empty_list(self):
        """
        Корзина пустая, возвращается пустой список
        """
        cart_data = self.cart_data_api_call()
        hamcrest.assert_that(
            cart_data['item_count'],
            hamcrest.equal_to(0),
            'Cart item_count should be zero',
        )

        hamcrest.assert_that(
            cart_data['items'],
            hamcrest.empty(),
            'Cart item list should be empty',
        )

    def test_get_with_single_account(self, cart_with_items):
        session = test_utils.get_test_session()
        client = cart_with_items.client
        client.creation_dt = session.config.SINGLE_ACCOUNT_MIN_CLIENT_DT
        session.flush()
        single_account.prepare.process_client(client)

        cart_data = self.cart_data_api_call()

        assert client.single_account_number is not None
        assert cart_data[u'client'][u'single_account_number'] == client.single_account_number

    @pytest.mark.charge_note_register
    def test_get_with_credit_invoices(self, cart_with_items):
        on_dt = datetime.datetime.now()
        session = test_utils.get_test_session()
        session.config.__dict__['SINGLE_ACCOUNT_MIN_CLIENT_DT'] = datetime.datetime(2000, 1, 1)
        session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = True

        client = cart_with_items.client
        single_account.prepare.process_client(client)

        contract = ob.create_credit_contract(session, client, commission=ContractTypeId.WHOLESALE_CLIENT)
        order = cart_with_items.items_query.first().order

        pa = ob.InvoiceBuilder(
            contract=contract,
            person=contract.person,
            paysys=ob.Getter(mapper.Paysys, RUR_BANK_PAYSYS_ID),
            postpay=2,
            request=None,
        ).build(session).obj
        pa.transfer(order, TransferMode.dst, 2, skip_check=True)
        order.calculate_consumption(on_dt, {order.shipment_type: 1})
        pa.generate_act(force=1, backdate=on_dt)
        order.calculate_consumption(on_dt, {order.shipment_type: 2})
        pa.generate_act(force=1, backdate=on_dt)

        response = self.test_client.get(
            self.BASE_API,
            data={'detailed': 1},
            headers={'HOST': DOMAIN},
            is_admin=False,
        )
        hamcrest.assert_that(response.status_code, hamcrest.assert_that(http.OK), 'Response code must be OK')
        hamcrest.assert_that(
            response.get_json()['data'],
            hamcrest.has_entries(
                invoices_count=2,
                invoices=hamcrest.contains_inanyorder(*[
                    hamcrest.has_entries(
                        id=i.id,
                        external_id=i.external_id,
                        person={'id': contract.person.id, 'name': contract.person.name},
                        contract={
                            'id': contract.id,
                            'external_id': contract.external_id,
                            'dt': contract.col0.dt.strftime('%Y-%m-%d'),
                        },
                        currency='RUB',
                        amount='100.00',
                        amount_nds='16.67',
                        amount_nsp='0.00',
                        dt=i.dt.strftime('%Y-%m-%d'),
                    )
                    for i in pa.repayments
                ]),
            ),
        )

    @pytest.mark.charge_note_register
    def test_get_with_overdraft_invoices(self, cart_with_items):
        session = test_utils.get_test_session()
        session.config.__dict__['SINGLE_ACCOUNT_MIN_CLIENT_DT'] = datetime.datetime(2000, 1, 1)
        session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = True

        client = cart_with_items.client
        single_account.prepare.process_client(client)

        invoice = create_common_invoice(session, 666, RUR_BANK_PAYSYS_ID, client=client, overdraft=1)
        invoice.turn_on_rows()

        response = self.test_client.get(
            self.BASE_API,
            data={'detailed': 1},
            headers={'HOST': DOMAIN},
            is_admin=False,
        )
        hamcrest.assert_that(response.status_code, hamcrest.assert_that(http.OK), 'Response code must be OK')
        hamcrest.assert_that(
            response.get_json()['data'],
            hamcrest.has_entries(
                invoices_count=1,
                invoices=hamcrest.contains(
                    hamcrest.has_entries(
                        id=invoice.id,
                        external_id=invoice.external_id,
                        person={'id': invoice.person.id, 'name': invoice.person.name},
                        contract={'id': None, 'external_id': None, 'dt': None},
                        currency='RUB',
                        amount='666.00',
                        amount_nds='111.00',
                        amount_nsp='0.00',
                        dt=invoice.dt.strftime('%Y-%m-%d'),
                    ),
                ),
            ),
        )

    @pytest.mark.charge_note_register
    def test_get_with_overdraft_invoices_wrong_service(self, cart_with_items):
        session = test_utils.get_test_session()
        session.config.__dict__['SINGLE_ACCOUNT_MIN_CLIENT_DT'] = datetime.datetime(2000, 1, 1)
        session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = {ServiceId.MKB}

        client = cart_with_items.client
        single_account.prepare.process_client(client)

        invoice = create_common_invoice(session, 666, RUR_BANK_PAYSYS_ID, client=client, overdraft=1)
        invoice.turn_on_rows()

        response = self.test_client.get(
            self.BASE_API,
            data={'detailed': 1},
            headers={'HOST': DOMAIN},
            is_admin=False,
        )
        hamcrest.assert_that(response.status_code, hamcrest.assert_that(http.OK), 'Response code must be OK')
        hamcrest.assert_that(
            response.get_json()['data'],
            hamcrest.has_entries(
                invoices_count=0,
                invoices=[],
            ),
        )

    @pytest.mark.parametrize(
        'w_service',
        [True, False],
    )
    @pytest.mark.parametrize(
        'w_single_account',
        [True, False],
    )
    def test_get_wo_single_account(self, client, cart, w_service, w_single_account):
        session = self.test_session

        service1 = ob.Getter(mapper.Service, ServiceId.DIRECT)
        service2 = ob.Getter(mapper.Service, ServiceId.MARKET)

        orders = [
            ob.OrderBuilder.construct(session, service=service1, client=client),
            ob.OrderBuilder.construct(session, service=service1, client=client),
            ob.OrderBuilder.construct(session, service=service2, client=client),
        ]
        for order in orders:
            cart.add(order, DEFAULT_QTY)

        single_account_number = ob.get_big_number() if w_single_account else None
        client.single_account_number = single_account_number
        session.flush()

        params = {}
        if w_service:
            params['service_id'] = SERVICE_ID

        cart_data = self.cart_data_api_call(params)

        expected_orders = []
        if w_single_account:
            expected_orders = orders
        elif w_service:
            expected_orders = list(filter(lambda o: o.service_id == SERVICE_ID, orders))

        hamcrest.assert_that(
            cart_data,
            hamcrest.has_entries({
                'client': hamcrest.has_entry('single_account_number', single_account_number),
                'item_count': len(expected_orders),
            }),
        )

        if expected_orders:
            hamcrest.assert_that(
                cart_data[u'items'],
                hamcrest.contains_inanyorder(*[
                    hamcrest.has_entries(
                        client_id=o.client_id,
                        order_id=o.id,
                        service_id=o.service_id,
                    )
                    for o in expected_orders
                ]),
            )
