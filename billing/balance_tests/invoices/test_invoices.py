# -*- coding: utf-8 -*-
"""Tests for order-request-invoice chain (before any money has been received).
See BALANCE-4462

Amounts:
    TestInvoiceAmounts
    TestInvoicePaysyses

Other:
    TestMisc
"""

import datetime
import itertools
import collections
import decimal

import pytest
import hamcrest
import sqlalchemy as sa

from balance import mapper
from balance import exc
from balance import muzzle_util as ut
import balance.actions.acts as a_a
from balance.actions.invoice_turnon import InvoiceTurnOn
from balance.actions.invoice_create import InvoiceFactory
from balance.constants import *
from butils import decimal_unit

from tests.base import BalanceTest
from tests import object_builder as ob

DU = decimal_unit.DecimalUnit
D = decimal.Decimal

DEFAULT_FIRM_ID = 1


class TestInvoiceAmounts(BalanceTest):
    """Test if amounts of money in InvoiceOrder's are calculated correctly"""

    def create_invoice(self, td, run_tests):  # td for test data
        # price = td['price'], td['type_rate'], td['nds']
        # row format is (order, quantity, price, discount_pct)
        product = ob.ProductBuilder()
        product._other.price.b.tax = td['nds']
        product._other.price.b.price = td['price']
        if 'product_unit' in td:
            product.b.unit = td['product_unit']
        else:
            product.b.unit = ob.GenericBuilder(
                mapper.ProductUnit,
                id=ob.get_big_number(),  # 123123123,  # No need to be different each time
                name=u"Кнуты",
                englishname="knuts",
                precision=6,
                type_rate=td['type_rate'],
                product_type_id=4
            )
        order = ob.OrderBuilder(product=product)
        row = ob.BasketItemBuilder(order=order, quantity=td['quantity'])
        request = ob.RequestBuilder(basket=ob.BasketBuilder(rows=[row]))
        invoice = ob.InvoiceBuilder(request=request)
        coeffs = []
        product.build(self.session)
        for coeff in td['coeffs']:
            coeffs.append(ob.GenericBuilder(
                mapper.ProdSeasonCoeff,
                dt=datetime.datetime.now() + datetime.timedelta(coeff['start']),
                finish_dt=datetime.datetime.now() + datetime.timedelta(coeff['end']),
                product=product,
                coeff=coeff['coeff']).build(self.session)
                          )
        # product._other.coeffs = coeffs # Attach it to product so it gets built

        invoice.build(self.session)
        if not run_tests:
            return invoice.obj
        # Finally, the tests!
        result_row = invoice.obj.invoice_orders[0]
        self.assertEqual(result_row.amount, td['amount'])
        self.assertEqual(result_row.amount_nds, td['amount_nds'])
        self.assertEqual(result_row.amount_no_discount, td['amount_no_discount'])

    def test_amounts1(self):
        quantity = 1
        price = 100 * 30
        type_rate = 7
        nds = 1
        coeffs = [dict(start=-20, end=-1, coeff=D('1.1')),
                  dict(start=-1, end=1, coeff=D('2'))]
        # Expected values
        amount = D('857.14')
        amount_nds = D('142.86')
        amount_no_discount = D('857.14')
        self.create_invoice(locals(), run_tests=True)

    def test_amounts2(self):
        quantity = 37
        price = D("21.13") * 30
        type_rate = 3
        nds = 0
        coeffs = [dict(start=-100, end=100, coeff=D('1.02'))]
        # Expected values
        amount = D('9569.35')
        amount_nds = D('1594.89')
        amount_no_discount = D('9569.35')
        self.create_invoice(locals(), run_tests=True)

    def test_amounts3(self):
        quantity = D("11.01")
        price = D("100001") * 30
        type_rate = 1
        nds = 0
        coeffs = [dict(start=-100, end=1, coeff=D('0.01'))]
        # Expected values
        amount = D('396363.96')
        amount_nds = D('66060.66')
        amount_no_discount = D('396363.96')
        self.create_invoice(locals(), run_tests=True)

    # Tests for InvoiceOrder's .patch() method

    def test_patch1(self):
        quantity = 1
        price = 100 * 30
        nds = 1
        coeffs = []
        # Unit type: 1 (Shows), precision = 0
        product_unit = self.session.query(
            mapper.ProductUnit).filter_by(product_type_id=1).first()
        invoice = self.create_invoice(locals(), run_tests=False)
        row = invoice.invoice_orders[0]
        row.patch(3100)
        self.assertEqual(row.quantity, D('1033'))
        row.patch(2900)
        self.assertEqual(row.quantity, D('967'))
        row.patch(4500)
        self.assertEqual(row.quantity, D('1500'))
        row.patch(1500)
        self.assertEqual(row.quantity, D('500'))
        row.patch(1000)
        self.assertEqual(row.quantity, D('333'))

    def test_patch2(self):
        quantity = 1
        price = 100 * 30
        nds = 1
        coeffs = []
        # Unit type: 4 (Bucks), precision = 6
        product_unit = self.session.query(
            mapper.ProductUnit).filter_by(product_type_id=4).first()
        invoice = self.create_invoice(locals(), run_tests=False)
        row = invoice.invoice_orders[0]
        row.patch(3100)
        self.assertEqual(row.quantity, D('1.033333'))
        row.patch(2900)
        self.assertEqual(row.quantity, D('0.966667'))
        row.patch(3030)
        self.assertEqual(row.quantity, D('1.01'))
        row.patch(2970)
        self.assertEqual(row.quantity, D('0.990000'))
        row.patch(1000)
        self.assertEqual(row.quantity, D('0.333333'))

    def test_patch3(self):
        quantity = 1
        price = 100 * 30
        nds = 1
        coeffs = []
        # Unit type 3 (Days)
        product_unit = self.session.query(
            mapper.ProductUnit).filter_by(product_type_id=3).first()
        invoice = self.create_invoice(locals(), run_tests=False)
        row = invoice.invoice_orders[0]
        self.assertRaises(exc.CANNOT_PATCH_DAYS_AND_UNITS, row.patch, 3100)

    def test_patch4(self):
        quantity = 1
        price = 100 * 30
        nds = 1
        coeffs = []
        # Unit type 5 (Units)
        product_unit = self.session.query(
            mapper.ProductUnit).filter_by(product_type_id=5).first()
        invoice = self.create_invoice(locals(), run_tests=False)
        row = invoice.invoice_orders[0]
        self.assertRaises(exc.CANNOT_PATCH_DAYS_AND_UNITS, row.patch, 3100)


class TestInvoicePaysyses(BalanceTest):
    """Test amounts calculation with different paysyses"""

    def _test_paysys(self, test_data):
        """Test prices with a given paysys"""
        s = self.session
        td = test_data

        currency_rate = td.get('currency_rate', None)
        if currency_rate is not None:
            rate_dt = ut.trunc_date(datetime.datetime.today())
            mapper.CurrencyRate.set_real_currency_rate(self.session, td['currency'], currency_rate, rate_dt=rate_dt,
                                                base_cc=None, rate_src_id=None, selling_rate=None)

        # Especially for test_uah_magic_product
        if 'product' in td:
            product = td['product']
        else:
            product = ob.ProductBuilder()
            product._other.price.b.price = td['price']
            product._other.price.b.tax = 0
            product.b.unit = ob.GenericBuilder(
                mapper.ProductUnit,
                id=ob.get_big_number(),  # 123123123,  # No need to be different each time
                name=u"Кнуты",
                englishname="knuts",
                precision=6,
                type_rate=td['type_rate'],
                product_type_id=4
            )
            product = product.build(self.session).obj
        # Delete existing season coeffs
        # Yes, it is a dirty way to do it, but it works
        for coeff in product.season_coeffs:
            self.session.delete(coeff)
        product.season_coeffs = []
        for coeff in product.product_group.season_coeffs:
            self.session.delete(coeff)

        self.session.flush()
        product.product_group.season_coeffs = []
        # Tax is where nds and nsp are taken from.
        order = ob.OrderBuilder(product=product)
        # row format is (order, quantity, price, discount_pct)
        row = ob.BasketItemBuilder(order=order, quantity=td['quantity'])
        request = ob.RequestBuilder(basket=ob.BasketBuilder(rows=[row]))
        invoice = ob.InvoiceBuilder(request=request,
                                 paysys=ob.Getter(mapper.Paysys, td['paysys_id']))
        invoice.build(self.session)
        # All ready. Now the tests.
        self.irow = irow = invoice.obj.invoice_orders[0]
        self.assertEqual(irow.effective_sum, td['effective_sum'])
        self.assertEqual(irow.amount_nds, td['amount_nds'])
        self.assertEqual(irow.amount, td['amount'])
        if invoice.obj.paysys and invoice.obj.paysys.certificate:
            self.assertIsNone(invoice.obj.exportable)

    def test_rur_nds20(self):
        paysys_id = 1003
        price = D('123.42') * 30
        quantity = 1
        type_rate = 1
        nds_pct = 20
        nsp_pct = 0
        currency = 'RUR'
        amount = D('4443.12')
        amount_nds = D('740.52')
        effective_sum = amount
        self._test_paysys(locals())

    def test_rur_nds0(self):
        paysys_id = 1014
        price = D('123.42') * 30
        quantity = 1
        type_rate = 1
        nds_pct = 0
        nsp_pct = 0
        currency = 'RUR'
        amount = D('3702.60')
        amount_nds = D('0')
        effective_sum = amount
        self._test_paysys(locals())

    def test_rur_nonds(self):
        # mapper.Paysys 1014, roubles without nds
        paysys_id = 1014
        price = D('123.42') * 30
        quantity = 1
        type_rate = 1
        nds_pct = 0
        nsp_pct = 0
        currency = 'RUR'
        amount = D('3702.60')
        amount_nds = D('0')
        #        effective_sum = D('123.42')
        effective_sum = amount
        self._test_paysys(locals())

    def test_usd(self):
        # mapper.Paysys 1013, usd without nds
        paysys_id = 1013
        price = D('123.42') * 30
        currency_rate = D('21.14')  # because i want to
        usd_rate = currency_rate
        quantity = 1
        type_rate = 1
        nds_pct = 0
        nsp_pct = 0
        currency = 'USD'
        amount = D('175.15')
        amount_nds = D('0')
        #        effective_sum = D('123.42')
        effective_sum = amount
        self._test_paysys(locals())

    def test_eur(self):
        # mapper.Paysys 1023, eur without nds
        paysys_id = 1023
        price = D('123.42') * 30
        currency_rate = D('21.14')  # because i want to
        quantity = 1
        type_rate = 1
        nds_pct = 0
        nsp_pct = 0
        currency = 'EUR'
        amount = D('175.15')
        amount_nds = D('0')
        #        effective_sum = D('123.42')
        effective_sum = amount
        self._test_paysys(locals())


class TestMisc(BalanceTest):
    def test_rur_media_client_sum_wo_nds(self):
        b_product = ob.ProductBuilder()
        b_product.b.media_discount = 1
        b_product._other.price.b.price = 1000
        b_product._other.price.b.currency_id = 810  # RUR
        b_product._other.price.b.iso_currency = 'RUB'
        b_order = ob.OrderBuilder(product=b_product, client=ob.ClientBuilder())
        b_basket = ob.BasketBuilder(rows=[ob.BasketItemBuilder(quantity=50, order=b_order)] * 2)
        b_request = ob.RequestBuilder(basket=b_basket)
        b_invoice = ob.InvoiceBuilder(request=b_request)
        invoice = b_invoice.build(self.session).obj
        self.assertEqual(
            ut.round00(invoice.rur_media_client_sum_wo_nds(
                b_invoice.request.basket.client.obj,
                [b_product.b.media_discount])),
            ut.round00(100000 * 100 / D(120)))

    def test_close_invoice(self):
        session = self.session
        dt_now = datetime.datetime.now()  # datetime.datetime(2008, 8, 8)
        invoice = ob.InvoiceBuilder().build(session).obj
        InvoiceTurnOn(invoice, manual=True).do()
        invoice.close_invoice(dt_now)
        self.assertEqual(invoice.acts[0].dt, datetime.datetime(dt_now.year, dt_now.month, dt_now.day))
        self.assertEqual(invoice.acts[0].amount, invoice.total_sum)


