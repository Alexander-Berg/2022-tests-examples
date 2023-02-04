# -*- coding: utf-8 -*-

import datetime

import pytest
import hamcrest

from balance.actions.invoice_turnon import InvoiceTurnOn
from balance import mapper
from balance.constants import (
    DIRECT_PRODUCT_RUB_ID,
    ServiceId,
    TransferMode,
    InvoiceReceiptLockType,
)

from tests import object_builder as ob
from tests.balance_tests.invoices.invoice_common import (
    create_invoice,
    UR_CARD_PAYSYS_ID,
    UR_BANK_PAYSYS_ID,
)


def make_overdraft_debt(invoice):
    invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(666)
    invoice.turn_on_rows()
    invoice.close_invoice(datetime.datetime.now())
    invoice.session.flush()


@pytest.fixture
def person(session):
    return ob.PersonBuilder.construct(session, type='ur')


class TestLock(object):
    @pytest.mark.parametrize(
        'debt_sums, locked_sums, invoice_sum, payment_sum, is_locked, consume_sum',
        [
            pytest.param([100, 200], [], 150, 150, True, 0, id='single_payment_less'),
            pytest.param([100, 200], [], 666, 666, True, 0, id='single_payment_more'),
            pytest.param([100, 200], [100], 666, 666, True, 0, id='multiple_payment_more'),
            pytest.param([100, 200], [150, 150], 10, 10, False, 10, id='unlocked'),
        ]
    )
    def test_sums(self, session, person, debt_sums, locked_sums, invoice_sum, payment_sum, is_locked, consume_sum):
        invoice = create_invoice(session, invoice_sum, 1033, person=person)

        for debt_sum in debt_sums:
            i = create_invoice(session, debt_sum, person=person, overdraft=1)
            make_overdraft_debt(i)

        for locked_sum in locked_sums:
            i = create_invoice(session, locked_sum, person=person)
            i.create_receipt(locked_sum)

        InvoiceTurnOn(invoice, auto=True, manual=False, sum=payment_sum).do()

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=payment_sum,
                consume_sum=consume_sum,
                unused_funds_lock=InvoiceReceiptLockType.OVERDRAFT if is_locked else InvoiceReceiptLockType.OFF
            )
        )

    def test_manual(self, session, person):
        invoice = create_invoice(session, 100, 1033, person=person)

        overdraft_invoice = create_invoice(session, 666, person=person, overdraft=1)
        make_overdraft_debt(overdraft_invoice)

        InvoiceTurnOn(invoice, auto=False, manual=True, sum=100).do()

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=100,
                consume_sum=100,
                unused_funds_lock=InvoiceReceiptLockType.OFF
            )
        )

    def test_services(self, session, person):
        client = person.client
        order = ob.OrderBuilder.construct(
            session,
            client=client,
            product_id=DIRECT_PRODUCT_RUB_ID,
            service_id=ServiceId.DIRECT
        )
        other_order = ob.OrderBuilder.construct(
            session,
            client=client,
            product_id=DIRECT_PRODUCT_RUB_ID,
            service_id=ServiceId.MARKET
        )

        invoice = create_invoice(session, person=person, orders=[(order, 100)])
        invoice.receipt_sum_1c = 100

        overdraft_invoice = create_invoice(session, person=person, orders=[(other_order, 666)], overdraft=1)
        make_overdraft_debt(overdraft_invoice)

        InvoiceTurnOn(invoice, auto=True, manual=False, sum=100).do()

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=100,
                consume_sum=100,
                unused_funds_lock=InvoiceReceiptLockType.OFF
            )
        )


