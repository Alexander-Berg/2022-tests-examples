# -*- coding: utf-8 -*-
import datetime
import pytest
import simplejson

from billing.contract_iface.constants import ContractTypeId

from balance.processors.oebs_api.api import _json_default
from balance import constants as cst, mapper, core
from balance.actions import acts as a_a
from balance.actions.invoice_create import InvoiceFactory
from balance.processors.oebs_api.wrappers import TransactionWrapper
from tests import object_builder as ob
from butils import decimal_unit
from decimal import Decimal as D
from balance.processors.oebs_api.utils import convert_DU

DU = decimal_unit.DecimalUnit

MONTH_BEFORE = datetime.datetime.now() - datetime.timedelta(days=30)
TWO_MONTH_BEFORE = datetime.datetime.now() - datetime.timedelta(days=60)
MONTH_AFTER = datetime.datetime.now() + datetime.timedelta(days=30)


@pytest.fixture
def person(session):
    return ob.PersonBuilder.construct(session)


@pytest.fixture
def contract(session, person):
    return ob.ContractBuilder(
        client=person.client,
        person=person,
        commission=0,
        firm=1,
        postpay=1,
        personal_account=1,
        personal_account_fictive=1,
        payment_type=3,
        payment_term=30,
        credit=3,
        credit_limit_single='9' * 20,
        services={7, 11, 35},
        is_signed=session.now(),
    ).build(session).obj


@pytest.fixture
def fpa_invoice(session, contract):
    invoice = InvoiceFactory.create(
        request=None,
        paysys=session.query(mapper.Paysys).filter_by(firm_id=1).getone(cc='ur'),
        contract=contract,
        postpay=2,
        temporary=False
    )
    assert isinstance(invoice, mapper.FictivePersonalAccount)
    return invoice


@pytest.fixture
def pa_invoice(session, contract):
    invoice = InvoiceFactory.create(
        request=None,
        paysys=session.query(mapper.Paysys).filter_by(firm_id=1).getone(cc='ur'),
        contract=contract,
        postpay=1,
        temporary=False
    )
    assert isinstance(invoice, mapper.PersonalAccount)
    return invoice


@pytest.fixture
def y_invoice(session, contract):
    return create_y_invoice(session, contract)


def create_y_invoice(session, contract, orders=None):
    if not orders:
        contract.client.is_agency = 1
        orders = [ob.OrderBuilder(product=ob.Getter(mapper.Product, 1475),
                                  service=ob.Getter(mapper.Service, 7),
                                  client=ob.ClientBuilder.construct(session),
                                  agency=contract.client
                                  ).build(session).obj]
    basket = ob.BasketBuilder(
        client=contract.client,
        rows=[
            ob.BasketItemBuilder(order=o, quantity=qty)
            for order in orders for o, qty in [(order, 10)]
        ]
    )
    request = ob.RequestBuilder(basket=basket).build(session).obj
    coreobj = core.Core(request.session)
    pa, = coreobj.pay_on_credit(
        request_id=request.id,
        paysys_id=1003,
        person_id=contract.person.id,
        contract_id=contract.id
    )
    request.session.flush()
    now = datetime.datetime.now()
    for order in orders:
        order.calculate_consumption(now, {order.shipment_type: 10})
    act, = a_a.ActAccounter(
        pa.client,
        mapper.ActMonth(for_month=now),
        invoices=[pa.id], dps=[],
        force=1
    ).do()
    invoice = act.invoice
    pa.session.flush()
    return invoice


def test_entity_type(session):
    invoice = ob.InvoiceBuilder.construct(session)
    invoice_info = TransactionWrapper(invoice).get_info()

    assert invoice_info['entity_type'] == 'SCHET'


def test_entity_id(session):
    invoice = ob.InvoiceBuilder.construct(session)
    invoice_info = TransactionWrapper(invoice).get_info()
    session.flush()
    assert invoice_info['entity_id'] == str(invoice.id)


def test_external_id(session):
    invoice = ob.InvoiceBuilder.construct(session)
    invoice_info = TransactionWrapper(invoice).get_info()
    session.flush()
    assert invoice_info['trx_number'] == invoice.external_id


def test_oe_code(session):
    invoice = ob.InvoiceBuilder.construct(session)
    invoice_info = TransactionWrapper(invoice).get_info()
    session.flush()
    assert invoice_info['oe_code'] == 'YARU'


def test_client_guid(session):
    invoice = ob.InvoiceBuilder.construct(session)
    invoice_info = TransactionWrapper(invoice).get_info()
    session.flush()
    assert invoice_info['client_guid'] == 'C' + str(invoice.client.id)


