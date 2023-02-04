# -*- coding: utf-8 -*-

import datetime
import decimal

from balance import mapper
import balance.muzzle_util as ut
import balance.actions.acts as a_a
from balance.actions.invoice_turnon import InvoiceTurnOn
from balance.constants import *

from tests.base import BalanceTest
from tests import object_builder as ob

D = decimal.Decimal
YA_MONEY_PAYSYS_ID = 1000


class TestActs(BalanceTest):
    """Test for acts creation"""
    split_act_creation = None

    def perform_integrity_checks(self, invoice):
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

    def test_amounts(self):
        # 1 order, multiple shipments, check amounts
        product = ob.ProductBuilder()
        product._other.price.b.price = 30

        # Direct has 10% bonuses this xmas, so let's make it any other service
        order_b = ob.OrderBuilder(product=product, service=ob.Getter(mapper.Service, 67))
        row = [ob.BasketItemBuilder(order=order_b, quantity=100)] # order, qty, price, discount_pct
        request_b = ob.RequestBuilder(basket=ob.BasketBuilder(rows = row, client = order_b.client))
        yesterday = datetime.datetime.now() - datetime.timedelta(1)
        day_before_yesterday = datetime.datetime.now() - datetime.timedelta(2)

        invoice = ob.InvoiceBuilder(request = request_b,
                                 paysys = ob.Getter(mapper.Paysys, YA_MONEY_PAYSYS_ID),
                                 internal_rate = 15, dt=day_before_yesterday - datetime.timedelta(1)
                ).build(self.session).obj

        InvoiceTurnOn(invoice, sum=invoice.effective_sum, manual=True).do()

        # Shipments: 30 day before yesterday, 40 yesterday, 30 today
        shipment_qty_2days_ago = 30
        shipment_qty_yesterday = 70
        shipment_qty_today = 100
        order_b.obj.calculate_consumption(dt = day_before_yesterday, stop = 1,
            shipment_info = {'Bucks': shipment_qty_2days_ago})
        order_b.obj.calculate_consumption(dt = yesterday, stop = 1,
            shipment_info = {'Bucks': shipment_qty_yesterday})
        order_b.obj.calculate_consumption(dt = datetime.datetime.today(), stop = 1,
            shipment_info = {'Bucks': shipment_qty_today})
        invoice = self.session.query(mapper.Invoice).get(invoice.id)

        # First test: day before yesterday
        acts = invoice.generate_act(force = 0, backdate = day_before_yesterday)
        # As 'force=0' means only generate act if the whole invoice's quantity
        # has been completed, no act should have been created
        self.assert_(not bool(acts))
        self.perform_integrity_checks(invoice)
        cross_rate = 30 / invoice.internal_rate # invoice.internal_rate should be
                                                # 1 now
        # Second test: day before yesterday, forced
        self.session.clear_cache()
        acts = invoice.generate_act(force = 1, backdate = day_before_yesterday)

        self.assert_(sum(len(act.rows) for act in acts) == 1)
        self.assertEqual(ut.dsum(act.rows[0].act_qty for act in acts), shipment_qty_2days_ago)
        self.assertEqual(ut.dsum(act.rows[0].act_sum / cross_rate for act in acts), shipment_qty_2days_ago)
        self.assertEqual(ut.dsum(act.rows[0].amount for act in acts), shipment_qty_2days_ago * 30)
        self.perform_integrity_checks(invoice)

        # Third test: defaults
        shipment_qty_delta = shipment_qty_today - shipment_qty_2days_ago
        self.session.clear_cache()
        acts = invoice.generate_act()
        self.assertEqual(sum(len(act.rows) for act in acts), 1)
        self.assertEqual(ut.dsum(act.rows[0].act_qty for act in acts), shipment_qty_delta)
        self.assertEqual(ut.dsum(act.rows[0].act_sum / cross_rate for act in acts), shipment_qty_delta)
        self.assertEqual(ut.dsum(act.rows[0].amount for act in acts), shipment_qty_delta * 30)
        self.perform_integrity_checks(invoice)

    def test_multiple_orders(self):
        # Create multi-row act, check amounts
        client = ob.ClientBuilder()
        product = ob.ProductBuilder()
        product._other.price.b.tax = 1
        product._other.price.b.price = 100 * 30

        rows = [ob.BasketItemBuilder(order=ob.OrderBuilder(
                        product=product,
                        client=client,
                        service_id=7),
                    quantity=400)
               for i in (1, 2, 3)]
        second_order = rows[0].order.obj
        rows.sort(key=lambda r: r.order.obj == second_order)
        request_b = ob.RequestBuilder(basket=ob.BasketBuilder(rows = rows, client = client))
        invoice = ob.InvoiceBuilder(request = request_b,
                                 paysys = ob.Getter(mapper.Paysys, YA_MONEY_PAYSYS_ID)
                ).build(self.session).obj
        InvoiceTurnOn(invoice, sum=invoice.effective_sum, manual=True).do()
        rows[0].order.obj.calculate_consumption(
            stop=1,
            shipment_info={'Bucks': 200},
            dt=datetime.datetime.today()
        )
        rows[1].order.obj.calculate_consumption(
            stop=1,
            shipment_info={'Bucks': 100},
            dt=datetime.datetime.today()
        )
        # orders: 0: 200/200, 1: 100/200, 2: 0/2000

        invoice = self.session.query(mapper.Invoice).get(invoice.id)
        self.session.clear_cache()
        acts = invoice.generate_act(force=1)
        rows_for_acts = [sorted(list(act.rows), key=lambda r: r.act_qty) for act in acts]
        self.assert_(sum(len(act_rows) for act_rows in rows_for_acts) == 2)
        self.assertEqual(ut.dsum(act_rows[0].act_qty for act_rows in rows_for_acts), 100)
        self.assertEqual(ut.dsum(act_rows[1].act_qty for act_rows in rows_for_acts), 200)
        self.perform_integrity_checks(invoice)


