# -*- coding: utf-8 -*-

from __future__ import with_statement

from balance.actions.invoice_turnon import InvoiceTurnOn
from balance import mapper
from tests.base import BalanceTest
from tests.object_builder import ClientBuilder, OrderBuilder, PersonBuilder, BasketBuilder, Getter, \
    BasketItemBuilder, RequestBuilder, InvoiceBuilder


class TestGetPayQuery(BalanceTest):
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

    def test_has_invoices_invoice(self):
        person = self.cr_person('yt')
        paysys_usd = Getter(mapper.Paysys, 1013)
        paysys_rur = Getter(mapper.Paysys, 1014)
        paysys_eur = Getter(mapper.Paysys, 1023)
        self.cr_invoice(person, paysys_usd, 7)
        self.cr_invoice(person, paysys_rur, 7)
        self.cr_invoice(person, paysys_eur, 11)
        self.assertTrue(self.client.has_invoices(7))
        self.assertTrue(self.client.has_invoices(7, 'RUR'))
        self.assertTrue(self.client.has_invoices(7, 'USD'))
        self.assertFalse(self.client.has_invoices(7, 'EUR'))

    def test_has_invoices_subclient(self):
        subclient = ClientBuilder(agency=self.client).build(self.session).obj
        person = self.cr_person('yt')
        paysys_usd = Getter(mapper.Paysys, 1013)
        paysys_rur = Getter(mapper.Paysys, 1014)
        paysys_eur = Getter(mapper.Paysys, 1023)
        self.cr_invoice(person, paysys_usd, 7, subclient)
        self.cr_invoice(person, paysys_rur, 7, subclient)
        self.cr_invoice(person, paysys_eur, 11, subclient)
        self.assertTrue(subclient.has_invoices(7))
        self.assertTrue(subclient.has_invoices(7, 'USD'))
        self.assertTrue(subclient.has_invoices(7, 'RUR'))
        self.assertFalse(subclient.has_invoices(7, 'EUR'))

    def test_has_invoices_no_invoice(self):
        self.cr_person('yt')
        self.assertFalse(self.client.has_invoices(7))
        self.assertFalse(self.client.has_invoices(7, 'RUR'))

    def test_has_invoices_no_person(self):
        self.assertFalse(self.client.has_invoices(7))
        self.assertFalse(self.client.has_invoices(7, 'RUR'))


if __name__ == '__main__':
    import nose.core

    # nose.core.run(defaultTest='test_clients:TestXXX.test_yyy') # add your test class/method
    nose.core.runmodule()
