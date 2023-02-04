# -*- coding: utf-8 -*-

"""
Tests for funds movement, see BALANCE-4603

Test that *initial* receipts are created and reverted corrrectly (BALANCE-4604):
    TestInvoiceTurnOn

Test that shipments are accepted and processed correctly (BALANCE-4657):
    TestShipments

Simple test for acts creation:
    TestActs

Tests for routines related to invoice.turn_on ported from pl/sql
(BALANCE-4835)
    TestPortedRoutines

Credit-related test
    TestCredits
"""
import datetime
from decimal import Decimal as D

import pytest

from balance.mapper import Paysys, ActInternal, Act, Product, ProductUnit, CurrencyRate, Service, \
    ProdSeasonCoeff
import balance.muzzle_util as ut
from balance import core
import balance.actions.withdraw as action_withdraw
from balance.actions.transfers_qty.interface import TransferMultipleMedium
import balance.actions.consumption as action_consumption
import balance.actions.process_completions as action_completion
from balance.actions.invoice_turnon import InvoiceTurnOn
from balance.actions.invoice_create import InvoiceFactory
from balance.actions.acts import ActAccounter
from balance.providers.pay_policy import get_pay_policy_manager
from balance.constants import DIRECT_PRODUCT_ID
from butils.application.logger import get_logger

from tests.base import BalanceTest
from tests.object_builder import ProductBuilder, OrderBuilder, ClientBuilder, Getter, BasketItemBuilder, \
    BasketBuilder, RequestBuilder, InvoiceBuilder, PersonBuilder, PayOnCreditCase, GenericBuilder, ContractBuilder, \
    MarkupBuilder, ProductMarkupBuilder, ServiceId

PAYSYS_WITH_INVOICE_NOT_SENDABLE_ID = 1000
YA_MONEY_PAYSYS_ID = 1000
BANK_JUR_PAYSYS_ID = 1003

AUTORU_PRODUCT_ID_1 = 504808
AUTORU_PRODUCT_ID_2 = 504719

log = get_logger()


