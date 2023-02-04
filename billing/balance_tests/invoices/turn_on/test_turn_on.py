# -*- coding: utf-8 -*-

import pytest
import hamcrest

from balance.actions.invoice_turnon import InvoiceTurnOn
from balance.constants import (
    DIRECT_PRODUCT_RUB_ID,
    FirmId,
    ServiceId,
    TransferMode,
    RegionId,
    InvoiceReceiptLockType,
)

from tests import object_builder as ob
from tests.balance_tests.invoices.invoice_common import (
    create_personal_account,
    create_invoice,
    create_charge_note_register,
    UR_BANK_PAYSYS_ID,
    UR_CARD_PAYSYS_ID,
    UR_CERT_PAYSYS_ID,
    YT_BANK_PAYSYS_ID,
)
from tests.balance_tests.pay_policy.pay_policy_common import create_pay_policy


@pytest.fixture
def service(session):
    service = ob.ServiceBuilder.construct(session)
    service.balance_service.extra_pay = True
    service.balance_service.chargenote_scheme = True
    session.flush()

    return service


class TestPrepay(object):
    @pytest.mark.parametrize(
        'paysys_id, manual, initial_sum, consume_sum',
        [
            pytest.param(UR_BANK_PAYSYS_ID, False, 100, 0, id='bank'),
            pytest.param(UR_BANK_PAYSYS_ID, True, 100, 100, id='bank_manual'),
            pytest.param(UR_CARD_PAYSYS_ID, False, 100, 100, id='card'),
            pytest.param(UR_CARD_PAYSYS_ID, True, 100, 100, id='card_manual'),
            pytest.param(UR_CERT_PAYSYS_ID, False, 100, 100, id='cert'),
        ]
    )
    def test_empty_unpaid(self, session, paysys_id, manual, initial_sum, consume_sum):
        invoice = create_invoice(session, initial_sum, paysys_id)
        InvoiceTurnOn(invoice, manual=manual, auto=False).do()

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=initial_sum,
                consume_sum=consume_sum
            )
        )

    @pytest.mark.parametrize(
        'paysys_id, initial_sum, turnon_sum, receipt_sum, consume_sum',
        [
            pytest.param(UR_BANK_PAYSYS_ID, 100, None, 100, 100, id='bank_none_sum'),
            pytest.param(UR_CARD_PAYSYS_ID, 100, None, 100, 100, id='card_none_sum'),
            pytest.param(UR_BANK_PAYSYS_ID, 100, 0, 0, 100, id='bank_zero_sum'),
            pytest.param(UR_CARD_PAYSYS_ID, 100, 0, 0, 0, id='card_zero_sum'),
            pytest.param(UR_BANK_PAYSYS_ID, 100, 100, 100, 100, id='bank_nonzero_sum'),
            pytest.param(UR_CARD_PAYSYS_ID, 100, 100, 100, 100, id='card_nonzero_sum'),
        ]
    )
    def test_empty_paid(self, session, paysys_id, initial_sum, turnon_sum, receipt_sum, consume_sum):
        invoice = create_invoice(session, initial_sum, paysys_id)
        invoice.receipt_sum_1c = initial_sum
        session.flush()

        InvoiceTurnOn(invoice, manual=False, auto=False, sum=turnon_sum).do()

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=receipt_sum,
                consume_sum=consume_sum
            )
        )

    @pytest.mark.parametrize(
        'auto, initial_sum, turnon_sum, receipt_sum, consume_sum',
        [
            pytest.param(False, 100, None, 200, 200, id='not_auto_none_sum'),
            pytest.param(False, 100, 0, 100, 200, id='not_auto_zero_sum'),
            pytest.param(True, 100, None, 200, 100, id='auto_none_sum'),
            pytest.param(True, 100, 0, 100, 100, id='auto_zero_sum'),  # единственный адекватный сценарий, чёрт возьми
        ]
    )
    def test_already_turned_on(self, session, auto, initial_sum, turnon_sum, receipt_sum, consume_sum):
        invoice = create_invoice(session, initial_sum)
        invoice.receipt_sum_1c = initial_sum
        invoice.create_receipt(initial_sum)
        invoice.transfer(invoice.invoice_orders[0].order, TransferMode.all)
        session.flush()

        InvoiceTurnOn(invoice, auto=auto, manual=False, sum=turnon_sum).do()

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=receipt_sum,
                consume_sum=consume_sum
            )
        )

    @pytest.mark.parametrize(
        'initial_sum, consume_sum, turnon_sum, receipt_sum',
        [
            pytest.param(100, 50, 60, 50),
            pytest.param(100, 50, 40, 60),
        ]
    )
    def test_auto_sum_negative(self, session, initial_sum, consume_sum, turnon_sum, receipt_sum):
        invoice = create_invoice(session, initial_sum)
        invoice.receipt_sum_1c = initial_sum
        invoice.create_receipt(initial_sum)
        invoice.transfer(invoice.invoice_orders[0].order, TransferMode.dst, consume_sum)
        session.flush()

        InvoiceTurnOn(invoice, auto=True, manual=False, sum=-turnon_sum).do()

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=receipt_sum,
                consume_sum=consume_sum
            )
        )

    @pytest.mark.parametrize(
        'paysys_id, initial_sum, turnon_sum, consume_sum, receipt_sum',
        [
            pytest.param(UR_BANK_PAYSYS_ID, 100, 90, 0, 90, id='resident_underpaid'),
            pytest.param(UR_BANK_PAYSYS_ID, 100, 110, 100, 110, id='resident_overpaid'),
            pytest.param(YT_BANK_PAYSYS_ID, 100, 90, 0, 90, id='nonresident_underpaid'),
            pytest.param(YT_BANK_PAYSYS_ID, 100, 110, 0, 110, id='nonresident_overpaid'),
        ]
    )
    def test_unequal_sum(self, session, paysys_id, initial_sum, turnon_sum, consume_sum, receipt_sum):
        invoice = create_invoice(session, initial_sum, paysys_id)
        invoice.receipt_sum_1c = turnon_sum

        InvoiceTurnOn(invoice, auto=True, manual=False, sum=turnon_sum).do()

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=receipt_sum,
                consume_sum=consume_sum
            )
        )