class TestCreditLimit(BalanceTest):
    connection = property(lambda s: s.session.connection(ob.ContractBuilder._class))

    def test_limit_no_credit(self):
        pc = ob.PayOnCreditCase(self.session)
        prod = pc.get_product_hierarchy()
        cont = pc.get_contract(
            commission=0,
            payment_type=3,
            services={7},
            is_signed=1,
            firm=1,
        )
        prod[2]._other.price.b.tax = 1
        prod[3]._other.price.b.tax = 1
        prod[2]._other.price.b.price = 50 * 30
        prod[3]._other.price.b.price = 51 * 30
        for p in prod:
            p.build(self.session)

        basket1 = ob.BasketBuilder(
            client=cont.client,
            rows=[
                ob.BasketItemBuilder(
                    quantity=1,
                    order=ob.OrderBuilder(product=prod[2], client=cont.client, agency=None)
                )
            ])

        self.assertRaises(exc.CREDIT_LIMIT_EXEEDED, pc.pay_on_credit, basket1, cont)

    def test_YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY_invoice(self):
        now = datetime.datetime.now()
        pc = ob.PayOnCreditCase(self.session)
        client = ob.ClientBuilder(is_agency=True)
        person = ob.PersonBuilder(client=client, type='ur')
        product = pc.get_product_hierarchy()
        contract = pc.get_contract(
            dt=now - datetime.timedelta(days=66),
            # finish_dt=now + datetime.timedelta(days=60),
            client=client,
            person=person,
            commission=1,
            payment_type=3,
            credit_type=1,
            payment_term=30,
            payment_term_max=60,
            personal_account=1,
            personal_account_fictive=1,
            currency=810,
            lift_credit_on_payment=1,
            commission_type=52,
            repayment_on_consume=1,
            credit_limit_single=1666666,
            services=set([11]),
            is_signed=1,
            firm=1,
        )
        clnt1 = ob.ClientBuilder(
            agency=contract.client,
            fullname='Mega unit tester, birobot of billing').build(self.session).obj

        order = ob.OrderBuilder(product=product[1], client=clnt1,
                             service=ob.Getter(mapper.Service, 11),
                             agency=contract.client)
        basket = ob.BasketBuilder(client=contract.client,
                               rows=[
                                   ob.BasketItemBuilder(quantity=6666, order=order),
                               ])

        p_inv = pc.pay_on_credit(basket, contract, self.session.query(mapper.Paysys)
                                 .filter_by(firm_id=DEFAULT_FIRM_ID).getone(cc='ur'))[0]
        self.assertEqual(isinstance(p_inv, mapper.FictivePersonalAccount), True)

        order.obj.calculate_consumption(dt=now - datetime.timedelta(66), shipment_info={order.obj.shipment_type: 666})
        self.session.flush()

        act_accounter = a_a.ActAccounter(contract.client, a_a.ActMonth(for_month=now), force=True,
                                         dps=[],
                                         invoices=[p_inv.id])
        acts = act_accounter.do(skip_cut_agava=False)
        self.assertEqual(len(acts) >= 1, True)
        self.session.flush()
        # self.session.expire_all()
        self.assertEqual(all(isinstance(a.invoice, mapper.YInvoice) for a in acts), True)
        self.assertEqual(all(a.invoice.fictives[0] == p_inv for a in acts), True)
        self.assertEqual(sum(r.act_qty for r in itertools.chain.from_iterable(a.rows for a in acts)), 666)
        self.assertEqual(all(a.invoice.amount == a.amount for a in acts), True)
        # self.assertEqual(all(a.invoice.amount_nds == a.amount_nds for a in acts), True)

    def test_limit_simple(self):
        pc = ob.PayOnCreditCase(self.session)
        prod = pc.get_product_hierarchy()
        cont = pc.get_contract(
            commission=0,
            payment_type=3,
            credit_limit={
                prod[0].activity_type.id: 4700,
                prod[1].activity_type.id: 1600,
            },
            services=set([7]),
            is_signed=1,
            firm=1
        )
        prod[2]._other.price.b.tax = 1
        prod[3]._other.price.b.tax = 1
        prod[2]._other.price.b.price = 50 * 30
        prod[3]._other.price.b.price = 51 * 30
        for p in prod:
            p.build(self.session)

        basket1 = ob.BasketBuilder(client=cont.client,
                                rows=[ob.BasketItemBuilder(quantity=1,
                                                        order=ob.OrderBuilder(product=prod[2], client=cont.client,
                                                                           agency=None))])
        basket2 = ob.BasketBuilder(client=cont.client,
                                rows=[ob.BasketItemBuilder(quantity=1,
                                                        order=ob.OrderBuilder(product=prod[2], client=cont.client,
                                                                           agency=None)),
                                      ob.BasketItemBuilder(quantity=1,
                                                        order=ob.OrderBuilder(product=prod[3], client=cont.client,
                                                                           agency=None))])

        pc.pay_on_credit(basket1, cont)

        limits = pc.get_credits_available(basket2, cont)
        self.assertEqual(limits[prod[0].obj.activity_type], [3200, 3030, 4700])
        self.assertEqual(limits[prod[1].obj.activity_type], [1600, 1530, 1600])

        inv = pc.pay_on_credit(basket2, cont)[0]

        assert inv.firm.id == 1

        person = inv.person
        print person.person_category.country
        # print [p.firm.firm_exports.get('OEBS') for p in person.paysyses if p.firm and ]

        limits = pc.get_credits_available(basket2, cont)
        self.assertEqual(limits[prod[0].obj.activity_type], [170, 3030, 4700])
        self.assertEqual(limits[prod[1].obj.activity_type], [70, 1530, 1600])

        self.assertRaises(exc.CREDIT_LIMIT_EXEEDED, pc.pay_on_credit, basket2, cont)

    def test_limit_turns(self):
        pc = ob.PayOnCreditCase(self.session)
        prod = pc.get_product_hierarchy()
        cont = pc.get_contract(
            commission=0,
            payment_type=3,
            credit_type=1,
            payment_term=30,
            turnover_forecast={
                prod[0].activity_type.id: 2350,
                prod[1].activity_type.id: 800,
            },
            services=set([7]),
            is_signed=1,
        )
        prod[2]._other.price.b.tax = 1
        prod[3]._other.price.b.tax = 1
        prod[2]._other.price.b.price = 50 * 30
        prod[3]._other.price.b.price = 51 * 30
        for p in prod:
            p.build(self.session)

        basket1 = ob.BasketBuilder(client=cont.client,
                                rows=[ob.BasketItemBuilder(quantity=1,
                                                        order=ob.OrderBuilder(product=prod[2], client=cont.client,
                                                                           agency=None))])
        basket2 = ob.BasketBuilder(client=cont.client,
                                rows=[ob.BasketItemBuilder(quantity=1,
                                                        order=ob.OrderBuilder(product=prod[2], client=cont.client,
                                                                           agency=None)),
                                      ob.BasketItemBuilder(quantity=1,
                                                        order=ob.OrderBuilder(product=prod[3], client=cont.client,
                                                                           agency=None))])

        pc.pay_on_credit(basket1, cont)

        limits = pc.get_credits_available(basket2, cont)
        self.assertEqual(limits[prod[0].obj.activity_type], [3200, 3030, 4700])
        self.assertEqual(limits[prod[1].obj.activity_type], [1600, 1530, 1600])

        pc.pay_on_credit(basket2, cont)

        limits = pc.get_credits_available(basket2, cont)
        self.assertEqual(limits[prod[0].obj.activity_type], [170, 3030, 4700])
        self.assertEqual(limits[prod[1].obj.activity_type], [70, 1530, 1600])

        self.assertRaises(exc.CREDIT_LIMIT_EXEEDED, pc.pay_on_credit, basket2, cont)

    def test_credit_exceeded(self):
        pc = ob.PayOnCreditCase(self.session)
        prod = pc.get_product_hierarchy()
        cont = pc.get_contract(
            commission=0,
            payment_type=3,
            credit_type=1,
            payment_term=30,
            turnover_forecast={
                prod[0].activity_type.id: 2350,
                prod[1].activity_type.id: 800,
            },
            services=set([7]),
            is_signed=1,
            firm=1,
        )
        prod[2]._other.price.b.tax = 1
        prod[3]._other.price.b.tax = 1
        prod[2]._other.price.b.price = 50 * 30
        prod[3]._other.price.b.price = 51 * 30
        for p in prod:
            p.build(self.session)

        basket1 = ob.BasketBuilder(client=cont.client,
                                rows=[ob.BasketItemBuilder(quantity=1,
                                                        order=ob.OrderBuilder(product=prod[2], client=cont.client,
                                                                           agency=None))])

        invoices = pc.pay_on_credit(basket1, cont)

        from balance import core
        coreobj = core.Core(self.session)

        request = coreobj.create_repayment_request(datetime.datetime.now(), invoices)
        self.session.add(request)
        self.session.flush()
        invoice = InvoiceFactory.create(request, invoices[0].paysys, invoices[0].person, contract=invoices[0].contract,
                                        status_id=1, credit=1, temporary=False,
                                        dt=datetime.datetime.now())
        invoice.update()

        basket1.rows[0].order.obj.calculate_consumption(
            dt=ut.trunc_date(datetime.datetime.now()), stop=1,
            shipment_info={'Bucks': 1}
        )

        acts = invoice.generate_act(force=1, backdate=mapper.ActMonth(for_month=datetime.datetime.now()).document_dt)
        self.session.flush()
        assert len(acts) == 1
        assert acts[0].amount == 1500

    def test_cutagava(self):
        pc = ob.PayOnCreditCase(self.session)
        client = ob.ClientBuilder(is_agency=True, full_repayment=1)
        person = ob.PersonBuilder(client=client, type='pu')
        prod = pc.get_product_hierarchy()
        cont = pc.get_contract(
            dt=datetime.datetime.now() - datetime.timedelta(50),
            client=client,
            person=person,
            commission=0,
            payment_type=3,
            credit_type=1,
            payment_term=30,
            payment_term_max=45,
            turnover_forecast={
                prod[0].activity_type.id: 63500000000,
                prod[1].activity_type.id: 8000000000,
            },
            services=set([7]),
            is_signed=1,
            firm=1,
            repayment_on_consume=1,
        )
        clnt1 = ob.ClientBuilder(
            agency=cont.client,
            fullname='Bred Ivanovich').build(self.session).obj
        basket = ob.BasketBuilder(
            client=cont.client,
            rows=[
                ob.BasketItemBuilder(
                    quantity=3000000,
                    order=ob.OrderBuilder(
                        product=prod[1],
                        client=clnt1,
                        service=ob.Getter(mapper.Service, 7),
                        agency=cont.client
                    ),
                ),
            ]
        )

        self.session.flush()

        crd = pc.pay_on_credit(basket, cont,
                               self.session.query(mapper.Paysys).filter_by(firm_id=DEFAULT_FIRM_ID).getone(cc='ur'))
        crd1 = pc.pay_on_credit(basket, cont,
                                self.session.query(mapper.Paysys).filter_by(firm_id=DEFAULT_FIRM_ID).getone(cc='ur'))
        d = ut.trunc_date(datetime.datetime.now()).replace(day=1) - datetime.timedelta(20)
        for i in crd + crd1:
            i.deferpay.issue_dt = i.dt = d
            i.agency_discount_pct = 66
        self.session.flush()

        assert len(crd1) == 1
        import balance.actions.withdraw as ret_cred
        wt = ret_cred.Withdraw(crd1[0])
        wt.do()

        order = crd[0].invoice_orders[0].order

        # assign static discount
        order.consumes[0].static_discount_pct = 2
        order.consumes[0].discount_pct = 2
        crd[0].invoice_orders[0].discount_pct = 2

        order.calculate_consumption(dt=d + datetime.timedelta(5), stop=0,
                                    shipment_info={order.shipment_type: 20000})

        cont.col0.finish_dt = datetime.datetime.now().replace(day=1) - datetime.timedelta(10)

        order.product.prices[0].dt = d
        p = order.product.prices[0]
        order.product.prices.append(
            mapper.Price(id=ob.get_big_number(), dt=d + datetime.timedelta(2), price=p.price / 2, tax=p.tax,
                  currency_id=p.currency_id, iso_currency=p.iso_currency))

        acc = a_a.ActAccounter(cont.client, a_a.ActMonth(), force=True)
        acc.cut_agava(dps=[i.deferpay for i in crd + crd1])

        self.assertEqual(order.consumes[-1].price, 100)
        self.assertEqual(order.consumes[-1].discount_pct, 2)

    def test_repayment(self):
        pc = ob.PayOnCreditCase(self.session)
        client = ob.ClientBuilder(is_agency=True)
        person = ob.PersonBuilder(client=client, type='pu')
        prod = pc.get_product_hierarchy()
        cont = pc.get_contract(
            dt=datetime.datetime.now() - datetime.timedelta(50),
            client=client,
            person=person,
            commission=0,
            payment_type=3,
            credit_type=1,
            payment_term=30,
            payment_term_max=45,
            turnover_forecast={
                prod[0].activity_type.id: 63500,
                prod[1].activity_type.id: 8000,
            },
            services=set([77]),
            is_signed=1,
            firm=1,
        )
        clnt1 = ob.ClientBuilder(
            agency=cont.client,
            fullname='Bred Ivanovich').build(self.session).obj
        basket = ob.BasketBuilder(client=cont.client,
                               rows=[
                                   ob.BasketItemBuilder(quantity=3, order=ob.OrderBuilder(product=prod[1], client=clnt1,
                                                                                    service=ob.Getter(mapper.Service, 77),
                                                                                    agency=cont.client)),
                               ])

        self.session.flush()

        crd = pc.pay_on_credit(basket, cont,
                               self.session.query(mapper.Paysys).filter_by(firm_id=DEFAULT_FIRM_ID).getone(cc='ur'))
        for i in crd:
            i.deferpay.issue_dt = i.dt = ut.trunc_date(datetime.datetime.now()).replace(day=1) - datetime.timedelta(20)
        self.session.flush()

        import balance.actions.consumption as action_consumption
        action_consumption.reverse_consume(crd[0].consumes[0], None, 1)
        crd[0].create_receipt(-100)

        self.session.flush()

        acc = a_a.ActAccounter(cont.client, a_a.ActMonth())
        acc.generate_invoices(acc.get_deferpays())

        # actsobj.generate_invoices([cont.client.id])
        inv = self.session.query(mapper.Invoice).filter_by(client=cont.client, credit=1).all()

        InvoiceTurnOn(inv[0], sum=crd[0].receipt_sum, manual=True).do()

        self.assertEqual(crd[0].deferpay.status_id, 1)

    def test_nonresident_acts(self):
        pc = ob.PayOnCreditCase(self.session)

        nr_rur_paysys = self.session.query(mapper.Paysys).filter_by(firm_id=DEFAULT_FIRM_ID).getone(cc='nr_rur')
        client = ob.ClientBuilder(is_agency=True)
        person = ob.PersonBuilder(client=client, type='pu')
        prod = pc.get_product_hierarchy()
        cont = pc.get_contract(
            dt=datetime.datetime.now() - datetime.timedelta(50),
            client=client,
            person=person,
            commission=0,
            payment_type=3,
            credit_type=1,
            payment_term=30,
            payment_term_max=45,
            turnover_forecast={
                prod[0].activity_type.id: 63500,
                prod[1].activity_type.id: 8000,
            },
            services={77},
            is_signed=1,
            firm=1,
            non_resident_clients=1,
        )
        prod[2]._other.price.b.tax = 0
        prod[3]._other.price.b.tax = 0
        prod[2]._other.price.b.price = 50 * 30
        prod[3]._other.price.b.price = 51 * 30
        for p in prod:
            p.build(self.session)

        clnt1 = ob.ClientBuilder(
            agency=cont.client,
            currency_payment='USD',
            is_non_resident=1,
            is_docs_separated=1,
            fullname='Bred Ivanovich').build(self.session).obj
        clnt2 = ob.ClientBuilder(
            agency=cont.client,
            currency_payment='USD',
            is_non_resident=1,
            is_docs_separated=1,
            payment_term=self.session.query(mapper.PaymentTerm).getone(15),
            fullname='Bred Ivanovich').build(self.session).obj

        basket = ob.BasketBuilder(
            client=cont.client,
            rows=[
                ob.BasketItemBuilder(
                    quantity=3,
                    order=ob.OrderBuilder(
                        product=prod[2],
                        client=clnt1,
                        service=ob.Getter(mapper.Service, 77),
                        agency=cont.client)
                ),
                ob.BasketItemBuilder(
                    quantity=5,
                    order=ob.OrderBuilder(
                        product=prod[2],
                        client=clnt2,
                        service=ob.Getter(mapper.Service, 77),
                        agency=cont.client)
                ),
            ])

        from balance import core

        for i in pc.pay_on_credit(basket, cont, nr_rur_paysys):
            i.deferpay.issue_dt = i.dt = ut.trunc_date(datetime.datetime.now()).replace(day=1) - datetime.timedelta(20)

        self.session.flush()

        acc = a_a.ActAccounter(cont.client, a_a.ActMonth())
        acc.generate_invoices(acc.get_deferpays())

        # actsobj.generate_invoices([cont.client.id])
        inv = self.session.query(mapper.Invoice).filter_by(client=cont.client, credit=1).all()

        inv = [i for i in inv if clnt1 == i.invoice_orders[0].order.client][0]

        # for i in inv:
        #    if i.subclient_payment_term():
        #       assert i.payment_term.term == 15
        #   else:
        #       assert i.payment_term.term == 30

        order = inv.invoice_orders[0].order

        order.calculate_consumption(dt=datetime.datetime.now() - datetime.timedelta(30), stop=1,
                                    shipment_info={order.shipment_type: 1})
        self.session.clear_cache()
        acts = inv.generate_act(force=1, backdate=datetime.datetime.now() - datetime.timedelta(30))
        self.session.flush()
        total = acts[0].amount

        order.calculate_consumption(dt=datetime.datetime.now() - datetime.timedelta(15), stop=1,
                                    shipment_info={order.shipment_type: 2})
        self.session.clear_cache()
        acts = inv.generate_act(force=1, backdate=datetime.datetime.now() - datetime.timedelta(15))
        self.session.flush()
        total += acts[0].amount
        order.calculate_consumption(dt=datetime.datetime.now() - datetime.timedelta(10), stop=1,
                                    shipment_info={order.shipment_type: 3})
        self.session.clear_cache()
        acts = inv.generate_act(force=1, backdate=datetime.datetime.now() - datetime.timedelta(10))
        self.session.flush()
        total += acts[0].amount
        self.assertEqual(total, inv.total_sum)

        invs = []
        basket = ob.BasketBuilder(
            client=cont.client,
            rows=[
                ob.BasketItemBuilder(
                    quantity=1,
                    order=ob.OrderBuilder(
                        product=prod[2],
                        client=clnt1,
                        service=ob.Getter(mapper.Service, 77),
                        agency=cont.client
                    )
                ),
            ])
        inv = pc.pay_on_credit(basket, cont, nr_rur_paysys)[0]
        inv.deferpay.issue_dt = inv.dt = datetime.datetime.now() - datetime.timedelta(40)
        invs.append(inv)
        basket = ob.BasketBuilder(
            client=cont.client,
            rows=[
                ob.BasketItemBuilder(
                    quantity=2,
                    order=ob.OrderBuilder(
                        product=prod[2],
                        client=clnt1,
                        service=ob.Getter(mapper.Service, 77),
                        agency=cont.client
                    )
                ),
            ])
        inv = pc.pay_on_credit(basket, cont, nr_rur_paysys)[0]
        inv.deferpay.issue_dt = inv.dt = datetime.datetime.now() - datetime.timedelta(40)
        invs.append(inv)
        basket = ob.BasketBuilder(
            client=cont.client,
            rows=[
                ob.BasketItemBuilder(
                    quantity=3,
                    order=ob.OrderBuilder(
                        product=prod[2],
                        client=clnt1,
                        service=ob.Getter(mapper.Service, 77),
                        agency=cont.client
                    )
                ),
            ])
        inv = pc.pay_on_credit(basket, cont, nr_rur_paysys)[0]
        inv.deferpay.issue_dt = inv.dt = datetime.datetime.now() - datetime.timedelta(40)
        invs.append(inv)

        self.session.flush()

        invoice = core.Core(self.session).issue_repayment_invoice(self.session.oper_id, [i.deferpay.id for i in invs],
                                                                  None)
        invoice.status_id = 1

        InvoiceTurnOn(invoice, sum=invoice.effective_sum / 2, manual=True).do()
        self.assertEqual([i.deferpay.status_id for i in invs], [1, 1, 0])

        InvoiceTurnOn(invoice, sum=-invoice.effective_sum / 3, manual=True).do()
        self.assertEqual([i.deferpay.status_id for i in invs], [1, 0, 0])

        InvoiceTurnOn(invoice, sum=invoice.effective_sum / 6 * 5, manual=True).do()
        self.assertEqual([i.deferpay.status_id for i in invs], [1, 1, 1])

        self.assertEqual(invoice.currency, 'USD')