class TestInvoiceTurnOn(BalanceTest):
    """Test that initial consumes are created and reverted correctly"""

    def perform_integrity_checks(self, invoice):
        """Check invoice.receipt_sum, consume.consume_*, order.current_qty"""
        self.assertEqual(invoice.receipt_sum,
                         sum(r.receipt_sum for r in invoice.receipts))
        for con in invoice.consumes:
            self.assertEqual(
                con.current_sum,
                con.consume_sum - sum(rev.reverse_sum for rev in con.reverses))
            self.assertEqual(
                con.current_qty,
                con.consume_qty - sum(rev.reverse_qty for rev in con.reverses))
        for order in (io.order for io in invoice.invoice_orders):
            self.assertEqual(order.consume_qty,
                             sum(con.current_qty for con in order.consumes))
            self.assertEqual(D(order.consume_sum),
                             sum(con.current_sum for con in order.consumes))

    def test_turnon_and_rollback(self):
        paysys = Getter(Paysys, YA_MONEY_PAYSYS_ID)
        client = ClientBuilder()
        (product1, product2, product3) = (ProductBuilder(price=100),
                                          ProductBuilder(price=200),
                                          ProductBuilder(price=300))

        (order1, order2, order3) = (OrderBuilder(client=client, product=product1),
                                    OrderBuilder(client=client, product=product2),
                                    OrderBuilder(client=client, product=product3))

        # row format is (order, quantity, price, discount_pct)
        # where price format is (price, type_rate, tax_policy_pct)
        request = RequestBuilder(basket=BasketBuilder(rows=[BasketItemBuilder(order=order1, quantity=1),
                                                            BasketItemBuilder(order=order2, quantity=1),
                                                            BasketItemBuilder(order=order3, quantity=1)]))

        invoice_builder = InvoiceBuilder(request=request, paysys=paysys)
        invoice = invoice_builder.build(self.session).obj
        InvoiceTurnOn(invoice, invoice.effective_sum, manual=True).do()

        self.perform_integrity_checks(invoice)
        # Exactly 1 receipt is created with sum equal to invoice's effective
        # sum
        self.assertEqual([invoice.effective_sum],
                         [r.receipt_sum for r in invoice.receipts])
        # Acts and consumes are created with consume_* corresponding to
        # invoice rows and completion_* = 0
        self.assertEqual(set((con.consume_qty,
                              con.consume_sum,
                              con.completion_sum,
                              con.completion_qty)
                             for con in invoice.consumes),
                         set((order.consume_qty,
                              D(order.consume_sum),
                              0, 0)
                             for order in [io.order for io in
                                           invoice.invoice_orders]))

        action_withdraw.Withdraw(invoice, check_perm=False).do(forced=True)
        self.perform_integrity_checks(invoice)
        # For every consume a corresponding Reverse a has been created
        for con in invoice.consumes:
            self.assertEqual({(con.consume_qty, con.consume_sum)},
                             set((rev.reverse_qty, rev.reverse_sum) for rev in con.reverses))
        # Since paysys.instant == 1 for yandex.money, a Receipt
        # should have been created
        self.assertEqual(set(rec.receipt_sum for rec in invoice.receipts),
                         {invoice.effective_sum, -invoice.effective_sum})

    def test_turnon_overdraft(self):
        # Test for two-phase turn_on for a special case: overdraft invoice paid
        # with yandex.money
        b_request = RequestBuilder()
        for row in b_request.b.basket.b.rows:
            row.b.order.b.product = Getter(Product, DIRECT_PRODUCT_ID)
        request = b_request.build(self.session).obj
        b_person = PersonBuilder(client=b_request.basket.client,
                                 type='ur', operator_uid=self.session.oper_id)
        person = b_person.build(self.session).obj
        self.session.flush()
        coreobj = core.Core(self.session)

        # We haven't set any credit limits yet, so taking overdraft should
        # yield an error
        self.assertRaises(ut.NOT_ENOUGH_OVERDRAFT_LIMIT,
                          coreobj.take_overdraft,
                          request.id, YA_MONEY_PAYSYS_ID, person.id, skip_verification=True)
        # The above Core.take_overdraft will make an nested begin() and
        # rollback() due to error, so we need to re-initialize session and
        # objects
        b_request.reset()
        b_person.reset()
        self.tearDown()
        self.setUp()
        request = b_request.build(self.session).obj
        person = b_person.build(self.session).obj
        coreobj = core.Core(self.session)
        limit = 999999999
        firm = get_pay_policy_manager(self.session).get_firm(
            service_id=7,
            region_id=(request.client and request.client.region_id),
            category=(person and person.type),
            is_agency=request.client and request.client.is_agency
        )

        firm_id = firm.id  # TODO правльно выбрать фирму
        service_id = ServiceId.DIRECT
        iso_currency = request.client.currency_on(datetime.datetime.now(), service_id=service_id)
        request.client.set_overdraft_limit(service_id, firm_id, limit, iso_currency)  # overdraft_limit = 999999999
        self.session.flush()
        # Phase 1: going overdraft
        invoice = coreobj.take_overdraft(request.id, YA_MONEY_PAYSYS_ID, person.id, skip_verification=True)
        # No receipts
        assert invoice.receipts == []
        # But there are consumes
        assert invoice.consumes
        # And these consumes correspond to invoice_orders
        received_set = set((con.consume_qty, con.consume_sum,
                            con.completion_sum, con.completion_qty)
                           for con in invoice.consumes)
        expected_set = set((io.order.consume_qty, D(io.order.consume_sum), 0, 0)
                           for io in invoice.invoice_orders)
        assert received_set == expected_set, \
            "%s != %s" % (received_set, expected_set)
        # And there is an operaion (BALANCE-5969)
        self.session.flush()
        assert invoice.consumes[0].operation
        # Ovedraft balance has been reduced
        #
        self.assertEqual(invoice.client.get_overdraft_balance(service_id, firm_id),
                         invoice.client.get_overdraft_limit(service_id, firm_id) - \
                         sum(io.quantity for io in invoice.invoice_orders))
        # Phase 2: paying
        InvoiceTurnOn(invoice, sum=invoice.effective_sum, manual=True).do()
        # Exactly 1 receipt is created with sum equal to invoice's effective
        # sum
        self.assertEqual([invoice.effective_sum],
                         [r.receipt_sum for r in invoice.receipts])
        # And overdraft balance has been restored
        assert invoice.client.get_overdraft_balance(service_id, firm_id) == \
               invoice.client.get_overdraft_limit(service_id, firm_id)

        # Phase 3: paying once more
        InvoiceTurnOn(invoice, sum=invoice.effective_sum, manual=True).do()
        # There are 2 receipts, each equal to invoice's effective sum
        self.assertEqual([invoice.effective_sum] * 2,
                         [r.receipt_sum for r in invoice.receipts])
        # And extra money is free funds
        assert invoice.receipt_sum - invoice.consume_sum == \
               invoice.effective_sum
        # But overdraft hasn't gone over limit
        assert invoice.client.get_overdraft_balance(service_id, firm_id) == \
               invoice.client.get_overdraft_limit(service_id, firm_id)


