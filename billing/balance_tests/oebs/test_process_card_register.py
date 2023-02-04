# -*- coding: utf-8 -*-

import datetime
import mock
import pytest

from balance.processors.oebs import CardRegisterExportController
from butils.decimal_unit import DecimalUnit as DU
from tests.balance_tests.oebs.conftest import (_create_request,
                      create_chargenote,
                      create_invoice_refund,
                      create_trust_api_payment,
                      create_trust_payment,
                      create_refund,
                      create_person,
                      create_card_register,
                      create_export_obj,
                      create_payment)
from tests import object_builder as ob

NOW = datetime.datetime.now()

TRUST_REFUND_AMOUNT = DU('150', 'RUB')
PAYMENT_AMOUNT = DU('666', 'RUB')
LOGICAL_PAYMENT_AMOUNT = DU('444', 'RUB')
INVOICE_REFUND_AMOUNT = DU('777', 'RUB')
CHARGE_INVOICE_AMOUNT = DU('888', 'RUB')


@pytest.mark.parametrize('amount_multiplier', [-1, 1])
@pytest.mark.parametrize('refund_amount', [DU('119.99', 'RUB'),
                                           DU('120.00', 'RUB'),
                                           DU('120.01', 'RUB')])
def test_chargenote_chargeback_w_invoice_refund(session, order, paysys, chargeback_terminal, refund_amount,
                                                amount_multiplier):
    """наш возврат по chargenote в случае точной оплаты, недоплаты, предоплаты"""
    client = order.client
    person = create_person(session, type='ph', client=client)
    request_obj = _create_request(session, client, [(order, 4)])
    refunded_invoice = ob.InvoiceBuilder.construct(session, paysys=paysys, person=person, request=request_obj)
    charge_invoice = ob.InvoiceBuilder.construct(session, paysys=paysys, person=person, request=request_obj)

    property_mock_charge_note_personal_account = mock.PropertyMock(return_value=charge_invoice)
    with mock.patch('balance.actions.invoice_create.InvoiceFactory.charge_note_personal_account',
                    new_callable=property_mock_charge_note_personal_account):
        charge_note_register_invoice = create_chargenote(session, request_obj, client, paysys, person,
                                                         single_account_number=123)
    invoice_refund = create_invoice_refund(session, refunded_invoice, amount=INVOICE_REFUND_AMOUNT)
    trust_payment = create_trust_payment(session, amount=PAYMENT_AMOUNT)
    create_trust_api_payment(session, amount=LOGICAL_PAYMENT_AMOUNT, invoice=charge_note_register_invoice,
                             transaction_id=trust_payment.purchase_token)
    refund = create_refund(session, amount_multiplier * refund_amount, trust_payment,
                           trust_refund_id=invoice_refund.system_uid)
    chargeback_card_register = create_card_register(session, chargeback_terminal.id)
    refund.register = chargeback_card_register
    export_obj = create_export_obj(session, chargeback_card_register)
    session.flush()
    payment_info = CardRegisterExportController(export_obj).format_payments_info()
    assert sorted(payment_info) == sorted([(refund.id,
                                            refunded_invoice.external_id,
                                            refund_amount,
                                            refund.payment_dt,
                                            refund.approval_code)])


@pytest.mark.parametrize('amount_multiplier', [-1, 1])
@pytest.mark.parametrize('refund_amount', [DU('119.99', 'RUB'),
                                           DU('120.00', 'RUB'),
                                           DU('120.01', 'RUB')])