class TestDealPassport(BalanceTest):
    def setUp(self):
        super(TestDealPassport, self).setUp()
        self.pc = pc = ob.PayOnCreditCase(self.session)
        client = ob.ClientBuilder(is_agency=False)
        person = ob.PersonBuilder(client=client, type='yt')
        prod = pc.get_product_hierarchy()
        self.cont = cont = pc.get_contract(
            dt=datetime.datetime.now() - datetime.timedelta(50),
            client=client,
            person=person,
            commission=0,
            payment_type=3,
            credit_type=1,
            payment_term=30,
            payment_term_max=45,
            turnover_forecast={
                prod[0].activity_type.id: 63500,
            },
            services={7},
            is_signed=1,
            firm=1,
            credit_limit_single=1000000,
        )
        order1 = ob.OrderBuilder(
            product=prod[0],
            client=client,
            service=ob.Getter(mapper.Service, 7)
        ).build(self.session).obj
        order2 = ob.OrderBuilder(
            product=prod[0],
            client=client,
            service=ob.Getter(mapper.Service, 7)
        ).build(self.session).obj
        self.basket = ob.BasketBuilder(
            client=cont.client,
            rows=[
                ob.BasketItemBuilder(quantity=3, order=order1),
                ob.BasketItemBuilder(quantity=5, order=order2),
            ]
        )
        self.paysys = self.session.query(mapper.Paysys).filter_by(firm_id=DEFAULT_FIRM_ID).getone(cc='rur_wo_nds')

    def test_passport_deal(self):
        self.assertRaises(
            exc.INCOMPATIBLE_INVOICE_PARAMS,
            self.pc.pay_on_credit,
            *(self.basket, self.cont, self.paysys, True)
        )

    def test_passport_deal2(self):
        self.cont.col0.deal_passport = datetime.datetime.now()
        self.pc.pay_on_credit(self.basket, self.cont, self.paysys, True)


class TestMarketPersonalAutopayment(BalanceTest):
    def test_market(self):
        pc = ob.PayOnCreditCase(self.session)
        client = ob.ClientBuilder(is_agency=False)
        person = ob.PersonBuilder(client=client, type='ph')
        prod = pc.get_product_hierarchy()
        cont = pc.get_contract(
            dt=datetime.datetime.now() - datetime.timedelta(50),
            finish_dt=datetime.datetime.now() + datetime.timedelta(days=666),
            client=client,
            person=person,
            commission=0,
            payment_type=3,
            credit_type=1,
            payment_term=30,
            payment_term_max=45,
            turnover_forecast={
                prod[0].activity_type.id: 63500,
            },
            services={11},
            is_signed=1,
            firm=1,
            credit_limit_single=1000000,
        )
        order1 = ob.OrderBuilder(
            product=prod[0],
            client=client,
            service=ob.Getter(mapper.Service, 11)
        ).build(self.session).obj
        order2 = ob.OrderBuilder(
            product=prod[0],
            client=client,
            service=ob.Getter(mapper.Service, 11)
        ).build(self.session).obj
        basket = ob.BasketBuilder(
            client=cont.client,
            rows=[
                ob.BasketItemBuilder(quantity=3, order=order1),
                ob.BasketItemBuilder(quantity=5, order=order2),
            ]
        )

        inv = pc.pay_on_credit(basket, cont,
                               self.session.query(mapper.Paysys).filter_by(firm_id=DEFAULT_FIRM_ID).getone(cc='ph'))[0]

        order1.calculate_consumption(dt=datetime.datetime.now() - datetime.timedelta(50), stop=0,
                                     shipment_info={order1.shipment_type: 1})
        order2.calculate_consumption(dt=datetime.datetime.now() - datetime.timedelta(50), stop=0,
                                     shipment_info={order2.shipment_type: 2})

        # inv.dt = ut.add_months_to_date(datetime.datetime.now(), -1)
        # inv.deferpay.issue_dt = inv.dt
        # self.session.flush()

        cont.col0.personal_account = 1
        cont.col0.auto_credit = 1
        self.session.flush()
        act_accounter = a_a.ActAccounter(cont.client,
                                         a_a.ActMonth(for_month=datetime.datetime.now()), force=True)
        acts = act_accounter.do()
        print acts

        invoices = self.session.query(mapper.Invoice).filter_by(person=cont.person).all()

        assert 1 in [i.postpay for i in invoices], str([i.postpay for i in invoices])

        pac = [i for i in invoices if i.postpay == 1][0]
        rep = [i for i in invoices if i.credit == 1][0]

        rep.receipt_sum_1c = rep.total_sum
        rep.update_on_payment_or_patch()

        self.assertEqual(pac.consume_sum, 800)

        order1.calculate_consumption(dt=datetime.datetime.now() - datetime.timedelta(2), stop=0,
                                     shipment_info={order1.shipment_type: 2})
        order2.calculate_consumption(dt=datetime.datetime.now() - datetime.timedelta(2), stop=0,
                                     shipment_info={order2.shipment_type: 3})
        pac.generate_act(force=1)

        pac.receipt_sum_1c = 400
        pac.update_on_payment_or_patch()

        order1.calculate_consumption(dt=datetime.datetime.now() - datetime.timedelta(1), stop=0,
                                     shipment_info={order1.shipment_type: 3})
        order2.calculate_consumption(dt=datetime.datetime.now() - datetime.timedelta(1), stop=0,
                                     shipment_info={order2.shipment_type: 4})
        pac.generate_act(force=1)

        self.assertEqual(pac.consume_sum, 1200)
        self.assertEqual(pac.receipt_sum, 400)


