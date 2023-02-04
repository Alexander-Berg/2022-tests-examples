# -*- coding: utf-8 -*-

import pytest
import hamcrest

from balance.constants import (
    ServiceId,
)

from tests import object_builder as ob
from tests.balance_tests.invoices.invoice_common import (
    create_personal_account,
    create_invoice,
    create_charge_note_register,
    create_order,
)

UR_BANK_PAYSYS_ID = 1003
UR_CARD_PAYSYS_ID = 1033


@pytest.fixture
def service(session):
    service = ob.ServiceBuilder.construct(session)
    service.balance_service.extra_pay = True
    service.balance_service.chargenote_scheme = True
    session.flush()

    return service


class TestPrepayment(object):
    @pytest.mark.parametrize(
        'paysys_id',
        [
            pytest.param(UR_BANK_PAYSYS_ID, id='bank'),
            pytest.param(UR_CARD_PAYSYS_ID, id='card'),
        ])
    def test_empty(self, session, paysys_id):
        invoice = create_invoice(session, 100, paysys_id)

        assert invoice.confirmed_amount == 0

    def test_bank(self, session):
        invoice = create_invoice(session, 100, UR_BANK_PAYSYS_ID)
        ob.OebsCashPaymentFactBuilder.construct(session, invoice=invoice, amount=90)
        session.expire_all()

        assert invoice.confirmed_amount == 90

    def test_instant_wo_cpf(self, session):
        invoice = create_invoice(session, 100, UR_CARD_PAYSYS_ID)
        payment = ob.CardPaymentBuilder.construct(session, invoice=invoice)
        payment.receipt_sum = 100
        session.flush()

        assert invoice.confirmed_amount == 100

    def test_instant_w_cpf(self, session):
        invoice = create_invoice(session, 100, UR_CARD_PAYSYS_ID)
        payment = ob.CardPaymentBuilder.construct(session, invoice=invoice)
        payment.receipt_sum = 100
        ob.OebsCashPaymentFactBuilder.construct(
            session,
            invoice=invoice,
            amount=100,
            orig_id=payment.id
        )
        session.expire_all()

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=100,
                confirmed_amount=100
            )
        )
        hamcrest.assert_that(
            payment,
            hamcrest.has_properties(
                receipt_sum=100,
                receipt_sum_1c=100
            )
        )

    def test_instant_w_unlinked_cpf(self, session):
        invoice = create_invoice(session, 100, UR_CARD_PAYSYS_ID)
        payment = ob.CardPaymentBuilder.construct(session, invoice=invoice)
        payment.receipt_sum = 100
        ob.OebsCashPaymentFactBuilder.construct(session, invoice=invoice, amount=90)
        session.expire_all()

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=90,
                confirmed_amount=190
            )
        )
        hamcrest.assert_that(
            payment,
            hamcrest.has_properties(
                receipt_sum=100,
                receipt_sum_1c=None
            )
        )


class TestPersonalAccount(object):
    @staticmethod
    def create_invoice(pa,
                       qty=100,
                       paysys_id=UR_BANK_PAYSYS_ID,
                       service_id=ServiceId.DIRECT,
                       turn_on_rows=False):
        return create_invoice(
            pa.session,
            qty,
            paysys_id,
            service_id=service_id,
            contract=pa.contract,
            requested_type='charge_note',
            turn_on_rows=turn_on_rows
        )

    def test_bank(self, session, service):
        pa = create_personal_account(session, service_id=service.id)
        ob.OebsCashPaymentFactBuilder.construct(session, invoice=pa, amount=666)
        session.expire_all()

        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum_1c=666,
                confirmed_amount=666
            )
        )

    def test_charge_note_instant(self, session, service):
        pa = create_personal_account(session, service_id=service.id)
        charge_note = self.create_invoice(pa, 100, UR_CARD_PAYSYS_ID, service.id)
        payment = ob.CardPaymentBuilder.construct(session, invoice=charge_note)
        payment.receipt_sum = 100
        session.flush()

        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum_1c=0,
                confirmed_amount=100
            )
        )
        hamcrest.assert_that(
            payment,
            hamcrest.has_properties(
                receipt_sum=100,
                receipt_sum_1c=None
            )
        )

    def test_charge_note_instant_w_cpf(self, session, service):
        pa = create_personal_account(session, service_id=service.id)
        charge_note = self.create_invoice(pa, 100, UR_CARD_PAYSYS_ID, service.id)
        payment = ob.CardPaymentBuilder.construct(session, invoice=charge_note)
        payment.receipt_sum = 100
        ob.OebsCashPaymentFactBuilder.construct(
            session,
            invoice=pa,
            amount=100,
            orig_id=payment.id
        )
        session.expire_all()

        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum_1c=100,
                confirmed_amount=100
            )
        )
        hamcrest.assert_that(
            payment,
            hamcrest.has_properties(
                receipt_sum=100,
                receipt_sum_1c=100
            )
        )

    @pytest.mark.parametrize(
        'initial_sum, payment_receipt_sum, confirmed_amount',
        [
            pytest.param(100, 0, 0, id='before_turnon'),
            pytest.param(100, 100, 100, id='after_turnon'),
        ]
    )
    def test_charge_note_bank(self, session, service, initial_sum, payment_receipt_sum, confirmed_amount):
        pa = create_personal_account(session, service_id=service.id)
        charge_note = self.create_invoice(pa, initial_sum, UR_BANK_PAYSYS_ID, service.id, turn_on_rows=True)
        payment, = charge_note.payments
        payment.receipt_sum = payment_receipt_sum
        ob.OebsCashPaymentFactBuilder.construct(
            session,
            invoice=pa,
            amount=initial_sum,
            orig_id=payment.id
        )
        session.expire_all()

        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum_1c=initial_sum,
                confirmed_amount=confirmed_amount
            )
        )
        hamcrest.assert_that(
            payment,
            hamcrest.has_properties(
                receipt_sum=payment_receipt_sum,
                receipt_sum_1c=initial_sum
            )
        )


