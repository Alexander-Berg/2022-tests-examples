# -*- coding: utf-8 -*-

from decimal import Decimal

import mock
import pytest

from balance.providers.personal_acc_manager import PersonalAccountManager
from balance.actions import single_account
from balance.actions.invoice_turnon import InvoiceTurnOn
from balance import core
from balance import mapper
from balance.constants import (
    DIRECT_PRODUCT_RUB_ID,
    PersonCategoryCodes,
    FirmId,
    PaymentMethodIDs,
)

from tests import object_builder as ob
from tests.balance_tests.invoices.invoice_common import (
    create_invoice,
    create_charge_note_register,
    UR_BANK_PAYSYS_ID,
    UR_CARD_PAYSYS_ID,
)

pytestmark = [
    pytest.mark.single_account,
    pytest.mark.charge_note_register,
]


@pytest.fixture(params=[False, True], ids=['charge_note', 'charge_note_register'])
def charge_note_type(request, session):
    session.config.__dict__['FORCE_CHARGE_NOTE_REGISTER_FOR_SINGLE_ACCOUNT'] = request.param


@pytest.fixture(autouse=True)
def enable_single_account_for_all_services(session):
    session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = True


@pytest.fixture(name='personal_account')
def create_personal_account(session):
    person_type = list(single_account.availability.ALLOWED_LEGAL_ENTITY_PERSON_CATEGORIES)[0]
    client = ob.ClientBuilder(with_single_account=True).build(session).obj
    person = ob.PersonBuilder(
        client=client, name='Sponge Bob',
        email='s.bob@nickelodeon.com',
        type=person_type
    ).build(session).obj
    single_account.prepare.process_client(client)
    personal_account = PersonalAccountManager(session).\
        for_person(person).\
        for_single_account(client.single_account_number).\
        get(auto_create=False)
    return personal_account


def patch_notify_about_free_funds():
    notify_about_free_funds_path = \
        'balance.actions.invoice_turnon.single_account.notifications.notify_about_free_funds'
    return mock.patch(notify_about_free_funds_path)


def check_notify_about_free_funds_call(notify_about_free_funds_mock, expected_free_funds_delta):
    notify_about_free_funds_mock.assert_called_once()
    free_funds_delta = notify_about_free_funds_mock.call_args[0][2]
    assert free_funds_delta == expected_free_funds_delta


@pytest.mark.usefixtures('charge_note_type')
def test_charge_note_underpayment(session, personal_account):
    request = ob.RequestBuilder(
        basket=ob.BasketBuilder(client=personal_account.client)
    ).build(session).obj
    charge_note, = core.Core(session).create_invoice(
        request_id=request.id,
        paysys_id=UR_BANK_PAYSYS_ID,
        person_id=personal_account.person_id
    )

    expected_free_funds = charge_note.effective_sum // 2
    with patch_notify_about_free_funds() as notify_about_free_funds_mock:
        InvoiceTurnOn(charge_note, sum=expected_free_funds).do()

    check_notify_about_free_funds_call(notify_about_free_funds_mock, expected_free_funds)
    assert not personal_account.had_consumes()


@pytest.mark.usefixtures('charge_note_type')
def test_charge_note_overpayment(session, personal_account):
    request = ob.RequestBuilder(
        basket=ob.BasketBuilder(client=personal_account.client)
    ).build(session).obj
    charge_note, = core.Core(session).create_invoice(
        request_id=request.id,
        paysys_id=UR_BANK_PAYSYS_ID,
        person_id=personal_account.person_id
    )

    overpayment = 100
    with patch_notify_about_free_funds() as notify_about_free_funds_mock:
        InvoiceTurnOn(charge_note, sum=charge_note.effective_sum + overpayment).do()

    check_notify_about_free_funds_call(notify_about_free_funds_mock, overpayment)


def test_charge_note_register(session, personal_account):
    order = ob.OrderBuilder.construct(
        session,
        client=personal_account.client,
        product_id=DIRECT_PRODUCT_RUB_ID
    )
    invoice = create_invoice(session, person=personal_account.person, orders=[(order, 200)])

    charge_note = create_charge_note_register(
        UR_CARD_PAYSYS_ID,
        person=personal_account.person,
        orders=[(order, 100)],
        invoices=[invoice],
        single_account_number=personal_account.single_account_number
    )

    with patch_notify_about_free_funds() as notify_about_free_funds_mock:
        InvoiceTurnOn(charge_note, sum=320).do()

    check_notify_about_free_funds_call(notify_about_free_funds_mock, 20)