def test_chargenote_chargeback_wo_invoice_refund(session, order, paysys, refund_amount, chargeback_terminal,
                                                 amount_multiplier):
    """трастовый возврат по chargenote в случае точной оплаты, недоплаты, предоплаты"""
    client = order.client
    person = create_person(session, type='ph', client=client)
    request_obj = _create_request(session, client, [(order, 4)])
    charge_invoice = ob.InvoiceBuilder.construct(session, paysys=paysys, person=person, request=request_obj)

    property_mock_charge_note_personal_account = mock.PropertyMock(return_value=charge_invoice)
    with mock.patch('balance.actions.invoice_create.InvoiceFactory.charge_note_personal_account',
                    new_callable=property_mock_charge_note_personal_account):
        charge_note_register_invoice = create_chargenote(session, request_obj, client, paysys, person,
                                                         single_account_number=123)
        charge_note_register_invoice.register_rows[1].amount = CHARGE_INVOICE_AMOUNT
    trust_payment = create_trust_payment(session, amount=PAYMENT_AMOUNT)
    create_trust_api_payment(session, amount=LOGICAL_PAYMENT_AMOUNT,
                             invoice=charge_note_register_invoice,
                             transaction_id=trust_payment.purchase_token)
    refund = create_refund(session, amount_multiplier * refund_amount, trust_payment)
    chargeback_card_register = create_card_register(session, chargeback_terminal.id)
    refund.register = chargeback_card_register
    export_obj = create_export_obj(session, chargeback_card_register)
    session.flush()
    payment_info = CardRegisterExportController(export_obj).format_payments_info()
    if refund_amount < charge_note_register_invoice.amount:
        assert sorted(payment_info) == sorted([(refund.id,
                                                charge_invoice.external_id,
                                                refund_amount,
                                                refund.payment_dt,
                                                refund.approval_code),
                                               ])
    elif refund_amount == charge_note_register_invoice.amount:
        assert sorted(payment_info) == sorted([(refund.id,
                                                charge_note_register_invoice.charge_invoice.external_id,
                                                CHARGE_INVOICE_AMOUNT,
                                                refund.payment_dt,
                                                refund.approval_code),

                                               (refund.id,
                                                charge_note_register_invoice.register_rows[0].ref_invoice.external_id,
                                                charge_note_register_invoice.register_rows[0].amount,
                                                refund.payment_dt,
                                                refund.approval_code),
                                               ])
    else:
        assert sorted(payment_info) == sorted([(refund.id,
                                                charge_note_register_invoice.charge_invoice.external_id,
                                                CHARGE_INVOICE_AMOUNT + (
                                                        refund_amount - charge_note_register_invoice.amount),
                                                refund.payment_dt,
                                                refund.approval_code),

                                               (refund.id,
                                                charge_note_register_invoice.register_rows[0].ref_invoice.external_id,
                                                charge_note_register_invoice.register_rows[0].amount,
                                                refund.payment_dt,
                                                refund.approval_code),
                                               ])


@pytest.mark.parametrize('amount_multiplier', [-1, 1])
@pytest.mark.parametrize('w_invoice', [True, False])
def test_invoice_trust_chargeback_wo_invoice_refund(session, order, paysys, chargeback_terminal, w_invoice,
                                                    amount_multiplier):
    """предоплатный счет, возврат через траст"""
    client = order.client
    person = create_person(session, type='ph', client=client)
    request_obj = _create_request(session, client, [(order, 4)])
    refunded_invoice = ob.InvoiceBuilder.construct(session, paysys=paysys, person=person, request=request_obj)
    trust_payment = create_trust_payment(session, amount=PAYMENT_AMOUNT)

    create_trust_api_payment(session, amount=LOGICAL_PAYMENT_AMOUNT, invoice=refunded_invoice if w_invoice else None,
                             transaction_id=trust_payment.purchase_token)

    refund = create_refund(session, amount_multiplier * TRUST_REFUND_AMOUNT, trust_payment)
    chargeback_card_register = create_card_register(session, chargeback_terminal.id)
    refund.register = chargeback_card_register
    export_obj = create_export_obj(session, chargeback_card_register)
    session.flush()
    if w_invoice:
        payment_info = CardRegisterExportController(export_obj).format_payments_info()
        assert sorted(payment_info) == sorted([(refund.id,
                                                refunded_invoice.external_id,
                                                TRUST_REFUND_AMOUNT,
                                                refund.payment_dt,
                                                refund.approval_code)])
    else:
        with pytest.raises(RuntimeError) as exc_info:
            CardRegisterExportController(export_obj).format_payments_info()
        assert exc_info.value.message == '{} has no invoice'.format(refund)


@pytest.mark.parametrize('payment_amount, w_invoice', [(DU('119.99', 'RUB'), True),
                                                       (DU('120.00', 'RUB'), True),
                                                       (DU('120.01', 'RUB'), True),
                                                       (DU('120', 'RUB'), False)])
