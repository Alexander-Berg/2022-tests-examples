# -*- coding: utf-8 -*-

import datetime as dt
import decimal

import pytest

from balance import mapper
from balance.actions.consumption import reverse_consume
from balance.actions.process_completions import ProcessCompletions

from autodasha.core.logic import transfers

from tests.autodasha_tests.common import db_utils

# TODO: тесты на корректные локи при пачковой обработке


@pytest.fixture(autouse=True)
def setup_config(session):
    prev = session.config.get('CONSUMPTION_NEGATIVE_REVERSE_ALLOWED')
    session.config.__dict__['CONSUMPTION_NEGATIVE_REVERSE_ALLOWED'] = 0
    yield
    session.config.__dict__['CONSUMPTION_NEGATIVE_REVERSE_ALLOWED'] = prev


class AbstractTransferTestGroup(object):
    @staticmethod
    def _make_acts(session, invoice, order, qty, restore=True):
        prev_qty = order.completion_qty

        ProcessCompletions(order).calculate_consumption({order.shipment_type: qty})
        session.flush()

        invoice.generate_act(backdate=dt.datetime.now(), force=1)
        session.flush()

        if restore:
            ProcessCompletions(order).calculate_consumption({order.shipment_type: prev_qty})
            session.flush()

    @staticmethod
    def _consume_order(invoice, order, qty, pct=0, price=None):
        return invoice.transfer(order, 2, qty, skip_check=True, discount_pct=pct, price_obj=price).consume

    @staticmethod
    def _complete_order(session, order, qty):
        ProcessCompletions(order).calculate_consumption({order.shipment_type: qty})
        session.flush()

    @staticmethod
    def _reverse_batches(res_iter):
        consumes_batches = []
        for batch in res_iter:
            consumes_batches.append(batch)
            for q, qty in batch:
                reverse_consume(q, None, qty)

        return consumes_batches

    @staticmethod
    def _get_invoice(session, orders_num, product_id=1475, orders_products=None):
        client, person = db_utils.create_client_person(session)
        if orders_products is None:
            _orders_products = [product_id] * orders_num
        else:
            _orders_products = orders_products

        rows = []
        for _product_id in _orders_products:
            rows.append((db_utils.create_order(session, client, product_id=_product_id), 1))

        invoice = db_utils.create_invoice(session, client, rows, person=person)

        return invoice


class TestGetFreeConsumes(AbstractTransferTestGroup):

    def test_base(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)

        res = transfers.get_free_consumes(session, invoice)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(10)), (q3, decimal.Decimal(10))]

    def test_wo_lock(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)

        # запрос for update не дружит с group_by e.t.c. - надо проверять оба случая
        res = transfers.get_free_consumes(session, invoice, lock=False)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(10)), (q3, decimal.Decimal(10))]

    def test_filter(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)

        flt = (mapper.Consume.parent_order_id == order_1.id)

        res = transfers.get_free_consumes(session, invoice, free_consumes_filter=flt)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(10))]

    def test_w_completed(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)

        self._complete_order(session, order_1, 15)
        self._complete_order(session, order_2, 7)

        res = transfers.get_free_consumes(session, invoice, ignore_completed=True)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(10)), (q3, decimal.Decimal(10))]

    def test_wo_completed(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)

        self._complete_order(session, order_1, 15)
        self._complete_order(session, order_2, 7)

        res = transfers.get_free_consumes(session, invoice, ignore_completed=False)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(5)), (q3, decimal.Decimal(3))]

    def test_all_completed_wo_completed(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        self._consume_order(invoice, order_1, 10)
        self._consume_order(invoice, order_1, 10)
        self._consume_order(invoice, order_2, 10)

        self._complete_order(session, order_1, 20)
        self._complete_order(session, order_2, 10)

        res = transfers.get_free_consumes(session, invoice, ignore_completed=False)

        consumes, = res
        assert consumes == []

    def test_acts(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)
        q4 = self._consume_order(invoice, order_2, 10)

        self._make_acts(session, invoice, order_1, 11, False)
        self._make_acts(session, invoice, order_2, 4, False)

        self._complete_order(session, order_1, 15)
        self._complete_order(session, order_2, 7)

        res = transfers.get_free_consumes(session, invoice, ignore_completed=True)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(9)), (q4, decimal.Decimal(10)), (q3, decimal.Decimal(6))]


# class TestGetFreeConsumesBatched(AbstractTransferTestGroup):
#
#     def test_base(self, session):
#         invoice = self._get_invoice(session, 2)
#         order_1, order_2 = [row.order for row in invoice.rows]
#
#         q1 = self._consume_order(invoice, order_1, 10)
#         q2 = self._consume_order(invoice, order_1, 10)
#         q3 = self._consume_order(invoice, order_2, 10)
#
#         res = transfers.get_free_consumes_batched(session, invoice)
#
#         consumes, = self._reverse_batches(res)
#         assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(10)), (q3, decimal.Decimal(10))]
#
#     def test_wo_lock(self, session):
#         invoice = self._get_invoice(session, 2)
#         order_1, order_2 = [row.order for row in invoice.rows]
#
#         q1 = self._consume_order(invoice, order_1, 10)
#         q2 = self._consume_order(invoice, order_1, 10)
#         q3 = self._consume_order(invoice, order_2, 10)
#
#         # запрос for update не дружит с group_by e.t.c. - надо проверять оба случая
#         res = transfers.get_free_consumes_batched(session, invoice, lock=False)
#
#         consumes, = self._reverse_batches(res)
#         assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(10)), (q3, decimal.Decimal(10))]
#
#     def test_filter(self, session):
#         invoice = self._get_invoice(session, 2)
#         order_1, order_2 = [row.order for row in invoice.rows]
#
#         q1 = self._consume_order(invoice, order_1, 10)
#         q2 = self._consume_order(invoice, order_1, 10)
#         q3 = self._consume_order(invoice, order_2, 10)
#
#         flt = (mapper.Consume.parent_order_id == order_1.id)
#
#         res = transfers.get_free_consumes_batched(session, invoice, free_consumes_filter=flt)
#
#         consumes, = self._reverse_batches(res)
#         assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(10))]
#
#     def test_w_completed(self, session):
#         invoice = self._get_invoice(session, 2)
#         order_1, order_2 = [row.order for row in invoice.rows]
#
#         q1 = self._consume_order(invoice, order_1, 10)
#         q2 = self._consume_order(invoice, order_1, 10)
#         q3 = self._consume_order(invoice, order_2, 10)
#
#         self._complete_order(session, order_1, 15)
#         self._complete_order(session, order_2, 7)
#
#         res = transfers.get_free_consumes_batched(session, invoice, ignore_completed=True)
#
#         consumes, = self._reverse_batches(res)
#         assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(10)), (q3, decimal.Decimal(10))]
#
#     def test_wo_completed(self, session):
#         invoice = self._get_invoice(session, 2)
#         order_1, order_2 = [row.order for row in invoice.rows]
#
#         q1 = self._consume_order(invoice, order_1, 10)
#         q2 = self._consume_order(invoice, order_1, 10)
#         q3 = self._consume_order(invoice, order_2, 10)
#
#         self._complete_order(session, order_1, 15)
#         self._complete_order(session, order_2, 7)
#
#         res = transfers.get_free_consumes_batched(session, invoice, ignore_completed=False)
#
#         consumes, = self._reverse_batches(res)
#         assert consumes == [(q2, decimal.Decimal(5)), (q3, decimal.Decimal(3))]
#
#     def test_all_completed_wo_completed(self, session):
#         invoice = self._get_invoice(session, 2)
#         order_1, order_2 = [row.order for row in invoice.rows]
#
#         self._consume_order(invoice, order_1, 10)
#         self._consume_order(invoice, order_2, 10)
#
#         self._complete_order(session, order_1, 10)
#         self._complete_order(session, order_2, 10)
#
#         res = list(transfers.get_free_consumes_batched(session, invoice, ignore_completed=False))
#
#         assert res == []
#
#     def test_acts(self, session):
#         invoice = self._get_invoice(session, 2)
#         order_1, order_2 = [row.order for row in invoice.rows]
#
#         q1 = self._consume_order(invoice, order_1, 10)
#         q2 = self._consume_order(invoice, order_1, 10)
#         q3 = self._consume_order(invoice, order_2, 10)
#         q4 = self._consume_order(invoice, order_2, 10)
#
#         self._make_acts(session, invoice, order_1, 11)
#         self._make_acts(session, invoice, order_2, 4)
#
#         self._complete_order(session, order_1, 15)
#         self._complete_order(session, order_2, 7)
#
#         res = transfers.get_free_consumes_batched(session, invoice, ignore_completed=True)
#
#         consumes, = self._reverse_batches(res)
#         assert consumes == [(q2, decimal.Decimal(9)), (q4, decimal.Decimal(10)), (q3, decimal.Decimal(6))]
#
#     def test_batch(self, session):
#         invoice = self._get_invoice(session, 2)
#         order_1, order_2 = [row.order for row in invoice.rows]
#
#         q1 = self._consume_order(invoice, order_1, 10)
#         q2 = self._consume_order(invoice, order_1, 10)
#         q3 = self._consume_order(invoice, order_2, 10)
#
#         res = transfers.get_free_consumes_batched(session, invoice, batch_size=2)
#
#         consumes_batches = self._reverse_batches(res)
#
#         assert len(consumes_batches) == 2
#         consumes_1, consumes_2 = consumes_batches
#         assert consumes_1 == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(10))]
#         assert consumes_2 == [(q3, decimal.Decimal(10))]
#
#     def test_batch_funky(self, session):
#         invoice = self._get_invoice(session, 2)
#         order_1, order_2 = [row.order for row in invoice.rows]
#
#         q1 = self._consume_order(invoice, order_1, 10)
#         q2 = self._consume_order(invoice, order_1, 10)
#         q3 = self._consume_order(invoice, order_2, 10)
#         q4 = self._consume_order(invoice, order_2, 10)
#
#         res = transfers.get_free_consumes_batched(session, invoice, batch_size=3)
#
#         consumes_batches = self._reverse_batches(res)
#
#         assert len(consumes_batches) == 2
#         consumes_1, consumes_2 = consumes_batches
#         assert consumes_1 == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(10)), (q4, decimal.Decimal(10))]
#         assert consumes_2 == [(q3, decimal.Decimal(10))]


