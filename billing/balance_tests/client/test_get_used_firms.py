# -*- coding: utf-8 -*-

from __future__ import with_statement

from balance.actions.invoice_turnon import InvoiceTurnOn
from balance import mapper

from tests.base import BalanceTest
from tests.object_builder import ClientBuilder, OrderBuilder, PersonBuilder, BasketBuilder, Getter, \
    BasketItemBuilder, RequestBuilder, InvoiceBuilder


class TestGetPayQuery(BalanceTest):
    """
    Тесты на производные от balance.mapper.clients.Client#get_pay_query
    get_used_currencies, get_used_person_categories, get_used_firms
    """

    def setUp(self):
        super(TestGetPayQuery, self).setUp()
        self.client = ClientBuilder(is_agency=1).build(self.session).obj

    def cr_person(self, category):
        return PersonBuilder(type=category, client=self.client).build(self.session).obj

    def cr_request(self, service_id=7, order_client=None):
        order = OrderBuilder(
            client=order_client or self.client,
            agency=self.client if order_client else None,
            service=Getter(mapper.Service, service_id)
        )
        basket = BasketBuilder(
            client=self.client,
            rows=[BasketItemBuilder(quantity=1, order=order)]
        )
        request = RequestBuilder(basket=basket).build(self.session).obj
        return request

    def cr_invoice(self, person, paysys, service_id=7, order_client=None):
        invoice = InvoiceBuilder(
            person=person,
            paysys=paysys,
            request=self.cr_request(service_id, order_client)
        ).build(self.session).obj
        InvoiceTurnOn(invoice, manual=True).do()
        return invoice

    @staticmethod
    def _extract_service_categories(res):
        return {
            s_id: {(f and f.id, pc.category) for f, pc in rows}
            for s_id, rows in res.iteritems()
            }

    def test_firms_invoice(self):
        person_1 = self.cr_person('ur')
        person_2 = self.cr_person('kzp')
        paysys_1 = Getter(mapper.Paysys, 1003)
        paysys_2 = Getter(mapper.Paysys, 1017)
        self.cr_invoice(person_1, paysys_1, 7)
        self.cr_invoice(person_2, paysys_2, 7)
        self.assertSetEqual(
            {f.id for f in self.client.get_used_firms()},
            {1, 25}
        )

    def test_firms_subclient(self):
        subclient = ClientBuilder(agency=self.client).build(self.session).obj
        person_1 = self.cr_person('ur')
        person_2 = self.cr_person('kzp')
        paysys_1 = Getter(mapper.Paysys, 1003)
        paysys_2 = Getter(mapper.Paysys, 1017)
        self.cr_invoice(person_1, paysys_1, 7, subclient)
        self.cr_invoice(person_2, paysys_2, 7, subclient)
        self.assertSetEqual(
            {f.id for f in subclient.get_used_firms()},
            {1, 25}
        )

    def test_firms_no_invoice(self):
        self.cr_person('ur')
        self.cr_person('ua')
        self.assertSetEqual(self.client.get_used_firms(), set())

    def test_firms_no_person(self):
        self.assertSetEqual(self.client.get_used_firms(), set())