class TestChargeNote(object):
    def create_charge_note(self, session, qty=100,
                           pa_paysys_id=UR_BANK_PAYSYS_ID,
                           paysys_id=UR_BANK_PAYSYS_ID,
                           product_id=DIRECT_PRODUCT_RUB_ID,
                           service_id=ServiceId.DIRECT):
        pa = create_personal_account(session, pa_paysys_id, service_id=service_id)

        charge_note = create_invoice(
            session,
            qty,
            paysys_id,
            product_id,
            contract=pa.contract,
            requested_type='charge_note',
            service_id=service_id
        )
        return charge_note, pa

    @pytest.mark.parametrize(
        'turn_on_rows, initial_sum, consume_sum',
        [
            pytest.param(False, 100, 0, id='without_consumption'),
            pytest.param(True, 100, 100, id='with_consumption')
        ]
    )
    def test_personal_account_transference(self, session, service, turn_on_rows, initial_sum, consume_sum):
        charge_note, pa = self.create_charge_note(session, initial_sum, service_id=service.id)
        charge_note.request.turn_on_rows = turn_on_rows

        InvoiceTurnOn(charge_note, auto=True, sum=initial_sum).do()

        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum=initial_sum,
                consume_sum=consume_sum
            )
        )


@pytest.mark.charge_note_register
class TestChargeNoteRegister(object):
    def test_invoices_w_pa_full(self, session, service):
        pa = create_personal_account(session, service_id=service.id)
        order = ob.OrderBuilder.construct(
            session,
            client=pa.client,
            product_id=DIRECT_PRODUCT_RUB_ID,
            service_id=service.id
        )
        invoices = [
            create_invoice(session, contract=pa.contract, orders=[(order, 222)])
            for _ in range(3)
        ]

        charge_note = create_charge_note_register(
            UR_CARD_PAYSYS_ID,
            contract=pa.contract,
            orders=[(order, 100)],
            invoices=invoices
        )

        InvoiceTurnOn(charge_note).do()

        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum=100,
                consume_sum=100,
                consumes=hamcrest.contains(hamcrest.has_properties(order=order))
            )
        )
        hamcrest.assert_that(
            invoices,
            hamcrest.only_contains(
                hamcrest.has_properties(
                    receipt_sum=222,
                    consume_sum=0
                )
            )
        )
        hamcrest.assert_that(
            charge_note.register_rows,
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(
                    ref_invoice=pa,
                    receipt_sum=100,
                    consume_sum=100,
                ),
                *[
                    hamcrest.has_properties(
                        ref_invoice=invoice,
                        receipt_sum=222
                    )
                    for invoice in invoices
                ]
            )
        )

    @pytest.mark.parametrize(
        'initial_sum, turnon_sum, pa_receipt_sum, pa_consume_sum, inv_receipt_sum',
        [
            (100, 80, 80, 0, 0),
            (100, 110, 100, 100, 10),
        ]
    )
    def test_invoices_w_pa_part(self, session,
                                service,
                                initial_sum,
                                turnon_sum,
                                pa_receipt_sum,
                                pa_consume_sum,
                                inv_receipt_sum):
        pa = create_personal_account(session, service_id=service.id)
        order = ob.OrderBuilder.construct(
            session,
            client=pa.client,
            product_id=DIRECT_PRODUCT_RUB_ID,
            service_id=service.id
        )

        invoice = create_invoice(session, contract=pa.contract, orders=[(order, initial_sum)])
        charge_note = create_charge_note_register(
            UR_CARD_PAYSYS_ID,
            contract=pa.contract,
            orders=[(order, initial_sum)],
            invoices=[invoice]
        )

        InvoiceTurnOn(charge_note, sum=turnon_sum).do()

        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum=pa_receipt_sum,
                consume_sum=pa_consume_sum
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=inv_receipt_sum,
                consume_sum=0
            )
        )
        hamcrest.assert_that(
            charge_note.register_rows,
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(
                    ref_invoice=pa,
                    receipt_sum=pa_receipt_sum,
                    consume_sum=pa_consume_sum,
                ),
                hamcrest.has_properties(
                    ref_invoice=invoice,
                    receipt_sum=inv_receipt_sum,
                ),
            )
        )

    @pytest.mark.parametrize(
        'initial_sum, turnon_sum, receipt_sum1, receipt_sum2',
        [
            (100, 80, 80, 0),
            (100, 200, 100, 100),
            (100, 220, 120, 100),
            (100, 330, 230, 100),
        ]
    )
    def test_invoices_wo_pa(self, session, service, initial_sum, turnon_sum, receipt_sum1, receipt_sum2):
        create_pay_policy(
            session, firm_id=FirmId.YANDEX_OOO, region_id=RegionId.RUSSIA, service_id=service.id,
            paymethods_params=[('USD', 1001)]
        )
        invoice1 = create_invoice(session, initial_sum, service_id=service.id)
        person = invoice1.person
        invoice2 = create_invoice(session, initial_sum, service_id=service.id, person=person)

        charge_note = create_charge_note_register(
            UR_CARD_PAYSYS_ID,
            person=person,
            orders=[],
            invoices=[invoice1, invoice2]
        )

        InvoiceTurnOn(charge_note, sum=turnon_sum).do()

        hamcrest.assert_that(
            invoice1,
            hamcrest.has_properties(
                receipt_sum=receipt_sum1,
                consume_sum=0
            )
        )
        hamcrest.assert_that(
            invoice2,
            hamcrest.has_properties(
                receipt_sum=receipt_sum2,
                consume_sum=0
            )
        )
        hamcrest.assert_that(
            charge_note.register_rows,
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(
                    ref_invoice=invoice1,
                    receipt_sum=receipt_sum1,
                ),
                hamcrest.has_properties(
                    ref_invoice=invoice2,
                    receipt_sum=receipt_sum2,
                ),
            )
        )

    def test_consecutive_turnons(self, session, service):
        pa = create_personal_account(session, service_id=service.id)
        order = ob.OrderBuilder.construct(
            session,
            client=pa.client,
            product_id=DIRECT_PRODUCT_RUB_ID,
            service_id=service.id
        )
        invoice = create_invoice(session, contract=pa.contract, orders=[(order, 100)])

        charge_note = create_charge_note_register(
            UR_CARD_PAYSYS_ID,
            contract=pa.contract,
            orders=[(order, 100)],
            invoices=[invoice]
        )

        InvoiceTurnOn(charge_note, sum=70).do()

        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum=70,
                consume_sum=0,
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(receipt_sum=0)
        )
        hamcrest.assert_that(
            charge_note.register_rows,
            hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        ref_invoice=pa,
                        receipt_sum=70,
                        consume_sum=0
                    ),
                    hamcrest.has_properties(
                        ref_invoice=invoice,
                        receipt_sum=0
                    )
            )
        )

        InvoiceTurnOn(charge_note, sum=140).do()

        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum=110,
                consume_sum=100,
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(receipt_sum=100)
        )
        hamcrest.assert_that(
            charge_note.register_rows,
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(
                    ref_invoice=pa,
                    receipt_sum=110,
                    consume_sum=100
                ),
                hamcrest.has_properties(
                    ref_invoice=invoice,
                    receipt_sum=100
                )
            )
        )

        InvoiceTurnOn(charge_note, sum=10).do()

        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum=120,
                consume_sum=100,
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(receipt_sum=100)
        )
        hamcrest.assert_that(
            charge_note.register_rows,
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(
                    ref_invoice=pa,
                    receipt_sum=120,
                    consume_sum=100
                ),
                hamcrest.has_properties(
                    ref_invoice=invoice,
                    receipt_sum=100
                )
            )
        )

    def test_bank_with_receipt_sum_1c(self, session, service):
        pa = create_personal_account(session, service_id=service.id)
        order = ob.OrderBuilder.construct(
            session,
            client=pa.client,
            product_id=DIRECT_PRODUCT_RUB_ID,
            service_id=service.id
        )
        invoice1, invoice2 = [
            create_invoice(session, contract=pa.contract, orders=[(order, 222)])
            for _ in range(2)
        ]

        charge_note = create_charge_note_register(
            UR_BANK_PAYSYS_ID,
            contract=pa.contract,
            orders=[(order, 100)],
            invoices=[invoice1, invoice2],
        )
        session.expire_all()
        row1, row2, row3 = charge_note.register_rows
        row1.receipt_sum_1c = 70
        row3.receipt_sum_1c = 80
        session.flush()

        InvoiceTurnOn(charge_note, sum=170).do()

        hamcrest.assert_that(
            row1,
            hamcrest.has_properties(
                receipt_sum=70,
                receipt_sum_1c=70,
                is_internal=1
            )
        )
        hamcrest.assert_that(
            row2,
            hamcrest.has_properties(
                receipt_sum=20,
                receipt_sum_1c=0,
                is_internal=0
            )
        )
        hamcrest.assert_that(
            row3,
            hamcrest.has_properties(
                receipt_sum=80,
                receipt_sum_1c=80,
                is_internal=0
            )
        )
