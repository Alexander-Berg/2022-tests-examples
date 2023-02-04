# -*- coding: utf-8 -*-

__author__ = 'ufian'

import hamcrest

from balance.mapper import *
from balance.actions.invoice_turnon import InvoiceTurnOn
from balance.constants import *

from tests.base import BalanceTest
from tests.object_builder import *


PAYSYS_WITH_INVOICE_NOT_SENDABLE_ID = 1000
YA_MONEY_PAYSYS_ID = 1000
BANK_JUR_PAYSYS_ID = 1003


AUTORU_PRODUCT_ID_1 = 504808
AUTORU_PRODUCT_ID_2 = 504719


class BalanceRoutineTest(BalanceTest):
    def setUp(self):
        super(BalanceRoutineTest, self).setUp()

        self.shipment_info_base = {'Days', 'Shows', 'Clicks', 'Units', 'Bucks', 'Money'}
        self.shipment_info_lower = {
            x.lower(): x
            for x in self.shipment_info_base
        }

        self.product = self.session.query(Product).getone(DIRECT_PRODUCT_ID)
        self.product_rub = self.session.query(Product).getone(DIRECT_PRODUCT_RUB_ID)
        self.product_usd = self.session.query(Product).getone(DIRECT_PRODUCT_USD_ID)

        self.service7 = Getter(Service, 7)

        self.client = ClientBuilder().build(self.session).obj
        self.order = self._create_order()
        self.session.flush()

        self.dt = datetime.datetime.today()

    def _create_order(self, product=None, **params):
        params = params or dict()
        params['service'] = self.service7
        params['product'] = product or self.product
        params['client'] = self.client

        order = OrderBuilder(**params).build(self.session).obj
        self.session.flush()
        return order

    def _create_invoice(self, qty, order=None):
        invoice = InvoiceBuilder(
            request = RequestBuilder(
                basket=BasketBuilder(
                    rows = [BasketItemBuilder(order=order or self.order, quantity=qty)])
            )
        ).build(self.session).obj
        InvoiceTurnOn(invoice, manual=True).do()
        return invoice

    def _add_shipment(self, dt=None, stop=0, order=None, **kwargs):
        shipment_info = {
            self.shipment_info_lower[key.lower()]: val
            for key, val in kwargs.iteritems()
            if key.lower() in self.shipment_info_lower
        }

        for arg in self.shipment_info_base:
            if arg not in shipment_info:
                shipment_info[arg] = 0

        if order is None:
            order = self.order

        order.shipment.update(dt or self.dt, shipment_info, stop)


def consumes_match(consumes_states, extra_params=None, forced_params=None):
    params = forced_params or [
        'invoice_id',
        'current_qty',
        'current_sum',
        'completion_qty',
        'completion_sum',
        'act_qty',
        'act_sum'
    ] + (extra_params or [])

    def _get_row_matcher(state):
        return hamcrest.has_properties({
            param: val
            for param, val in zip(params, state)
        })

    return hamcrest.contains(*[_get_row_matcher(state) for state in consumes_states])