# class TestGetFreeConsumesBatchedByOrders(AbstractTransferTestGroup):
#
#     def test_base(self, session):
#         invoice = self._get_invoice(session, 2)
#         order_1, order_2 = [row.order for row in invoice.rows]
#
#         q1 = self._consume_order(invoice, order_1, 10)
#         q2 = self._consume_order(invoice, order_1, 10)
#         q3 = self._consume_order(invoice, order_2, 10)
#
#         res = transfers.get_free_consumes_batched_by_orders(session, invoice)
#
#         consumes, = self._reverse_batches(res)
#         assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(10)), (q3, decimal.Decimal(10))]
#
#     def test_wo_lock(self, session):
#         invoice = self._get_invoice(session, 2)
#         order_1, order_2 = [row.order for row in invoice.rows]
#
#         q1 = self._consume_order(invoice, order_1, 10)
#         q2 = self._consume_order(invoice, order_1, 10)
#         q3 = self._consume_order(invoice, order_2, 10)
#
#         # запрос for update не дружит с group_by e.t.c. - надо проверять оба случая
#         res = transfers.get_free_consumes_batched_by_orders(session, invoice, lock=False)
#
#         consumes, = self._reverse_batches(res)
#         assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(10)), (q3, decimal.Decimal(10))]
#
#     def test_filter_orders(self, session):
#         invoice = self._get_invoice(session, 2)
#         order_1, order_2 = [row.order for row in invoice.rows]
#
#         q1 = self._consume_order(invoice, order_1, 10)
#         q2 = self._consume_order(invoice, order_1, 10)
#         q3 = self._consume_order(invoice, order_2, 10)
#
#         flt = (mapper.Order.id == order_1.id)
#
#         res = transfers.get_free_consumes_batched_by_orders(session, invoice, orders_filter=flt)
#
#         consumes, = self._reverse_batches(res)
#         assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(10))]
#
#     def test_filter_consumes(self, session):
#         invoice = self._get_invoice(session, 2)
#         order_1, order_2 = [row.order for row in invoice.rows]
#
#         q1 = self._consume_order(invoice, order_1, 10)
#         q2 = self._consume_order(invoice, order_1, 10)
#         q3 = self._consume_order(invoice, order_2, 10)
#
#         flt = (mapper.Consume.parent_order_id == order_1.id)
#
#         res = transfers.get_free_consumes_batched_by_orders(session, invoice, free_consumes_filter=flt)
#
#         consumes, = self._reverse_batches(res)
#         assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(10))]
#
#     def test_w_completed(self, session):
#         invoice = self._get_invoice(session, 2)
#         order_1, order_2 = [row.order for row in invoice.rows]
#
#         q1 = self._consume_order(invoice, order_1, 10)
#         q2 = self._consume_order(invoice, order_1, 10)
#         q3 = self._consume_order(invoice, order_2, 10)
#
#         self._complete_order(session, order_1, 15)
#         self._complete_order(session, order_2, 7)
#
#         res = transfers.get_free_consumes_batched_by_orders(session, invoice, ignore_completed=True)
#
#         consumes, = self._reverse_batches(res)
#         assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(10)), (q3, decimal.Decimal(10))]
#
#     def test_wo_completed(self, session):
#         invoice = self._get_invoice(session, 2)
#         order_1, order_2 = [row.order for row in invoice.rows]
#
#         q1 = self._consume_order(invoice, order_1, 10)
#         q2 = self._consume_order(invoice, order_1, 10)
#         q3 = self._consume_order(invoice, order_2, 10)
#
#         self._complete_order(session, order_1, 15)
#         self._complete_order(session, order_2, 7)
#
#         res = transfers.get_free_consumes_batched_by_orders(session, invoice, ignore_completed=False)
#
#         consumes, = self._reverse_batches(res)
#         assert consumes == [(q2, decimal.Decimal(5)), (q3, decimal.Decimal(3))]
#
#     def test_all_completed_wo_completed(self, session):
#         invoice = self._get_invoice(session, 2)
#         order_1, order_2 = [row.order for row in invoice.rows]
#
#         self._consume_order(invoice, order_1, 10)
#         self._consume_order(invoice, order_2, 10)
#
#         self._complete_order(session, order_1, 10)
#         self._complete_order(session, order_2, 10)
#
#         res = list(transfers.get_free_consumes_batched_by_orders(session, invoice, ignore_completed=False))
#
#         assert res == []
#
#     def test_acts(self, session):
#         invoice = self._get_invoice(session, 2)
#         order_1, order_2 = [row.order for row in invoice.rows]
#
#         q1 = self._consume_order(invoice, order_1, 10)
#         q2 = self._consume_order(invoice, order_1, 10)
#         q3 = self._consume_order(invoice, order_2, 10)
#         q4 = self._consume_order(invoice, order_2, 10)
#
#         self._make_acts(session, invoice, order_1, 11)
#         self._make_acts(session, invoice, order_2, 4)
#
#         self._complete_order(session, order_1, 15)
#         self._complete_order(session, order_2, 7)
#
#         res = transfers.get_free_consumes_batched_by_orders(session, invoice, ignore_completed=True)
#
#         consumes, = self._reverse_batches(res)
#         assert consumes == [(q2, decimal.Decimal(9)), (q4, decimal.Decimal(10)), (q3, decimal.Decimal(6))]
#
#     def test_batch(self, session):
#         invoice = self._get_invoice(session, 3)
#         order_1, order_2, order_3 = [row.order for row in invoice.rows]
#
#         q1 = self._consume_order(invoice, order_1, 10)
#         q2 = self._consume_order(invoice, order_1, 10)
#         q3 = self._consume_order(invoice, order_2, 10)
#         q4 = self._consume_order(invoice, order_2, 10)
#         q5 = self._consume_order(invoice, order_3, 10)
#
#         res = transfers.get_free_consumes_batched_by_orders(session, invoice, batch_size=2)
#
#         consumes_batches = self._reverse_batches(res)
#
#         assert len(consumes_batches) == 2
#         consumes_1, consumes_2 = consumes_batches
#         assert consumes_1 == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(10)),
#                               (q4, decimal.Decimal(10)), (q3, decimal.Decimal(10))]
#         assert consumes_2 == [(q5, decimal.Decimal(10))]


