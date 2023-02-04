# -*- coding: utf-8 -*-
import datetime as dt
import decimal

import pytest

from balance import muzzle_util as ut
from balance.actions.invoice_turnon import InvoiceTurnOn
from balance.constants import RegionId, ServiceId
from balance.core import Core
from balance.mapper import (
    ClientDiscount2010, Service, Paysys, Group, Firm, CurrencyRate, Contract, Scale,
)
from tests.base import BalanceTest
from tests.object_builder import (
    ClientBuilder, PersonBuilder, PayOnCreditCase, BasketBuilder, BasketItemBuilder, OrderBuilder, Getter,
    RequestBuilder, InvoiceBuilder, GroupBuilder, ProductBuilder,
)

RUR_BANK_PAYSYS_ID = 1003


class TestDiscount(BalanceTest):
    @pytest.mark.skip()
    def test_bel_discounts(self):
        b_client = ClientBuilder(is_agency=True)
        b_person = PersonBuilder(client=b_client, type=u'ur').build(self.session)

        pc = PayOnCreditCase(self.session)
        prod = pc.get_product_hierarchy(media_discount=4)

        cont = pc.get_contract(
            client=b_client,
            person=b_person,
            commission=1,
            payment_type=3,
            credit_type=1,
            payment_term=30,
            services={77},
            is_signed=1,
            firm=1,
            discount_policy_type=7,
            turnover_forecast={
                prod[0].activity_type.id: 235000000,
                prod[1].activity_type.id: 235000000,
            },
        )
        prod[1]._other.price.b.tax = 0
        prod[1]._other.price.b.price = 1000

        def client(budget=None):
            clnt = ClientBuilder(agency=cont.client).build(self.session).obj
            if budget is not None:
                clnt.discounts_2010 = ClientDiscount2010(belarus_mode=1, belarus_budget=budget)
            return clnt

        def pay_on_credit(lst):
            basket = BasketBuilder(rows=[BasketItemBuilder(
                order=OrderBuilder(product=prod[1], client=c[0], agency=cont.client, service=Getter(Service, 77)),
                quantity=c[1]) for c in lst], client=cont.client)
            return pc.pay_on_credit(basket, cont)

        def pay(lst):
            rows = [BasketItemBuilder(
                order=OrderBuilder(product=prod[1], client=c[0], agency=cont.client, service=Getter(Service, 77)),
                quantity=c[1]) for c in lst]
            b_request = RequestBuilder(basket=BasketBuilder(rows=rows, client=cont.client))
            inv = InvoiceBuilder(request=b_request, person=cont.person, contract=cont).build(self.session).obj
            InvoiceTurnOn(inv, manual=True).do()
            return inv

        # test budget mode
        clnt = client(100000)
        invoices = pay_on_credit([(clnt, 10 / decimal.Decimal('0.95'))])
        self.assertEqual(invoices[0].agency_discount_pct, decimal.Decimal('27.75'))
        self.assertEqual(invoices[0].invoice_orders[0].discount_pct, ut.mul_discounts(decimal.Decimal('27.75'), 0))
        invoices = pay_on_credit([(clnt, 189 / decimal.Decimal('0.95'))])
        self.assertEqual(invoices[0].invoice_orders[0].discount_pct,
                         ut.mul_discounts(decimal.Decimal('27.75'), decimal.Decimal('20')))
        invoices = pay_on_credit([(clnt, 1 / decimal.Decimal('0.9'))])
        self.assertEqual(invoices[0].invoice_orders[0].discount_pct, ut.mul_discounts(decimal.Decimal('27.75'), 0))

        # test invoice mode
        clnt = client()
        invoices = pay_on_credit([(clnt, 20)])
        self.assertEqual(invoices[0].invoice_orders[0].discount_pct,
                         ut.round00(ut.mul_discounts(decimal.Decimal('27.75'), 5)))
        invoices = pay_on_credit([(clnt, 70)])
        self.assertEqual(invoices[0].invoice_orders[0].discount_pct,
                         ut.round00(ut.mul_discounts(decimal.Decimal('27.75'), 10)))
        invoice = pay([(clnt, 30), (client(), 70)])
        self.assertEqual([i for i in invoice.invoice_orders if i.client == clnt][0].discount_pct,
                         ut.mul_discounts(decimal.Decimal('27.75'), 8))

        # test agency scale
        clnt = client()
        invoices = pay_on_credit([(clnt, 12500)])
        self.assertEqual(invoices[0].agency_discount_pct, decimal.Decimal('27.75'))
        invoices = pay_on_credit([(clnt, 81000)])
        self.assertEqual(invoices[0].agency_discount_pct, decimal.Decimal('27.75'))

    @pytest.mark.skip()
    def test_ukr_discounts(self):
        b_client = ClientBuilder(is_agency=True)
        b_person = PersonBuilder(client=b_client, type=u'pu').build(self.session)

        pc = PayOnCreditCase(self.session)
        prod = pc.get_product_hierarchy(media_discount=8, currency='UAH')

        cont = pc.get_contract(
            client=b_client,
            person=b_person,
            commission=5,
            payment_type=3,
            credit_type=1,
            payment_term=30,
            services={77},
            is_signed=1,
            firm=2,
            discount_policy_type=12,
            turnover_forecast={
                prod[0].activity_type.id: 235000000,
                prod[1].activity_type.id: 235000000,
            },
            finish_dt=dt.datetime.now() + dt.timedelta(1000)
        )
        prod[1]._other.price.b.tax = 0
        prod[1]._other.price.b.price = 1000

        def client(budget=None):
            clnt = ClientBuilder(agency=cont.client).build(self.session).obj
            if budget is not None:
                pc.get_contract(
                    client=clnt,
                    commission=42,
                    payment_type=3,
                    credit_type=1,
                    payment_term=30,
                    services={77},
                    is_signed=1,
                    firm=2,
                    ukr_budget=budget,
                    finish_dt=dt.datetime.now() + dt.timedelta(1000)
                )
            return clnt

        def pay_on_credit(lst):
            basket = BasketBuilder(rows=[BasketItemBuilder(
                order=OrderBuilder(product=prod[1], client=c[0], agency=cont.client, service=Getter(Service, 77)),
                quantity=c[1]) for c in lst], client=cont.client, dt=dt.datetime.now() + dt.timedelta(60))
            return pc.pay_on_credit(basket, cont, paysys=self.session.query(Paysys).getone(1018))

        def pay(lst):
            rows = [BasketItemBuilder(
                order=OrderBuilder(product=prod[1], client=c[0], agency=cont.client, service=Getter(Service, 77)),
                quantity=c[1]) for c in lst]
            b_request = RequestBuilder(basket=BasketBuilder(rows=rows, client=cont.client))
            inv = InvoiceBuilder(request=b_request, person=cont.person, contract=cont).build(self.session).obj
            InvoiceTurnOn(inv, manual=True).do()
            return inv

        # test invoice mode
        clnt = client(500000)
        invoices = pay_on_credit([(clnt, 100)])
        self.assertEqual(invoices[0].invoice_orders[0].discount_pct, decimal.Decimal('28.72'))

    @pytest.mark.skip()
    def test_ukr_discounts_direct(self):
        b_client = ClientBuilder(is_agency=True)
        b_person = PersonBuilder(client=b_client, type=u'pu').build(self.session)

        pc = PayOnCreditCase(self.session)
        prod = pc.get_product_hierarchy(media_discount=7, currency='UAH')

        cont = pc.get_contract(
            client=b_client,
            person=b_person,
            commission=5,
            payment_type=3,
            credit_type=1,
            payment_term=30,
            services={77},
            is_signed=1,
            firm=2,
            discount_policy_type=12,
            turnover_forecast={
                prod[0].activity_type.id: 235000000,
                prod[1].activity_type.id: 235000000,
            },
            finish_dt=dt.datetime.now() + dt.timedelta(1000)
        )
        prod[1]._other.price.b.tax = 0
        prod[1]._other.price.b.price = 1000

        def client(budget=None):
            clnt = ClientBuilder(agency=cont.client).build(self.session).obj
            if budget is not None:
                pc.get_contract(
                    client=clnt,
                    commission=42,
                    payment_type=3,
                    credit_type=1,
                    payment_term=30,
                    services={77},
                    is_signed=1,
                    firm=2,
                    ukr_budget=budget,
                    finish_dt=dt.datetime.now() + dt.timedelta(1000)
                )
            return clnt

        def pay_on_credit(lst):
            basket = BasketBuilder(rows=[BasketItemBuilder(
                order=OrderBuilder(product=prod[1], client=c[0], agency=cont.client, service=Getter(Service, 77)),
                quantity=c[1]) for c in lst], client=cont.client, dt=dt.datetime.now() + dt.timedelta(60))
            return pc.pay_on_credit(basket, cont, paysys=self.session.query(Paysys).getone(1018))

        def pay(lst):
            rows = [BasketItemBuilder(
                order=OrderBuilder(product=prod[1], client=c[0], agency=cont.client, service=Getter(Service, 77)),
                quantity=c[1]) for c in lst]
            b_request = RequestBuilder(basket=BasketBuilder(rows=rows, client=cont.client))
            inv = InvoiceBuilder(request=b_request, person=cont.person, contract=cont).build(self.session).obj
            InvoiceTurnOn(inv, manual=True).do()
            return inv

        # test invoice mode
        clnt = client(500000)
        invoices = pay_on_credit([(clnt, 100)])

        self.assertEqual(ut.round00(invoices[0].agency_discount_pct), decimal.Decimal('13'))

    @pytest.mark.skip()
    def test_reg_discounts(self):
        pc = PayOnCreditCase(self.session)
        prod = pc.get_product_hierarchy(media_discount=3)
        prod[1]._other.price.b.tax = 0
        prod[1]._other.price.b.price = 1000

        def person(**kwargs):
            pers = PersonBuilder(type=u'ph').build(self.session).obj
            if kwargs:
                pers.client.discounts_2010 = ClientDiscount2010(**kwargs)
            return pers

        def pay(person, sum):
            lst = [(person, sum)]
            rows = [
                BasketItemBuilder(order=OrderBuilder(product=prod[1], client=c[0].client, service=Getter(Service, 77)),
                                  quantity=c[1]) for c in lst]
            b_request = RequestBuilder(basket=BasketBuilder(rows=rows, client=lst[0][0].client))
            inv = InvoiceBuilder(request=b_request, person=lst[0][0]).build(self.session).obj
            InvoiceTurnOn(inv, manual=True).do()
            return inv

        p = person()
        invoice = pay(p, 30)
        self.assertEqual(invoice.invoice_orders[0].discount_pct, 12)
        invoice = pay(p, 20)
        self.assertEqual(invoice.invoice_orders[0].discount_pct, 14)

    @pytest.mark.skip()
    def test_fed_discounts(self):
        pc = PayOnCreditCase(self.session)
        prod = pc.get_product_hierarchy(media_discount=1)
        prod[1]._other.price.b.tax = 0
        prod[1]._other.price.b.price = 1000

        def person(**kwargs):
            pers = PersonBuilder(type='ph').build(self.session).obj
            if kwargs:
                pers.client.discounts_2010 = ClientDiscount2010(**kwargs)
            return pers

        def pay(person, sum):
            lst = [(person, sum)]
            rows = [
                BasketItemBuilder(order=OrderBuilder(product=prod[1], client=c[0].client, service=Getter(Service, 77)),
                                  quantity=c[1]) for c in lst]
            b_request = RequestBuilder(basket=BasketBuilder(rows=rows, client=lst[0][0].client))
            inv = InvoiceBuilder(request=b_request, person=lst[0][0]).build(self.session).obj
            InvoiceTurnOn(inv, manual=True).do()
            return inv

        p = person()

        invoice = pay(p, 700 / decimal.Decimal('0.84'))
        self.assertEqual(invoice.invoice_orders[0].discount_pct, 16)
        invoice = pay(p, 1)
        self.assertEqual(invoice.invoice_orders[0].discount_pct, 16)

        p = PersonBuilder(client=ClientBuilder(is_agency=True), type='ur').build(self.session).obj

        cl1 = ClientBuilder(agency=p.client).build(self.session).obj
        cl1.discounts_2010 = ClientDiscount2010(federal_budget=2500000)

        cl = ClientBuilder(agency=p.client).build(self.session).obj
        cl.make_equivalent(cl1)

        cl2 = ClientBuilder(agency=p.client).build(self.session).obj

        grp1 = GroupBuilder(parent=self.session.query(Group).getone(code='promotion_brand')).build(self.session).obj
        cl.promotion_brand = grp1
        cl2.promotion_brand = grp1

        def pay(sum, client):
            rows = [BasketItemBuilder(
                order=OrderBuilder(product=prod[1], client=client, agency=client.agency, service=Getter(Service, 77)),
                quantity=sum)]
            b_request = RequestBuilder(basket=BasketBuilder(rows=rows, client=p.client))
            inv = InvoiceBuilder(request=b_request, person=p).build(self.session).obj
            InvoiceTurnOn(inv, manual=True).do()
            return inv

        # test equivalent
        invoice = pay(2000 / (1 - decimal.Decimal('18.62') / 100), cl)
        self.assertEqual(invoice.invoice_orders[0].discount_pct, decimal.Decimal('18.62'))

        invoice = pay(1500 / (1 - decimal.Decimal('19.06') / 100), cl2)
        self.assertEqual(invoice.invoice_orders[0].discount_pct, decimal.Decimal('19.06'))

    @pytest.mark.skip()
    def test_fed_contract(self):
        pc = PayOnCreditCase(self.session)
        prod = pc.get_product_hierarchy(media_discount=1)
        prod[1]._other.price.b.tax = 0
        prod[1]._other.price.b.price = 1000
        b_client = ClientBuilder()
        b_person = PersonBuilder(client=b_client, type='ur').build(self.session)
        cont = pc.get_contract(
            client=b_client,
            person=b_person,
            commission=7,
            payment_type=3,
            credit_type=1,
            payment_term=30,
            services={77},
            is_signed=1,
            firm=1,
            federal_budget=2500000,
            turnover_forecast={
                prod[0].activity_type.id: 235000000,
                prod[1].activity_type.id: 235000000,
            },
        )

        def pay_on_credit(lst):
            basket = BasketBuilder(rows=[BasketItemBuilder(
                order=OrderBuilder(product=prod[1], client=c[0], agency=c[0].agency, service=Getter(Service, 77)),
                quantity=c[1]) for c in lst])
            return pc.pay_on_credit(basket, cont)

        invoices = pay_on_credit([(cont.client, 10)])
        self.assertEqual(invoices[0].invoice_orders[0].discount_pct, decimal.Decimal('5'))

    @pytest.mark.skip()
    def test_kz_discount(self):
        pc = PayOnCreditCase(self.session)
        prod = pc.get_product_hierarchy(media_discount=1)
        kz_firm = self.session.query(Firm).getone(3)
        rate = CurrencyRate.get_by_date(self.session, 'KZT', dt.datetime.now())
        prod[1]._other.price.b.tax = 0
        prod[1]._other.price.b.price = rate * 100
        cont = self.session.query(Contract).getone(kz_firm.contract_id)
        cl2 = ClientBuilder(agency=cont.client).build(self.session).obj

        def pay(sum):
            rows = [BasketItemBuilder(
                order=OrderBuilder(product=prod[1], client=cl2, agency=cont.client, service=Getter(Service, 77)),
                quantity=sum)]
            b_request = RequestBuilder(basket=BasketBuilder(rows=rows, client=cont.client))
            inv = InvoiceBuilder(request=b_request, person=cont.person, contract=cont).build(self.session).obj
            InvoiceTurnOn(inv, manual=True).do()
            return inv

        first = self.session.query(Scale).getone(('discount', 'kz_client_discount')).dpoints(None)[1]

        invoice = pay(first.x / 100 - 1)
        self.assertEqual(ut.round00(ut.mul_discounts(0, invoice.agency_discount_pct)),
                         invoice.invoice_orders[0].discount_pct)
        invoice = pay(first.x / 100 + 1)
        self.assertEqual(ut.round00(ut.mul_discounts(first.y, invoice.agency_discount_pct)),
                         invoice.invoice_orders[0].discount_pct)

    def test_fixed_discount(self):
        pc = PayOnCreditCase(self.session)
        prod = pc.get_product_hierarchy(media_discount=1)
        prod[1]._other.price.b.tax = 0
        prod[1]._other.price.b.price = 1000
        b_client = ClientBuilder(is_agency=True)
        b_person = PersonBuilder(client=b_client, type='ur').build(self.session)
        cont = pc.get_contract(
            client=b_client,
            person=b_person,
            commission=7,
            payment_type=3,
            credit_type=1,
            payment_term=30,
            services={77},
            is_signed=1,
            firm=1,
            discount_fixed=15,
            turnover_forecast={
                prod[0].activity_type.id: 235000000,
                prod[1].activity_type.id: 235000000,
            },
        )

        def pay_on_credit(lst):
            basket = BasketBuilder(rows=[BasketItemBuilder(
                order=OrderBuilder(product=prod[1], client=c[0], agency=c[0].agency, service=Getter(Service, 77)),
                quantity=c[1]) for c in lst])
            return pc.pay_on_credit(basket, cont)

        invoices = pay_on_credit([(cont.client, 10)])
        self.assertEqual(invoices[0].invoice_orders[0].discount_pct, decimal.Decimal('15'))
        cont.col0.discount_policy_type = 8
        invoices = pay_on_credit([(cont.client, 10)])
        self.assertEqual(invoices[0].invoice_orders[0].discount_pct, decimal.Decimal('15'))

    def test_autoru_client_discount(self):
        # TODO: дописать тест
        client = ClientBuilder(region_id=RegionId.RUSSIA)
        person = PersonBuilder(client=client, type=u'ur').build(self.session).obj
        product = ProductBuilder()
        order = OrderBuilder(product=product, client=client, service=Getter(Service, ServiceId.AUTORU))
        paysys = Getter(Paysys, RUR_BANK_PAYSYS_ID).build(self.session).obj

        rows = [BasketItemBuilder(order=order, quantity=10)]
        request = RequestBuilder(basket=BasketBuilder(rows=rows, client=client)).build(self.session).obj

        core = Core(self.session)
        invoice = core.create_invoice(request.id, paysys.id, person.id)[0]

        assert invoice.invoice_orders[0].discount_pct == 0

    # тест-кейс скидки при выборе определённых тарифных опций кабинета разработчика
    def test_apikeys_tariffs_discount(self):
        pc = PayOnCreditCase(self.session)
        prod = pc.get_product_hierarchy(media_discount=1)
        prod[1]._other.price.b.tax = 0
        prod[1]._other.price.b.price = 1000
        b_client = ClientBuilder(is_agency=True)
        b_person = PersonBuilder(client=b_client, type='ur').build(self.session)
        cont = pc.get_contract(
            client=b_client,
            person=b_person,
            commission=7,
            payment_type=3,
            credit_type=1,
            payment_term=30,
            services={ServiceId.APIKEYS},
            is_signed=1,
            firm=1,
            discount_fixed=15,
            turnover_forecast={
                prod[0].activity_type.id: 235000000,
                prod[1].activity_type.id: 235000000,
            },
        )

        def pay_on_credit(lst):
            basket = BasketBuilder(rows=[BasketItemBuilder(order=OrderBuilder(product=prod[1],
                                                                              client=c[0],
                                                                              agency=c[0].agency,
                                                                              service=Getter(Service,
                                                                                             ServiceId.APIKEYS)),
                                                           quantity=c[1])
                                         for c in lst])
            return pc.pay_on_credit(basket, cont)

        invoices = pay_on_credit([(cont.client, 10)])
        self.assertEqual(invoices[0].invoice_orders[0].discount_pct, decimal.Decimal('15'))
        cont.col0.discount_policy_type = 8
        invoices = pay_on_credit([(cont.client, 10)])
        self.assertEqual(invoices[0].invoice_orders[0].discount_pct, decimal.Decimal('15'))