class TestOverdraftInvoice(BalanceTest):
    def test_assign_contract(self):
        """
        Проверяет, что при попытке привязать договор к овердрафтному счёту,
        вызывается исключение.
        BALANCE-20623
        """
        client = ob.ClientBuilder()
        person = ob.PersonBuilder()
        contract = ob.ContractBuilder(
            client=client,
            person=person,
            is_signed=datetime.datetime.now(),
            services={7}
        ).build(self.session).obj
        invoice = ob.InvoiceBuilder(
            overdraft=1,
            client=client,
            person=person,
        ).build(self.session).obj
        assert type(invoice) == mapper.OverdraftInvoice
        with self.assertRaises(exc.CANNOT_ASSIGN_CONTRACT_TO_OVERDRAFT):
            invoice.assign_contract(contract)


class TestAct(BalanceTest):
    """ Тестирование актов"""

    def get_paid_act(self):
        invoice = ob.InvoiceBuilder().build(self.session).obj
        InvoiceTurnOn(invoice, manual=True).do()
        invoice.close_invoice(self.session.now())

        invoice.receipt_sum_1c = invoice.total_sum
        invoice.update_on_payment_or_patch()

        for a in invoice.acts:
            for r in a.rows:
                self.assertEqual(r.paid_amount, r.amount)

        return invoice.acts[0]

    def test_hide_paid_acts(self):
        """
        Проверяет, что при закрытии акта попадают отриц. корр-ки
        """
        act = self.get_paid_act()
        act.hide()
        # предлагаем сессии протухнуть и
        # перечитать данные из БД
        self.session.expire(act)
        # убеждаемся, что оплат после закрытия акта нет
        for r in act.rows:
            self.assertEqual(0, sum([p.amount for p in r.paids]),
                             'После удаления акта сумма оплат != 0')

    def test_instant_payment(self):
        """
        Проверяет, что в акты попадают оплаты от мгновенных платежей не дожидаясь выписки
        """
        invoice = ob.InvoiceBuilder(paysys=ob.Getter(mapper.Paysys, 1000)).build(self.session).obj
        payment = ob.YandexMoneyPaymentBuilder(invoice=invoice).build(self.session).obj

        payment.turn_on()
        self.session.flush()
        invoice.update_on_payment_or_patch()

        invoice.close_invoice(self.session.now())

        for a in invoice.acts:
            for r in a.rows:
                self.assertEqual(r.paid_amount, r.amount)

    def test_unhide_paid_acts(self):
        """
        Проверяет, что при "воскрешении" акта отриц. корр-ки удаляются
        """
        cdt = self.session.now()
        act = self.get_paid_act()
        act_trans_id = act.rows[0].id

        # создаём оплату по акту
        self.session.execute(
            '''declare q number; begin bo.pk_acts2.sp_create_paid_acts(q, :dt, :id, :sum); end;''',
            {
                'dt': self.session.now(), 'id': act_trans_id, 'sum': D(act.rows[0].amount)
            }
        )

        # закрываем акт и проверяем, что появилась
        # отрицательная корректировка
        act.hide()
        self.session.flush()
        get_negative_act_trans = self.session.query(mapper.PaidActs.act_trans_id) \
            .filter(mapper.PaidActs.act_trans_id == act_trans_id) \
            .filter(mapper.PaidActs.amount < 0) \
            .filter(mapper.PaidActs.dt >= datetime.datetime(cdt.year, cdt.month, 1))
        negative_act_trans_id = get_negative_act_trans.scalar()
        self.assertEqual(act_trans_id, negative_act_trans_id,
                         'Отрицательная корректировка после удаления акта не найдена')

        # воскрешаем акт и убеждаемся,
        # что отрицательной корректировки больше нет
        act.unhide()
        negative_act_trans_cnt = get_negative_act_trans.count()
        self.assertEqual(0, negative_act_trans_cnt,
                         'Остались отрицательные корректировки после "воскрешения" акта')


class TestBonusAccount(BalanceTest):
    @property
    def _test_passport(self):
        passport = ob.PassportBuilder().build(self.session).obj
        passport.simple_client = ob.ClientBuilder().build(self.session).obj
        self.session.flush()
        return passport

    def test_not_exist(self):
        passport = self._test_passport
        self.session.flush()
        self.assertRaises(exc.INVOICE_NOT_FOUND,
                          mapper.BonusAccount.get_bonus_account,
                          *(self.session, passport.simple_client.id)
                          )

    def test_get_or_create(self):
        paysys = self.session.query(mapper.Paysys).getone(2900)
        passport = self._test_passport
        ba = mapper.BonusAccount.get_or_create_bonus_account(self.session,
                                                      client=passport.simple_client,
                                                      paysys=paysys)
        self.assertEqual(0, ba.real_balance)

    def test_bonus_account_currency(self):
        paysys = self.session.query(mapper.Paysys).getone(2900)
        passport = self._test_passport
        ba = mapper.BonusAccount.get_or_create_bonus_account(self.session,
                                                      client=passport.simple_client,
                                                      paysys=paysys)
        self.assertEqual('YSTBN', ba.currency)


@pytest.fixture
def credit_contract(session):
    return ob.create_credit_contract(session)


class TestYInvoiceReceipts(object):
    @pytest.fixture
    def invoice(self, session, credit_contract):
        pc = ob.PayOnCreditCase(session)
        product = ob.Getter(mapper.Product, DIRECT_PRODUCT_RUB_ID).build(session).obj

        subclient = ob.ClientBuilder(agency=credit_contract.client).build(session).obj
        order = ob.OrderBuilder(
            product=product, service=ob.Getter(mapper.Service, 7),
            client=subclient, agency=credit_contract.client
        ).build(session).obj
        basket = ob.BasketBuilder(
            client=credit_contract.client,
            rows=[ob.BasketItemBuilder(quantity=6666, order=order)]
        )

        paysys = session.query(mapper.Paysys).filter_by(firm_id=DEFAULT_FIRM_ID).getone(cc='ur')
        pa = pc.pay_on_credit(basket, credit_contract, paysys)[0]
        order.calculate_consumption(datetime.datetime.now() - datetime.timedelta(days=32), {order.shipment_type: 100})
        act_accounter = a_a.ActAccounter(
            credit_contract.client, a_a.ActMonth(for_month=datetime.datetime.now()),
            force=True, dps=[], invoices=[pa.id]
        )
        act, = act_accounter.do(skip_cut_agava=True)
        return act.invoice

    @pytest.mark.parametrize(
        'cur_y_sum, cur_pa_sum, delta_sum, req_y_sum, req_pa_sum',
        [
            (0, 0, 70, 70, 70),
            (70, 70, 60, 130, 100),
            (130, 100, 35, 165, 100),
            (130, 130, 35, 165, 130),
            (150, 200, -40, 110, 200),
            (150, 200, -60, 90, 190),
            (90, 200, -30, 60, 170),
        ]
    )
    def test_receipt(self, invoice, cur_y_sum, cur_pa_sum, delta_sum, req_y_sum, req_pa_sum):
        pa, = invoice.fictives

        # инициализация начального состояния
        invoice.create_receipt(cur_y_sum)
        pa.create_receipt(cur_pa_sum - pa.receipt_sum)
        assert invoice.receipt_sum == cur_y_sum
        assert pa.receipt_sum == cur_pa_sum

        # создание нового ресипта
        invoice.create_receipt(delta_sum)
        assert invoice.receipt_sum == req_y_sum
        assert pa.receipt_sum == req_pa_sum


@pytest.mark.taxes_update
class TestTurnOnTaxUpdate(object):
    @pytest.fixture
    def product(self, session):
        past = datetime.datetime(2000, 1, 1)
        present = datetime.datetime.now() - datetime.timedelta(1)

        return ob.ProductBuilder(
            taxes=ob.TaxPolicyBuilder(
                tax_pcts=[(past, 18), (present, 20)]
            ),
            prices=[
                (past, 'RUR', 100),
            ]
        ).build(session).obj

    @pytest.fixture
    def invoice(self, session, product):
        client = ob.ClientBuilder().build(session).obj
        orders = [ob.OrderBuilder(product=product, client=client) for _ in range(3)]
        invoice = ob.InvoiceBuilder(
            request=ob.RequestBuilder(
                basket=ob.BasketBuilder(
                    dt=datetime.datetime.now() - datetime.timedelta(10),
                    client=client,
                    rows=[
                        ob.BasketItemBuilder(quantity=(i + 1) * 10, order=o)
                        for i, o in enumerate(orders)
                        ]
                )
            ),
        ).build(session).obj
        return invoice

    def test_base(self, invoice, product):
        invoice.turn_on_rows()
        price, = product.prices
        tax, = product.taxes
        tpp1, tpp2 = tax.policy.taxes

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                effective_sum=7080,
                consume_sum=7200
            )
        )

        hamcrest.assert_that(
            invoice.invoice_orders,
            hamcrest.contains(*[
                hamcrest.has_properties(
                    quantity=qty,
                    effective_sum=qty * 118,
                    price_id=price.id,
                    tax_policy_pct_id=tpp1.id
                )
                for qty in [10, 20, 30]
                ])
        )
        hamcrest.assert_that(
            invoice.consumes,
            hamcrest.contains(*[
                hamcrest.has_properties(
                    current_qty=qty,
                    current_sum=qty * 120,
                    price_id=price.id,
                    tax_policy_pct_id=tpp2.id
                )
                for qty in [10, 20, 30]
                ])
        )


@pytest.fixture
def patch_discount_proofs(request):
    from balance import discounts

    if isinstance(request.param, collections.Iterable):
        def new_func(ns):
            return [
                discounts.DiscountProof('mock', discount=pct, adjust_quantity=False) if pct else None
                for pct in request.param
            ]
    else:
        new_func = request.param

    old_func = discounts.calc_from_ns
    discounts.calc_from_ns = new_func
    yield request.param
    discounts.calc_from_ns = old_func


class BaseTestInvoiceOrderSums(object):
    def _create_request(self, client, product, qty,
                        desired_discount_pct=0, forced_discount_pct=0, adjust_qty=False,
                        order_begin_dt=None):
        order = ob.OrderBuilder(client=client, product=product, begin_dt=order_begin_dt)
        basket = ob.BasketBuilder(
            rows=[ob.BasketItemBuilder(
                quantity=qty, order=order,
                desired_discount_pct=desired_discount_pct,
                forced_discount_pct=forced_discount_pct
            )],
            adjust_qty=adjust_qty
        )
        return ob.RequestBuilder(basket=basket).build(client.session).obj

    def _create_invoice(self, session, paysys, product, qty,
                        desired_discount_pct=0, forced_discount_pct=0, adjust_qty=False,
                        order_begin_dt=None, crossfirm=None):
        client = ob.ClientBuilder(manual_discount=1)
        person = ob.PersonBuilder(client=client, type=paysys.category).build(session).obj
        request = self._create_request(
            person.client,
            product, qty,
            desired_discount_pct, forced_discount_pct,
            adjust_qty, order_begin_dt
        )
        invoice = InvoiceFactory.create(
            request,
            paysys,
            person,
            crossfirm=crossfirm,
            temporary=False
        )
        return invoice

    def _create_product(self, session, unit, currency, price):
        return ob.ProductBuilder(unit=unit, currency=currency, price=price, manual_discount=1).build(session).obj

    def _get_paysys(self, session, *request_params):
        params = sa.tuple_(
            mapper.Paysys.firm_id,
            mapper.Paysys.currency,
            mapper.Paysys.category,
            mapper.PaymentMethod.cc
        )
        return session.query(mapper.Paysys).join(mapper.PaymentMethod) \
            .filter(params.in_([sa.tuple_(*request_params)])) \
            .filter(mapper.Paysys.extern == 1) \
            .first()

    @pytest.fixture
    def paysys(self, request, session):
        return self._get_paysys(session, *request.param)

    @pytest.fixture
    def unit(self, request, session):
        return ob.Getter(mapper.ProductUnit, request.param).build(session).obj