class TestGetFreeConsumesByOrdersQty(AbstractTransferTestGroup):

    def test_base(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)
        q4 = self._consume_order(invoice, order_2, 10)

        orders_data = [
            (order_1, 17),
            (order_2, 12)
        ]

        res = transfers.get_free_consumes_by_orders_qty(session, invoice, orders_data)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(7)),
                            (q4, decimal.Decimal(10)), (q3, decimal.Decimal(2))]

    def test_wo_lock(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)
        q4 = self._consume_order(invoice, order_2, 10)

        orders_data = [
            (order_1, 17),
            (order_2, 12)
        ]

        # запрос for update не дружит с group_by e.t.c. - надо проверять оба случая
        res = transfers.get_free_consumes_by_orders_qty(session, invoice, orders_data, lock=False)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(7)),
                            (q4, decimal.Decimal(10)), (q3, decimal.Decimal(2))]

    def test_eq_gt(self, session):
        invoice = self._get_invoice(session, 3)
        order_1, order_2, order_3 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)
        q4 = self._consume_order(invoice, order_2, 10)
        q5 = self._consume_order(invoice, order_3, 10)
        q6 = self._consume_order(invoice, order_3, 10)

        orders_data = [
            (order_1, 10),
            (order_2, 25),
            (order_3, 20)
        ]

        res = transfers.get_free_consumes_by_orders_qty(session, invoice, orders_data)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(10)),
                            (q4, decimal.Decimal(10)), (q3, decimal.Decimal(10)),
                            (q6, decimal.Decimal(10)), (q5, decimal.Decimal(10))]

    def test_w_completed(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)
        q4 = self._consume_order(invoice, order_2, 10)

        self._complete_order(session, order_1, 5)
        self._complete_order(session, order_2, 15)

        orders_data = [
            (order_1, 13),
            (order_2, 15)
        ]

        res = transfers.get_free_consumes_by_orders_qty(session, invoice, orders_data, ignore_completed=True)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(3)),
                            (q4, decimal.Decimal(10)), (q3, decimal.Decimal(5))]

    def test_wo_completed(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)
        q4 = self._consume_order(invoice, order_2, 10)

        self._complete_order(session, order_1, 5)
        self._complete_order(session, order_2, 15)

        orders_data = [
            (order_1, 13),
            (order_2, 15)
        ]

        res = transfers.get_free_consumes_by_orders_qty(session, invoice, orders_data, ignore_completed=False)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(3)),
                            (q4, decimal.Decimal(5))]

    def test_all_completed_wo_completed(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        self._consume_order(invoice, order_1, 10)
        self._consume_order(invoice, order_2, 10)

        self._complete_order(session, order_1, 10)
        self._complete_order(session, order_2, 10)

        orders_data = [
            (order_1, 7),
            (order_2, 7)
        ]

        res = transfers.get_free_consumes_by_orders_qty(session, invoice, orders_data, ignore_completed=False)

        consumes, = res
        assert consumes == []

    def test_acts(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)
        q4 = self._consume_order(invoice, order_2, 10)

        self._make_acts(session, invoice, order_1, 7)
        self._make_acts(session, invoice, order_2, 6)

        self._complete_order(session, order_1, 10)
        self._complete_order(session, order_2, 15)

        orders_data = [
            (order_1, 13),
            (order_2, 15)
        ]

        res = transfers.get_free_consumes_by_orders_qty(session, invoice, orders_data, ignore_completed=True)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(3)),
                            (q4, decimal.Decimal(10)), (q3, decimal.Decimal(4))]


class TestGetFreeConsumesByOrdersSum(AbstractTransferTestGroup):

    def test_base(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)
        q4 = self._consume_order(invoice, order_2, 10)

        orders_data = [
            (order_1, 17 * 30),
            (order_2, 12 * 30)
        ]

        res = transfers.get_free_consumes_by_orders_sum(session, invoice, orders_data)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(7)),
                            (q4, decimal.Decimal(10)), (q3, decimal.Decimal(2))]

    def test_wo_lock(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)
        q4 = self._consume_order(invoice, order_2, 10)

        orders_data = [
            (order_1, 17 * 30),
            (order_2, 12 * 30)
        ]

        # запрос for update не дружит с group_by e.t.c. - надо проверять оба случая
        res = transfers.get_free_consumes_by_orders_sum(session, invoice, orders_data, lock=False)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(7)),
                            (q4, decimal.Decimal(10)), (q3, decimal.Decimal(2))]

    def test_eq_gt(self, session):
        invoice = self._get_invoice(session, 3)
        order_1, order_2, order_3 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)
        q4 = self._consume_order(invoice, order_2, 10)
        q5 = self._consume_order(invoice, order_3, 10)
        q6 = self._consume_order(invoice, order_3, 10)

        orders_data = [
            (order_1, 10 * 30),
            (order_2, 25 * 30),
            (order_3, 20 * 30)
        ]

        res = transfers.get_free_consumes_by_orders_sum(session, invoice, orders_data)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(10)),
                            (q4, decimal.Decimal(10)), (q3, decimal.Decimal(10)),
                            (q6, decimal.Decimal(10)), (q5, decimal.Decimal(10))]

    def test_w_completed(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)
        q4 = self._consume_order(invoice, order_2, 10)

        self._complete_order(session, order_1, 5)
        self._complete_order(session, order_2, 15)

        orders_data = [
            (order_1, 13 * 30),
            (order_2, 15 * 30)
        ]

        res = transfers.get_free_consumes_by_orders_sum(session, invoice, orders_data, ignore_completed=True)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(3)),
                            (q4, decimal.Decimal(10)), (q3, decimal.Decimal(5))]

    def test_wo_completed(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)
        q4 = self._consume_order(invoice, order_2, 10)

        self._complete_order(session, order_1, 5)
        self._complete_order(session, order_2, 15)

        orders_data = [
            (order_1, 13 * 30),
            (order_2, 15 * 30)
        ]

        res = transfers.get_free_consumes_by_orders_sum(session, invoice, orders_data, ignore_completed=False)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(3)),
                            (q4, decimal.Decimal(5))]

    def test_all_completed_wo_completed(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        self._consume_order(invoice, order_1, 10)
        self._consume_order(invoice, order_2, 10)

        self._complete_order(session, order_1, 10)
        self._complete_order(session, order_2, 10)

        orders_data = [
            (order_1, 7 * 30),
            (order_2, 7 * 30)
        ]

        res = transfers.get_free_consumes_by_orders_sum(session, invoice, orders_data, ignore_completed=False)

        consumes, = res
        assert consumes == []

    def test_acts(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)
        q4 = self._consume_order(invoice, order_2, 10)

        self._make_acts(session, invoice, order_1, 7)
        self._make_acts(session, invoice, order_2, 6)

        self._complete_order(session, order_1, 10)
        self._complete_order(session, order_2, 15)

        orders_data = [
            (order_1, 13 * 30),
            (order_2, 15 * 30)
        ]

        res = transfers.get_free_consumes_by_orders_sum(session, invoice, orders_data, ignore_completed=True)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(3)),
                            (q4, decimal.Decimal(10)), (q3, decimal.Decimal(4))]

    def test_zero_end(self, session):
        invoice = self._get_invoice(session, 1, 503162)
        order_1, = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, decimal.Decimal('0.001'))
        q3 = self._consume_order(invoice, order_1, 10)

        orders_data = [
            (order_1, 10),
        ]

        res = transfers.get_free_consumes_by_orders_sum(session, invoice, orders_data)

        consumes, = res
        assert consumes == [(q3, decimal.Decimal(10)),
                            (q2, decimal.Decimal('0.001'))]

    def test_zero_middle(self, session):
        invoice = self._get_invoice(session, 1, 503162)
        order_1, = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, decimal.Decimal('0.001'))
        q3 = self._consume_order(invoice, order_1, 10)

        orders_data = [
            (order_1, 15),
        ]

        res = transfers.get_free_consumes_by_orders_sum(session, invoice, orders_data)

        consumes, = res
        assert consumes == [(q3, decimal.Decimal(10)),
                            (q2, decimal.Decimal('0.001')),
                            (q1, decimal.Decimal(5))]

    def test_precision(self, session):
        # цена на данный момент - 125000
        invoice = self._get_invoice(session, 1, 503849)
        order, = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order, 2)
        price = invoice.effective_price(q1.price_obj, 0)
        assert price > 1000
        assert order.product.unit.precision == 0

        orders_data = [
            (order, 666),
        ]

        res = transfers.get_free_consumes_by_orders_sum(session, invoice, orders_data)
        consumes, = res

        def quantize(qty): return qty.quantize(decimal.Decimal('0.000001'), decimal.ROUND_HALF_UP)
        consumes = map(lambda (q, qty): (q, quantize(qty)), consumes)
        assert consumes == [(q1, quantize(decimal.Decimal(666) / price))]