def test_invoice_trust_payment(session, order, paysys, terminal, w_invoice, payment_amount):
    """трастовый платеж по chargenote в случае точной оплаты, недоплаты, предоплаты + проверка исключения, если
    в trust_api платеже не указан счет"""
    client = order.client
    person = create_person(session, type='ph', client=client)
    request_obj = _create_request(session, client, [(order, 4)])
    payment_invoice = ob.InvoiceBuilder.construct(session, paysys=paysys, person=person, request=request_obj)
    card_register = create_card_register(session, terminal.id)
    trust_payment = create_trust_payment(session, payment_amount)
    logical_payment = create_trust_api_payment(session, payment_invoice if w_invoice else None,
                                               transaction_id=trust_payment.purchase_token,
                                               amount=LOGICAL_PAYMENT_AMOUNT)
    trust_payment.register = card_register
    export_obj = create_export_obj(session, card_register)
    session.flush()
    if w_invoice:
        payment_info = CardRegisterExportController(export_obj).format_payments_info()
        assert payment_info == [(logical_payment.id,
                                 payment_invoice.external_id,
                                 trust_payment.amount,
                                 trust_payment.payment_dt,
                                 trust_payment.approval_code)]
    else:
        with pytest.raises(RuntimeError) as exc_info:
            CardRegisterExportController(export_obj).format_payments_info()
        assert exc_info.value.message == '{} has no invoice'.format(logical_payment)


@pytest.mark.parametrize('payment_amount', [DU('119.99', 'RUB'),
                                            DU('120.00', 'RUB'),
                                            DU('120.01', 'RUB')])
def test_invoice_non_trust_payment(session, order, paysys, terminal, payment_amount):
    """карточный платеж по предоплатному счету в случае точной оплаты, недоплаты, предоплаты"""
    client = order.client
    person = create_person(session, type='ph', client=client)
    request_obj = _create_request(session, client, [(order, 4)])
    payment_invoice = ob.InvoiceBuilder.construct(session, paysys=paysys, person=person, request=request_obj)
    card_register = create_card_register(session, terminal.id)
    payment = create_payment(session, invoice=payment_invoice, amount=payment_amount)
    payment.register = card_register
    export_obj = create_export_obj(session, card_register)
    session.flush()
    payment_info = CardRegisterExportController(export_obj).format_payments_info()
    assert payment_info == [(payment.id,
                             payment_invoice.external_id,
                             payment_amount,
                             payment.payment_dt,
                             payment.approval_code)]


@pytest.mark.parametrize('payment_amount', [DU('119.99', 'RUB'),
                                            DU('120.00', 'RUB'),
                                            DU('120.01', 'RUB')])
def test_chargenote_non_trust_payment(session, order, paysys, payment_amount, terminal):
    """карточный платеж по chargenote в случае точной оплаты, недоплаты, предоплаты"""
    client = order.client
    person = create_person(session, type='ph', client=client)
    request_obj = _create_request(session, client, [(order, 4)])
    charge_invoice = ob.InvoiceBuilder.construct(session, paysys=paysys, person=person, request=request_obj)
    property_mock_charge_note_personal_account = mock.PropertyMock(return_value=charge_invoice)
    with mock.patch('balance.actions.invoice_create.InvoiceFactory.charge_note_personal_account',
                    new_callable=property_mock_charge_note_personal_account):
        charge_note_register_invoice = create_chargenote(session, request_obj, client, paysys, person,
                                                         single_account_number=123)
    charge_note_register_invoice.register_rows[1].amount = CHARGE_INVOICE_AMOUNT
    card_register = create_card_register(session, terminal.id)
    payment = create_payment(session, invoice=charge_note_register_invoice, amount=payment_amount)
    payment.register = card_register
    export_obj = create_export_obj(session, card_register)
    session.flush()
    payment_info = CardRegisterExportController(export_obj).format_payments_info()
    if payment_amount < charge_note_register_invoice.amount:
        assert sorted(payment_info) == sorted([(payment.id,
                                                charge_invoice.external_id,
                                                payment_amount,
                                                payment.payment_dt,
                                                payment.approval_code)])

    elif payment_amount == charge_note_register_invoice.amount:
        assert sorted(payment_info) == sorted([(payment.id,
                                                charge_note_register_invoice.charge_invoice.external_id,
                                                CHARGE_INVOICE_AMOUNT,
                                                payment.payment_dt,
                                                payment.approval_code),

                                               (payment.id,
                                                charge_note_register_invoice.register_rows[0].ref_invoice.external_id,
                                                charge_note_register_invoice.register_rows[0].amount,
                                                payment.payment_dt,
                                                payment.approval_code),
                                               ])
    else:
        assert sorted(payment_info) == sorted([(payment.id,
                                                charge_note_register_invoice.charge_invoice.external_id,
                                                CHARGE_INVOICE_AMOUNT + (
                                                        payment_amount - charge_note_register_invoice.amount),
                                                payment.payment_dt,
                                                payment.approval_code),

                                               (payment.id,
                                                charge_note_register_invoice.register_rows[0].ref_invoice.external_id,
                                                charge_note_register_invoice.register_rows[0].amount,
                                                payment.payment_dt,
                                                payment.approval_code),
                                               ])