def test_payment_without_charge_note(session, personal_account):
    expected_free_funds = 100

    with patch_notify_about_free_funds() as notify_about_free_funds_mock:
        InvoiceTurnOn(personal_account, sum=expected_free_funds).do()

    check_notify_about_free_funds_call(notify_about_free_funds_mock, expected_free_funds)


@pytest.mark.parametrize(
    ['notify_about_free_funds_must_be_called', 'payment_amount_delta'],
    [
        pytest.param(False, Decimal('-0.01'), id='underpayment'),  # На счете остался долг
        pytest.param(False, 0, id='exact'),  # Сумма свободных средств = 0
        pytest.param(True, Decimal('0.01'), id='overpayment'),  # Переплата (оплата на сумму > долг)
    ],
)
@pytest.mark.usefixtures('charge_note_type')
def test_debt(session, personal_account, notify_about_free_funds_must_be_called, payment_amount_delta):
    request = ob.RequestBuilder(
        basket=ob.BasketBuilder(client=personal_account.client)
    ).build(session).obj
    charge_note, = core.Core(session).create_invoice(
        request_id=request.id,
        paysys_id=UR_BANK_PAYSYS_ID,
        person_id=personal_account.person_id
    )

    # Create debt
    personal_account.turn_on_rows(operation=None, ref_invoice=charge_note)
    assert personal_account.raw_unused_funds < 0

    payment_amount = charge_note.effective_sum.as_decimal() + payment_amount_delta
    with patch_notify_about_free_funds() as notify_about_free_funds_mock:
        InvoiceTurnOn(personal_account, sum=payment_amount).do()

    assert personal_account.raw_unused_funds == payment_amount - charge_note.effective_sum
    if notify_about_free_funds_must_be_called:
        check_notify_about_free_funds_call(
            notify_about_free_funds_mock,
            payment_amount - charge_note.effective_sum
        )
    else:
        notify_about_free_funds_mock.assert_not_called()


def test_not_notified_without_single_account(session):
    person = ob.PersonBuilder(
        client=ob.ClientBuilder(with_single_account=False),
        type=PersonCategoryCodes.russia_resident_individual
    ).build(session).obj

    personal_account = PersonalAccountManager(session).\
        for_person(person).\
        for_firm(session.query(mapper.Firm).getone(FirmId.YANDEX_OOO)).\
        for_paysys(session.query(mapper.Paysys).getone(UR_BANK_PAYSYS_ID)).\
        get(auto_create=True)

    with patch_notify_about_free_funds() as notify_about_free_funds_mock:
        InvoiceTurnOn(personal_account, sum=100).do()

    notify_about_free_funds_mock.assert_not_called()


@pytest.mark.usefixtures('charge_note_type')
def test_not_notified_if_free_funds_unchanged(session, personal_account):
    free_funds = 100
    with patch_notify_about_free_funds():
        InvoiceTurnOn(personal_account, sum=free_funds).do()

    assert personal_account.unused_funds == free_funds

    # Just a random instant paysys for a change
    paysys = session.query(mapper.Paysys).filter_by(
        extern=1,
        iso_currency=personal_account.iso_currency,
        payment_method_id=PaymentMethodIDs.credit_card,
        category=personal_account.person.type,
        firm_id=personal_account.firm_id,
        group_id=personal_account.paysys.group_id
    ).first()

    request = ob.RequestBuilder(
        basket=ob.BasketBuilder(client=personal_account.client)
    ).build(session).obj

    charge_note, = core.Core(session).create_invoice(
        request_id=request.id,
        paysys_id=paysys.id,
        person_id=personal_account.person_id
    )

    with patch_notify_about_free_funds() as notify_about_free_funds_mock:
        InvoiceTurnOn(charge_note, sum=charge_note.effective_sum).do()

    assert personal_account.unused_funds == free_funds
    notify_about_free_funds_mock.assert_not_called()


def test_successful_termination_on_notify_error(session, personal_account):
    expected_free_funds = 100

    with patch_notify_about_free_funds() as notify_about_free_funds_mock:
        notify_about_free_funds_mock.side_effect = RuntimeError('Mr. Krabs is angry !')
        with session.begin_nested():
            InvoiceTurnOn(personal_account, sum=expected_free_funds).do()

    notify_about_free_funds_mock.assert_called_once()

    # Убеждаемся, что основная транзакция не откатилась
    session.refresh(personal_account)
    assert personal_account.unused_funds == expected_free_funds
