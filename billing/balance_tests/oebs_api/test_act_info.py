# -*- coding: utf-8 -*-
import datetime
import pytest
import simplejson

from balance.processors.oebs_api.api import _json_default
from balance.processors.oebs_api.utils import convert_DU
from balance.processors.oebs_api.wrappers import TransactionWrapper
from butils import decimal_unit
from tests import object_builder as ob

DU = decimal_unit.DecimalUnit

NOW = datetime.datetime.now()
MONTH_BEFORE = datetime.datetime.now() - datetime.timedelta(days=30)
TWO_MONTH_BEFORE = datetime.datetime.now() - datetime.timedelta(days=60)
MONTH_AFTER = datetime.datetime.now() + datetime.timedelta(days=30)


@pytest.fixture
def act(session):
    invoice = ob.InvoiceBuilder.construct(session)
    order = invoice.invoice_orders[0].order
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows(on_dt=invoice.dt)
    order.calculate_consumption(NOW, {order.shipment_type: 10})
    act, = invoice.generate_act(backdate=NOW, force=1)
    return act


def test_entity_type(act):
    act_info = TransactionWrapper(act).get_info()
    assert act_info['entity_type'] == 'AKT'


def test_entity_id(act):
    act_info = TransactionWrapper(act).get_info()
    assert act_info['entity_id'] == str(act.id)


def test_trx_number(act):
    act_info = TransactionWrapper(act).get_info()
    assert act_info['trx_number'] == act.external_id


def test_oe_code(act):
    act_info = TransactionWrapper(act).get_info()
    assert act_info['oe_code'] == 'YARU'


def test_client_guid(session, act):
    client = ob.ClientBuilder.construct(session)
    act.client_id = client.id
    session.flush()
    act_info = TransactionWrapper(act).get_info()
    assert act_info['client_guid'] == 'C' + str(act.client.id)


@pytest.mark.parametrize('w_endbuyer', [True, False])
def test_customer_guid(session, act, w_endbuyer):
    if w_endbuyer:
        endbuyer = ob.PersonBuilder.construct(session)
        act.invoice.endbuyer_id = endbuyer.id
        session.flush()
    act_info = TransactionWrapper(act).get_info()
    if w_endbuyer:
        assert act_info['customer_guid'] == 'P' + str(endbuyer.id)
    else:
        assert act_info['customer_guid'] == 'P' + str(act.invoice.person.id)


def test_trx_date(session, act):
    act_info = TransactionWrapper(act).get_info()
    session.flush()
    assert act_info['trx_date'] == act.dt


@pytest.mark.parametrize('w_contract', [True, False])
def test_contract_guid(session, act, w_contract):
    invoice = ob.InvoiceBuilder.construct(session)
    if w_contract:
        contract = ob.ContractBuilder.construct(session)
        invoice.contract = contract
    session.flush()
    act_info = TransactionWrapper(act).get_info()
    assert 'contract_guid' not in act_info


def test_unilateral(session, act):
    act_info = TransactionWrapper(act).get_info()
    assert 'unilateral' not in act_info


def test_overdraft(session, act):
    act_info = TransactionWrapper(act).get_info()
    assert 'overdraft' not in act_info


@pytest.mark.parametrize('payment_term_dt', [None, MONTH_AFTER])
def test_payment_term_days(session, act, payment_term_dt):
    act.payment_term_dt = payment_term_dt
    session.flush()
    act_info = TransactionWrapper(act).get_info()
    if payment_term_dt:
        assert act_info['payment_term_days'] == 30
    else:
        assert 'payment_term_days' not in act_info


@pytest.mark.parametrize('is_docs_separated', [1, 0])
@pytest.mark.parametrize('is_docs_detailed', [1, 0])
def test_printable_docs_type(session, act, is_docs_detailed, is_docs_separated):
    act.is_docs_detailed = is_docs_detailed
    act.is_docs_separated = is_docs_separated
    session.flush()
    act_info = TransactionWrapper(act).get_info()
    json_info = simplejson.dumps(convert_DU(act_info), default=_json_default,
                                 ensure_ascii=False).encode('utf8')
    if is_docs_separated:
        if is_docs_detailed:
            assert act_info['printable_docs_type'] == 3
        else:
            assert act_info['printable_docs_type'] == 1
    else:
        if is_docs_detailed:
            assert act_info['printable_docs_type'] == 2
        else:
            assert act_info['printable_docs_type'] == 0


def test_bank_code(session, act):
    session.flush()
    act_info = TransactionWrapper(act).get_info()
    assert act_info['bank_code'] == u'Сбербанк'


@pytest.mark.parametrize('barter', [None, 1, 0, 2])
def test_barter(session, barter, act):
    act.invoice.paysys.barter = barter
    session.flush()
    act_info = TransactionWrapper(act).get_info()
    assert 'barter' not in act_info


@pytest.mark.parametrize('is_docs_separated', [1, 0])
@pytest.mark.parametrize('is_docs_detailed', [1, 0])
@pytest.mark.parametrize('empty_row_product_id', [True, False])
def test_product_id(act, session, empty_row_product_id, is_docs_separated, is_docs_detailed):
    act.is_docs_detailed = is_docs_detailed
    act.is_docs_separated = is_docs_separated
    if not empty_row_product_id:
        act_row_product_id = ob.generate_int(6)
        act.rows[0].product_id = act_row_product_id
        session.flush()
    act_info = TransactionWrapper(act).get_info()
    if empty_row_product_id:
        assert act_info['trx_lines'][0]['item_guid'] == act.rows[0].consume.order.product.id
    else:
        assert act_info['trx_lines'][0]['item_guid'] == act_row_product_id