class TestUnlock(object):
    @pytest.mark.parametrize(
        'locked_invoices, debt_invoices, invoices_params',
        [
            pytest.param(
                [(100, InvoiceReceiptLockType.OVERDRAFT), (200, InvoiceReceiptLockType.OVERDRAFT)],
                [(300, 0)],
                [(0, InvoiceReceiptLockType.OVERDRAFT), (0, InvoiceReceiptLockType.OVERDRAFT)],
                id='all_debt'
            ),
            pytest.param(
                [(100, InvoiceReceiptLockType.OVERDRAFT), (200, InvoiceReceiptLockType.OVERDRAFT)],
                [(200, 0)],
                [(100, InvoiceReceiptLockType.OVERDRAFT), (0, InvoiceReceiptLockType.OVERDRAFT)],
                id='part_debt'
            ),
            pytest.param(
                [(100, InvoiceReceiptLockType.OVERDRAFT), (200, InvoiceReceiptLockType.OVERDRAFT)],
                [(201, 0)],
                [(0, InvoiceReceiptLockType.OVERDRAFT), (0, InvoiceReceiptLockType.OVERDRAFT)],
                id='part_debt_not_enough'
            ),
            pytest.param(
                [(100, InvoiceReceiptLockType.OVERDRAFT), (200, InvoiceReceiptLockType.OVERDRAFT)],
                [(150, 0), (150, 0)],
                [(0, InvoiceReceiptLockType.OVERDRAFT), (0, InvoiceReceiptLockType.OVERDRAFT)],
                id='multiple_debts'
            ),
            pytest.param(
                [(100, InvoiceReceiptLockType.OVERDRAFT), (200, InvoiceReceiptLockType.OVERDRAFT)],
                [],
                [(100, InvoiceReceiptLockType.OVERDRAFT), (200, InvoiceReceiptLockType.OVERDRAFT)],
                id='no_debt'
            ),
            pytest.param(
                [(100, InvoiceReceiptLockType.OVERDRAFT), (200, InvoiceReceiptLockType.OVERDRAFT)],
                [(666, 666)],
                [(100, InvoiceReceiptLockType.OVERDRAFT), (200, InvoiceReceiptLockType.OVERDRAFT)],
                id='paid_debt'
            ),
            pytest.param(
                [
                    (300, InvoiceReceiptLockType.REFUND),
                    (400, InvoiceReceiptLockType.TRANSFER),
                    (500, InvoiceReceiptLockType.OFF),
                    (600, None),
                    (100, InvoiceReceiptLockType.OVERDRAFT),
                    (200, InvoiceReceiptLockType.OVERDRAFT),
                ],
                [(100, 0)],
                [
                    (0, InvoiceReceiptLockType.REFUND),
                    (0, InvoiceReceiptLockType.TRANSFER),
                    (0, InvoiceReceiptLockType.OFF),
                    (0, InvoiceReceiptLockType.OFF),
                    (100, InvoiceReceiptLockType.OVERDRAFT),
                    (0, InvoiceReceiptLockType.OVERDRAFT),
                ],
                id='wrong_lock_type'
            )
        ]
    )
    def test_unlock(self, session, person, locked_invoices, debt_invoices, invoices_params):
        locked_invoices_objs = []

        for invoice_sum, lock_type in locked_invoices:
            i = create_invoice(session, invoice_sum, UR_CARD_PAYSYS_ID, person=person)
            i.unused_funds_lock = lock_type
            i.create_receipt(invoice_sum)
            locked_invoices_objs.append(i)

        for invoice_sum, receipt_sum in debt_invoices:
            i = create_invoice(session, invoice_sum, UR_CARD_PAYSYS_ID, person=person, overdraft=1)
            make_overdraft_debt(i)
            if receipt_sum:
                i.create_receipt(receipt_sum)

        invoice = create_invoice(session, 100, UR_CARD_PAYSYS_ID, person=person, overdraft=1)
        make_overdraft_debt(invoice)

        InvoiceTurnOn(invoice, auto=True, manual=False, sum=100).do()

        hamcrest.assert_that(
            locked_invoices_objs,
            hamcrest.contains(*[
                hamcrest.has_properties(
                    consume_sum=cons_sum,
                    unused_funds_lock=lock_type
                )
                for cons_sum, lock_type in invoices_params
            ])
        )

    def test_manual(self, session, person):
        locked_invoice = create_invoice(session, 100, UR_CARD_PAYSYS_ID, person=person)
        locked_invoice.unused_funds_lock = InvoiceReceiptLockType.OVERDRAFT
        locked_invoice.create_receipt(100)

        overdraft_invoice = create_invoice(session, 100, UR_CARD_PAYSYS_ID, person=person, overdraft=1)
        make_overdraft_debt(overdraft_invoice)

        InvoiceTurnOn(overdraft_invoice, auto=True, manual=True, sum=100).do()

        assert locked_invoice.consume_sum == 0

    @pytest.mark.parametrize(
        'initial_sum, consume_sum, receipt_sum, is_turned_on',
        [
            (100, 50, 50, True),
            (100, 50, 40, False),
            (100, 50, 60, False),
            (100, 110, 100, False),
        ]
    )
    def test_payment_sum(self, session, person, initial_sum, consume_sum, receipt_sum, is_turned_on):
        locked_invoice = create_invoice(session, 100, UR_CARD_PAYSYS_ID, person=person)
        locked_invoice.unused_funds_lock = InvoiceReceiptLockType.OVERDRAFT
        locked_invoice.create_receipt(100)

        overdraft_invoice = create_invoice(session, initial_sum, UR_CARD_PAYSYS_ID, person=person, overdraft=1)
        overdraft_invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(666)
        io, = overdraft_invoice.invoice_orders
        overdraft_invoice.transfer(io.order, TransferMode.dst, consume_sum, skip_check=True)
        overdraft_invoice.close_invoice(datetime.datetime.now())
        session.flush()

        InvoiceTurnOn(overdraft_invoice, auto=True, manual=False, sum=receipt_sum).do()

        if is_turned_on:
            assert locked_invoice.consume_sum == 100
        else:
            assert locked_invoice.consume_sum == 0

    def test_invoice_with_debt(self, session, person):
        locked_invoice = create_invoice(session, 100, UR_CARD_PAYSYS_ID, person=person)
        locked_invoice.unused_funds_lock = InvoiceReceiptLockType.OVERDRAFT
        locked_invoice.create_receipt(100)

        overdraft_invoice = create_invoice(session, 100, UR_BANK_PAYSYS_ID, person=person, overdraft=1)
        make_overdraft_debt(overdraft_invoice)

        InvoiceTurnOn(overdraft_invoice, auto=True, manual=False, sum=100).do()

        assert locked_invoice.consume_sum == 0

    def test_services(self, session, person):
        order = ob.OrderBuilder.construct(
            session,
            client=person.client,
            product_id=DIRECT_PRODUCT_RUB_ID,
            service_id=ServiceId.DIRECT
        )
        other_order = ob.OrderBuilder.construct(
            session,
            client=person.client,
            product_id=DIRECT_PRODUCT_RUB_ID,
            service_id=ServiceId.MARKET
        )

        locked_invoice = create_invoice(session, paysys_id=UR_CARD_PAYSYS_ID, person=person, orders=[(order, 100)])
        locked_invoice.unused_funds_lock = InvoiceReceiptLockType.OVERDRAFT
        locked_invoice.create_receipt(100)

        overdraft_invoice = create_invoice(
            session,
            paysys_id=UR_CARD_PAYSYS_ID,
            person=person,
            orders=[(other_order, 100)],
            overdraft=1
        )
        make_overdraft_debt(overdraft_invoice)

        InvoiceTurnOn(overdraft_invoice, auto=True, manual=False, sum=100).do()

        assert locked_invoice.consume_sum == 0
