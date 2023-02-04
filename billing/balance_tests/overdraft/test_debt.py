# -*- coding: utf-8 -*-

import datetime

import pytest

from balance import muzzle_util as ut
from balance import overdraft
from balance import mapper
from balance.actions import single_account, invoice_turnon
from balance.constants import *

from tests import object_builder as ob

from tests.balance_tests.invoices.invoice_common import (
    create_refund,
    create_invoice,
)

NOW = datetime.datetime.now()


class TestLockedInvoicesQuery(object):
    @pytest.mark.parametrize(
        'lock, is_found',
        [
            (InvoiceReceiptLockType.OFF, False),
            (InvoiceReceiptLockType.TRANSFER, False),
            (InvoiceReceiptLockType.REFUND, False),
            (InvoiceReceiptLockType.OVERDRAFT, True),
        ]
    )
    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_locks(self, session, lock, is_found, client):
        invoice = create_invoice(session, client=client)
        invoice.unused_funds_lock = lock
        invoice.create_receipt(invoice.effective_sum)

        res = overdraft.debt.get_locked_invoices(invoice.person, {ServiceId.DIRECT})
        if is_found:
            assert res == [invoice]
        else:
            assert res == []

    @pytest.mark.parametrize(
        'invoice_sum, receipt_sum, consume_sum, is_found',
        [
            (100, 0, 0, False),
            (100, 100, 0, True),
            (100, 200, 0, True),
            (100, 100, 1, False),
            (100, 100, 100, False),
            (100, 201, 101, True),
        ]
    )
    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_sums(self, session, invoice_sum, receipt_sum, consume_sum, is_found, client):
        invoice = create_invoice(session, invoice_sum, client=client)
        if receipt_sum:
            invoice.create_receipt(receipt_sum)
        if consume_sum:
            io, = invoice.invoice_orders
            invoice.transfer(io.order, TransferMode.dst, consume_sum, skip_check=True)
        invoice.unused_funds_lock = InvoiceReceiptLockType.OVERDRAFT
        session.flush()

        res = overdraft.debt.get_locked_invoices(invoice.person, {ServiceId.DIRECT})
        if is_found:
            assert res == [invoice]
        else:
            assert res == []

    @pytest.mark.invoice_refunds
    @pytest.mark.parametrize(
        'invoice_sum, receipt_sum, lock_sum, is_found',
        [
            (100, 100, 10, False),
            (100, 110, 10, True),
        ]
    )
    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_locked(self, session, invoice_sum, receipt_sum, lock_sum, is_found, client):
        invoice = create_invoice(session, invoice_sum, client=client)
        if receipt_sum:
            invoice.create_receipt(receipt_sum)
        if lock_sum:
            create_refund(invoice, lock_sum)
        invoice.unused_funds_lock = InvoiceReceiptLockType.OVERDRAFT

        res = overdraft.debt.get_locked_invoices(invoice.person, {ServiceId.DIRECT})
        if is_found:
            assert res == [invoice]
        else:
            assert res == []

    @pytest.mark.parametrize(
        'service_ids, is_found',
        [
            pytest.param((ServiceId.DIRECT, ServiceId.MARKET,), True, id='direct_market'),
            pytest.param((ServiceId.MARKET,), False, id='market'),
        ]
    )
    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_services(self, session, client, service_ids, is_found):
        order = ob.OrderBuilder.construct(
            session,
            client=client,
            product_id=DIRECT_PRODUCT_RUB_ID,
            service_id=ServiceId.DIRECT,
        )
        invoice = create_invoice(session, client=client, orders=[(order, 100)])
        invoice.unused_funds_lock = InvoiceReceiptLockType.OVERDRAFT
        invoice.create_receipt(invoice.effective_sum)

        res = overdraft.debt.get_locked_invoices(invoice.person, service_ids)
        if is_found:
            assert res == [invoice]
        else:
            assert res == []

    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_overdraft(self, session, client):
        invoice = create_invoice(session, overdraft=1, client=client)
        invoice.unused_funds_lock = InvoiceReceiptLockType.OVERDRAFT
        invoice.create_receipt(invoice.effective_sum)

        assert overdraft.debt.get_locked_invoices(invoice.person, {ServiceId.DIRECT}) == []

    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_credit(self, session, client):
        invoice = create_invoice(session, client=client)
        invoice.unused_funds_lock = InvoiceReceiptLockType.OVERDRAFT
        invoice.credit = 1
        invoice.create_receipt(invoice.effective_sum)

        assert overdraft.debt.get_locked_invoices(invoice.person, {ServiceId.DIRECT}) == []

    def test_single_account(self, session):
        client = ob.ClientBuilder(with_single_account=True).build(session).obj
        person = ob.PersonBuilder(client=client, type='ur').build(session).obj
        single_account.prepare.process_client(client)
        (personal_account, _), = client.get_single_account_subaccounts()

        request = ob.RequestBuilder(
            basket=ob.BasketBuilder(
                rows=[
                    ob.BasketItemBuilder(
                        quantity=100,
                        order=ob.OrderBuilder(
                            client=client,
                            product=ob.ProductBuilder(
                                price=1,
                                engine_id=ServiceId.DIRECT
                            ),
                            service_id=ServiceId.DIRECT
                        )
                    )
                ]
            )
        ).build(session).obj

        invoice = ob.InvoiceBuilder(
            dt=datetime.datetime.now(),
            person=person,
            paysys=ob.Getter(mapper.Paysys, 1003).build(session).obj,
            request=request,
            single_account_number=client.single_account_number,
            type='charge_note'
        ).build(session).obj

        invoice_turnon.InvoiceTurnOn(invoice, sum=invoice.effective_sum, manual=True).do()

        personal_account.create_receipt(200)

        assert personal_account.receipt_sum == 300
        assert personal_account.consume_sum == 100
        assert personal_account.unused_funds == 200
        assert personal_account.effective_sum == 0
        personal_account.unused_funds_lock = InvoiceReceiptLockType.OVERDRAFT

        assert overdraft.debt.get_locked_invoices(person, {ServiceId.DIRECT}) == []

    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_multiple_persons(self, session, client):
        invoices = []
        for _ in range(2):
            invoice = create_invoice(session, client=client)
            invoice.unused_funds_lock = InvoiceReceiptLockType.OVERDRAFT
            invoice.create_receipt(invoice.effective_sum)
            invoices.append(invoice)

        assert overdraft.debt.get_locked_invoices(invoices[0].person, {ServiceId.DIRECT}) == invoices[0:1]

    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_multiple_invoices(self, session, client):
        invoices = []
        person = None
        for idx in range(3):
            invoice = create_invoice(session, client=client, person=person)
            person = invoice.person
            invoice.unused_funds_lock = InvoiceReceiptLockType.OVERDRAFT
            invoice.create_receipt(invoice.effective_sum)
            invoice.dt = datetime.datetime.now() + datetime.timedelta(idx)
            invoices.append(invoice)

        assert overdraft.debt.get_locked_invoices(person, {ServiceId.DIRECT}) == invoices

    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_multiple_orders(self, session, client):
        orders = [
            ob.OrderBuilder.construct(
                session,
                client=client,
                product_id=DIRECT_PRODUCT_RUB_ID,
                service_id=ServiceId.DIRECT,
            )
            for _ in range(3)
        ]
        invoice = create_invoice(session, client=client, orders=[(o, 10) for o in orders])
        invoice.unused_funds_lock = InvoiceReceiptLockType.OVERDRAFT
        invoice.create_receipt(invoice.effective_sum)

        assert overdraft.debt.get_locked_invoices(invoice.person, {ServiceId.DIRECT}) == [invoice]