class TestCheckFreeConsumesQty(AbstractTransferTestGroup):
    def test_good_eq_qty(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_2, 10)

        free_consumes = [
            (q1, 10),
            (q2, 10)
        ]

        orders_data = [
            (order_1, 10),
            (order_2, 10),
        ]

        consumes, = transfers.check_free_consumes_by_orders_qty([free_consumes], orders_data)

        assert consumes == free_consumes

    def test_bad_eq_sum(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_2, 10)

        free_consumes = [
            (q1, decimal.Decimal('10.0001')),
            (q2, 10)
        ]

        orders_data = [
            (order_1, 10),
            (order_2, 10),
        ]

        with pytest.raises(transfers.TransferCheckQtyException):
            list(transfers.check_free_consumes_by_orders_qty([free_consumes], orders_data))

    def test_bad_gt(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_2, 10)

        free_consumes = [
            (q1, 11),
            (q2, 10)
        ]

        orders_data = [
            (order_1, 10),
            (order_2, 10),
        ]

        with pytest.raises(transfers.TransferCheckQtyException):
            list(transfers.check_free_consumes_by_orders_qty([free_consumes], orders_data))

    def test_bad_lt(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_2, 10)

        free_consumes = [
            (q1, 11),
            (q2, 10)
        ]

        orders_data = [
            (order_1, 10),
            (order_2, 9),
        ]

        with pytest.raises(transfers.TransferCheckQtyException):
            list(transfers.check_free_consumes_by_orders_qty([free_consumes], orders_data))

    def test_bad_precision(self, session):
        # целые дни
        invoice = self._get_invoice(session, 1, 503849)
        order, = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order, 2)
        assert order.product.unit.precision == 0

        free_consumes = [
            (q1, decimal.Decimal('1.5')),
        ]

        orders_data = [
            (order, decimal.Decimal('1.5')),
        ]

        with pytest.raises(transfers.TransferCheckQtyException):
            list(transfers.check_free_consumes_by_orders_qty([free_consumes], orders_data))


class TestCheckFreeConsumesSum(AbstractTransferTestGroup):
    def test_good_eq_qty(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_2, 10)

        free_consumes = [
            (q1, 10),
            (q2, 10)
        ]

        orders_data = [
            (order_1, 10 * 30),
            (order_2, 10 * 30),
        ]

        consumes, = transfers.check_free_consumes_by_orders_sum([free_consumes], orders_data)

        assert consumes == free_consumes

    def test_good_ne_qty(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_2, 10)

        free_consumes = [
            (q1, decimal.Decimal('10.0001')),
            (q2, 10)
        ]

        orders_data = [
            (order_1, 10 * 30),
            (order_2, 10 * 30),
        ]

        consumes, = transfers.check_free_consumes_by_orders_sum([free_consumes], orders_data)

        assert consumes == free_consumes

    def test_bad_gt(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_2, 10)

        free_consumes = [
            (q1, 11),
            (q2, 10)
        ]

        orders_data = [
            (order_1, 10 * 30),
            (order_2, 10 * 30),
        ]

        with pytest.raises(transfers.TransferCheckSumException):
            list(transfers.check_free_consumes_by_orders_sum([free_consumes], orders_data))

    def test_bad_lt(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_2, 10)

        free_consumes = [
            (q1, 10),
            (q2, 11)
        ]

        orders_data = [
            (order_1, 10 * 30),
            (order_2, 10 * 30),
        ]

        with pytest.raises(transfers.TransferCheckSumException):
            list(transfers.check_free_consumes_by_orders_sum([free_consumes], orders_data))

    def test_bad_precision(self, session):
        # цена на данный момент - 125000
        invoice = self._get_invoice(session, 1, 503849)
        order, = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order, 2)
        price = invoice.effective_price(q1.price_obj, 0)
        assert price > 1000
        assert order.product.unit.precision == 0

        free_consumes = [
            (q1, decimal.Decimal('666') / price),
        ]

        orders_data = [
            (order, 666),
        ]

        with pytest.raises(transfers.TransferCheckSumException):
            list(transfers.check_free_consumes_by_orders_sum([free_consumes], orders_data))

    def test_bad_precision_broken_q(self, session):
        invoice = self._get_invoice(session, 1, 503369)
        order, = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order, 1)
        price_obj = q1.price_obj
        effective_price = invoice.effective_price(price_obj, 0)
        assert effective_price > 10
        assert order.product.unit.precision == 0

        price_obj.price *= decimal.Decimal('1.01')
        q2 = self._consume_order(invoice, order, 1, price=price_obj)
        q3 = self._consume_order(invoice, order, 1)

        free_consumes = [
            (q3, 1),
            (q2, 1),
        ]

        orders_data = [
            (order, 2 * effective_price),
        ]

        with pytest.raises(transfers.TransferCheckSumException) as exc_info:
            list(transfers.check_free_consumes_by_orders_sum([free_consumes], orders_data))
        exc = exc_info.value

        assert exc.wrong_precision is False
        assert exc.required == 2 * effective_price
        assert exc.available.as_decimal() == effective_price + effective_price * decimal.Decimal('1.01')


class TestHandleOveracted(AbstractTransferTestGroup):

    @pytest.fixture(params=[False, True], ids=['base', 'by_client'])
    def overact_by_clients(self, request):
        return request.param

    def test_base(self, session, overact_by_clients):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)

        self._make_acts(session, invoice, order_2, 10)
        reverse_consume(q3, None, 10)

        free_consumes = [(q1, decimal.Decimal(10)), (q2, decimal.Decimal(10))]
        res = transfers.handle_overacted(session, invoice, [free_consumes], overact_by_clients=overact_by_clients)

        consumes, = res
        assert consumes == [(q1, decimal.Decimal(10))]

    def test_filter(self, session, overact_by_clients):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 6)

        self._make_acts(session, invoice, order_2, 6)
        reverse_consume(q3, None, 6)

        flt = (mapper.Consume.parent_order_id == order_1.id)

        free_consumes = [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(10))]
        res = transfers.handle_overacted(
            session,
            invoice,
            [free_consumes],
            overacted_consumes_filter=flt,
            overact_by_clients=overact_by_clients,
        )

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(10))]

    def test_partial(self, session, overact_by_clients):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 6)

        self._make_acts(session, invoice, order_2, 6)
        reverse_consume(q3, None, 6)

        free_consumes = [(q1, decimal.Decimal(10)), (q2, decimal.Decimal(10))]
        res = transfers.handle_overacted(session, invoice, [free_consumes], overact_by_clients=overact_by_clients)

        consumes, = res
        assert consumes == [(q1, decimal.Decimal(10)), (q2, decimal.Decimal(4))]

    def test_partial_quant(self, session, overact_by_clients):
        invoice = self._get_invoice(session, 2, 1475)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 4)

        self._make_acts(session, invoice, order_2, decimal.Decimal('3.314667'))
        reverse_consume(q3, None, 4)

        free_consumes = [(q1, decimal.Decimal(10)), (q2, decimal.Decimal(10))]
        res = transfers.handle_overacted(session, invoice, [free_consumes], overact_by_clients=overact_by_clients)

        consumes, = res
        assert consumes == [(q1, decimal.Decimal(10)), (q2, decimal.Decimal('6.685333'))]

    def test_no_overacted(self, session, overact_by_clients):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 6)

        free_consumes = [(q1, decimal.Decimal(10)), (q2, decimal.Decimal(10)), (q3, decimal.Decimal(6))]
        res = transfers.handle_overacted(session, invoice, [free_consumes], overact_by_clients=overact_by_clients)

        consumes, = res
        assert consumes == [(q1, decimal.Decimal(10)), (q2, decimal.Decimal(10)), (q3, decimal.Decimal(6))]

    def test_eq_overacted(self, session, overact_by_clients):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 20)

        self._make_acts(session, invoice, order_2, 20)
        reverse_consume(q3, None, 20)

        free_consumes = [(q1, decimal.Decimal(10)), (q2, decimal.Decimal(10))]
        res = transfers.handle_overacted(session, invoice, [free_consumes], overact_by_clients=overact_by_clients)

        consumes, = res
        assert consumes == []

    def test_gt_overacted(self, session, overact_by_clients):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 666)

        self._make_acts(session, invoice, order_2, 666)
        reverse_consume(q3, None, 666)

        free_consumes = [(q1, decimal.Decimal(10)), (q2, decimal.Decimal(10))]
        res = transfers.handle_overacted(session, invoice, [free_consumes], overact_by_clients=overact_by_clients)

        consumes, = res
        assert consumes == []

    def test_partial_w_acts_1(self, session, overact_by_clients):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 6)

        self._make_acts(session, invoice, order_1, 6)

        self._make_acts(session, invoice, order_2, 6)
        reverse_consume(q3, None, 6)

        free_consumes = [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(2))]
        res = transfers.handle_overacted(session, invoice, [free_consumes], overact_by_clients=overact_by_clients)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(8))]

    def test_partial_w_acts_2(self, session, overact_by_clients):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 6)

        self._make_acts(session, invoice, order_1, 2)

        self._make_acts(session, invoice, order_2, 6)
        reverse_consume(q3, None, 6)

        free_consumes = [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(2))]
        res = transfers.handle_overacted(session, invoice, [free_consumes], overact_by_clients=overact_by_clients)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(2))]

    def test_partial_w_acts_3(self, session, overact_by_clients):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 8)

        self._make_acts(session, invoice, order_1, 2)

        self._make_acts(session, invoice, order_2, 8)
        reverse_consume(q3, None, 8)

        free_consumes = [(q2, decimal.Decimal(10)), (q1, decimal.Decimal(2))]
        res = transfers.handle_overacted(session, invoice, [free_consumes], overact_by_clients=overact_by_clients)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(10))]

    def test_batch_1(self, session, overact_by_clients):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 6)

        self._make_acts(session, invoice, order_2, 6)
        reverse_consume(q3, None, 6)

        free_consumes_batches = [(q2, decimal.Decimal(10))], [(q1, decimal.Decimal(10))]
        res = transfers.handle_overacted(session, invoice, free_consumes_batches, overact_by_clients=overact_by_clients)

        consumes_batches = self._reverse_batches(res)

        assert len(consumes_batches) == 2
        consumes_1, consumes_2 = consumes_batches
        assert consumes_1 == [(q2, decimal.Decimal(10))]
        assert consumes_2 == [(q1, decimal.Decimal(4))]

    def test_batch_2(self, session, overact_by_clients):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 16)

        self._make_acts(session, invoice, order_2, 16)
        reverse_consume(q3, None, 16)

        free_consumes_batches = [(q2, decimal.Decimal(10))], [(q1, decimal.Decimal(10))]
        res = transfers.handle_overacted(session, invoice, free_consumes_batches, overact_by_clients=overact_by_clients)

        consumes_batches = list(self._reverse_batches(res))
        assert consumes_batches == [[(q2, decimal.Decimal(4))], []]

    def test_leftovers(self, session, overact_by_clients):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 8)

        self._make_acts(session, invoice, order_2, 8)
        reverse_consume(q3, None, 8)

        free_consumes = [(q2, decimal.Decimal(5)), (q1, decimal.Decimal(5))]
        res = transfers.handle_overacted(session, invoice, [free_consumes], overact_by_clients=overact_by_clients)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(5)), (q1, decimal.Decimal(5))]

    def test_leftovers_eq(self, session, overact_by_clients):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)

        self._make_acts(session, invoice, order_2, 10)
        reverse_consume(q3, None, 10)

        free_consumes = [(q2, decimal.Decimal(5)), (q1, decimal.Decimal(5))]
        res = transfers.handle_overacted(session, invoice, [free_consumes], overact_by_clients=overact_by_clients)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(5)), (q1, decimal.Decimal(5))]

    def test_leftovers_partial(self, session, overact_by_clients):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 12)

        self._make_acts(session, invoice, order_2, 12)
        reverse_consume(q3, None, 12)

        free_consumes = [(q2, decimal.Decimal(5)), (q1, decimal.Decimal(5))]
        res = transfers.handle_overacted(session, invoice, [free_consumes], overact_by_clients=overact_by_clients)

        consumes, = res
        assert consumes == [(q2, decimal.Decimal(5)), (q1, decimal.Decimal(3))]

    def test_by_client(self, session):
        invoice = self._get_invoice(session, 1)
        order_1, = [row.order for row in invoice.rows]

        overact_client = db_utils.create_client(session)
        overact_order = db_utils.create_order(session, overact_client)
        order_2 = db_utils.create_order(session, overact_client)

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_2, 10)

        overact_q = self._consume_order(invoice, overact_order, 7)
        self._make_acts(session, invoice, overact_order, 7)
        reverse_consume(overact_q, None, 7)

        free_consumes = [(q1, decimal.Decimal(10)), (q2, decimal.Decimal(10))]
        res = transfers.handle_overacted(session, invoice, [free_consumes], overact_by_clients=True)

        consumes, = res
        assert consumes == [(q1, decimal.Decimal(10)), (q2, decimal.Decimal(3))]


