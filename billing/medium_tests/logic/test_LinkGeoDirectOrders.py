# -*- coding: utf-8 -*-

from __future__ import with_statement

import datetime
import random
import balance.muzzle_util as ut
from tests.base import MediumTest
from tests.object_builder import *
import xmlrpclib
from balance.mapper import *
from balance.actions.invoice_turnon import InvoiceTurnOn
from balance.constants import *


PAYSYS_UR = 1003


class TestLinkGeoDirectOrders(MediumTest):
    def create_orders(self, client):
        dir_prod = Getter(Product, DIRECT_PRODUCT_ID)
        geo_prod = Getter(Product, GEOCON_PRODUCT_ID)

        dir_order = OrderBuilder(client = client, product = dir_prod, service = Getter(Service, ServiceId.DIRECT)).build(self.session).obj
        geo_order = OrderBuilder(client = client, product = geo_prod, service = Getter(Service, ServiceId.GEOCON)).build(self.session).obj
        return (dir_order, geo_order)

    def create_geocontext_payment(self, client, dir_order, geo_order, geo_qty):
        paysys = Getter(Paysys, PAYSYS_UR)

        request = RequestBuilder(basket = BasketBuilder(
            dt = datetime.datetime.now(),
            rows = [BasketItemBuilder(order = geo_order, quantity = geo_qty)]
        ))

        invoice_builder = InvoiceBuilder(dt=datetime.datetime.now(), request = request, paysys = paysys)
        invoice = invoice_builder.build(self.session).obj
        InvoiceTurnOn(invoice, manual=True).do()
        return invoice

    def call_link(self, dir_o, geo_o, qty):
        return self.xmlrpcserver.LinkGeoDirectOrders(None, {
            'GeoContextServiceOrderID': geo_o,
            'DirectServiceOrderID': dir_o,
            'ConsumeQTY': qty
        })

    def test_failed(self):
        with self.session.begin():
            client = ClientBuilder().build(self.session).obj
            orders = self.create_orders(client)
        try:
            self.call_link(orders[0].service_order_id, orders[1].service_order_id, 50)
            assert(False)
        except xmlrpclib.Fault, e:
            assert(str(e).find('not payed')>0)

    def test_increase(self):
        with self.session.begin():
            client = ClientBuilder().build(self.session).obj
            (dir_o, geo_o) = self.create_orders(client)
            self.create_geocontext_payment(client, dir_o, geo_o, 50)
        assert(dir_o.consume_qty == 0)
        res = self.call_link(dir_o.service_order_id, geo_o.service_order_id, 50)
        assert(res)
        assert(dir_o.consume_qty == 50)

    def test_decrease(self):
        with self.session.begin():
            client = ClientBuilder().build(self.session).obj
            (dir_o, geo_o) = self.create_orders(client)
            self.create_geocontext_payment(client, dir_o, geo_o, 50)
        res = self.call_link(dir_o.service_order_id, geo_o.service_order_id, 50)
        assert(res)
        assert(dir_o.consume_qty == 50)
        res = self.call_link(dir_o.service_order_id, geo_o.service_order_id, 20)
        assert(dir_o.consume_qty == 20)
        try:
            res = self.call_link(dir_o.service_order_id, geo_o.service_order_id, 20)
        except xmlrpclib.Fault, e:
            assert(str(e).find('already has')>0)