class TestInvoiceOrderSums(BaseTestInvoiceOrderSums):
    @pytest.mark.parametrize(
        'paysys, unit, price, discount_pct, req_price',
        [
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, 66, 0, DU(66, 'FISH', [1, 'QTY'])],
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, 66, 66, DU(66, 'FISH', [1, 'QTY'])],
            [(1, 'RUR', 'ur', 'bank'), SHOWS_1000_UNIT_ID, 66, 0, DU(66, 'FISH', [1000, 'QTY'])],
            [(1, 'USD', 'yt', 'bank'), SHOWS_1000_UNIT_ID, 66, 0, DU(66, 'FISH', [1000, 'QTY'])],
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, D('0.6666'), 0, DU('0.6666', 'FISH', [1, 'QTY'])],
        ],
        ['paysys', 'unit'],
        ids=lambda o: str(o)
    )
    def test_internal_price(self, session, paysys, unit, price, discount_pct, req_price):
        product = self._create_product(session, unit, paysys.currency, price)
        invoice = self._create_invoice(session, paysys, product, 1, discount_pct)
        invoice_order, = invoice.invoice_orders

        assert req_price == invoice_order.internal_price
        assert unit.type_rate == invoice_order.type_rate

    @pytest.mark.parametrize(
        'paysys, unit, price, discount_pct, req_price',
        [
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, 66, 0, DU(66, 'RUR', [1, 'QTY'])],
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, 66, 66, DU(66, 'RUR', [1, 'QTY'])],
            [(1, 'RUR', 'ur', 'bank'), SHOWS_1000_UNIT_ID, 66, 0, DU(66, 'RUR', [1000, 'QTY'])],
            [(1, 'USD', 'yt', 'bank'), SHOWS_1000_UNIT_ID, 66, 0, DU(66, 'USD', [1000, 'QTY'])],
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, D('0.6666'), 0, DU('0.67', 'RUR', [D(1), 'QTY'])],
        ],
        ['paysys', 'unit'],
        ids=lambda o: str(o)
    )
    def test_price(self, session, paysys, unit, price, discount_pct, req_price):
        product = self._create_product(session, unit, paysys.currency, price)
        invoice = self._create_invoice(session, paysys, product, 1, discount_pct)
        invoice_order, = invoice.invoice_orders

        assert req_price == invoice_order.price

    @pytest.mark.parametrize(
        'paysys, unit, price, discount_pct, req_price',
        [
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, 66, 0, DU('55', 'RUR', [1, 'QTY'])],
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, 66, 66, DU('55', 'RUR', [1, 'QTY'])],
            [(1, 'RUR', 'ur', 'bank'), SHOWS_1000_UNIT_ID, 66, 0, DU('55', 'RUR', [1000, 'QTY'])],
            [(1, 'USD', 'yt', 'bank'), SHOWS_1000_UNIT_ID, 66, 0, DU('66', 'USD', [1000, 'QTY'])],
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, D('0.6666'), 0, DU('0.56', 'RUR', [D(1), 'QTY'])],
        ],
        ['paysys', 'unit'],
        ids=lambda o: str(o)
    )
    def test_price_wo_nds(self, session, paysys, unit, price, discount_pct, req_price):
        product = self._create_product(session, unit, paysys.currency, price)
        invoice = self._create_invoice(session, paysys, product, 1, discount_pct)
        invoice_order, = invoice.invoice_orders

        assert req_price == invoice_order.price_wo_nds

    @pytest.mark.parametrize(
        'paysys, unit, price, discount_pct, qty, req_amount',
        [
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, 66, 0, 10, DU(660, 'RUR')],
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, 66, 10, 10, DU(594, 'RUR')],
            [(1, 'RUR', 'ur', 'bank'), SHOWS_1000_UNIT_ID, D('66.66'), 0, 10, DU('0.67', 'RUR')],
            [(1, 'RUR', 'ur', 'bank'), SHOWS_1000_UNIT_ID, D('66.66'), 0, 1000, DU('66.66', 'RUR')],
            [(1, 'USD', 'yt', 'bank'), AUCTION_UNIT_ID, 66, 0, 10, DU(660, 'USD')],
        ],
        ['paysys', 'unit'],
        ids=lambda o: str(o)
    )
    def test_amount(self, session, paysys, unit, price, discount_pct, qty, req_amount):
        product = self._create_product(session, unit, paysys.currency, price)
        invoice = self._create_invoice(session, paysys, product, qty, discount_pct)
        invoice_order, = invoice.invoice_orders

        assert req_amount == invoice_order.amount
        assert DU(req_amount.as_decimal(), 'FISH') == invoice_order.effective_sum

    @pytest.mark.parametrize(
        'paysys, unit, price, discount_pct, adjust_qty, qty, req_amount',
        [
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, 66, 0, False, 10, DU(660, 'RUR')],
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, 66, 10, False, 10, DU(660, 'RUR')],
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, 66, 10, True, 10, DU(D('733.33'), 'RUR')],
            [(1, 'RUR', 'ur', 'bank'), SHOWS_1000_UNIT_ID, D('66.66'), 0, False, 10, DU('0.67', 'RUR')],
            [(1, 'RUR', 'ur', 'bank'), SHOWS_1000_UNIT_ID, D('66.66'), 0, False, 1000, DU('66.66', 'RUR')],
            [(1, 'USD', 'yt', 'bank'), AUCTION_UNIT_ID, 66, 0, False, 10, DU(660, 'USD')],
        ],
        ['paysys', 'unit'],
        ids=lambda o: str(o)
    )
    def test_amount_no_discount(self, session, paysys, unit, price, discount_pct, adjust_qty, qty, req_amount):
        product = self._create_product(session, unit, paysys.currency, price)
        invoice = self._create_invoice(session, paysys, product, qty, discount_pct, adjust_qty=adjust_qty)
        invoice_order, = invoice.invoice_orders

        assert req_amount == invoice_order.amount_no_discount

    @pytest.mark.parametrize(
        'paysys, unit, price, discount_pct, qty, req_amount',
        [
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, 66, 0, 10, DU('110', 'RUR')],
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, 66, 10, 10, DU('99', 'RUR')],
            [(1, 'RUR', 'ur', 'bank'), SHOWS_1000_UNIT_ID, D('66.66'), 0, 10, DU('0.11', 'RUR')],
            [(1, 'RUR', 'ur', 'bank'), SHOWS_1000_UNIT_ID, D('66.66'), 0, 1000, DU('11.11', 'RUR')],
            [(1, 'USD', 'yt', 'bank'), AUCTION_UNIT_ID, 66, 0, 10, DU(0, 'USD')],
        ],
        ['paysys', 'unit'],
        ids=lambda o: str(o)
    )
    def test_amount_nds(self, session, paysys, unit, price, discount_pct, qty, req_amount):
        product = self._create_product(session, unit, paysys.currency, price)
        invoice = self._create_invoice(session, paysys, product, qty, discount_pct)
        invoice_order, = invoice.invoice_orders

        assert req_amount == invoice_order.amount_nds

    @pytest.mark.parametrize(
        'qty, discount_pct, req_qty',
        [
            [10, 0, DU(10, 'QTY')],
            [10, 10, DU('11.111111', 'QTY')],
        ],
        ids=lambda o: str(o)
    )
    def test_quantity(self, session, discount_pct, qty, req_qty):
        paysys = self._get_paysys(session, 1, 'RUR', 'ur', 'bank')
        unit = ob.Getter(mapper.ProductUnit, AUCTION_UNIT_ID)
        product = self._create_product(session, unit, 'RUR', 666)

        invoice = self._create_invoice(session, paysys, product, qty, discount_pct, adjust_qty=True)
        invoice_order, = invoice.invoice_orders

        assert qty == invoice_order.initial_quantity
        assert req_qty == invoice_order.quantity

    def test_discount_forced(self, session):
        qty = 10
        price = 666
        discount_pct = 66

        paysys = self._get_paysys(session, 1, 'RUR', 'ur', 'bank')
        unit = ob.Getter(mapper.ProductUnit, AUCTION_UNIT_ID)
        product = self._create_product(session, unit, 'RUR', price)

        old_prop = mapper.Invoice.has_forced_discount
        mapper.Invoice.has_forced_discount = property(lambda self: True)
        invoice = self._create_invoice(session, paysys, product, qty, forced_discount_pct=discount_pct)
        mapper.Invoice.has_forced_discount = old_prop

        invoice_order, = invoice.invoice_orders
        assert invoice_order.discount_obj == mapper.DiscountObj(DU(66, '%'))
        assert DU(qty * price * (1 - discount_pct / D(100)), 'RUR') == invoice_order.amount

    def test_price_date(self, session):
        qty = 10

        product = ob.ProductBuilder(price=666).build(session).obj
        ob.PriceBuilder(
            product=product,
            dt=datetime.datetime.now() + datetime.timedelta(10),
            price=66
        ).build(session)

        paysys = self._get_paysys(session, 1, 'RUR', 'ur', 'bank')

        invoice = self._create_invoice(
            session, paysys, product, 10,
            order_begin_dt=datetime.datetime.now() + datetime.timedelta(20)
        )
        invoice_order, = invoice.invoice_orders

        assert ut.trunc_date(datetime.datetime.now()) == ut.trunc_date(invoice.dt)
        assert DU(qty * 66, 'RUR') == invoice_order.amount

    @pytest.mark.taxes_update
    @pytest.mark.parametrize('subtype', ['tax_excluded', 'tax_excluded_different_rows', 'tax_included'])
    def test_price_date_tax_change_future(self, session, subtype):
        past = datetime.datetime(2000, 1, 1)
        close_future = datetime.datetime.now() + datetime.timedelta(1)
        future = datetime.datetime.now() + datetime.timedelta(10)

        tax_policy = ob.TaxPolicyBuilder(
            tax_pcts=[(past, 18), (future, 20)]
        ).build(session).obj
        tpp1, tpp2 = tax_policy.taxes

        if subtype == 'tax_excluded':
            product = ob.ProductBuilder(
                taxes=tax_policy,
                prices=[(past, 'RUR', 100)]
            ).build(session).obj
        elif subtype == 'tax_excluded_different_rows':
            product = ob.ProductBuilder(
                taxes=tax_policy,
                prices=[
                    (past, 'RUR', 100),
                    (close_future, 'RUR', 100),
                ]
            ).build(session).obj
        elif subtype == 'tax_included':
            product = ob.ProductBuilder(
                taxes=tax_policy,
                prices=[
                    (past, 'RUR', 118, tpp1),
                    (future, 'RUR', 118, tpp2),
                ]
            ).build(session).obj
        else:
            raise ValueError('invalid parametrize')
        price_mapper = product.prices[-1]

        paysys = self._get_paysys(session, 1, 'RUR', 'ur', 'bank')

        invoice = self._create_invoice(
            session, paysys, product, 10,
            order_begin_dt=datetime.datetime.now() + datetime.timedelta(20)
        )
        invoice_order, = invoice.invoice_orders

        assert DU(1180, 'RUR') == invoice_order.amount
        assert DU(10, 'QTY') == invoice_order.quantity
        assert invoice_order.tax_policy_pct == tpp1
        assert invoice_order.price_mapper == price_mapper

    @pytest.mark.taxes_update
    @pytest.mark.parametrize('subtype', ['tax_excluded', 'tax_included'])
    def test_price_date_tax_change_past(self, session, subtype):
        past = datetime.datetime(2000, 1, 1)
        now = datetime.datetime.now()
        today = ut.trunc_date(now)

        tax_policy = ob.TaxPolicyBuilder(
            tax_pcts=[(past, 18), (today, 20)]
        ).build(session).obj
        tpp1, tpp2 = tax_policy.taxes

        if subtype == 'tax_excluded':
            product = ob.ProductBuilder(
                taxes=tax_policy,
                prices=[(past, 'RUR', 100)]
            ).build(session).obj
        elif subtype == 'tax_included':
            product = ob.ProductBuilder(
                taxes=tax_policy,
                prices=[
                    (past, 'RUR', 120, tpp1),
                    (today, 'RUR', 120, tpp2),
                ]
            ).build(session).obj
        else:
            raise ValueError('invalid parametrize')
        price_mapper = product.prices[0]

        paysys = self._get_paysys(session, 1, 'RUR', 'ur', 'bank')

        invoice = self._create_invoice(
            session, paysys, product, 10,
            order_begin_dt=datetime.datetime.now() - datetime.timedelta(20)
        )
        invoice_order, = invoice.invoice_orders

        assert DU(1200, 'RUR') == invoice_order.amount
        assert DU(10, 'QTY') == invoice_order.quantity
        assert invoice_order.tax_policy_pct == tpp2
        assert invoice_order.price_mapper == price_mapper

    @pytest.mark.taxes_update
    def test_price_date_tax_change_currency_product(self, session):
        past = datetime.datetime(2000, 1, 1)
        future = datetime.datetime.now() + datetime.timedelta(10)

        tax_policy = ob.TaxPolicyBuilder(
            tax_pcts=[(past, 18), (future, 20)]
        ).build(session).obj
        tpp1, tpp2 = tax_policy.taxes

        product = ob.ProductBuilder(
            taxes=tax_policy,
            unit=ob.Getter(mapper.ProductUnit, DIRECT_RUB_UNIT_ID)
        ).build(session).obj

        paysys = self._get_paysys(session, 1, 'RUR', 'ur', 'bank')

        invoice = self._create_invoice(
            session, paysys, product, 10,
            order_begin_dt=datetime.datetime.now() + datetime.timedelta(20)
        )
        invoice_order, = invoice.invoice_orders

        assert DU(10, 'RUR') == invoice_order.amount
        assert DU(10, 'QTY') == invoice_order.quantity
        assert invoice_order.tax_policy_pct == tpp1
        assert invoice_order.price_mapper is None

    @pytest.mark.taxes_update
    def test_price_date_tax_change_season_coeff(self, session):
        past = datetime.datetime(2000, 1, 1)
        future = datetime.datetime.now() + datetime.timedelta(10)

        tax_policy = ob.TaxPolicyBuilder(
            tax_pcts=[(past, 18), (future, 20)]
        ).build(session).obj
        tpp1, tpp2 = tax_policy.taxes

        product = ob.ProductBuilder(
            taxes=tax_policy,
            prices=[(past, 'RUR', 100)]
        ).build(session).obj
        price_mapper = product.prices[0]

        ob.ProdSeasonCoeffBuilder(
            target_id=product.id, coeff=666,
            dt=future, finish_dt=future + datetime.timedelta(666),
        ).build(session)

        paysys = self._get_paysys(session, 1, 'RUR', 'ur', 'bank')

        invoice = self._create_invoice(
            session, paysys, product, 10,
            order_begin_dt=datetime.datetime.now() + datetime.timedelta(20)
        )
        invoice_order, = invoice.invoice_orders

        assert DU('7858.8', 'RUR') == invoice_order.amount
        assert DU(10, 'QTY') == invoice_order.quantity
        assert invoice_order.tax_policy_pct == tpp1
        assert invoice_order.price_mapper == price_mapper

    @pytest.mark.parametrize(
        'patch_discount_proofs',
        [
            (6, None, None),
            (None, 6, None),
            (None, None, 6),
        ],
        ['patch_discount_proofs'],
        ids=lambda x: str(x)
    )
    def test_discount_proofs(self, session, patch_discount_proofs):
        paysys = self._get_paysys(session, 1, 'RUR', 'ur', 'bank')
        product = ob.Getter(mapper.Product, DIRECT_PRODUCT_ID).build(session).obj

        invoice = self._create_invoice(session, paysys, product, 10)
        io, = invoice.invoice_orders

        req_client, req_agency, req_row = patch_discount_proofs
        assert (io.client_discount_proof and io.client_discount_proof.discount) == req_client
        assert (invoice.agency_discount_proof and invoice.agency_discount_proof.discount) == req_agency
        assert (io.row_discount_proof and io.row_discount_proof.discount) == req_row

    def test_internal_price_func(self, session):
        paysys = self._get_paysys(session, 1, 'RUR', 'ur', 'bank')
        product = ob.Getter(mapper.Product, DIRECT_PRODUCT_ID).build(session).obj
        invoice = self._create_invoice(session, paysys, product, 10)
        invoice_order, = invoice.invoice_orders
        order = invoice_order.order

        price_obj = invoice.internal_price(order, invoice.dt)
        assert DU('30', 'FISH', [1, 'QTY']) == price_obj.price
        assert 1 == price_obj.type_rate
        assert TAX_POLICY_RUSSIA_RESIDENT == price_obj.tax_policy_pct.tax_policy_id
        assert 'FISH' == price_obj.currency

    @pytest.mark.parametrize(
        'paysys, unit',
        [
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID],
        ],
        ['paysys', 'unit'],
        ids=lambda o: str(o)
    )
    def test_tax_policy_price_mapper(self, session, paysys, unit):
        product = self._create_product(session, unit, paysys.currency, 42)
        price_mapper, = product.prices
        invoice = self._create_invoice(session, paysys, product, 1)
        session.flush()
        invoice_order, = invoice.invoice_orders

        assert price_mapper.id == invoice_order.price_id
        assert price_mapper.tax_policy_pct_id == invoice_order.tax_policy_pct_id

    def test_crossfirm_default_tax(self, session):
        product = ob.ProductBuilder(
            prices=[('RUR', 100)],
            create_taxes=False
        ).build(session).obj
        paysys = self._get_paysys(session, 1, 'RUR', 'ur', 'bank')

        invoice = self._create_invoice(
            session, paysys, product, 10,
            order_begin_dt=datetime.datetime.now() + datetime.timedelta(20),
            crossfirm=True
        )
        invoice_order, = invoice.invoice_orders

        tpp = invoice_order.tax_policy_pct
        assert tpp.tax_policy_id == TaxPolicyId.RUS_STANDARD_NDS