class TestCheckOveracted(AbstractTransferTestGroup):
    @pytest.fixture(params=[False, True], ids=['base', 'by_client'])
    def overact_by_clients(self, request):
        return request.param

    def test_good(self, session, overact_by_clients):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)

        self._make_acts(session, invoice, order_2, 10)
        reverse_consume(q3, None, 10)

        free_consumes = [(q1, decimal.Decimal(10))]
        consumes, = transfers.check_overacted(session, invoice, [free_consumes], overact_by_clients=overact_by_clients)
        assert consumes == free_consumes

    def test_bad_eq(self, session, overact_by_clients):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)

        self._make_acts(session, invoice, order_2, 10)
        reverse_consume(q3, None, 10)

        free_consumes = [(q1, decimal.Decimal(10)), (q2, decimal.Decimal(10))]
        with pytest.raises(transfers.TransferCheckOveractedException):
            list(transfers.check_overacted(session, invoice, [free_consumes], overact_by_clients=overact_by_clients))

    def test_bad_partial(self, session, overact_by_clients):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 7)

        self._make_acts(session, invoice, order_2, 7)
        reverse_consume(q3, None, 7)

        free_consumes = [(q1, decimal.Decimal(10)), (q2, decimal.Decimal(10))]
        with pytest.raises(transfers.TransferCheckOveractedException):
            list(transfers.check_overacted(session, invoice, [free_consumes], overact_by_clients=overact_by_clients))

    def test_filter(self, session, overact_by_clients):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)

        self._make_acts(session, invoice, order_2, 10)
        reverse_consume(q3, None, 10)

        free_consumes = [(q1, decimal.Decimal(10)), (q2, decimal.Decimal(10))]
        flt = (mapper.Consume.order == order_1)

        consumes, = transfers.check_overacted(session, invoice, [free_consumes], flt, overact_by_clients)
        assert consumes == free_consumes

    def test_good_batched(self, session, overact_by_clients):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)
        q4 = self._consume_order(invoice, order_2, 10)

        self._make_acts(session, invoice, order_2, 10)
        reverse_consume(q3, None, 10)

        fq_1 = [(q1, decimal.Decimal(10))]
        fq_2 = [(q2, decimal.Decimal(10))]

        res = transfers.check_overacted(session, invoice, [fq_1, fq_2], overact_by_clients=overact_by_clients)
        rq_1, rq_2 = self._reverse_batches(res)

        assert fq_1 == rq_1
        assert fq_2 == rq_2

    def test_bad_batched(self, session, overact_by_clients):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)
        q4 = self._consume_order(invoice, order_2, 10)

        self._make_acts(session, invoice, order_2, 10)
        reverse_consume(q3, None, 10)

        fq_1 = [(q1, decimal.Decimal(10)), (q2, decimal.Decimal(10))]
        fq_2 = [(q4, decimal.Decimal(10))]
        with pytest.raises(transfers.TransferCheckOveractedException):
            res = transfers.check_overacted(session, invoice, [fq_1, fq_2], overact_by_clients=overact_by_clients)
            self._reverse_batches(res)

    @pytest.mark.parametrize(
        'same_client',
        [
            pytest.param(True, id='same'),
            pytest.param(False, id='different'),
        ]
    )
    def test_by_client(self, session, same_client):
        invoice = self._get_invoice(session, 1)
        order, = [row.order for row in invoice.rows]

        if same_client:
            overact_client = invoice.client
        else:
            overact_client = db_utils.create_client(session)
        overact_order = db_utils.create_order(session, overact_client)

        q = self._consume_order(invoice, order, 10)

        overact_q = self._consume_order(invoice, overact_order, 7)
        self._make_acts(session, invoice, overact_order, 7)
        reverse_consume(overact_q, None, 7)

        free_consumes = [(q, decimal.Decimal(10))]

        def _do():
            return list(transfers.check_overacted(session, invoice, [free_consumes], overact_by_clients=True))

        if same_client:
            with pytest.raises(transfers.TransferCheckOveractedException):
                _do()
        else:
            consumes, = _do()
            assert consumes == free_consumes


class TestReverseConsumes(AbstractTransferTestGroup):

    def test_base(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)

        free_consumes = [(q2, decimal.Decimal(6)), (q1, decimal.Decimal(10)), (q3, decimal.Decimal(10))]
        res = transfers.reverse_consumes(invoice, [free_consumes])

        reverses, = res
        reverses_q = [(r.consume, r.reverse_qty) for r in reverses]

        assert reverses_q == free_consumes

    def test_batch(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)

        free_consumes = [(q2, decimal.Decimal(6)), (q1, decimal.Decimal(10))], [(q3, decimal.Decimal(10))]
        res = transfers.reverse_consumes(invoice, free_consumes)

        reverses_q_1, reverses_q_2 = map(lambda revs: [(r.consume, r.reverse_qty) for r in revs], res)

        assert reverses_q_1 == free_consumes[0]
        assert reverses_q_2 == free_consumes[1]

    def test_precision(self, session):
        invoice = self._get_invoice(session, 2, 503849)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 1)
        q2 = self._consume_order(invoice, order_2, 1)

        free_consumes = [(q1, decimal.Decimal('0.9')), (q2, decimal.Decimal('0.4'))]
        res = transfers.reverse_consumes(invoice, [free_consumes])

        reverses, = res
        reverses_q = [(r.consume, r.reverse_qty) for r in reverses]

        assert reverses_q == [(q1, 1), (q2, 0)]