@pytest.mark.parametrize('w_endbuyer', [True, False])
def test_customer_guid(session, w_endbuyer):
    invoice = ob.InvoiceBuilder.construct(session)
    if w_endbuyer:
        endbuyer = ob.PersonBuilder.construct(session)
        session.flush()
        invoice.endbuyer_id = endbuyer.id
    invoice_info = TransactionWrapper(invoice).get_info()
    if w_endbuyer:
        assert invoice_info['customer_guid'] == 'P' + str(endbuyer.id)
    else:
        assert invoice_info['customer_guid'] == 'P' + str(invoice.person.id)


def test_trx_date(session):
    invoice = ob.InvoiceBuilder.construct(session)
    invoice_info = TransactionWrapper(invoice).get_info()
    session.flush()
    assert invoice_info['trx_date'] == invoice.dt


@pytest.mark.parametrize('w_contract', [True, False])
def test_contract_guid(session, w_contract):
    invoice = ob.InvoiceBuilder.construct(session)
    if w_contract:
        contract = ob.ContractBuilder.construct(session)
        invoice.contract = contract
    session.flush()
    invoice_info = TransactionWrapper(invoice).get_info()
    if w_contract:
        assert invoice_info['contract_guid'] == str(invoice.contract.id)
    else:
        assert 'contract_guid' not in invoice_info


@pytest.mark.parametrize('unilateral', [1, 0, None])
def test_unilateral(session, unilateral):
    invoice = ob.InvoiceBuilder.construct(session)
    invoice.unilateral = unilateral
    session.flush()
    invoice_info = TransactionWrapper(invoice).get_info()
    assert invoice_info['unilateral'] == ('Y' if unilateral == 1 else 'N')


@pytest.mark.parametrize('overdraft', [1, 0])
def test_overdraft(session, overdraft):
    invoice = ob.InvoiceBuilder.construct(session)
    invoice.overdraft = overdraft
    session.flush()
    invoice_info = TransactionWrapper(invoice).get_info()
    assert invoice_info['overdraft'] == ('Y' if overdraft == 1 else 'N')


@pytest.mark.parametrize('is_docs_separated', [1, 0])
@pytest.mark.parametrize('is_docs_detailed', [1, 0])
def test_printable_docs_type(session, is_docs_detailed, is_docs_separated):
    invoice = ob.InvoiceBuilder.construct(session)
    invoice.is_docs_detailed = is_docs_detailed
    invoice.is_docs_separated = is_docs_separated
    session.flush()
    invoice_info = TransactionWrapper(invoice).get_info()
    if is_docs_separated:
        if is_docs_detailed:
            assert invoice_info['printable_docs_type'] == 3
        else:
            assert invoice_info['printable_docs_type'] == 1
    else:
        if is_docs_detailed:
            assert invoice_info['printable_docs_type'] == 2
        else:
            assert invoice_info['printable_docs_type'] == 0


@pytest.mark.parametrize('commission_type', [1, 0, None])
@pytest.mark.parametrize('discount_type', [1, 0, None])
def test_discount_type(session, discount_type, commission_type):
    invoice = ob.InvoiceBuilder.construct(session)
    invoice.discount_type = discount_type
    invoice.commission_type = commission_type
    session.flush()
    invoice_info = TransactionWrapper(invoice).get_info()
    if commission_type is not None:
        assert invoice_info['discount_type'] == commission_type
    elif discount_type is not None:
        assert invoice_info['discount_type'] == discount_type
    else:
        assert 'discount_type' not in invoice_info


@pytest.mark.parametrize('paysys_group_id', [
    cst.PaysysGroupIDs.default,
    cst.PaysysGroupIDs.nr_via_agency
])
@pytest.mark.parametrize('w_contract', [
    True,
    False
])
@pytest.mark.parametrize('commission_type', [
    110,
    111
])
@pytest.mark.parametrize('service_code', [
    'AGENT_REWARD',
    'DEPOSITION'
])
@pytest.mark.parametrize('client_is_agency', [
    0,
    1
])
def test_is_agency(session, client_is_agency, service_code, commission_type, w_contract, paysys_group_id):
    invoice = ob.InvoiceBuilder.construct(session)
    invoice.client.is_agency = client_is_agency
    invoice.service_code = service_code
    invoice.commission_type = commission_type
    if w_contract:
        contract = ob.ContractBuilder.construct(session,
                                                ctype='GENERAL',
                                                commission=ContractTypeId.COMMISSION,
                                                dt=TWO_MONTH_BEFORE,
                                                is_signed=TWO_MONTH_BEFORE,
                                                services={cst.ServiceId.TAXI_CORP},
                                                firm=1,
                                                )
        invoice.contract = contract
        invoice.paysys.group_id = paysys_group_id
    session.flush()
    invoice_info = TransactionWrapper(invoice).get_info()
    if service_code == 'DEPOSITION':
        assert invoice_info['is_agency'] == 10
    elif commission_type == 111:
        assert invoice_info['is_agency'] == 0
    elif w_contract:
        if paysys_group_id == 3:
            assert invoice_info['is_agency'] == 1
        else:
            assert invoice_info['is_agency'] == 0
    else:
        assert invoice_info['is_agency'] == client_is_agency


