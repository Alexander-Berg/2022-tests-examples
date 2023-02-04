import datetime
import pytest
import uuid

from balance import mapper
from balance.actions.invoice_create import InvoiceFactory
from balance.constants import ServiceId, DIRECT_PRODUCT_ID
from tests import object_builder as ob

GENERAL = 'GENERAL'

NOW = datetime.datetime.now()


@pytest.fixture(params=[['Person'], []], ids=['oebs_api', 'oebs'])
def export_w_oebs_api(request, session):
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = request.param
    return request.param


@pytest.fixture(params=[False, True], ids=['without_cache', 'with_cache'])
def use_cache_cfg(request, session):
    session.config.__dict__['USE_FIRMS_FROM_PERSON_FIRM_CACHE'] = request.param
    return request.param


@pytest.fixture(name='person')
def create_person(session, type=None, country=None, **kwargs):
    if not type:
        type = create_person_category(session, country=country or create_country(session)).category
    return ob.PersonBuilder.construct(session, type=type, **kwargs)


@pytest.fixture(name='country')
def create_country(session):
    return ob.CountryBuilder().build(session).obj


@pytest.fixture(name='person_category')
def create_person_category(session, country=None, **kwargs):
    return ob.PersonCategoryBuilder(country=country or create_country(session), **kwargs).build(session).obj


@pytest.fixture(name='firm')
def create_firm(session, w_firm_export=True, country=None, **kwargs):
    firm = ob.FirmBuilder(country=country or create_country(session), title=str(ob.get_big_number()) + '_firm',
                          **kwargs).build(session).obj
    if w_firm_export:
        create_firm_export(session, firm)
    return firm


def create_firm_export(session, firm):
    oebs_org_id = ob.get_big_number()
    session.execute(
        '''INSERT INTO BO.T_FIRM_EXPORT (FIRM_ID, EXPORT_TYPE, OEBS_ORG_ID) VALUES (:firm_id, 'OEBS', :oebs_org_id)''',
        {'firm_id': firm.id, 'oebs_org_id': oebs_org_id})


@pytest.fixture(name='contract')
def create_contract(session, client=None, person=None, firm_id=None, ctype=GENERAL, **kwargs):
    if not person:
        person = create_person(session)
    return ob.ContractBuilder.construct(session,
                                        client=client or person.client,
                                        person=person,
                                        firm=firm_id or create_firm(session, w_firm_export=True).id,
                                        ctype=ctype,
                                        **kwargs)


@pytest.fixture(name='invoice')
def create_invoice(session, **kwargs):
    return ob.InvoiceBuilder(**kwargs).build(session).obj


@pytest.fixture
def client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture
def paysys(session):
    return ob.Getter(mapper.Paysys, 1002).build(session).obj


@pytest.fixture
def order(session, client):
    return ob.OrderBuilder(
        product=ob.Getter(mapper.Product, DIRECT_PRODUCT_ID).build(session).obj,
        service=ob.Getter(mapper.Service, ServiceId.DIRECT),
        client=client,
        agency=client.agency
    ).build(session).obj


def _create_request(session, client, orders_qty=None):
    basket = ob.BasketBuilder(
        client=client,
        rows=[
            ob.BasketItemBuilder(order=o, quantity=qty)
            for o, qty in orders_qty
        ]
    )
    return ob.RequestBuilder(basket=basket).build(session).obj


@pytest.fixture
def contract(session, client, person):
    contract = ob.ContractBuilder(
        dt=datetime.datetime.now() - datetime.timedelta(days=66),
        client=client,
        person=person,
        commission=1,
        payment_type=3,
        credit_type=1,
        payment_term=30,
        payment_term_max=60,
        personal_account=1,
        personal_account_fictive=1,
        currency=810,
        lift_credit_on_payment=1,
        commission_type=57,
        repayment_on_consume=1,
        oebs_firm=1,
        credit_limit_single=1666666,
        services={7},
        is_signed=datetime.datetime.now(),
        firm=1,
    ).build(session).obj
    contract.col0.OEBS_FIRM = 1
    session.flush()
    return contract


def create_payment(session, invoice, amount, transaction_id=None):
    payment = ob.CardPaymentBuilder.construct(session, invoice=invoice)
    payment.transaction_id = transaction_id
    payment.amount = amount
    payment.payment_dt = NOW
    payment.approval_code = ob.get_big_number()
    return payment


def create_trust_payment(session, amount):
    payment = ob.TrustPaymentBuilder.construct(session)
    payment.amount = amount
    payment.purchase_token = uuid.uuid4().hex
    payment.payment_dt = NOW + datetime.timedelta(days=2)
    payment.approval_code = ob.get_big_number()
    payment.invoice = None
    return payment


def create_trust_api_payment(session, invoice, transaction_id, amount):
    payment = ob.TrustApiPaymentBuilder.construct(session, transaction_id=transaction_id, invoice=invoice)
    payment.currency = 'RUB'
    payment.amount = amount
    payment.purchase_token = uuid.uuid4().hex
    payment.approval_code = ob.get_big_number()
    return payment


def create_invoice_refund(session, invoice, amount):
    payment = ob.CardPaymentBuilder.construct(session, invoice=invoice)
    return ob.InvoiceRefundBuilder.construct(session,
                                             invoice=invoice,
                                             payment_id=payment.id,
                                             system_uid=uuid.uuid4().hex,
                                             amount=amount)


def create_card_register(session, terminal_id):
    return ob.CardRegister.construct(session, register_dt=NOW, amount=1, terminal_id=terminal_id)


def create_refund(session, refund_amount, refund_payment, trust_refund_id=None):
    refund = ob.RefundBuilder.construct(session, payment=refund_payment,
                                        amount=refund_amount,
                                        description='',
                                        operation=None)
    refund.payment_dt = NOW + datetime.timedelta(days=1)
    refund.trust_refund_id = trust_refund_id
    refund.approval_code = ob.get_big_number()
    return refund


def create_chargenote(session, request_obj, client, paysys, person, **kwargs):
    ref_invoice = ob.InvoiceBuilder.construct(session, paysys=paysys, person=person, request=request_obj)
    request = ob.RequestBuilder.construct(
        session,
        basket=ob.BasketBuilder(
            client=client,
            rows=[],
            register_rows=[
                ob.BasketRegisterRowBuilder(ref_invoice=ref_invoice),
            ]
        )
    )

    return InvoiceFactory.create(
        request=request,
        person=person,
        paysys=paysys,
        type='charge_note_register',
        temporary=False,
        **kwargs
    )


@pytest.fixture
def processing(session):
    return ob.ProcessingBuilder.construct(session)


@pytest.fixture
def terminal(session, contract, processing):
    terminal = ob.TerminalBuilder.construct(session, processing=processing)
    terminal.oebs_register_type = 'SBERBANK_RUR'
    terminal.contract = contract
    return terminal


@pytest.fixture
def chargeback_terminal(session, contract, processing):
    terminal = ob.TerminalBuilder.construct(session, processing=processing)
    terminal.oebs_register_type = 'CHARGEBACK'
    terminal.contract = contract
    return terminal


def create_export_obj(session, card_register):
    return ob.ExportBuilder.construct(session,
                                      classname='CardRegister',
                                      object_id=card_register.id,
                                      type='OEBS',
                                      state=1,
                                      export_dt=NOW,
                                      )