class AbstractTransferConsumeTestGroup(AbstractTransferTestGroup):

    @staticmethod
    def extract_batch_data(batch):
        return [(oq, r, nq.order, nq.consume_qty, nq.consume_sum) for oq, r, nq in batch]


class TestConsumeReversesCopy(AbstractTransferConsumeTestGroup):

    def test_base(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)

        r1 = reverse_consume(q1, None, 6)
        r2 = reverse_consume(q2, None, 4)
        r3 = reverse_consume(q3, None, 7)

        reverses = [r3, r2, r1]
        res = transfers.consume_reverses_copy(invoice, [reverses])

        res, = map(self.extract_batch_data, res)

        req_res = [
            (q1, r1, order_1, decimal.Decimal(6), decimal.Decimal(6) * 30),
            (q2, r2, order_1, decimal.Decimal(4), decimal.Decimal(4) * 30),
            (q3, r3, order_2, decimal.Decimal(7), decimal.Decimal(7) * 30)
        ]

        assert res == req_res

    def test_batch(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)

        r1 = reverse_consume(q1, None, 6)
        r2 = reverse_consume(q2, None, 4)
        r3 = reverse_consume(q3, None, 7)

        res = transfers.consume_reverses_copy(invoice, [[r2, r1], [r3]])

        batch1, batch2 = map(self.extract_batch_data, res)

        req_batch1 = [
            (q1, r1, order_1, decimal.Decimal(6), decimal.Decimal(6) * 30),
            (q2, r2, order_1, decimal.Decimal(4), decimal.Decimal(4) * 30)
        ]

        req_batch2 = [
            (q3, r3, order_2, decimal.Decimal(7), decimal.Decimal(7) * 30)
        ]

        assert batch1 == req_batch1
        assert batch2 == req_batch2

    def test_discounts(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10, pct=20)
        q2 = self._consume_order(invoice, order_1, 10, pct=30)
        q3 = self._consume_order(invoice, order_2, 10, pct=40)

        r1 = reverse_consume(q1, None, 6)
        r2 = reverse_consume(q2, None, 4)
        r3 = reverse_consume(q3, None, 7)

        reverses = [r3, r2, r1]
        res = transfers.consume_reverses_copy(invoice, [reverses])

        res, = map(self.extract_batch_data, res)

        req_res = [
            (q1, r1, order_1, decimal.Decimal(6), decimal.Decimal(6) * 30 * decimal.Decimal('0.80')),
            (q2, r2, order_1, decimal.Decimal(4), decimal.Decimal(4) * 30 * decimal.Decimal('0.70')),
            (q3, r3, order_2, decimal.Decimal(7), decimal.Decimal(7) * 30 * decimal.Decimal('0.60'))
        ]

        assert res == req_res


class TestConsumeReversesRecalc(AbstractTransferConsumeTestGroup):

    def test_base(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)

        r1 = reverse_consume(q1, None, 6)
        r2 = reverse_consume(q2, None, 4)
        r3 = reverse_consume(q3, None, 7)

        reverses = [r3, r2, r1]
        res = transfers.consume_reverses_recalc(session, invoice, [reverses])

        res, = map(self.extract_batch_data, res)

        req_res = [
            (q1, r1, order_1, decimal.Decimal(6), decimal.Decimal(6) * 30),
            (q2, r2, order_1, decimal.Decimal(4), decimal.Decimal(4) * 30),
            (q3, r3, order_2, decimal.Decimal(7), decimal.Decimal(7) * 30)
        ]

        assert res == req_res

    def test_batch(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)

        r1 = reverse_consume(q1, None, 6)
        r2 = reverse_consume(q2, None, 4)
        r3 = reverse_consume(q3, None, 7)

        res = transfers.consume_reverses_recalc(session, invoice, [[r2, r1], [r3]])

        batch1, batch2 = map(self.extract_batch_data, res)

        req_batch1 = [
            (q1, r1, order_1, decimal.Decimal(6), decimal.Decimal(6) * 30),
            (q2, r2, order_1, decimal.Decimal(4), decimal.Decimal(4) * 30)
        ]

        req_batch2 = [
            (q3, r3, order_2, decimal.Decimal(7), decimal.Decimal(7) * 30)
        ]

        assert batch1 == req_batch1
        assert batch2 == req_batch2

    def test_had_discounts(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10, pct=20)
        q2 = self._consume_order(invoice, order_1, 10, pct=30)
        q3 = self._consume_order(invoice, order_2, 10, pct=40)

        r1 = reverse_consume(q1, None, 6)
        r2 = reverse_consume(q2, None, 4)
        r3 = reverse_consume(q3, None, 7)

        reverses = [r3, r2, r1]
        res = transfers.consume_reverses_recalc(session, invoice, [reverses])

        res, = map(self.extract_batch_data, res)

        req_res = [
            (q1, r1, order_1, decimal.Decimal(6), decimal.Decimal(6) * 30),
            (q2, r2, order_1, decimal.Decimal(4), decimal.Decimal(4) * 30),
            (q3, r3, order_2, decimal.Decimal(7), decimal.Decimal(7) * 30)
        ]

        assert res == req_res

    def test_new_discounts(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)

        r1 = reverse_consume(q1, None, 6)
        r2 = reverse_consume(q2, None, 4)
        r3 = reverse_consume(q3, None, 7)

        reverses = [r3, r2, r1]
        res = transfers.consume_reverses_recalc(session, invoice, [reverses], discount_pct=20)

        res, = map(self.extract_batch_data, res)

        req_res = [
            (q1, r1, order_1, decimal.Decimal(6), decimal.Decimal(6) * 30 * decimal.Decimal('0.80')),
            (q2, r2, order_1, decimal.Decimal(4), decimal.Decimal(4) * 30 * decimal.Decimal('0.80')),
            (q3, r3, order_2, decimal.Decimal(7), decimal.Decimal(7) * 30 * decimal.Decimal('0.80'))
        ]

        assert res == req_res

    def test_change_discounts(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10, pct=20)
        q2 = self._consume_order(invoice, order_1, 10, pct=30)
        q3 = self._consume_order(invoice, order_2, 10, pct=40)

        r1 = reverse_consume(q1, None, 6)
        r2 = reverse_consume(q2, None, 4)
        r3 = reverse_consume(q3, None, 7)

        reverses = [r3, r2, r1]
        res = transfers.consume_reverses_recalc(session, invoice, [reverses], discount_pct=20)

        res, = map(self.extract_batch_data, res)

        req_res = [
            (q1, r1, order_1, decimal.Decimal(6), decimal.Decimal(6) * 30 * decimal.Decimal('0.80')),
            (q2, r2, order_1, decimal.Decimal(4), decimal.Decimal(4) * 30 * decimal.Decimal('0.80')),
            (q3, r3, order_2, decimal.Decimal(7), decimal.Decimal(7) * 30 * decimal.Decimal('0.80'))
        ]

        assert res == req_res

    def test_price(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)

        r1 = reverse_consume(q1, None, 6)
        r2 = reverse_consume(q2, None, 4)
        r3 = reverse_consume(q3, None, 7)
        reverses = [r3, r2, r1]

        price_obj = q1.price_obj
        price_obj.price = 20

        res = transfers.consume_reverses_recalc(session, invoice, [reverses], price=price_obj)

        res, = map(self.extract_batch_data, res)

        req_res = [
            (q1, r1, order_1, decimal.Decimal(6), decimal.Decimal(6) * 20),
            (q2, r2, order_1, decimal.Decimal(4), decimal.Decimal(4) * 20),
            (q3, r3, order_2, decimal.Decimal(7), decimal.Decimal(7) * 20)
        ]

        assert res == req_res

    def test_group_order(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_2, 10)
        q2 = self._consume_order(invoice, order_2, 10)

        r1 = reverse_consume(q1, None, 6)
        r2 = reverse_consume(q2, None, 4)

        order_2.parent_group_order = order_1
        session.flush()

        reverses = [r2, r1]
        res = transfers.consume_reverses_recalc(session, invoice, [reverses])

        res, = map(self.extract_batch_data, res)

        req_res = [
            (q1, r1, order_2, decimal.Decimal(6), decimal.Decimal(6) * 30),
            (q2, r2, order_2, decimal.Decimal(4), decimal.Decimal(4) * 30),
        ]

        assert res == req_res


