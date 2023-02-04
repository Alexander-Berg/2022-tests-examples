# -*- coding: utf-8 -*-

import pytest

from balance import core
from balance import mapper
from balance.constants import ServiceId, LOCALE_RU, LOCALE_EN, PaymentMethodIDs, PaysysGroupIDs
from balance.xmlizer import getxmlizer
from balance.actions.invoice_turnon import InvoiceTurnOn
from balance.actions import single_account
from balance.providers.personal_acc_manager import PersonalAccountManager

from tests.object_builder import PersonBuilder, ClientBuilder
from tests.balance_tests.single_account.common import get_request_builder, SERVICE_PRODUCT_DATA

pytestmark = [pytest.mark.single_account]

RUR_BANK_PAYSYS_ID = 1003


@pytest.fixture
def client(session):
    return ClientBuilder(with_single_account=True).build(session).obj


@pytest.fixture(params=[False, True], ids=['charge_note', 'charge_note_register'])
def charge_note_type(request, session):
    session.config.__dict__['FORCE_CHARGE_NOTE_REGISTER_FOR_SINGLE_ACCOUNT'] = request.param
    if request.param:
        return mapper.ChargeNoteRegister
    else:
        return mapper.ChargeNote


def test_generate_single_account_number(client):
    single_account.prepare.client_processor(client)
    assert client.single_account_number is not None
    assert single_account.prepare.calc_checksum(client.single_account_number) == 0


@pytest.mark.charge_note_register
@pytest.mark.parametrize(
    'manually',
    [True, False],
    ids=['manually', 'automatically']
)
def test_personal_account_creation(manually, session, client, charge_note_type):
    """
    Выполняем процедуру выставления счета в двух вариациях:
    - создание ЛС при включении ЕЛС для клиента
    - автоматическое создание ЛС при выставлении счета
    """
    service_id = ServiceId.DIRECT
    service_ids = [service_id]
    session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = service_ids

    person_type = list(single_account.availability.ALLOWED_INDIVIDUAL_PERSON_CATEGORIES)[0]

    paysys = session.query(mapper.Paysys)\
        .filter_by(payment_method_id=PaymentMethodIDs.credit_card,
                   group_id=PaysysGroupIDs.default,
                   firm_id=single_account.prepare.DEFAULT_SINGLE_ACCOUNT_FIRM,
                   iso_currency=single_account.prepare.DEFAULT_ISO_CURRENCY,
                   category=person_type,
                   extern=1)\
        .one()

    person = PersonBuilder(client=client, type=person_type).build(client.session).obj

    request = get_request_builder(client, service_ids).build(session).obj

    if manually:
        single_account.prepare.process_person(person, client.single_account_number)
        # Убеждаемся, что счет создался
        personal_account = (
            PersonalAccountManager(session)
                .for_person(person)
                .for_single_account(client.single_account_number)
                .get(auto_create=False)
        )
        assert personal_account.single_account_number == client.single_account_number

    session.flush()

    core_obj = core.Core(session=session)
    invoices = core_obj.create_invoice(request.id, paysys.id, person.id)
    invoice, = invoices

    InvoiceTurnOn(invoice).do()

    assert isinstance(invoice, charge_note_type)
    personal_account = invoice.charge_invoice
    assert isinstance(personal_account, mapper.PersonalAccount)
    assert personal_account.single_account_number == client.single_account_number

    # Проверяем, что у плательщика один лицевой счет
    all_personal_accounts = (
        session.query(mapper.PersonalAccount)
            .filter_by(person=person)
            .all()
    )
    assert all_personal_accounts == [personal_account]


@pytest.mark.charge_note_register
@pytest.mark.parametrize(
    'service_id, is_chargenote',
    [
        (ServiceId.DIRECT, True),
        (ServiceId.MEDIA_SELLING, False),
        (ServiceId.MARKET, False),
    ],
    ids=['enabled_service_id', 'disabled_service_id', 'bu_service_id']
)
def test_create_invoice(client, service_id, is_chargenote, charge_note_type):
    session = client.session
    session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = [ServiceId.DIRECT, ServiceId.MARKET]

    paysys_id = RUR_BANK_PAYSYS_ID
    person_type = list(single_account.availability.ALLOWED_LEGAL_ENTITY_PERSON_CATEGORIES)[0]
    person = PersonBuilder(client=client, type=person_type).build(client.session).obj
    request = get_request_builder(client, [service_id]).build(session).obj
    session.flush()

    core_obj = core.Core(session=session)
    for i in range(2):  # проверка для первого счёта и последующих
        invoices = core_obj.create_invoice(request_id=request.id, paysys_id=paysys_id, person_id=person.id)
        invoice, = invoices

        InvoiceTurnOn(invoice).do()

        payment_purpose = invoice.get_payment_purpose()

        if is_chargenote:
            assert isinstance(invoice, charge_note_type)
            assert isinstance(invoice.charge_invoice, mapper.PersonalAccount)
            assert invoice.charge_invoice.single_account_number == client.single_account_number
            assert invoice.charge_invoice.person == invoice.person
            assert invoice.payments

            assert u"ЕЛС-%i ОПЛ-%i" % (client.single_account_number, invoice.payments[0].id,) in payment_purpose

            invoice_xmlizer = getxmlizer(invoice)
            xml = invoice_xmlizer.xmlize_grouped_products(LOCALE_RU, act=None, erep=False)
            assert xml.find('payment-purpose').text == payment_purpose
            assert u'лицевой' not in xml.find('products/grouped-product/name').text
        else:
            assert isinstance(invoice, mapper.Invoice)
            assert payment_purpose == invoice.external_id


def test_multi_service(client):
    session = client.session

    service_ids = SERVICE_PRODUCT_DATA.keys()
    session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = service_ids

    paysys_id = RUR_BANK_PAYSYS_ID

    person_type = list(single_account.availability.ALLOWED_LEGAL_ENTITY_PERSON_CATEGORIES)[0]

    person = PersonBuilder(client=client, type=person_type).build(client.session).obj

    request = get_request_builder(client, service_ids).build(session).obj

    session.flush()

    core_obj = core.Core(session=session)
    for i in range(2):  # проверка для первого счёта и последующих
        invoices = core_obj.create_invoice(request_id=request.id, paysys_id=paysys_id, person_id=person.id)
        invoice, = invoices

        InvoiceTurnOn(invoice).do()

        assert isinstance(invoice.charge_invoice, mapper.PersonalAccount)
        assert {consume.order for consume in invoice.charge_invoice.consumes} == {row.order for row in request.rows}
        assert invoice.charge_invoice.single_account_number == client.single_account_number
        assert invoice.charge_invoice.person == invoice.person
        assert invoice.payments