@pytest.mark.parametrize('payment_term_dt', [None, MONTH_AFTER])
def test_payment_term_days(session, payment_term_dt):
    invoice = ob.InvoiceBuilder.construct(session)
    invoice.payment_term_dt = payment_term_dt
    session.flush()
    invoice_info = TransactionWrapper(invoice).get_info()
    if payment_term_dt:
        assert invoice_info['payment_term_days'] == 30
    else:
        assert 'payment_term_days' not in invoice_info


@pytest.mark.parametrize('loyal_clients', [1, 0])
def test_loyal_clients(session, loyal_clients):
    invoice = ob.InvoiceBuilder.construct(session)
    invoice.loyal_clients = loyal_clients
    session.flush()
    invoice_info = TransactionWrapper(invoice).get_info()
    if loyal_clients:
        assert invoice_info['loyal_clients'] == 'Y'
    else:
        assert invoice_info['loyal_clients'] == 'N'


def test_y_invoice(session, y_invoice):
    invoice_info = convert_DU(TransactionWrapper(y_invoice).get_info())
    assert invoice_info['personal_account'] == 'N'
    assert invoice_info['nds_pct'] == D('20')
    assert invoice_info['trx_lines']
    assert 'nsp_pct' not in invoice_info


def test_personal_account_fpa(session, fpa_invoice):
    session.flush()
    invoice_info = convert_DU(TransactionWrapper(fpa_invoice).get_info())
    assert invoice_info['personal_account'] == 'Y'
    assert invoice_info['nds_pct'] == D('20')
    assert 'nsp_pct' not in invoice_info
    assert 'trx_lines' not in invoice_info


def test_personal_account_pa(session, pa_invoice):
    session.flush()
    invoice_info = TransactionWrapper(pa_invoice).get_info()
    assert invoice_info['personal_account'] == 'Y'
    assert invoice_info['nds_pct'] == D('20')
    assert 'nsp_pct' not in invoice_info
    assert 'trx_lines' not in invoice_info


def test_personal_account_prepayment(session):
    invoice = ob.InvoiceBuilder.construct(session)
    assert type(invoice) == mapper.Invoice
    session.flush()
    invoice_info = TransactionWrapper(invoice).get_info()
    assert invoice_info['personal_account'] == 'N'


@pytest.mark.parametrize('w_manager, w_manager_login', [(False, False),
                                                        (True, False),
                                                        (True, True)])
def test_manager(session, w_manager, w_manager_login):
    invoice = ob.InvoiceBuilder.construct(session)
    manager = ob.SingleManagerBuilder.construct(session,
                                                domain_login=ob.generate_character_string(2) if w_manager_login else '')
    if w_manager:
        invoice.manager = manager
    else:
        invoice.manager = None
    session.flush()
    invoice_info = TransactionWrapper(invoice).get_info()
    if w_manager:
        if w_manager_login:
            assert invoice_info['manager'] == {'code': manager.manager_code,
                                               'login': manager.domain_login}
        else:
            assert invoice_info['manager'] == {'code': manager.manager_code}
    else:
        assert 'manager' not in invoice_info


@pytest.mark.parametrize('barter', [None, 1, 0, 2])
def test_barter(session, barter):
    invoice = ob.InvoiceBuilder.construct(session)
    invoice.paysys.barter = barter
    session.flush()
    invoice_info = TransactionWrapper(invoice).get_info()
    assert 'barter' not in invoice_info


@pytest.mark.parametrize('currency', ['RUR', 'USD'])
def test_currency_code(session, currency):
    invoice = ob.InvoiceBuilder.construct(session)
    invoice.currency = currency
    session.flush()
    invoice_info = TransactionWrapper(invoice).get_info()
    assert invoice_info['currency_code'] == (currency if currency != 'RUR' else 'RUB')


def test_bank_code(session):
    invoice = ob.InvoiceBuilder.construct(session)
    session.flush()
    invoice_info = TransactionWrapper(invoice).get_info()
    assert invoice_info['bank_code'] == u'Сбербанк'


@pytest.mark.parametrize('service_code', [
    'YANDEX_SERVICE_WO_VAT',
    'YANDEX_SERVICE',
    None])