@pytest.mark.charge_note_register
class TestChargeNoteRegister(object):
    def test_instant_wo_cpf(self, session, service):
        pa = create_personal_account(session, service_id=service.id)
        order = create_order(pa.client, service.id)
        invoice = create_invoice(session, contract=pa.contract, orders=[(order, 200)])

        charge_note = create_charge_note_register(
            UR_CARD_PAYSYS_ID,
            contract=pa.contract,
            orders=[(order, 100)],
            invoices=[invoice]
        )
        session.expire_all()
        payment = ob.CardPaymentBuilder.construct(session, invoice=charge_note)
        payment.turn_on()
        session.flush()

        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum=100,
                confirmed_amount=100
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=200,
                confirmed_amount=200
            )
        )

    def test_instant_w_cpf(self, session, service):
        pa = create_personal_account(session, service_id=service.id)
        order = create_order(pa.client, service.id)
        invoice = create_invoice(session, contract=pa.contract, orders=[(order, 200)])

        charge_note = create_charge_note_register(
            UR_CARD_PAYSYS_ID,
            contract=pa.contract,
            orders=[(order, 100)],
            invoices=[invoice]
        )
        session.expire_all()
        payment = ob.CardPaymentBuilder.construct(session, invoice=charge_note)
        payment.turn_on()
        ob.OebsCashPaymentFactBuilder.construct(
            session,
            invoice=pa,
            amount=100,
            orig_id=payment.id
        )
        ob.OebsCashPaymentFactBuilder.construct(
            session,
            invoice=invoice,
            amount=200,
            orig_id=payment.id
        )
        session.expire_all()

        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum=100,
                receipt_sum_1c=100,
                confirmed_amount=100
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=200,
                receipt_sum_1c=200,
                confirmed_amount=200
            )
        )

    def test_bank_before_turnon(self, session, service):
        pa = create_personal_account(session, service_id=service.id)
        order = create_order(pa.client, service.id)
        invoice = create_invoice(session, contract=pa.contract, orders=[(order, 200)])

        charge_note = create_charge_note_register(
            UR_BANK_PAYSYS_ID,
            contract=pa.contract,
            orders=[(order, 100)],
            invoices=[invoice]
        )
        payment, = charge_note.payments
        ob.OebsCashPaymentFactBuilder.construct(
            session,
            invoice=pa,
            amount=100,
            orig_id=payment.id
        )
        ob.OebsCashPaymentFactBuilder.construct(
            session,
            invoice=invoice,
            amount=200,
            orig_id=payment.id
        )
        session.expire_all()

        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum=0,
                receipt_sum_1c=100,
                confirmed_amount=0
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=0,
                receipt_sum_1c=200,
                confirmed_amount=0
            )
        )

    def test_bank_after_turnon(self, session, service):
        pa = create_personal_account(session, service_id=service.id)
        order = create_order(pa.client, service.id)
        invoice = create_invoice(session, contract=pa.contract, orders=[(order, 200)])

        charge_note = create_charge_note_register(
            UR_BANK_PAYSYS_ID,
            contract=pa.contract,
            orders=[(order, 100)],
            invoices=[invoice]
        )
        payment, = charge_note.payments
        ob.OebsCashPaymentFactBuilder.construct(
            session,
            invoice=pa,
            amount=100,
            orig_id=payment.id
        )
        ob.OebsCashPaymentFactBuilder.construct(
            session,
            invoice=invoice,
            amount=200,
            orig_id=payment.id
        )
        session.expire_all()
        payment.turn_on()

        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum=100,
                receipt_sum_1c=100,
                confirmed_amount=100
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=200,
                receipt_sum_1c=200,
                confirmed_amount=200
            )
        )