@pytest.mark.parametrize('payment_amount', [DU('119.99', 'RUB'),
                                            DU('120.00', 'RUB'),
                                            DU('120.01', 'RUB')])
def test_chargenote_trust_payment(session, order, paysys, payment_amount, terminal):
    """трастовый платеж по chargenote в случае точной оплаты, недоплаты, предоплаты"""
    client = order.client
    person = create_person(session, type='ph', client=client)
    request_obj = _create_request(session, client, [(order, 4)])
    charge_invoice = ob.InvoiceBuilder.construct(session, paysys=paysys, person=person, request=request_obj)
    property_mock_charge_note_personal_account = mock.PropertyMock(return_value=charge_invoice)
    with mock.patch('balance.actions.invoice_create.InvoiceFactory.charge_note_personal_account',
                    new_callable=property_mock_charge_note_personal_account):
        charge_note_register_invoice = create_chargenote(session, request_obj, client, paysys, person,
                                                         single_account_number=123)
    charge_note_register_invoice.register_rows[1].amount = CHARGE_INVOICE_AMOUNT
    card_register = create_card_register(session, terminal.id)
    trust_payment = create_trust_payment(session, payment_amount)
    logical_payment = create_trust_api_payment(session, charge_note_register_invoice,
                                               transaction_id=trust_payment.purchase_token,
                                               amount=LOGICAL_PAYMENT_AMOUNT)
    trust_payment.register = card_register
    export_obj = create_export_obj(session, card_register)
    session.flush()
    payment_info = CardRegisterExportController(export_obj).format_payments_info()
    if payment_amount < charge_note_register_invoice.amount:
        assert sorted(payment_info) == sorted([(logical_payment.id,
                                                charge_invoice.external_id,
                                                payment_amount,
                                                trust_payment.payment_dt,
                                                trust_payment.approval_code)])

    elif payment_amount == charge_note_register_invoice.amount:
        assert sorted(payment_info) == sorted([(logical_payment.id,
                                                charge_note_register_invoice.charge_invoice.external_id,
                                                CHARGE_INVOICE_AMOUNT,
                                                trust_payment.payment_dt,
                                                trust_payment.approval_code),

                                               (logical_payment.id,
                                                charge_note_register_invoice.register_rows[0].ref_invoice.external_id,
                                                charge_note_register_invoice.register_rows[0].amount,
                                                trust_payment.payment_dt,
                                                trust_payment.approval_code),
                                               ])
    else:
        assert sorted(payment_info) == sorted([(logical_payment.id,
                                                charge_note_register_invoice.charge_invoice.external_id,
                                                CHARGE_INVOICE_AMOUNT + (
                                                        payment_amount - charge_note_register_invoice.amount),
                                                trust_payment.payment_dt,
                                                trust_payment.approval_code),

                                               (logical_payment.id,
                                                charge_note_register_invoice.register_rows[0].ref_invoice.external_id,
                                                charge_note_register_invoice.register_rows[0].amount,
                                                trust_payment.payment_dt,
                                                trust_payment.approval_code),
                                               ])
