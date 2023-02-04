# -*- coding: utf-8 -*-

import pytest

from balance import core
from balance import mapper
from balance.constants import (
    ServiceId,
    RegionId,
    FirmId,
    PaymentMethodIDs,
    PaysysGroupIDs,
    PersonCategoryCodes,
)
from balance.actions import single_account

from tests import object_builder as ob
from tests.balance_tests.single_account.common import get_request_builder, PRICE, QUANTITY

pytestmark = [pytest.mark.single_account]


@pytest.fixture(params=[False, True], ids=['charge_note', 'charge_note_register'])
def charge_note_type(request, session):
    session.config.__dict__['FORCE_CHARGE_NOTE_REGISTER_FOR_SINGLE_ACCOUNT'] = request.param
    if request.param:
        return mapper.ChargeNoteRegister
    else:
        return mapper.ChargeNote


@pytest.fixture
def client(session):
    return ob.ClientBuilder(with_single_account=True).build(session).obj


@pytest.fixture
def person(client):
    return ob.PersonBuilder.construct(
        client.session,
        client=client,
        type=PersonCategoryCodes.russia_resident_individual
    )


@pytest.fixture(autouse=True)
def mock_enabled_services(session):
    session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = [ServiceId.DIRECT]


def test_pay_on_single_account(session, client, person):
    total_sum = PRICE * QUANTITY

    single_account.prepare.process_client(client)
    (personal_account, _), = client.get_single_account_subaccounts()
    personal_account.create_receipt(total_sum)

    paysys = session.query(mapper.Paysys)\
        .filter_by(payment_method_id=PaymentMethodIDs.single_account,
                   group_id=PaysysGroupIDs.default,
                   firm_id=single_account.prepare.DEFAULT_SINGLE_ACCOUNT_FIRM,
                   iso_currency=single_account.prepare.DEFAULT_ISO_CURRENCY,
                   category=person.type,
                   extern=1)\
        .one()

    request = get_request_builder(client, [ServiceId.DIRECT]).build(session).obj

    pay_policy_id = ob.create_pay_policy_service(session, ServiceId.DIRECT, FirmId.YANDEX_OOO, [('RUB', PaymentMethodIDs.single_account)])
    ob.create_pay_policy_region(session, pay_policy_id, RegionId.RUSSIA)

    core_obj = core.Core(session=session)
    invoices = core_obj.create_invoice(request.id, paysys.id, person_id=person.id)
    assert invoices == [personal_account]
    assert isinstance(personal_account, mapper.PersonalAccount)
    assert personal_account.consume_sum == total_sum


@pytest.mark.charge_note_register
def test_charge_note(session, client, person, charge_note_type):
    single_account.prepare.process_client(client)
    (personal_account, _), = client.get_single_account_subaccounts()

    request = get_request_builder(client, [ServiceId.DIRECT]).build(session).obj
    paysys = ob.Getter(mapper.Paysys, 1000).build(session).obj

    core_obj = core.Core(session=session)
    invoice, = core_obj.create_invoice(request.id, paysys.id, person_id=person.id)

    assert isinstance(invoice, charge_note_type)
    assert invoice.total_sum == QUANTITY * PRICE
    assert invoice.charge_invoice is personal_account


@pytest.mark.charge_note_register
def test_charge_note_register_w_prepay(session, client, person):
    paysys = ob.Getter(mapper.Paysys, 1000).build(session).obj

    single_account.prepare.process_client(client)
    (personal_account, _), = client.get_single_account_subaccounts()

    ref_invoice = ob.InvoiceBuilder.construct(
        session,
        paysys=paysys,
        person=person,
        request=get_request_builder(client, [ServiceId.DIRECT])
    )

    request_builder = get_request_builder(client, [ServiceId.DIRECT])
    request_builder.b.basket.b.register_rows = [ob.BasketRegisterRowBuilder(ref_invoice=ref_invoice)]
    request = request_builder.build(session).obj

    core_obj = core.Core(session=session)
    invoice, = core_obj.create_invoice(request.id, paysys.id, person_id=person.id)

    assert isinstance(invoice, mapper.ChargeNoteRegister)
    assert invoice.total_sum == 2 * QUANTITY * PRICE
    assert invoice.charge_invoice is personal_account


@pytest.mark.charge_note_register
def test_charge_note_register_wo_prepay(session, client, person):
    paysys = ob.Getter(mapper.Paysys, 1000).build(session).obj

    single_account.prepare.process_client(client)
    (personal_account, _), = client.get_single_account_subaccounts()

    ref_invoice = ob.InvoiceBuilder.construct(
        session,
        paysys=paysys,
        person=person,
        request=get_request_builder(client, [ServiceId.DIRECT])
    )

    request = ob.RequestBuilder(
        basket=ob.BasketBuilder(
            client=client,
            rows=[],
            register_rows=[ob.BasketRegisterRowBuilder(ref_invoice=ref_invoice)]
        )
    ).build(session).obj

    core_obj = core.Core(session=session)
    invoice, = core_obj.create_invoice(request.id, paysys.id, person_id=person.id)

    assert isinstance(invoice, mapper.ChargeNoteRegister)
    assert invoice.total_sum == 1 * QUANTITY * PRICE
    assert invoice.charge_invoice is personal_account