class TestCredits(BalanceTest):
    def test_update_repayment(self):
        # row format is (order, quantity, price, discount_pct)
        product = ProductBuilder()
        # Now we split orders into invoices only by client class (not EVERY row
        # in its invoice, each order must have its own client (all sublcients
        # of one agency)
        agency = ClientBuilder(is_agency=1, name="Test agency").build(self.session).obj
        clients = [ClientBuilder(name="Test client", agency=agency).build(self.session).obj for i in xrange(10)]

        rows = [BasketItemBuilder(order=OrderBuilder(product=product,
                                                     client=clients[i], agency=agency),
                                  quantity=(i * 10)) for i in range(1, 10)]

        b_request = RequestBuilder(basket=BasketBuilder(client=agency, rows=rows))

        b_person = PersonBuilder(client=agency, type='ph',
                                 operator_uid=self.session.oper_id).build(self.session)
        request = b_request.build(self.session).obj

        product.build(self.session)

        b_contract = ContractBuilder(
            firm=1,
            client=agency,
            person=b_person,
            commission=1,
            payment_type=3,
            payment_term=15,
            credit_limit={
                product.obj.activity_type.id: '9' * 20,
            },
            services={7},
            is_signed=datetime.datetime.now(),
            calc_defermant=1,
        )
        b_contract.build(self.session)

        coreobj = core.Core(self.session)
        invoices = coreobj.pay_on_credit(
            request_id=request.id,
            paysys_id=YA_MONEY_PAYSYS_ID,
            person_id=b_person.obj.id,
            contract_id=b_contract.obj.id)

        log.debug('invoices: %s' % len(invoices))
        # Just to be on the safe side
        invoices = sorted(invoices, key=lambda o: o.id)
        rep_request = coreobj.create_repayment_request(
            dt=datetime.datetime.now(),
            invoices=invoices)
        #        self.session.add(rep_request)
        self.session.flush()
        rep_invoice = InvoiceFactory.create(request=rep_request,
                                            paysys=self.session.query(Paysys).get(YA_MONEY_PAYSYS_ID),
                                            person=b_person.obj,
                                            credit=1, status_id=5, temporary=False)
        rep_invoice.update()
        self.session.add(rep_invoice)
        self.session.flush()

        invoices[0].invoice_orders[0].order.calculate_consumption(dt=datetime.datetime.today(),
                                                                  shipment_info={'Bucks': 1000, 'Clicks': 1000,
                                                                                 'Days': 1000,
                                                                                 'Shows': 1000, 'Units': 1000})

        act = rep_invoice.generate_act(force=1)[0]
        log.debug(act.payment_term_dt)

        sums = [inv.effective_sum for inv in invoices]
        # Sum to cover leading 3 invoices
        sum123 = sum(sums[:3], 0)
        # update_repayment is called in turn_on
        InvoiceTurnOn(rep_invoice, sum=sum123, manual=True).do()
        rep_invoice.update_repayment()
        for inv in invoices[:3]:
            self.assert_(inv.deferpay.status_id == 1)
        for inv in invoices[3:]:
            self.assert_(inv.deferpay.status_id == 0)
        # One more receipt for 1.5 more orders
        rep_invoice.create_receipt(sum_=sums[3] + sums[4] / 2)
        rep_invoice.update_repayment()
        for inv in invoices[:4]:
            self.assert_(inv.deferpay.status_id == 1)
        for inv in invoices[4:]:
            self.assert_(inv.deferpay.status_id == 0)
        # Negative recept for 3 invoices
        rep_invoice.create_receipt(sum_=-sums[1] / 2 - sums[2] - sums[3] - sums[4] / 2)
        rep_invoice.update_repayment()
        for inv in invoices[:1]:
            self.assert_(inv.deferpay.status_id == 1)
        for inv in invoices[1:]:
            self.assert_(inv.deferpay.status_id == 0)

    def test_markups(self):
        b_product = ProductBuilder(price=1, tax=1, manual_discount=1)
        markups = [
            ProductMarkupBuilder(markup=MarkupBuilder(), product=b_product, pct=30,
                                 dt=datetime.datetime.now() - datetime.timedelta(1)),
            ProductMarkupBuilder(markup=MarkupBuilder(), product=b_product, pct=25,
                                 dt=datetime.datetime.now() - datetime.timedelta(1)),
            ProductMarkupBuilder(markup=MarkupBuilder(), product=b_product, pct=15,
                                 dt=datetime.datetime.now() - datetime.timedelta(1)),
        ]
        markups = [p.build(self.session).obj.markup.id for p in markups]

        b_request = RequestBuilder(basket=BasketBuilder(rows=[
            BasketItemBuilder(order=OrderBuilder(product=b_product, client=ClientBuilder()), quantity=100,
                              markups=markups[:2])]))
        request = b_request.build(self.session).obj

        b_person = PersonBuilder(client=request.client, type='ph',
                                 operator_uid=self.session.oper_id).build(self.session)
        b_contract = ContractBuilder(
            client=request.client,
            person=b_person,
            commission=0,
            payment_type=3,
            credit_limit={
                b_product.activity_type.id: '9' * 20,
            },
            services={7},
            is_signed=datetime.datetime.now(),
        )
        b_contract.build(self.session)

        coreobj = core.Core(self.session)
        invoices = coreobj.pay_on_credit(
            request_id=request.id,
            paysys_id=YA_MONEY_PAYSYS_ID,
            person_id=b_person.obj.id,
            contract_id=b_contract.obj.id)
        # Just to be on the safe side
        invoices = sorted(invoices, key=lambda o: o.id)
        rep_request = coreobj.create_repayment_request(
            dt=datetime.datetime.now(),
            invoices=invoices)
        self.session.flush()
        rep_invoice = InvoiceFactory.create(request=rep_request,
                                            paysys=self.session.query(Paysys).get(YA_MONEY_PAYSYS_ID),
                                            person=b_person.obj,
                                            credit=1, status_id=5, temporary=False)
        rep_invoice.update()
        self.session.add(rep_invoice)
        self.session.flush()
        self.assertEqual(len(invoices), 1)
        self.assertEqual(invoices[0].total_sum, D('162.50'))
        self.assertEqual(rep_invoice.total_sum, D('162.50'))

    def test_season_coeff_and_discount(self):
        # 100 pts of product, 1 rur each, with season coeff 140% must cost 140
        # roubles in both fictive invoice and repayment invoice
        # Also, set product.manual_discount to 1 and request.u_discount_pct and
        # make sure it goes to both invoices


        b_product = ProductBuilder(price=1, tax=1, manual_discount=1)
        b_request = RequestBuilder(basket=BasketBuilder(
            rows=[BasketItemBuilder(order=OrderBuilder(product=b_product, client=ClientBuilder()), quantity=100)]))
        request = b_request.build(self.session).obj
        GenericBuilder(
            ProdSeasonCoeff,
            dt=datetime.datetime.now() - datetime.timedelta(1),
            finish_dt=datetime.datetime.now() + datetime.timedelta(1),
            product=b_product,
            coeff=D('140.20')).build(self.session)

        b_person = PersonBuilder(client=request.client, type='ph',
                                 operator_uid=self.session.oper_id).build(self.session)
        b_contract = ContractBuilder(
            client=request.client,
            person=b_person,
            commission=0,
            payment_type=3,
            credit_limit={
                b_product.activity_type.id: '9' * 20,
            },
            services={7},
            is_signed=datetime.datetime.now(),
        )
        b_contract.build(self.session)

        coreobj = core.Core(self.session)
        invoices = coreobj.pay_on_credit(
            request_id=request.id,
            paysys_id=YA_MONEY_PAYSYS_ID,
            person_id=b_person.obj.id,
            contract_id=b_contract.obj.id)
        # Just to be on the safe side
        invoices = sorted(invoices, key=lambda o: o.id)
        rep_request = coreobj.create_repayment_request(
            dt=datetime.datetime.now(),
            invoices=invoices)
        self.session.flush()
        rep_invoice = InvoiceFactory.create(request=rep_request,
                                            paysys=self.session.query(Paysys).get(YA_MONEY_PAYSYS_ID),
                                            person=b_person.obj,
                                            credit=1, status_id=5, temporary=False)
        rep_invoice.update()
        self.session.add(rep_invoice)
        self.session.flush()
        self.assertEqual(len(invoices), 1)
        self.assertEqual(invoices[0].total_sum, D('140.20'))
        self.assertEqual(rep_invoice.total_sum, D('140.20'))

    def test_usa_postpaid(self):
        pc = PayOnCreditCase(self.session)
        prod = pc.get_product_hierarchy(media_discount=7)
        prod[1]._other.price.b.tax = 0
        prod[1]._other.price.b.price = 1000
        b_client = ClientBuilder()
        b_person = PersonBuilder(client=b_client, type='usu').build(self.session)
        cont = pc.get_contract(
            client=b_client,
            person=b_person,
            commission=7,
            payment_type=3,
            credit_type=2,
            payment_term=30,
            services={7},
            is_signed=1,
            firm=4,
            currency=840,
            credit_limit_single=10000000,
        )

        def pay_on_credit(lst):
            basket = BasketBuilder(rows=[BasketItemBuilder(
                order=OrderBuilder(product=prod[1], client=c[0], agency=c[0].agency, service=Getter(Service, 7)),
                quantity=c[1]) for c in lst])
            return pc.pay_on_credit(basket, cont, paysys=self.session.query(Paysys).getone(1028))

        quantities = [10, 10, 20]
        iter_quantities = iter(quantities)

        invoices = pay_on_credit([(cont.client, next(iter_quantities))])
        inv = invoices[0]
        sm = ut.dsum([ut.round00(quantity * 1000 / inv.currency_rate) for quantity in quantities])
        invoices = pay_on_credit([(cont.client, next(iter_quantities)), (cont.client, next(iter_quantities))])
        assert invoices[0] == inv
        self.assertEqual(inv.consume_sum, sm)

        rate_dt = ut.trunc_date(datetime.datetime.today())

        CurrencyRate.set_real_currency_rate(self.session, inv.currency, D(35), rate_dt=rate_dt,
                                            base_cc=None, rate_src_id=None, selling_rate=None)
        # cr = CurrencyRate(inv.currency, rate_dt, rate=D(35), id=get_big_number())
        # self.session.add(cr)
        invoices = pay_on_credit([(cont.client, 10)])
        assert invoices[0] == inv
        assert inv.consume_sum - sm == ut.round00(10 * 1000 / D(35))
        o1, o2, o3, o4 = [cons.order for cons in inv.consumes]
        o3.transfer(o4)
        assert len(inv.consumes) == 5
        assert inv.consumes[-1].current_sum == inv.consumes[-3].consume_sum

        TransferMultipleMedium(
            self.session,
            [{'ServiceID': o2.service_id, 'ServiceOrderID': o2.service_order_id, 'QtyNew': o2.consume_qty - 1,
              'QtyOld': o2.consume_qty}],
            [{'ServiceID': o4.service_id, 'ServiceOrderID': o4.service_order_id, 'QtyDelta': 1}]
        ).do()

        self.session.flush()

    def test_CreateTransferMultipleNDS(self):
        # More of a smoke test, really
        client = ClientBuilder()

        b_order = OrderBuilder(product=Getter(Product, DIRECT_PRODUCT_ID), client=client)
        b_order_dst = [OrderBuilder(product=Getter(Product, DIRECT_PRODUCT_ID), client=client) for x in range(7)]
        b_request = RequestBuilder(basket=BasketBuilder(rows=[
            BasketItemBuilder(order=b_order, quantity=20)]  # +
            # [BasketItemBuilder(order=b, quantity=1) for b in b_order_dst]
        ))
        person = PersonBuilder(client=b_request.b.basket.b.client, type='ph')
        inv = InvoiceBuilder(person=person, paysys=Getter(Paysys, 1001), request=b_request).build(self.session).obj
        InvoiceTurnOn(inv, manual=True).do()

        # inv2 = InvoiceBuilder(paysys=Getter(Paysys, 1018), request=b_request).build(self.session).obj
        # inv2.internal_rate /= 2
        # inv2.currency_rate /= 2
        # InvoiceTurnOn(inv2, manual=True).do()


        src_order = b_order.obj
        dst_orders = [(b.build(self.session), b.obj)[1] for b in b_order_dst]

        inv.nds_pct = D('20.5')
        self.assertEqual(inv.nds, 1)

        self.session.flush()

        props = [(p + 1) / 2 * p for p in range(1, 1 + len(b_order_dst))]
        props = [ut.rounded_delta(sum(props), sum(props[:p]), sum(props[:p + 1]), D(20)) for p in xrange(len(props))]
        src_list = [
            {
                'ServiceID': src_order.service_id,
                'ServiceOrderID': src_order.service_order_id,
                'QtyOld': src_order.consume_qty,
                'QtyNew': 0
            },
        ]
        dst_list = [
            {
                'ServiceID': dst_orders[p].service_id,
                'ServiceOrderID': dst_orders[p].service_order_id,
                'QtyDelta': props[p]
            }
            for p in xrange(len(dst_orders))
            ]
        TransferMultipleMedium(self.session, src_list, dst_list).do()

        assert [(o.consumes[0].current_sum / o.consumes[0].current_qty) for o in dst_orders] == [D(30)] * len(props), \
            ([(o.consumes[0].current_sum / o.consumes[0].current_qty) for o in dst_orders], [D(30)] * len(props))