class TestOverdueSumQuery(object):
    def _make_act(self, invoice, act_sum=None):
        q, = invoice.consumes
        q.order.calculate_consumption(NOW, {q.order.shipment_type: act_sum or invoice.effective_sum})
        act, = invoice.generate_act(force=1, backdate=NOW)
        return act

    @pytest.mark.parametrize(
        'invoice_sums, receipt_sums, act_sums, res_sum',
        [
            pytest.param([100, 100], [0, 0], [100, 100], 200, id='unpaid'),
            pytest.param([100, 100], [20, 0], [100, 100], 180, id='partial_unpaid'),
            pytest.param([100, 100], [120, 0], [100, 100], 100, id='partial_overpaid'),
            pytest.param([100, 100], [100, 100], [100, 100], 0, id='paid'),
        ]
    )
    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_sum(self, session, client, person, invoice_sums, receipt_sums, act_sums, res_sum):
        for invoice_sum, receipt_sum, act_sum in zip(invoice_sums, receipt_sums, act_sums):
            invoice = create_invoice(session, invoice_sum, 1033, person=person, overdraft=1, client=client)
            invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(666)
            invoice.turn_on_rows()
            if receipt_sum:
                invoice.create_receipt(receipt_sum)
            if act_sum:
                self._make_act(invoice, act_sum)

        assert overdraft.debt.get_overdue_rub_sum(person, NOW, {ServiceId.DIRECT}) == res_sum

    @pytest.mark.parametrize(
        'paysys_id, invoice_sum, receipt_sum, receipt_sum_1c, res_sum',
        [
            pytest.param(1003, 100, 100, 0, 100, id='bank_unpaid'),
            pytest.param(1003, 100, 0, 100, 0, id='bank_paid'),
            pytest.param(1033, 100, 0, 100, 100, id='card_unpaid'),
            pytest.param(1033, 100, 100, 0, 0, id='card_paid'),
        ]
    )
    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_payment_method_sum(self, session, paysys_id, invoice_sum, receipt_sum, receipt_sum_1c, res_sum, client):
        invoice = create_invoice(session, invoice_sum, paysys_id, overdraft=1, client=client)
        invoice.payment_term_dt = NOW - datetime.timedelta(666)
        invoice.turn_on_rows()
        self._make_act(invoice, invoice_sum)
        if receipt_sum:
            invoice.create_receipt(receipt_sum)
        if receipt_sum_1c:
            invoice.receipt_sum_1c = receipt_sum_1c
            session.flush()

        assert overdraft.debt.get_overdue_rub_sum(invoice.person, NOW, {ServiceId.DIRECT}) == res_sum

    @pytest.mark.parametrize(
        'conf_delta, payment_term_delta, has_debt',
        [
            (10, -1, False),
            (10, 0, False),
            (10, 10, False),
            (10, 11, True),
            (666, 666, False),
        ]
    )
    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_payment_term_dt(self, session, conf_delta, payment_term_delta, has_debt, client):
        session.config.__dict__['CANCELLATION_OVERDRAFT_BEGIN_DELTA'] = conf_delta

        invoice = create_invoice(session, overdraft=1, client=client)
        invoice.payment_term_dt = NOW - datetime.timedelta(payment_term_delta)
        invoice.turn_on_rows()
        self._make_act(invoice)

        res = overdraft.debt.get_overdue_rub_sum(invoice.person, NOW, {ServiceId.DIRECT})
        assert res == int(has_debt) * invoice.effective_sum

    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_currency(self, session, client):
        product = ob.ProductBuilder.construct(session, price=1, currency='USD')
        person = ob.PersonBuilder.construct(session, client=client, type='yt')
        with ob.patched_currency([{'RUB': 1, 'USD': 70}]):
            invoice = create_invoice(session, 10, 1013, product.id, person=person, overdraft=1)
        invoice.payment_term_dt = NOW - datetime.timedelta(666)
        invoice.turn_on_rows()
        self._make_act(invoice)

        assert invoice.total_act_sum == 10
        assert overdraft.debt.get_overdue_rub_sum(person, NOW, {ServiceId.DIRECT}) == 700

    @pytest.mark.parametrize(
        'overdraft_flag, has_debt',
        [
            pytest.param(0, False, id='prepayment'),
            pytest.param(1, True, id='overdraft'),
        ]
    )
    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_type(self, session, overdraft_flag, has_debt, client):
        invoice = create_invoice(session, 100, overdraft=overdraft_flag, client=client)
        invoice.payment_term_dt = NOW - datetime.timedelta(666)
        invoice.turn_on_rows()
        self._make_act(invoice)

        res = overdraft.debt.get_overdue_rub_sum(invoice.person, NOW, {ServiceId.DIRECT})
        assert res == int(has_debt) * invoice.effective_sum

    @pytest.mark.parametrize(
        'service_ids, has_debt',
        [
            pytest.param((ServiceId.DIRECT, ServiceId.MARKET,), True, id='direct_market'),
            pytest.param((ServiceId.MARKET,), False, id='market'),
        ]
    )
    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_services(self, session, service_ids, has_debt, client):
        invoice = create_invoice(session, 100, overdraft=1, client=client)
        invoice.payment_term_dt = NOW - datetime.timedelta(666)
        invoice.turn_on_rows()
        self._make_act(invoice)

        res = overdraft.debt.get_overdue_rub_sum(invoice.person, NOW, service_ids)
        assert res == int(has_debt) * invoice.effective_sum

    @pytest.mark.parametrize(
        'invoice_sums, our_fault, debt_sum',
        [
            pytest.param([100, 120], True, 100, id='our_fault'),
            pytest.param([100, 120], False, 220, id='theirs_fault'),
        ]
    )
    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_bad_debt(self, session, client, person, invoice_sums, our_fault, debt_sum):
        acts = []
        for invoice_sum in invoice_sums:
            invoice = create_invoice(session, invoice_sum, client=client, person=person, overdraft=1)
            invoice.payment_term_dt = NOW - datetime.timedelta(666)
            invoice.turn_on_rows()
            acts.append(self._make_act(invoice))

        ob.BadDebtActBuilder.construct(session, act=acts[-1], our_fault=our_fault)

        res = overdraft.debt.get_overdue_rub_sum(person, NOW, {ServiceId.DIRECT})
        assert res == debt_sum

    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_by_invoice(self, session, client, person):
        invoices = []
        for invoice_sum in [100, 200]:
            invoice = create_invoice(session, invoice_sum, client=client, person=person, overdraft=1)
            invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(666)
            invoice.turn_on_rows()
            self._make_act(invoice)
            invoices.append(invoice)

        assert overdraft.debt.get_overdue_rub_sum(person, NOW, {ServiceId.DIRECT}, invoices[-1]) == 200

    @pytest.mark.parametrize(
        'conf_dt, dt_delta, has_debt',
        [
            (10, 9, True),
            (10, 11, False),
        ]
    )
    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_since_dt(self, session, conf_dt, dt_delta, has_debt, client):
        session.config.__dict__['CANCELLATION_OVERDRAFT_SINCE_DT'] = ut.trunc_date(NOW - datetime.timedelta(conf_dt))

        invoice = create_invoice(session, 100, overdraft=1, client=client)
        invoice.payment_term_dt = NOW - datetime.timedelta(666)
        invoice.turn_on_rows()
        invoice.dt = NOW - datetime.timedelta(dt_delta)
        self._make_act(invoice)

        res = overdraft.debt.get_overdue_rub_sum(invoice.person, NOW, {ServiceId.DIRECT})
        assert res == int(has_debt) * invoice.effective_sum

    @pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
    def test_multiple_orders(self, session, client):
        orders = [
            ob.OrderBuilder.construct(
                session,
                client=client,
                product_id=DIRECT_PRODUCT_RUB_ID,
                service_id=ServiceId.DIRECT,
            )
            for _ in range(3)
        ]
        invoice = create_invoice(session, client=client, orders=[(o, 10) for o in orders], overdraft=1)
        invoice.payment_term_dt = NOW - datetime.timedelta(666)
        invoice.turn_on_rows()
        invoice.close_invoice(datetime.datetime.now())

        assert overdraft.debt.get_overdue_rub_sum(invoice.person, NOW, {ServiceId.DIRECT}) == 30