class ActsSplitActCreationBase(object):

    split_act_creation = None

    def test_trp(self):
        order = ob.OrderBuilder(
            service=ob.Getter(mapper.Service, 7),
            product=ob.ProductBuilder(media_discount=7)
        ).build(self.session).obj
        row = [ob.BasketItemBuilder(order=order, quantity=900)] # order, qty, price, discount_pct
        request_b = ob.RequestBuilder(basket=ob.BasketBuilder(rows=row, client=order.client))
        invoice = ob.InvoiceBuilder(request=request_b,
                                 internal_rate=15,
                                 credit=2
                                 ).build(self.session).obj

        InvoiceTurnOn(invoice, sum=invoice.effective_sum, manual=True).do()

        order.calculate_consumption(dt=datetime.datetime.today() - datetime.timedelta(days=2), stop=0,
                                    shipment_info={'Money': 800, 'Shows': 0, 'Clicks': 0, 'Units': 0, 'Bucks': 800})
        order.calculate_consumption(dt=datetime.datetime.today() - datetime.timedelta(days=1), stop=0,
                                    shipment_info={'Money': 46, 'Shows': 0, 'Clicks': 0, 'Units': 0, 'Bucks': 46})
        order.calculate_consumption(dt=datetime.datetime.today(), stop=0,
                                    shipment_info={'Money': 50, 'Shows': 0, 'Clicks': 0, 'Units': 0, 'Bucks': 50})
        self.session.flush()
        ac = a_a.ActAccounter(order.client, a_a.ActMonth(for_month=datetime.datetime.now()), force=1,
                              split_act_creation=self.split_act_creation)
        acts = ac.do()

    def test_batch_completions(self):
        session = self.session

        client = ob.ClientBuilder()
        b_order = ob.OrderBuilder(product = ob.Getter(mapper.Product, DIRECT_PRODUCT_ID), client = client)
        b_order1 = ob.OrderBuilder(product = ob.Getter(mapper.Product, DIRECT_PRODUCT_ID), client = client)
        b_request = ob.RequestBuilder(basket=ob.BasketBuilder(rows = [
            ob.BasketItemBuilder(order=b_order, quantity=100000000),
            ob.BasketItemBuilder(order=b_order1, quantity=100000000)]))
        b_invoice = ob.InvoiceBuilder(request = b_request)
        invoice = b_invoice.build(session).obj

        order = b_order.obj

        InvoiceTurnOn(invoice, manual=True).do()
        now = datetime.datetime.now()
        order.calculate_consumption(now - datetime.timedelta(days=3), {'Bucks': 30000})
        order.calculate_consumption(now - datetime.timedelta(days=1), {'Bucks': 50000})
        session.flush()

        acts1 = invoice.generate_act(backdate=now - datetime.timedelta(3))

        act_month = a_a.ActMonth()
        act_accounter = a_a.ActAccounter(invoice.client, act_month, force=0, split_act_creation=self.split_act_creation)
        acts2 = act_accounter.do()

        act_enq = a_a.ActEnqueuer(session, act_month, force=1)
        act_enq.get_deferpays()


class TestBalanceSplitActCreationTrue(ActsSplitActCreationBase, BalanceTest):
    split_act_creation = True


class TestBalanceSplitActCreationFalse(ActsSplitActCreationBase, BalanceTest):
    split_act_creation = False