class TestInvoiceOrderSumsQtyIsAmount(BaseTestInvoiceOrderSums):
    def _create_request(self, *args, **kwargs):
        request = super(TestInvoiceOrderSumsQtyIsAmount, self)._create_request(*args, **kwargs)
        request.force_amount = True
        request.session.flush()
        return request

    @pytest.mark.parametrize(
        'paysys, unit, price, discount_pct, qty, req_qty',
        [
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, 66, 0, DU(660, 'QTY'), DU(10, 'QTY')],
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, 66, 10, DU(660, 'QTY'), DU('11.111111', 'QTY')],
        ],
        ['paysys', 'unit'],
        ids=lambda o: str(o)
    )
    def test_quantity(self, session, paysys, unit, price, discount_pct, qty, req_qty):
        product = self._create_product(session, unit, paysys.currency, price)
        invoice = self._create_invoice(session, paysys, product, qty, discount_pct)
        invoice_order, = invoice.invoice_orders

        assert req_qty == invoice_order.quantity
        assert req_qty == invoice_order.initial_quantity

    @pytest.mark.parametrize(
        'paysys, unit, price, discount_pct, qty, req_amount',
        [
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, 66, 0, DU(660, 'QTY'), DU(660, 'RUR')],
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, 66, 10, DU(660, 'QTY'), DU(660, 'RUR')],
        ],
        ['paysys', 'unit'],
        ids=lambda o: str(o)
    )
    def test_amount(self, session, paysys, unit, price, discount_pct, qty, req_amount):
        product = self._create_product(session, unit, paysys.currency, price)
        invoice = self._create_invoice(session, paysys, product, qty, discount_pct)
        invoice_order, = invoice.invoice_orders

        assert req_amount == invoice_order.amount

    @pytest.mark.parametrize(
        'paysys, unit, price, discount_pct, qty, req_amount',
        [
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, 66, 0, DU(660, 'QTY'), DU(660, 'RUR')],
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, 66, 10, DU(660, 'QTY'), DU('733.33', 'RUR')],
        ],
        ['paysys', 'unit'],
        ids=lambda o: str(o)
    )
    def test_amount_no_discount(self, session, paysys, unit, price, discount_pct, qty, req_amount):
        product = self._create_product(session, unit, paysys.currency, price)
        invoice = self._create_invoice(session, paysys, product, qty, discount_pct)
        invoice_order, = invoice.invoice_orders

        assert req_amount == invoice_order.amount_no_discount

    @pytest.mark.parametrize(
        'paysys, unit, price, discount_pct, qty, req_amount',
        [
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, 66, 0, DU(660, 'QTY'), DU('110', 'RUR')],
            [(1, 'RUR', 'ur', 'bank'), AUCTION_UNIT_ID, 66, 10, DU(660, 'QTY'), DU('110', 'RUR')],
        ],
        ['paysys', 'unit'],
        ids=lambda o: str(o)
    )
    def test_amount_nds(self, session, paysys, unit, price, discount_pct, qty, req_amount):
        product = self._create_product(session, unit, paysys.currency, price)
        invoice = self._create_invoice(session, paysys, product, qty, discount_pct)
        invoice_order, = invoice.invoice_orders

        assert req_amount == invoice_order.amount_nds

    @pytest.mark.parametrize(
        'paysys, qty, req_amount',
        [
            [(1, 'RUR', 'ur', 'bank'), DU(666, 'QTY'), DU('111', 'RUR')],
            [(1, 'RUR', 'yt', 'bank'), DU(666, 'QTY'), DU('0', 'RUR')],
        ],
        ['paysys'],
        ids=lambda o: str(o)
    )
    def test_amount_nds_currency_product(self, session, paysys, qty, req_amount):
        product = ob.Getter(mapper.Product, DIRECT_PRODUCT_RUB_ID).build(session).obj
        invoice = self._create_invoice(session, paysys, product, qty)
        invoice_order, = invoice.invoice_orders

        assert req_amount == invoice_order.amount_nds


class BaseTestInvoicePrice(object):
    @pytest.fixture
    def client(self, session):
        return ob.ClientBuilder().build(session).obj

    @pytest.fixture
    def person(self, session, client):
        return ob.PersonBuilder(client=client, type='ur').build(session).obj

    @pytest.fixture
    def paysys(self, request, session):
        paysys_id = getattr(request, 'param', None) or 1003
        return ob.Getter(mapper.Paysys, paysys_id).build(session).obj


class TestInvoiceOrderAdfoxPrice(BaseTestInvoicePrice):
    def _create_product(self, session, media_discount):
        from balance.discounts.constants import DISCOUNT_MAP
        discount_id = DISCOUNT_MAP[media_discount]
        return ob.ProductBuilder(price=100, media_discount=discount_id, engine_id=ServiceId.ADFOX).build(session).obj

    def _create_scale(self, session, x_unit, y_unit, points, namespace='discount'):
        scale_code = str(ob.get_big_number())
        scale = mapper.StaircaseScale(namespace=namespace, code=scale_code, x_unit_id=x_unit, y_unit_id=y_unit)
        scale.points = [
            mapper.ScalePoint(
                scale_code=scale_code,
                start_dt=datetime.datetime(2015, 1, 1),
                x=x, y=y
            ) for x, y in points
            ]
        session.add(scale)
        session.flush()
        return scale

    @pytest.mark.parametrize(
        'qty, required_sum, required_price',
        [
            [5, 500, 100],
            [15, 3000, 200],
            [25, 7500, 300],
        ]
    )
    def test_adfox_contract(self, session, client, person, paysys, qty, required_sum, required_price):
        product = self._create_product(session, 'ADFox')
        price_mapper, = product.prices
        scale = self._create_scale(
            session, SHOWS_1000_UNIT_ID, COEFFICIENT_UNIT_ID,
            [
                (0, 1),
                (10, 2),
                (20, 3),
            ],
            namespace='adfox'
        )

        contract = ob.ContractBuilder(
            client=client,
            person=person,
            is_signed=datetime.datetime.now(),
            services={ServiceId.ADFOX},
            adfox_products={product.id: {u'account': u'', u'scale': scale.code}}
        ).build(session).obj
        contract.is_signed_dt = datetime.datetime(2001, 1, 1)
        session.flush()

        order = ob.OrderBuilder(client=client, product=product, service_id=ServiceId.ADFOX)
        basket = ob.BasketBuilder(rows=[ob.BasketItemBuilder(quantity=qty, order=order)])
        request = ob.RequestBuilder(basket=basket).build(session).obj
        invoice = InvoiceFactory.create(request, paysys, contract=contract, temporary=False)
        session.flush()
        invoice_order, = invoice.invoice_orders

        assert DU(qty, 'QTY') == invoice_order.quantity
        assert DU(qty, 'QTY') == invoice_order.initial_quantity
        assert DU(required_sum, 'RUR') == invoice_order.amount
        assert DU(0, '%') == invoice_order.discount_pct
        assert 1 == invoice_order.type_rate
        assert DU(required_price, 'RUR', [1, 'QTY']) == invoice_order.price
        assert DU(required_price, 'FISH', [1, 'QTY']) == invoice_order.internal_price
        assert price_mapper.id == invoice_order.price_id
        assert price_mapper.tax_policy_pct_id == invoice_order.tax_policy_pct_id

        assert DU(required_sum, 'RUR') == invoice.amount
        assert DU(required_sum, 'RUR') == invoice.total_sum
        assert DU(required_sum, 'FISH') == invoice.effective_sum

    @pytest.mark.parametrize(
        'qty, required_sum, required_price, required_type_rate',
        [
            [5, 1, 1, 5],
            [15, 2, 2, 15],
            [25, 3, 3, 25],
        ]
    )
    def test_adfox_offer(self, session, client, person, paysys, qty, required_sum, required_price, required_type_rate):
        product = self._create_product(session, 'ADFox оферта')
        price_mapper, = product.prices
        scale = self._create_scale(
            session, REQUEST_UNIT_ID, None,
            [
                (0, 1),
                (10, 2),
                (20, 3),
            ]
        )

        from balance.discounts.calculators import ADFoxOfferPriceCalculator
        from balance.invoice_calc import price as calc_price

        ADFoxOfferPriceCalculator.resident_scales[1] = scale.code
        calc_price.ADFOX_OFFER_RESIDENT_SCALES[1] = scale.code

        order = ob.OrderBuilder(client=client, product=product, service_id=ServiceId.ADFOX)
        basket = ob.BasketBuilder(rows=[ob.BasketItemBuilder(quantity=qty, order=order)])
        request = ob.RequestBuilder(basket=basket).build(session).obj
        invoice = InvoiceFactory.create(request, paysys, person, temporary=False)
        session.flush()
        invoice_order, = invoice.invoice_orders

        assert DU(qty, 'QTY') == invoice_order.quantity
        assert DU(qty, 'QTY') == invoice_order.initial_quantity
        assert DU(required_sum, 'RUR') == invoice_order.amount
        assert DU(0, '%') == invoice_order.discount_pct
        assert required_type_rate == invoice_order.type_rate
        assert DU(required_price, 'RUR', [required_type_rate, 'QTY']) == invoice_order.price
        assert DU(required_price, 'FISH', [required_type_rate, 'QTY']) == invoice_order.internal_price
        assert price_mapper.id == invoice_order.price_id
        assert price_mapper.tax_policy_pct_id == invoice_order.tax_policy_pct_id

        assert DU(required_sum, 'RUR') == invoice.amount
        assert DU(required_sum, 'RUR') == invoice.total_sum
        assert DU(required_sum, 'FISH') == invoice.effective_sum