@pytest.mark.usefixtures("session")
class TestTransferMultiple(object):
    def assertEqual(self, first, second):
        assert first == second

    def perform_integrity_checks(self, invoice):
        invoice.session.flush()
        invoice.session.refresh(invoice)
        for con in invoice.consumes:
            self.assertEqual(con.act_qty,
                             sum(row.act_qty for row in con.acttranses
                                 if row.act and row.act.hidden < 4))
            self.assertEqual(con.act_sum,
                             sum(row.act_sum for row in con.acttranses))
        for act in invoice.acts:
            self.assertEqual(act.amount,
                             sum(row.amount for row in act.rows))
            self.assertEqual(act.amount_nds,
                             sum(row.amount_nds for row in act.rows))
            self.assertEqual(act.amount_nsp,
                             sum(row.amount_nsp for row in act.rows))
            self.assertEqual(act.paid_amount,
                             sum(row.paid_amount for row in act.rows))

        self.assertEqual(invoice.total_act_sum,
                         sum(act.amount for act in invoice.acts if act.hidden < 4))

    def test_internal_autoru_transfer(self):
        """
        https://st.yandex-team.ru/BALANCE-19257
        """

        unit = Getter(ProductUnit, 796).build(self.session).obj  # штуки
        product_1 = ProductBuilder(unit=unit, price=6000, engine_id=99).build(self.session).obj
        product_2 = ProductBuilder(unit=unit, price=6000, engine_id=99).build(self.session).obj

        client = ClientBuilder()
        src_order = OrderBuilder(
            product=product_1,
            client=client,
            service_id=product_1.engine_id
        )
        dst_order = OrderBuilder(
            product=product_2,
            client=client,
            service_id=product_2.engine_id
        ).build(self.session).obj

        b_request = RequestBuilder(basket=BasketBuilder(rows=[
            BasketItemBuilder(order=src_order, quantity=20)]
        ))

        inv = InvoiceBuilder(paysys=Getter(Paysys, 1003),
                             request=b_request).build(self.session).obj
        InvoiceTurnOn(inv, manual=True).do()
        src_order = src_order.obj

        src_list = [{'ServiceID': src_order.service_id,
                     'ServiceOrderID': src_order.service_order_id,
                     'QtyOld': src_order.consume_qty,
                     'QtyNew': 0}, ]

        dst_list = [{'ServiceID': o.service_id,
                     'ServiceOrderID': o.service_order_id,
                     'QtyDelta': 1} for o in [src_order, dst_order]]

        TransferMultipleMedium(
            self.session,
            src_list, dst_list
        ).do()

    @pytest.mark.parametrize('split_act_creation', [True, False])
    def test_zero_consume_transfer(self, split_act_creation):
        """
        https://st.yandex-team.ru/BALANCE-20458
        """
        all_qty = D(20)
        product = Getter(Product, DIRECT_PRODUCT_ID).build(self.session).obj
        b_order_src = OrderBuilder(product=product)
        b_order_dst = OrderBuilder(product=product, client=b_order_src.client)

        b_request = RequestBuilder(basket=BasketBuilder(rows=[
            BasketItemBuilder(order=b_order_src, quantity=all_qty)]
        ))

        invoice = InvoiceBuilder(paysys=Getter(Paysys, 1003),
                                 request=b_request).build(self.session).obj
        InvoiceTurnOn(invoice, manual=True).do()

        src_order = b_order_src.build(self.session).obj
        dst_order = b_order_dst.build(self.session).obj

        micro_item = D('0.000001')
        src_list = [{'ServiceID': src_order.service_id,
                     'ServiceOrderID': src_order.service_order_id,
                     'QtyOld': src_order.consume_qty,
                     'QtyNew': src_order.consume_qty - micro_item}]

        dst_list = [{'ServiceID': dst_order.service_id,
                     'ServiceOrderID': dst_order.service_order_id,
                     'QtyDelta': 1}]

        TransferMultipleMedium(self.session, src_list, dst_list).do()
        self.assertEqual(len(src_order.consumes), 1)
        self.assertEqual(len(dst_order.consumes), 1)

        src_consume = src_order.consumes[0]
        dst_consume = dst_order.consumes[0]
        self.assertEqual(src_consume.consume_qty - src_consume.current_qty, micro_item)
        self.assertEqual(dst_consume.current_qty, micro_item)

        # актим исходный заказ на микрофишку. Цель - получить внутренний акт с нулевой суммой
        pr_compl = action_completion.ProcessCompletions(src_order,
                                                        on_dt=datetime.datetime.now() - datetime.timedelta(days=2))
        pr_compl.calculate_consumption(
            shipment_info={src_order.shipment.shipment_type: micro_item}, stop=0
        )
        self.assertEqual(src_consume.completion_qty, micro_item)

        acts = ActAccounter(
            invoice.client, backdate=self.session.now(), invoices=[invoice.id], dps=[], force=1,
            split_act_creation=split_act_creation
        ).do(skip_cut_agava=True)
        self.perform_integrity_checks(invoice)

        acted_qty = sum(row.act_qty for act in acts for row in act.rows)

        self.assertEqual(src_consume.act_qty, micro_item)
        self.assertEqual(len(acts), 1)
        self.assertEqual(acted_qty, micro_item)
        self.assertEqual(all(row.netting for act in acts for row in act.rows), True)
        self.assertEqual(all(act.__class__ == ActInternal for act in acts), True)

        # делаем акт с нулевой и ненулевой строчками
        ##        self.assertEqual(all(row.netting for row in acts[0].rows), True)

        pr_compl.calculate_consumption({src_order.shipment_type: src_order.consume_qty}, 1)
        pr_compl = action_completion.ProcessCompletions(dst_order,
                                                        on_dt=datetime.datetime.now() - datetime.timedelta(days=2))
        pr_compl.calculate_consumption({dst_order.shipment_type: dst_order.consume_qty}, 1)
        self.assertEqual(src_consume.completion_qty, all_qty - micro_item)
        self.assertEqual(dst_consume.completion_qty, micro_item)

        acts = ActAccounter(
            invoice.client, backdate=self.session.now(), invoices=[invoice.id], dps=[], force=0,
            split_act_creation=split_act_creation
        ).do(skip_cut_agava=True)
        self.perform_integrity_checks(invoice)

        self.assertEqual(len(acts), 1)
        self.assertEqual(src_consume.act_qty, all_qty - micro_item)

        src_order_act_rows = [row for act in acts for row in act.rows if row.consume == src_consume]
        dst_order_act_rows = [row for act in acts for row in act.rows if row.consume == dst_consume]
        self.assertEqual(sum(row.act_qty for row in src_order_act_rows), all_qty - micro_item - acted_qty)
        self.assertEqual(sum(row.act_qty for row in dst_order_act_rows), acted_qty)

        self.assertEqual(all(row.netting for row in dst_order_act_rows), True)
        self.assertEqual(any(row.netting for row in src_order_act_rows), False)
        self.assertEqual(all(act.__class__ == Act for act in acts), True)
