# -*- coding: utf-8 -*-

import decimal

import mock
import pytest

from balance import mapper
from balance.actions.invoice_create import InvoiceFactory

from autodasha.core.logic import invoice_sum_patcher as isp

from tests.autodasha_tests.common import db_utils

"""
Используемые продукты:
* 1475 - директ, у.е., цена 30, точность 6 - серьёзно, вы не знаете что это?
* 503162 - директ, рубли, цена 1, точность 4
* 503165 - директ, гривны, цена 1, точность 5
* 504056 - медийка, дни, цена 10, точность 0
* 507175 - цена 30, точность 6
"""


def patch_price(product, dt, currency, *args, **kwargs):
    price_map = {
        1475: 30,
        503162: 1,
        503165: 1,
        504056: 10,
        507175: 30
    }
    price = price_map[product.id]
    tpp = product.session.query(mapper.TaxPolicyPct).getone(1)
    return mapper.PriceObject(price=price, dt=dt, currency=currency, tax_policy_pct=tpp, type_rate=1)


@mock.patch('balance.mapper.products.Product.get_price', patch_price)
class TestWPatchedPrice(object):

    @staticmethod
    def create_invoice(session, rows, paysys_id=1000, overdraft=0):
        client, person = db_utils.create_client_person(session)
        paysys = session.query(mapper.Paysys).getone(paysys_id)
        firm = paysys.firm

        basket_items = []
        for row in rows:
            if len(row) == 2:
                product_id, qty = row
                discount_pct = 0
            elif len(row) == 3:
                product_id, qty, discount_pct = row
            else:
                raise ValueError('Invalid rows format')

            order = db_utils.create_order(session, client, product_id=product_id)
            basket_items.append(mapper.BasketItem(qty, order, desired_discount_pct=discount_pct))

        request = mapper.Request(session.oper_id, mapper.Basket(client, basket_items))
        session.add(request)
        session.flush()
        invoice = InvoiceFactory.create(request, paysys, person,
                                        status_id=0, credit=0, temporary=False, firm=firm, overdraft=overdraft)
        session.add(invoice)
        session.flush()

        return invoice

    def test_simple_positive(self, session):
        invoice = self.create_invoice(session, [(1475, 10), (1475, 9)])
        io, _ = invoice.rows

        req_i_sum = 575
        req_io_qty = decimal.Decimal('10.166667')
        req_io_sum = 305

        res = isp.InvoiceSumPatcher(invoice).do(req_i_sum)

        assert invoice.effective_sum == req_i_sum
        assert io.quantity == req_io_qty
        assert io.amount == req_io_sum
        assert io.effective_sum == req_io_sum
        assert res == [(io, 10, 300, req_io_qty, req_io_sum)]

    def test_simple_negative(self, session):
        invoice = self.create_invoice(session, [(1475, 9), (1475, 10)])
        _, io = invoice.rows

        req_i_sum = 565
        req_io_qty = decimal.Decimal('9.833333')
        req_io_sum = 295

        res = isp.InvoiceSumPatcher(invoice).do(req_i_sum)

        assert invoice.effective_sum == req_i_sum
        assert io.quantity == req_io_qty
        assert io.amount == req_io_sum
        assert io.effective_sum == req_io_sum
        assert res == [(io, 10, 300, req_io_qty, req_io_sum)]

    def test_prec0_use_prec0(self, session):
        invoice = self.create_invoice(session, [(507175, 2), (504056, 10)])

        _, io = invoice.rows

        req_i_sum = 130
        req_io_qty = 7
        req_io_sum = 70

        res = isp.InvoiceSumPatcher(invoice).do(req_i_sum)

        assert invoice.effective_sum == req_i_sum
        assert io.quantity == req_io_qty
        assert io.amount == req_io_sum
        assert io.effective_sum == req_io_sum
        assert res == [(io, 10, 100, req_io_qty, req_io_sum)]

    def test_prec0_use_prec6(self, session):
        invoice = self.create_invoice(session, [(507175, 2), (504056, 10)])

        io6, io0 = invoice.rows

        req_i_sum = 163
        req_io6_qty = decimal.Decimal('2.1')
        req_io6_sum = 63
        req_io0_qty = 10
        req_io0_sum = 100

        res = isp.InvoiceSumPatcher(invoice).do(req_i_sum)

        assert invoice.effective_sum == req_i_sum
        assert io6.quantity == req_io6_qty
        assert io6.amount == req_io6_sum
        assert io6.effective_sum == req_io6_sum
        assert io0.quantity == req_io0_qty
        assert io0.amount == req_io0_sum
        assert io0.effective_sum == req_io0_sum
        assert res == [(io6, 2, 60, req_io6_qty, req_io6_sum)]

    def test_multiple_simple(self, session):
        invoice = self.create_invoice(session, [(1475, 11), (1475, 10), (1475, 9)])
        io1, io2, io3 = invoice.rows

        req_i_sum = 30
        req_io_qty12 = decimal.Decimal('0.000001')
        req_io_sum12 = 0
        req_io_qty3 = 1
        req_io_sum3 = 30

        res = isp.InvoiceSumPatcher(invoice).do(req_i_sum)

        assert invoice.effective_sum == req_i_sum
        assert io1.quantity == req_io_qty12
        assert io1.amount == req_io_sum12
        assert io1.effective_sum == req_io_sum12
        assert io2.quantity == req_io_qty12
        assert io2.amount == req_io_sum12
        assert io2.effective_sum == req_io_sum12
        assert io3.quantity == req_io_qty3
        assert io3.amount == req_io_sum3
        assert io3.effective_sum == req_io_sum3
        assert res == [
            (io1, 11, 330, req_io_qty12, req_io_sum12),
            (io2, 10, 300, req_io_qty12, req_io_sum12),
            (io3, 9, 270, req_io_qty3, req_io_sum3)
        ]

    def test_multiple_prec_v1(self, session):
        invoice = self.create_invoice(session, [(504056, 30), (504056, 20), (507175, 5)])
        io1, io2, io3 = invoice.rows

        req_i_sum = 317
        req_io_qty1 = 1
        req_io_sum1 = 10
        req_io_qty2 = 16
        req_io_sum2 = 160
        req_io_qty3 = decimal.Decimal('4.9')
        req_io_sum3 = 147

        res = isp.InvoiceSumPatcher(invoice).do(req_i_sum)

        assert invoice.effective_sum == req_i_sum
        assert io1.quantity == req_io_qty1
        assert io1.amount == req_io_sum1
        assert io1.effective_sum == req_io_sum1
        assert io2.quantity == req_io_qty2
        assert io2.amount == req_io_sum2
        assert io2.effective_sum == req_io_sum2
        assert io3.quantity == req_io_qty3
        assert io3.amount == req_io_sum3
        assert io3.effective_sum == req_io_sum3
        assert res == [
            (io1, 30, 300, req_io_qty1, req_io_sum1),
            (io2, 20, 200, req_io_qty2, req_io_sum2),
            (io3, 5, 150, req_io_qty3, req_io_sum3)
        ]

    def test_multiple_prec_v2(self, session):
        invoice = self.create_invoice(session, [(504056, 30), (504056, 20), (507175, 5)])
        io1, io2, io3 = invoice.rows

        req_i_sum = 347
        req_io_qty1 = 1
        req_io_sum1 = 10
        req_io_qty2 = 19
        req_io_sum2 = 190
        req_io_qty3 = decimal.Decimal('4.9')
        req_io_sum3 = 147

        res = isp.InvoiceSumPatcher(invoice).do(req_i_sum)

        assert invoice.effective_sum == req_i_sum
        assert io1.quantity == req_io_qty1
        assert io1.amount == req_io_sum1
        assert io1.effective_sum == req_io_sum1
        assert io2.quantity == req_io_qty2
        assert io2.amount == req_io_sum2
        assert io2.effective_sum == req_io_sum2
        assert io3.quantity == req_io_qty3
        assert io3.amount == req_io_sum3
        assert io3.effective_sum == req_io_sum3
        assert res == [
            (io1, 30, 300, req_io_qty1, req_io_sum1),
            (io2, 20, 200, req_io_qty2, req_io_sum2),
            (io3, 5, 150, req_io_qty3, req_io_sum3)
        ]

    def test_multiple_order(self, session):
        invoice = self.create_invoice(session, [(504056, 30), (507175, 9), (504056, 10)])
        io1, io2, io3 = invoice.rows

        req_i_sum = 330
        req_io_qty1 = 1
        req_io_sum1 = 10
        req_io_qty2 = decimal.Decimal('7.333333')
        req_io_sum2 = 220
        req_io_qty3 = 10
        req_io_sum3 = 100

        res = isp.InvoiceSumPatcher(invoice).do(req_i_sum)

        assert invoice.effective_sum == req_i_sum
        assert io1.quantity == req_io_qty1
        assert io1.amount == req_io_sum1
        assert io1.effective_sum == req_io_sum1
        assert io2.quantity == req_io_qty2
        assert io2.amount == req_io_sum2
        assert io2.effective_sum == req_io_sum2
        assert io3.quantity == req_io_qty3
        assert io3.amount == req_io_sum3
        assert io3.effective_sum == req_io_sum3
        assert res == [
            (io1, 30, 300, req_io_qty1, req_io_sum1),
            (io2, 9, 270, req_io_qty2, req_io_sum2)
        ]

    def test_prec_fail(self, session):
        invoice = self.create_invoice(session, [(504056, 30), (504056, 20)])

        req_i_sum = 347

        with pytest.raises(isp.CouldNotFixException) as exc_info:
            isp.InvoiceSumPatcher(invoice).do(req_i_sum)

        assert exc_info.value.remaining_diff == -3

    def test_prec_greedy_fail(self, session):
        invoice = self.create_invoice(session, [(507175, 10), (504056, 5)])

        req_i_sum = 3

        with pytest.raises(isp.CouldNotFixException) as exc_info:
            isp.InvoiceSumPatcher(invoice).do(req_i_sum)

        assert exc_info.value.remaining_diff == -7

    def test_unexpected_fail(self, session):
        invoice = self.create_invoice(session, [(504056, 30)])

        req_i_sum = 340

        def careless_patch_invoice_row(self, io, diff_sum):
            return 0, True

        patch_path = 'autodasha.core.logic.invoice_sum_patcher.InvoiceSumPatcher._patch_invoice_row'
        with mock.patch(patch_path, careless_patch_invoice_row):
            with pytest.raises(isp.FixFailException) as exc_info:
                isp.InvoiceSumPatcher(invoice).do(req_i_sum)

        exc = exc_info.value
        assert exc.required_sum == 340
        assert exc.current_sum == 300