class TestInvoiceOrderServiceProductPrice(BaseTestInvoicePrice):
    def _create_service_product(self, session, service_id, product, client, price):
        service_product = mapper.ServiceProduct(
            service_id=service_id,
            partner_id=client.id,
            product_id=product.id,
            name=product.name,
            external_id=ob.get_big_number()
        )
        session.add(service_product)

        service_price = mapper.ServicePrice(
            service_product=service_product,
            dt=datetime.datetime(1666, 06, 06),
            region_id=225,
            clid=666666,
            price=price,
            currency='RUR'
        )
        session.add(service_price)
        session.flush()

        return service_product

    def test(self, session, client, person, paysys):
        product = ob.ProductBuilder(price=666).build(session).obj
        price_mapper, = product.prices
        service_product = self._create_service_product(session, 7, product, client, 42)

        order = ob.OrderBuilder(
            client=client,
            product=product,
            service_product=service_product,
            region_id=225,
            clid=666666
        )
        basket = ob.BasketBuilder(rows=[ob.BasketItemBuilder(quantity=10, order=order)])
        request = ob.RequestBuilder(basket=basket).build(session).obj

        invoice = InvoiceFactory.create(request, paysys, person, temporary=False)
        session.flush()
        invoice_order, = invoice.invoice_orders

        assert DU(420, 'RUR') == invoice_order.amount
        assert DU('70', 'RUR') == invoice_order.amount_nds
        assert DU(42, 'RUR', [1, 'QTY']) == invoice_order.price
        assert DU(42, 'FISH', [1, 'QTY']) == invoice_order.internal_price
        assert price_mapper.id == invoice_order.price_id
        assert price_mapper.tax_policy_pct_id == invoice_order.tax_policy_pct_id

        assert DU(420, 'RUR') == invoice.amount
        assert DU(420, 'RUR') == invoice.total_sum
        assert DU(420, 'FISH') == invoice.effective_sum


class TestInvoiceOrderPriceFactors(BaseTestInvoicePrice):
    @pytest.fixture
    def product(self, request, session):
        price = getattr(request, 'param', None) or 666
        return ob.ProductBuilder(price=price).build(session).obj

    def _create_markup(self, session, product, pct):
        markup = ob.MarkupBuilder().build(session).obj
        ob.ProductMarkupBuilder(
            markup=markup,
            product=product,
            pct=pct,
            dt=datetime.datetime(2000, 01, 01)
        ).build(session)
        return markup

    @pytest.fixture
    def markup(self, request, session, product):
        pct = getattr(request, 'param', None) or 66
        markup = self._create_markup(session, product, pct)

        # для проверки фильтрации по заказу или строке
        self._create_markup(session, product, 666)

        return markup

    def test_order_price_factor(self, session, client, person, paysys, product):
        order = ob.OrderBuilder(client=client, product=product, price_factor=10)
        basket = ob.BasketBuilder(rows=[ob.BasketItemBuilder(quantity=10, order=order)])
        request = ob.RequestBuilder(basket=basket).build(session).obj

        invoice = InvoiceFactory.create(request, paysys, person, temporary=False)
        invoice_order, = invoice.invoice_orders

        assert DU(66600, 'RUR') == invoice_order.amount
        assert DU('11100', 'RUR') == invoice_order.amount_nds
        assert DU(6660, 'RUR', [1, 'QTY']) == invoice_order.price
        assert DU(6660, 'FISH', [1, 'QTY']) == invoice_order.internal_price

        assert DU(66600, 'RUR') == invoice.amount
        assert DU(66600, 'RUR') == invoice.total_sum
        assert DU(66600, 'FISH') == invoice.effective_sum

    def test_order_markups(self, session, client, person, paysys, product, markup):
        order = ob.OrderBuilder(client=client, product=product, markups=[markup.id])
        basket = ob.BasketBuilder(rows=[ob.BasketItemBuilder(quantity=10, order=order)])
        request = ob.RequestBuilder(basket=basket).build(session).obj

        invoice = InvoiceFactory.create(request, paysys, person, temporary=False)
        invoice_order, = invoice.invoice_orders

        assert DU('11055.60', 'RUR') == invoice_order.amount
        assert DU('1842.60', 'RUR') == invoice_order.amount_nds
        assert DU('1105.56', 'RUR', [1, 'QTY']) == invoice_order.price
        assert DU('1105.56', 'FISH', [1, 'QTY']) == invoice_order.internal_price

        assert DU('11055.60', 'RUR') == invoice.amount
        assert DU('11055.60', 'RUR') == invoice.total_sum
        assert DU('11055.60', 'FISH') == invoice.effective_sum

    def test_row_markups(self, session, client, person, paysys, product, markup):
        order = ob.OrderBuilder(client=client, product=product)
        basket = ob.BasketBuilder(rows=[ob.BasketItemBuilder(quantity=10, order=order, markups=[markup.id])])
        request = ob.RequestBuilder(basket=basket).build(session).obj

        invoice = InvoiceFactory.create(request, paysys, person, temporary=False)
        invoice_order, = invoice.invoice_orders

        assert DU('11055.60', 'RUR') == invoice_order.amount
        assert DU('1842.60', 'RUR') == invoice_order.amount_nds
        assert DU('1105.56', 'RUR', [1, 'QTY']) == invoice_order.price
        assert DU('1105.56', 'FISH', [1, 'QTY']) == invoice_order.internal_price

        assert DU('11055.60', 'RUR') == invoice.amount
        assert DU('11055.60', 'RUR') == invoice.total_sum
        assert DU('11055.60', 'FISH') == invoice.effective_sum

    def test_markups_price_factor(self, session, client, person, paysys, product, markup):
        order = ob.OrderBuilder(client=client, product=product, price_factor=3)
        basket = ob.BasketBuilder(rows=[ob.BasketItemBuilder(quantity=10, order=order, markups=[markup.id])])
        request = ob.RequestBuilder(basket=basket).build(session).obj

        invoice = InvoiceFactory.create(request, paysys, person, temporary=False)
        invoice_order, = invoice.invoice_orders

        assert DU('33166.8', 'RUR') == invoice_order.amount
        assert DU('5527.80', 'RUR') == invoice_order.amount_nds
        assert DU('3316.68', 'RUR', [1, 'QTY']) == invoice_order.price
        assert DU('3316.68', 'FISH', [1, 'QTY']) == invoice_order.internal_price

        assert DU('33166.8', 'RUR') == invoice.amount
        assert DU('33166.8', 'RUR') == invoice.total_sum
        assert DU('33166.8', 'FISH') == invoice.effective_sum