def test_trx_type(session, service_code):
    invoice = ob.InvoiceBuilder.construct(session)
    invoice.service_code = service_code
    session.flush()
    invoice_info = TransactionWrapper(invoice).get_info()
    if service_code == 'YANDEX_SERVICE':
        assert invoice_info['trx_type'] == u'REVENUE'
    else:
        assert 'trx_type' not in invoice_info


@pytest.mark.parametrize('paysys_cc', ['ce_ofd', 'ph'])
def test_promo_payed(session, paysys_cc):
    invoice = ob.InvoiceBuilder.construct(session)
    invoice.paysys.cc = paysys_cc
    session.flush()
    invoice_info = TransactionWrapper(invoice).get_info()
    if paysys_cc == 'ce_ofd':
        assert invoice_info['promo_payed'] == 'Y'
    else:
        assert invoice_info['promo_payed'] == 'N'


def test_nds_percent(session):
    invoice = ob.InvoiceBuilder.construct(session)
    session.flush()
    invoice_info = convert_DU(TransactionWrapper(invoice).get_info())
    assert invoice_info['nds_pct'] == 20


@pytest.mark.parametrize('nsp_percent', [0, 20])
def test_nsp_percent(session, nsp_percent):
    invoice = ob.InvoiceBuilder.construct(session)
    invoice.invoice_orders[0].tax_policy_pct.nsp_pct = nsp_percent
    session.flush()
    invoice_info = convert_DU(TransactionWrapper(invoice).get_info())
    if nsp_percent == 0:
        assert 'nsp_pct' not in invoice_info
    else:
        assert invoice_info['nsp_pct'] == D(nsp_percent)


@pytest.mark.parametrize('w_els_number', [True, False])
def test_els_number(session, w_els_number):
    invoice = ob.InvoiceBuilder.construct(session)
    if w_els_number:
        invoice.single_account_number = ob.generate_int(5)
    session.flush()
    invoice_info = TransactionWrapper(invoice).get_info()
    if w_els_number:
        assert invoice_info['els_number'] == str(invoice.single_account_number)
    else:
        assert 'els_number' not in invoice_info


@pytest.mark.parametrize('type_rate', [1, 2])
def test_quantity(session, type_rate):
    """кратность количества к type_rate"""
    invoice = ob.InvoiceBuilder.construct(session)
    invoice.invoice_orders[0].type_rate = type_rate
    session.flush()
    invoice_info = convert_DU(TransactionWrapper(invoice).get_info())
    json_info = simplejson.dumps(invoice_info, default=_json_default,
                                 ensure_ascii=False).encode('utf8')
    assert invoice.invoice_orders[0].quantity == D('2')
    assert invoice_info['trx_lines']
    if type_rate == 1:
        assert invoice_info['trx_lines'][0]['quantity'] == D('2')
    else:
        assert invoice_info['trx_lines'][0]['quantity'] == D('1')


@pytest.mark.parametrize('type_rate', [1, 2])
def test_price(session, type_rate):
    invoice = ob.InvoiceBuilder.construct(session)
    invoice.invoice_orders[0].type_rate = type_rate
    session.flush()
    invoice_info = convert_DU(TransactionWrapper(invoice).get_info())
    assert invoice_info['trx_lines'][0]['price_wo_nds'] == D('83.33')


def test_product_id(session):
    invoice = ob.InvoiceBuilder.construct(session)
    session.flush()
    invoice_info = convert_DU(TransactionWrapper(invoice).get_info())
    assert invoice_info['trx_lines'][0]['item_guid'] == invoice.invoice_orders[0].order.service_code


def test_order_eid(session):
    invoice = ob.InvoiceBuilder.construct(session)
    session.flush()
    invoice_info = convert_DU(TransactionWrapper(invoice).get_info())
    assert invoice_info['trx_lines'][0]['order_eid'] == invoice.invoice_orders[0].order.eid


def test_y_invoice_product_id(session, contract):
    contract.client.is_agency = 1
    orders = [ob.OrderBuilder(product=ob.Getter(mapper.Product, 1475),
                              service=ob.Getter(mapper.Service, 7),
                              client=ob.ClientBuilder.construct(session),
                              agency=contract.client
                              ).build(session).obj for _ in range(2)]

    y_invoice = create_y_invoice(session, contract, orders)
    act = y_invoice.acts[0]
    act.rows[0].product_id = 503162
    y_invoice._invoice_orders_ = None
    session.flush()
    invoice_info = convert_DU(TransactionWrapper(y_invoice).get_info())
    assert [trx_line_info['item_guid'] for trx_line_info in invoice_info['trx_lines']] == [1475, 503162]