class TestHandleProcessCompletions(AbstractTransferTestGroup):

    def test_reverses_enqueue(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)

        r1 = reverse_consume(q1, None, 5)
        r2 = reverse_consume(q2, None, 5)
        r3 = reverse_consume(q3, None, 5)

        reverses = [r1, r2, r3]
        res = transfers.handle_process_completions(reverses=[reverses],
                                                   enqueue_process_completions=True, do_process_completions=False)

        res_reverses, = res

        assert res_reverses == reverses
        assert not any(r.order.exports['PROCESS_COMPLETION'].state for r in res_reverses)

    def test_reverses_do(self, session):
        invoice = self._get_invoice(session, 1)
        order, = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order, 10)
        q2 = self._consume_order(invoice, order, 10)

        self._complete_order(session, order, 10)

        r1 = reverse_consume(q1, None, 5)
        r2 = reverse_consume(q2, None, 5)

        reverses = [r1, r2]
        res = transfers.handle_process_completions(reverses=[reverses],
                                                   enqueue_process_completions=False, do_process_completions=True)

        list(res)
        session.flush()

        assert q1.completion_qty == decimal.Decimal(5)
        assert q2.completion_qty == decimal.Decimal(5)

    def test_reverses_do_batch(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)
        q4 = self._consume_order(invoice, order_2, 10)

        self._complete_order(session, order_1, 10)
        self._complete_order(session, order_2, 7)

        r1 = reverse_consume(q1, None, 5)
        r2 = reverse_consume(q2, None, 5)
        r3 = reverse_consume(q3, None, 5)
        r4 = reverse_consume(q4, None, 5)

        rev_1 = [r1, r2, r3]
        rev_2 = [r4]
        res = transfers.handle_process_completions(reverses=[rev_1, rev_2],
                                                   enqueue_process_completions=False, do_process_completions=True)

        res_rev_1, res_rev_2 = res
        session.flush()

        assert res_rev_1 == rev_1
        assert res_rev_2 == rev_2
        assert q1.completion_qty == decimal.Decimal(5)
        assert q2.completion_qty == decimal.Decimal(5)
        assert q3.completion_qty == decimal.Decimal(5)
        assert q4.completion_qty == decimal.Decimal(2)

    def test_trinity_enqueue(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)

        r1 = reverse_consume(q1, None, 5)
        r2 = reverse_consume(q2, None, 5)
        r3 = reverse_consume(q3, None, 5)

        trinitys = [
            (q1, r1, 1),
            (q2, r2, 2),
            (q3, r3, 3)
        ]
        res = transfers.handle_process_completions(transfer_results=[trinitys],
                                                   enqueue_process_completions=True, do_process_completions=False)

        res_trinitys, = res

        assert res_trinitys == trinitys
        assert not any(r.order.exports['PROCESS_COMPLETION'].state for _, r, _ in res_trinitys)

    def test_trinity_do(self, session):
        invoice = self._get_invoice(session, 1)
        order, = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order, 10)
        q2 = self._consume_order(invoice, order, 10)

        self._complete_order(session, order, 10)

        r1 = reverse_consume(q1, None, 5)
        r2 = reverse_consume(q2, None, 5)

        trinitys = [
            (q1, r1, 1),
            (q2, r2, 2),
        ]
        res = transfers.handle_process_completions(transfer_results=[trinitys],
                                                   enqueue_process_completions=False, do_process_completions=True)

        res_trinitys, = res
        session.flush()
        assert res_trinitys == trinitys
        assert q1.completion_qty == decimal.Decimal(5)
        assert q2.completion_qty == decimal.Decimal(5)

    def test_trinity_do_batch(self, session):
        invoice = self._get_invoice(session, 2)
        order_1, order_2 = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order_1, 10)
        q2 = self._consume_order(invoice, order_1, 10)
        q3 = self._consume_order(invoice, order_2, 10)
        q4 = self._consume_order(invoice, order_2, 10)

        self._complete_order(session, order_1, 10)
        self._complete_order(session, order_2, 7)

        r1 = reverse_consume(q1, None, 5)
        r2 = reverse_consume(q2, None, 5)
        r3 = reverse_consume(q3, None, 5)
        r4 = reverse_consume(q4, None, 5)

        tr_1 = [
            (q1, r1, 1),
            (q2, r2, 2),
            (q3, r3, 3)
        ]
        tr_2 = [(q4, r4, 4)]
        res = transfers.handle_process_completions(transfer_results=[tr_1, tr_2],
                                                   enqueue_process_completions=False, do_process_completions=True)

        res_tr_1, res_tr_2 = res
        session.flush()

        assert res_tr_1 == tr_1
        assert res_tr_2 == tr_2
        assert q1.completion_qty == decimal.Decimal(5)
        assert q2.completion_qty == decimal.Decimal(5)
        assert q3.completion_qty == decimal.Decimal(5)
        assert q4.completion_qty == decimal.Decimal(2)