class TestInvoiceAggregates(object):
    @pytest.fixture
    def patch_update_io_amounts(self, request):
        old_property = mapper.Invoice.need_update_io_amounts
        mapper.Invoice.need_update_io_amounts = property(lambda self: request.param)
        yield
        mapper.Invoice.need_update_io_amounts = old_property

    def _create_invoice(self, session, qtys, adjust_qty=False, force_amount=False):
        client = ob.ClientBuilder().build(session).obj
        person = ob.PersonBuilder(client=client, type='ur').build(session).obj
        paysys = ob.Getter(mapper.Paysys, 1003).build(session).obj
        product = ob.Getter(mapper.Product, DIRECT_PRODUCT_ID)

        rows = [
            ob.BasketItemBuilder(
                quantity=qty,
                order=ob.OrderBuilder(client=client, product=product)
            ) for qty in qtys
            ]
        request = ob.RequestBuilder(
            basket=ob.BasketBuilder(
                rows=rows,
                adjust_qty=adjust_qty,
                force_amount=force_amount,
            )
        ).build(session).obj
        return InvoiceFactory.create(request, paysys, person, temporary=False)

    @pytest.mark.parametrize(
        [
            'patch_update_io_amounts',
            'qtys',
            'req_amount',
            'req_amount_nds',
            'req_rows_amount',
            'req_rows_amount_nds'
        ],
        [
            [
                False,
                [DU('3.333333', 'QTY'), DU('3.333333', 'QTY'), DU('3.333333', 'QTY')],
                DU('300', 'RUR'),
                DU('50.01', 'RUR'),
                [DU('100', 'RUR'), DU('100', 'RUR'), DU('100', 'RUR')],
                [DU('16.67', 'RUR'), DU('16.67', 'RUR'), DU('16.67', 'RUR')],
            ],
            [
                True,
                [DU('3.333333', 'QTY'), DU('3.333333', 'QTY'), DU('3.333333', 'QTY')],
                DU('300', 'RUR'),
                DU('50', 'RUR'),
                [DU('100', 'RUR'), DU('100', 'RUR'), DU('100', 'RUR')],
                [DU('16.67', 'RUR'), DU('16.66', 'RUR'), DU('16.67', 'RUR')],
            ],
        ],
        ['patch_update_io_amounts'],
        ids=['wo_update_io_amounts', 'w_update_io_amounts']
    )
    @pytest.mark.usefixtures('patch_update_io_amounts')
    def test_amounts(self, session, qtys, req_amount, req_amount_nds, req_rows_amount, req_rows_amount_nds):
        invoice = self._create_invoice(session, qtys)

        assert req_amount == invoice.amount
        assert req_amount == invoice.total_sum
        assert DU(req_amount.as_decimal(), 'FISH') == invoice.effective_sum
        assert req_amount_nds == invoice.amount_nds
        assert DU(0, 'RUR') == invoice.amount_nsp
        assert 20 == invoice.nds_pct

        assert req_rows_amount == [io.amount for io in invoice.invoice_orders]
        assert req_rows_amount_nds == [io.amount_nds for io in invoice.invoice_orders]

    @pytest.mark.parametrize(
        'patch_discount_proofs, req_agency_pct, req_pct, req_amount',
        [
            [(10, None, None), 0, 10, DU('675', 'RUR')],
            [(None, 10, None), 10, 10, DU('675', 'RUR')],
            [(None, None, 10), 0, 10, DU('675', 'RUR')],
        ],
        ['patch_discount_proofs'],
        ids=['client', 'agency', 'unconditional']
    )
    @pytest.mark.usefixtures('patch_discount_proofs')
    def test_agency_discount_pct(self, session, req_agency_pct, req_pct, req_amount):
        invoice = self._create_invoice(session, [DU(10, 'QTY'), DU(15, 'QTY')])

        assert req_agency_pct == invoice.agency_discount_pct
        assert req_agency_pct == (invoice.agency_discount_proof.discount if invoice.agency_discount_proof else 0)
        assert req_amount == invoice.total_sum
        assert {req_pct} == {io.discount_pct for io in invoice.invoice_orders}

    @pytest.mark.parametrize(
        'patch_discount_proofs',
        [
            (10, None, None)
        ],
        ['patch_discount_proofs'],
        ids=['^_^']
    )
    @pytest.mark.usefixtures('patch_discount_proofs')
    def test_client_discounts_proofs(self, session):
        invoice = self._create_invoice(session, [DU(10, 'QTY'), DU(15, 'QTY')])

        act_proofs = {
            (cl, p.type, p.currency, p.discount, p.adjust_quantity)
            for cl, p in invoice.client_discount_proofs.iteritems()
            }
        req_proofs = {(invoice.client, 'mock', 'RUR', 10, False)}
        assert req_proofs == act_proofs

    @pytest.mark.parametrize(
        'patch_discount_proofs',
        [
            (10, None, None)
        ],
        ['patch_discount_proofs'],
        ids=['^_^']
    )
    @pytest.mark.parametrize(
        'adjust_qty, qtys, req_sum, req_initial_qtys, req_qtys',
        [
            [
                False,
                [DU('10', 'QTY'), DU('20', 'QTY')],
                DU('810', 'RUR'),
                [DU('10', 'QTY'), DU('20', 'QTY')],
                [DU('10', 'QTY'), DU('20', 'QTY')]],
            [
                True,
                [DU('10', 'QTY'), DU('20', 'QTY')],
                DU('900', 'RUR'),
                [DU('10', 'QTY'), DU('20', 'QTY')],
                [DU('11.111111', 'QTY'), DU('22.222222', 'QTY')]
            ],
        ],
        ids=['qty_discount', 'sum_discount']
    )
    @pytest.mark.usefixtures('patch_discount_proofs')
    def test_multiple_updates(self, session, adjust_qty, qtys, req_sum, req_initial_qtys, req_qtys):
        invoice = self._create_invoice(session, qtys, adjust_qty=adjust_qty)

        assert req_sum == invoice.total_sum
        assert req_qtys == [io.quantity for io in invoice.invoice_orders]
        assert req_initial_qtys == [io.initial_quantity for io in invoice.invoice_orders]

        invoice.update()

        assert req_sum == invoice.total_sum
        assert req_qtys == [io.quantity for io in invoice.invoice_orders]
        assert req_initial_qtys == [io.initial_quantity for io in invoice.invoice_orders]

    def test_qty_is_amount_multiple_updates(self, session):
        invoice = self._create_invoice(
            session,
            [DU('123', 'QTY'), DU('543', 'QTY')],
            force_amount=True
        )

        req_amount = DU('666', 'RUR')
        req_amount_nds = DU('111', 'RUR')
        req_row_amounts = [DU('123', 'RUR'), DU('543', 'RUR')]
        req_row_qtys = [DU('4.1', 'QTY'), DU('18.1', 'QTY')]

        assert req_amount == invoice.amount
        assert req_amount == invoice.total_sum
        assert DU(req_amount.as_decimal(), 'FISH') == invoice.effective_sum
        assert req_amount_nds == invoice.amount_nds
        assert DU(0, 'RUR') == invoice.amount_nsp
        assert 20 == invoice.nds_pct

        assert req_row_amounts == [io.amount for io in invoice.invoice_orders]
        assert req_row_qtys == [io.quantity for io in invoice.invoice_orders]
        assert req_row_qtys == [io.initial_quantity for io in invoice.invoice_orders]

        invoice.update()

        assert req_amount == invoice.amount
        assert req_amount == invoice.total_sum
        assert DU(req_amount.as_decimal(), 'FISH') == invoice.effective_sum
        assert req_amount_nds == invoice.amount_nds
        assert DU(0, 'RUR') == invoice.amount_nsp
        assert 20 == invoice.nds_pct

        assert req_row_amounts == [io.amount for io in invoice.invoice_orders]
        assert req_row_qtys == [io.quantity for io in invoice.invoice_orders]
        assert req_row_qtys == [io.initial_quantity for io in invoice.invoice_orders]

    @pytest.mark.taxes_update
    def test_nds_pct_price_date_tax_change(self, session):
        client = ob.ClientBuilder().build(session).obj
        person = ob.PersonBuilder(client=client, type='ur').build(session).obj
        paysys = ob.Getter(mapper.Paysys, 1003).build(session).obj

        tax_policy = ob.TaxPolicyBuilder(
            tax_pcts=[(datetime.datetime(2000, 1, 1), 18), (datetime.datetime.now() + datetime.timedelta(10), 20)]
        ).build(session).obj
        tpp1, tpp2 = tax_policy.taxes

        product = ob.ProductBuilder(
            taxes=tax_policy,
            prices=[(datetime.datetime(2000, 1, 1), 'RUR', 100)]
        ).build(session).obj

        request = ob.RequestBuilder(
            basket=ob.BasketBuilder(
                rows=[
                    ob.BasketItemBuilder(
                        quantity=10,
                        order=ob.OrderBuilder(
                            client=client,
                            product=product,
                            begin_dt=datetime.datetime.now() + datetime.timedelta(20)
                        )
                    )
                ],
            )
        ).build(session).obj
        invoice = InvoiceFactory.create(request, paysys, person, temporary=False)
        session.flush()

        assert invoice.tax_policy_pct == tpp1
        assert invoice.nds_pct == 18

    def test_multiple_taxes(self, session):
        client = ob.ClientBuilder().build(session).obj
        person = ob.PersonBuilder(client=client, type='ur').build(session).obj
        paysys = ob.Getter(mapper.Paysys, 1003).build(session).obj

        tax_policy1 = ob.TaxPolicyBuilder.construct(session, tax_pcts=[20])
        tax_policy2 = ob.TaxPolicyBuilder.construct(session, tax_pcts=[30])

        product1 = ob.ProductBuilder.construct(session, taxes=tax_policy1, prices=[('RUR', 100)])
        product2 = ob.ProductBuilder.construct(session, taxes=tax_policy2, prices=[('RUR', 100)])

        request = ob.RequestBuilder(
            basket=ob.BasketBuilder(
                rows=[
                    ob.BasketItemBuilder(
                        quantity=10,
                        order=ob.OrderBuilder(
                            client=client,
                            product=product,
                        )
                    )
                    for product in [product1, product2]
                ],
            )
        ).build(session).obj

        with pytest.raises(exc.INVALID_PARAM) as exc_info:
            InvoiceFactory.create(request, paysys, person, temporary=False)

        assert 'Attempt to create invoice with different taxes' in exc_info.value.msg


class TestYInvoiceSums(object):
    def _pay_on_credit(self, contract, subclient, qtys):
        paysys_id = 1003
        product = ob.Getter(mapper.Product, DIRECT_PRODUCT_ID)

        rows = [
            ob.BasketItemBuilder(
                quantity=qty,
                order=ob.OrderBuilder(agency=contract.client, client=subclient, product=product)
            ) for qty in qtys
            ]
        request = ob.RequestBuilder(
            basket=ob.BasketBuilder(client=contract.client, rows=rows)
        ).build(contract.session).obj

        from balance import core
        coreobj = core.Core(request.session)
        inv, = coreobj.pay_on_credit(
            request_id=request.id,
            paysys_id=paysys_id,
            person_id=contract.person.id,
            contract_id=contract.id
        )
        request.session.flush()
        return inv

    def _generate_y_invoice(self, pa):
        now = datetime.datetime.now()
        for consume in pa.consumes:
            order = consume.order
            order.calculate_consumption(now, {order.shipment_type: consume.current_qty})

        act, = a_a.ActAccounter(
            pa.client,
            mapper.ActMonth(for_month=now),
            invoices=[pa.id], dps=[],
            force=1
        ).do()
        invoice = act.invoice
        pa.session.flush()
        return invoice

    def _do_rows_asserts(self,
                         invoice, qtys,
                         req_amounts_rur, req_amounts_fish, req_amounts_nds,
                         req_int_price, req_price, req_price_wo_nds):
        assert qtys == [io.quantity for io in invoice.invoice_orders]
        assert qtys == [io.initial_quantity for io in invoice.invoice_orders]
        assert req_amounts_rur == [io.amount for io in invoice.invoice_orders]
        assert req_amounts_rur == [io.amount_no_discount for io in invoice.invoice_orders]
        assert req_amounts_fish == [io.effective_sum for io in invoice.invoice_orders]
        assert req_amounts_nds == [io.amount_nds for io in invoice.invoice_orders]

        # TODO: в mapper.YInvoice'ах перепутаны price и internal_price
        assert {req_int_price} == {io.internal_price for io in invoice.invoice_orders}
        assert {req_price} == {ut.round(io.price, 10) for io in invoice.invoice_orders}
        assert {req_price_wo_nds} == {io.price_wo_nds for io in invoice.invoice_orders}

    @pytest.mark.parametrize(
        [
            'is_non_resident',
            'qtys',
            'req_total_amount',
            'req_total_nds',
            'req_amounts',
            'req_amounts_nds',
            'req_int_price',
            'req_price',
            'req_price_wo_nds'
        ],
        [
            [
                False,
                [DU(10, 'QTY'), DU(10, 'QTY'), DU(10, 'QTY')],
                900, D('150'),
                [300, 300, 300],
                [DU('50', 'RUR'), DU('50', 'RUR'), DU('50', 'RUR')],
                30, 30, D('25')
            ],
            [
                True,
                [DU(10, 'QTY'), DU(10, 'QTY'), DU(10, 'QTY')],
                D('750'), D('0'),
                [D('250'), D('250'), D('250')],
                [DU('0', 'RUR'), DU('0', 'RUR'), DU('0', 'RUR')],
                D('25'), D('25'), D('25')
            ],
        ],
        ids=['common', 'is_non_resident']
    )
    def test_full(self, session, credit_contract,
                  is_non_resident, qtys,
                  req_total_amount, req_total_nds,
                  req_amounts, req_amounts_nds,
                  req_int_price, req_price, req_price_wo_nds):

        if is_non_resident:
            credit_contract.col0.non_resident_clients = True
            session.flush()

        if is_non_resident:
            client = ob.ClientBuilder(
                agency=credit_contract.client,
                is_non_resident=True,
                fullname='full_name',
                currency_payment='RUR',
            ).build(session).obj
        else:
            client = ob.ClientBuilder(agency=credit_contract.client).build(session).obj
        pa = self._pay_on_credit(credit_contract, client, qtys)
        invoice = self._generate_y_invoice(pa)

        assert DU(req_total_amount, 'RUR') == invoice.amount
        assert DU(req_total_amount, 'RUR') == invoice.total_sum
        assert DU(req_total_amount, 'FISH') == invoice.effective_sum
        assert DU(req_total_nds, 'RUR') == invoice.amount_nds
        assert DU('0', 'RUR') == invoice.amount_nsp
        assert DU('0', '%') == invoice.agency_discount_pct

        req_amounts_rur = [DU(s, 'RUR') for s in req_amounts]
        req_amounts_fish = [DU(s, 'FISH') for s in req_amounts]

        self._do_rows_asserts(
            invoice, qtys,
            req_amounts_rur, req_amounts_fish, req_amounts_nds,
            req_int_price, req_price, req_price_wo_nds
        )

        # Проверяем инициализацию строк из базы
        invoice_id = invoice.id
        session.flush()
        session.expunge(invoice)
        del invoice
        session.expire_all()
        invoice = session.query(mapper.Invoice).getone(invoice_id)

        self._do_rows_asserts(
            invoice, qtys,
            req_amounts_rur, req_amounts_fish, req_amounts_nds,
            req_int_price, req_price, req_price_wo_nds
        )


def test_contract_manager_preserved(session):
    """
    Regression test case for BALANCE-31824

    In process of Invoice creation Invoice.update_head_attributes is called after
    Invoice.assign_contract. In assign_contract manager is assigned via `manager_code` attribute (column),
    while in update_head_attributes it is assigned via `manager` property (relation).
    When `manager_code` is set to non-empty value, and then `manager` is set to None,
    SQLAlchemy==0.9.8 writes non-empty manager_code to db, while SQLAlchemy==1.2.12
    properly synchronizes relation/column accesses, and writes None to manager_code column
    (because it is set after non-empty value, and overrides it).
    The fix is to not lose manager_code from contract, if there is no manager in
    invoice orders (or no invoice orders).
    """
    client = ob.ClientBuilder()
    person = ob.PersonBuilder(client=client)
    manager_code = ob.SingleManagerBuilder.construct(session).manager_code
    contract = ob.ContractBuilder(client=client, person=person, manager_code=manager_code)
    invoice = ob.InvoiceBuilder.construct(
        session, client=client, person=person, contract=contract, postpay=1,
        request=ob.RequestBuilder(basket=ob.BasketBuilder(rows=[], client=client))
    )
    session.expire(invoice)
    assert invoice.manager_code == manager_code