class TestTransferChains(AbstractTransferTestGroup):

    @staticmethod
    def _extract_consumes_batch_data(batch):
        return [(oq, r.consume, r.reverse_qty, nq.invoice, nq.consume_qty, nq.consume_sum) for oq, r, nq in batch]

    def test_reverses(self, session):
        invoice_from = self._get_invoice(session, 2)

        order_1, order_2 = [row.order for row in invoice_from.rows]

        q1 = self._consume_order(invoice_from, order_1, 10)
        q2 = self._consume_order(invoice_from, order_1, 10)
        q3 = self._consume_order(invoice_from, order_2, 10)
        q4 = self._consume_order(invoice_from, order_2, 10)

        tr_chain = transfers.TransferActionsChain(session=session, invoice=invoice_from)
        tr_chain.add_action(transfers.get_free_consumes)
        tr_chain.add_action(transfers.handle_overacted)
        tr_chain.add_action(transfers.reverse_consumes)

        reverses, = tr_chain.do()
        res = [(r.consume, r.reverse_qty) for r in reverses]

        req_res = [
            (q2, decimal.Decimal(10)),
            (q1, decimal.Decimal(10)),
            (q4, decimal.Decimal(10)),
            (q3, decimal.Decimal(10))
        ]

        assert res == req_res

    def test_transfer(self, session):
        invoice_from = self._get_invoice(session, 2)
        invoice_to = self._get_invoice(session, 1)

        order_1, order_2 = [row.order for row in invoice_from.rows]

        q1 = self._consume_order(invoice_from, order_1, 10)
        q2 = self._consume_order(invoice_from, order_1, 10)
        q3 = self._consume_order(invoice_from, order_2, 10)
        q4 = self._consume_order(invoice_from, order_2, 10)

        tr_chain = transfers.TransferActionsChain(session=session, invoice=invoice_from, dest_invoice=invoice_to)
        tr_chain.add_action(transfers.get_free_consumes)
        tr_chain.add_action(transfers.handle_overacted)
        tr_chain.add_action(transfers.reverse_consumes)
        tr_chain.add_action(transfers.consume_reverses_copy)

        res, = map(self._extract_consumes_batch_data, tr_chain.do())

        req_res = [
            (q3, q3, decimal.Decimal(10), invoice_to, decimal.Decimal(10), decimal.Decimal(300)),
            (q4, q4, decimal.Decimal(10), invoice_to, decimal.Decimal(10), decimal.Decimal(300)),
            (q1, q1, decimal.Decimal(10), invoice_to, decimal.Decimal(10), decimal.Decimal(300)),
            (q2, q2, decimal.Decimal(10), invoice_to, decimal.Decimal(10), decimal.Decimal(300)),
        ]

        assert res == req_res

    def test_transfer_overacted(self, session):
        invoice_from = self._get_invoice(session, 2)
        invoice_to = self._get_invoice(session, 1)

        order_1, order_2 = [row.order for row in invoice_from.rows]

        q1 = self._consume_order(invoice_from, order_1, 10)
        q2 = self._consume_order(invoice_from, order_1, 10)
        q3 = self._consume_order(invoice_from, order_2, 10)

        self._make_acts(session, invoice_from, order_2, 10)
        reverse_consume(q3, None, 10)

        tr_chain = transfers.TransferActionsChain(session=session, invoice=invoice_from, dest_invoice=invoice_to)
        tr_chain.add_action(transfers.get_free_consumes)
        tr_chain.add_action(transfers.handle_overacted)
        tr_chain.add_action(transfers.reverse_consumes)
        tr_chain.add_action(transfers.consume_reverses_copy)

        res, = map(self._extract_consumes_batch_data, tr_chain.do())

        req_res = [
            (q2, q2, decimal.Decimal(10), invoice_to, decimal.Decimal(10), decimal.Decimal(300)),
        ]

        assert res == req_res

    # def test_transfer_batched(self, session):
    #     invoice_from = self._get_invoice(session, 2)
    #     invoice_to = self._get_invoice(session, 1)
    #
    #     order_1, order_2 = [row.order for row in invoice_from.rows]
    #
    #     q1 = self._consume_order(invoice_from, order_1, 10)
    #     q2 = self._consume_order(invoice_from, order_1, 10)
    #     q3 = self._consume_order(invoice_from, order_2, 10)
    #     q4 = self._consume_order(invoice_from, order_2, 10)
    #
    #     tr_chain = transfers.TransferActionsChain(session=session, invoice=invoice_from, dest_invoice=invoice_to,
    #                                               batch_size=3)
    #     tr_chain.add_action(transfers.get_free_consumes_batched)
    #     tr_chain.add_action(transfers.handle_overacted)
    #     tr_chain.add_action(transfers.reverse_consumes)
    #     tr_chain.add_action(transfers.consume_reverses_copy)
    #
    #     res_1, res_2 = map(self._extract_consumes_batch_data, tr_chain.do())
    #
    #     # Стрёмная последовательность зачислений из-за особенность get_free_consume_batched
    #     # (см. TestGetFreeConsumesBatched.test_batch_funky)
    #     req_res_1 = [
    #         (q4, q4, decimal.Decimal(10), invoice_to, decimal.Decimal(10), decimal.Decimal(300)),
    #         (q1, q1, decimal.Decimal(10), invoice_to, decimal.Decimal(10), decimal.Decimal(300)),
    #         (q2, q2, decimal.Decimal(10), invoice_to, decimal.Decimal(10), decimal.Decimal(300)),
    #     ]
    #     req_res_2 = [
    #         (q3, q3, decimal.Decimal(10), invoice_to, decimal.Decimal(10), decimal.Decimal(300)),
    #     ]
    #
    #     assert res_1 == req_res_1
    #     assert res_2 == req_res_2

    # def test_transfer_batched_by_orders(self, session):
    #     invoice_from = self._get_invoice(session, 3)
    #     invoice_to = self._get_invoice(session, 1)
    #
    #     order_1, order_2, order_3 = [row.order for row in invoice_from.rows]
    #
    #     q1 = self._consume_order(invoice_from, order_1, 10)
    #     q2 = self._consume_order(invoice_from, order_1, 10)
    #     q3 = self._consume_order(invoice_from, order_2, 10)
    #     q4 = self._consume_order(invoice_from, order_2, 10)
    #     q5 = self._consume_order(invoice_from, order_3, 10)
    #     q6 = self._consume_order(invoice_from, order_3, 10)
    #
    #     tr_chain = transfers.TransferActionsChain(session=session, invoice=invoice_from, dest_invoice=invoice_to,
    #                                               batch_size=2)
    #     tr_chain.add_action(transfers.get_free_consumes_batched_by_orders)
    #     tr_chain.add_action(transfers.handle_overacted)
    #     tr_chain.add_action(transfers.reverse_consumes)
    #     tr_chain.add_action(transfers.consume_reverses_copy)
    #
    #     res_1, res_2 = map(self._extract_consumes_batch_data, tr_chain.do())
    #
    #     req_res_1 = [
    #         (q3, q3, decimal.Decimal(10), invoice_to, decimal.Decimal(10), decimal.Decimal(300)),
    #         (q4, q4, decimal.Decimal(10), invoice_to, decimal.Decimal(10), decimal.Decimal(300)),
    #         (q1, q1, decimal.Decimal(10), invoice_to, decimal.Decimal(10), decimal.Decimal(300)),
    #         (q2, q2, decimal.Decimal(10), invoice_to, decimal.Decimal(10), decimal.Decimal(300)),
    #     ]
    #     req_res_2 = [
    #         (q5, q5, decimal.Decimal(10), invoice_to, decimal.Decimal(10), decimal.Decimal(300)),
    #         (q6, q6, decimal.Decimal(10), invoice_to, decimal.Decimal(10), decimal.Decimal(300)),
    #     ]
    #
    #     assert res_1 == req_res_1
    #     assert res_2 == req_res_2

    # def test_transfer_batched_overacted(self, session):
    #     invoice_from = self._get_invoice(session, 2)
    #     invoice_to = self._get_invoice(session, 1)
    #
    #     order_1, order_2 = [row.order for row in invoice_from.rows]
    #
    #     q1 = self._consume_order(invoice_from, order_1, 10)
    #     q2 = self._consume_order(invoice_from, order_1, 10)
    #     q3 = self._consume_order(invoice_from, order_1, 10)
    #     q4 = self._consume_order(invoice_from, order_2, 33)
    #     q5 = self._consume_order(invoice_from, order_2, 10)
    #     q6 = self._consume_order(invoice_from, order_2, 10)
    #
    #     self._make_acts(session, invoice_from, order_2, 33)
    #     reverse_consume(q4, None, 33)
    #
    #     tr_chain = transfers.TransferActionsChain(session=session, invoice=invoice_from, dest_invoice=invoice_to,
    #                                               batch_size=3)
    #     tr_chain.add_action(transfers.get_free_consumes_batched)
    #     tr_chain.add_action(transfers.handle_overacted)
    #     tr_chain.add_action(transfers.reverse_consumes)
    #     tr_chain.add_action(transfers.consume_reverses_copy)
    #
    #     transfer_res = map(self._extract_consumes_batch_data, tr_chain.do())
    #
    #     req_res = [
    #         (q2, q2, decimal.Decimal(7), invoice_to, decimal.Decimal(7), decimal.Decimal(210)),
    #         (q3, q3, decimal.Decimal(10), invoice_to, decimal.Decimal(10), decimal.Decimal(300)),
    #     ]
    #
    #     assert len(transfer_res) == 1
    #     assert transfer_res[0] == req_res

    def test_check_sum_broken_q(self, session):
        invoice = self._get_invoice(session, 1, 503369)
        order, = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order, 1)

        price_obj = q1.price_obj
        effective_price = invoice.effective_price(price_obj, 0)
        assert effective_price > 10
        assert order.product.unit.precision == 0

        price_obj.price *= decimal.Decimal('1.01')
        q2 = self._consume_order(invoice, order, 1, price=price_obj)
        q3 = self._consume_order(invoice, order, 1)

        orders_data = [
            (order, 2 * effective_price),
        ]

        tr_chain = transfers.TransferActionsChain(session=session, invoice=invoice, orders_data=orders_data)
        tr_chain.add_action(transfers.get_free_consumes_by_orders_sum)
        tr_chain.add_action(transfers.check_free_consumes_by_orders_sum)

        with pytest.raises(transfers.TransferCheckSumException) as exc_info:
            list(tr_chain.do())
        exc = exc_info.value

        assert exc.wrong_precision is True
        assert exc.required == 2 * effective_price
        assert exc.available.as_decimal() == effective_price + effective_price * decimal.Decimal('1.01')

    def test_reverse_sum_broken_q(self, session):
        invoice = self._get_invoice(session, 1, 503162)
        order, = [row.order for row in invoice.rows]

        q1 = self._consume_order(invoice, order, decimal.Decimal('5000.0022'))

        orders_data = [
            (order, 500),
        ]

        tr_chain = transfers.TransferActionsChain(session=session, invoice=invoice, orders_data=orders_data)
        tr_chain.add_action(transfers.get_free_consumes_by_orders_sum)
        tr_chain.add_action(transfers.reverse_consumes)

        (reverse, ), = tr_chain.do()

        assert reverse.consume is q1
        assert reverse.reverse_sum == 500
        assert reverse.reverse_qty == decimal.Decimal('500.0002')

    def test_reverse_all_acted_oa_check(self, session):
        invoice = self._get_invoice(session, 2)

        order_1, order_2 = [row.order for row in invoice.rows]

        self._consume_order(invoice, order_1, 10)
        self._consume_order(invoice, order_2, 10)

        self._make_acts(session, invoice, order_1, 10, False)
        self._make_acts(session, invoice, order_2, 10, False)

        tr_chain = transfers.TransferActionsChain(session=session, invoice=invoice)
        tr_chain.add_action(transfers.get_free_consumes)
        tr_chain.add_action(transfers.check_overacted)
        tr_chain.add_action(transfers.reverse_consumes)

        res, = tr_chain.do()

        assert res == []
