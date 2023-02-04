# -*- coding: utf-8 -*-

import json
import uuid

import datetime
import hamcrest
import mock
import pytest

from balance import mapper
from balance.constants import *
from balance.oebs_config import OebsConfig
from butils.application.plugins.components_cfg import get_component_cfg
from tests import object_builder as ob
from tests.balance_tests.invoice_refunds import common

PAYSYS_ID_YM = 1000
PAYSYS_ID_BANK = 1001
PAYSYS_ID_WM = 1052

OEBS_CONFIG_OEBSAPI_FILTER = OebsConfig.CONFIG_PREFIX + 'OEBSAPI_FILTERS'

NOW = datetime.datetime.now()


def check_export_obj(export_obj, state, output, error, input, next_export=None, rate=0):
    hamcrest.assert_that(
        export_obj,
        hamcrest.has_properties(
            state=state,
            rate=rate,
            output=output,
            input=input,
            next_export=next_export,
            error=error
        )
    )


def assert_call_start(mock_obj):
    hamcrest.assert_that(
        mock_obj,
        hamcrest.has_properties(
            call_count=1,
            call_args=hamcrest.contains(
                hamcrest.contains(hamcrest.contains_string('billingImport')),
                hamcrest.has_entries(
                    headers=hamcrest.has_entry('Content-Type', 'application/json;charset=UTF-8'),
                    data=hamcrest.contains_string('"entity_type": "CUSTOMER"'),
                )
            )
        )
    )


def assert_call_check(mock_obj, request_id):
    hamcrest.assert_that(
        mock_obj,
        hamcrest.has_properties(
            call_count=1,
            call_args=hamcrest.contains(
                hamcrest.contains(hamcrest.contains_string('getStatusBilling')),
                hamcrest.has_entries(
                    headers=hamcrest.has_entry('Content-Type', 'application/json;charset=UTF-8'),
                    data='[{"request_id": %s}]' % request_id,
                )
            )
        )
    )


@pytest.fixture
def client(session):
    return ob.ClientBuilder(name='Вася', fullname='Василий Петрович Пупкин').build(session).obj


@pytest.fixture
def use_oebs_api(session):
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Person': 1}
    session.config.__dict__[OEBS_CONFIG_OEBSAPI_FILTER] = None


def mock_post(answer, status_code=200):
    return_value = mock.MagicMock(
        status_code=status_code,
        text=json.dumps(answer),
        json=lambda: answer
    )

    return mock.patch('requests.post', return_value=return_value)


@pytest.fixture
def patch_get_application():
    return mock.patch('balance.processors.oebs_api.wrappers.person.getApplication')


def create_invoice(session, paysys_id, amount):
    client = ob.ClientBuilder().build(session).obj
    order = common.create_order(client)
    return common.create_invoice(client, order, paysys_id, amount)


def create_bank_cpf(session, amount):
    invoice = create_invoice(session, PAYSYS_ID_BANK, amount)
    cpf = common.create_ocpf(invoice, amount)
    cpf.inn = 'inn'
    cpf.customer_name = 'customer_name'
    cpf.bik = 'bik'
    cpf.account_name = 'account_name'
    cpf.source_id = uuid.uuid4().int
    cpf.cash_receipt_number = uuid.uuid4().hex
    session.flush()
    session.expire_all()
    return cpf


def create_ym_cpf(session, amount):
    invoice = create_invoice(session, PAYSYS_ID_YM, amount)

    payment = common.create_ym_payment(invoice)
    payment.transaction_id = uuid.uuid4().hex
    payment.user_account = 'qweqoijfrhrgwuihgbirgwbirgw'

    cpf = common.create_payment_ocpf(payment, amount)
    cpf.source_id = uuid.uuid4().int
    cpf.cash_receipt_number = uuid.uuid4().hex

    session.flush()
    session.expire_all()
    return cpf


def create_wm_cpf(session, amount):
    invoice = create_invoice(session, PAYSYS_ID_WM, amount)

    payment = common.create_wm_payment(invoice)
    payment.receipt_sum = 0
    payment.transaction_id = uuid.uuid4().hex

    cpf = common.create_payment_ocpf(payment, amount)
    cpf.source_id = uuid.uuid4().int
    cpf.cash_receipt_number = uuid.uuid4().hex

    session.flush()
    session.expire_all()
    return cpf


def create_refund(cpf, amount=None, payload=None):
    refund = ob.InvoiceRefundBuilder(
        invoice=cpf.invoice,
        payment_id=cpf.id,
        amount=amount or cpf.amount,
        payload=payload,
    ).build(cpf.session).obj
    refund.set_status(InvoiceRefundStatus.not_exported)
    refund.export()
    cpf.session.flush()
    return refund, refund.exports['OEBS_API']


def create_invoice_transfer(src_invoice, dst_invoice, amount=None):
    if not amount:
        amount = src_invoice.total_sum
    invoice_transfer = ob.InvoiceTransferBuilder(
        src_invoice=src_invoice,
        dst_invoice=dst_invoice,
        amount=amount
    ).build(src_invoice.session).obj
    invoice_transfer.set_status(InvoiceTransferStatus.not_exported)
    invoice_transfer.export()
    return invoice_transfer, invoice_transfer.exports['OEBS_API']


@pytest.fixture
def bank_cpf(session):
    return create_bank_cpf(session, 100)


@pytest.fixture
def ym_cpf(session):
    return create_ym_cpf(session, 100)


@pytest.fixture
def wm_cpf(session):
    return create_wm_cpf(session, 100)


@pytest.fixture(name='person')
def create_person(session):
    return ob.PersonBuilder.construct(session)


@pytest.fixture(name='person_category')
def create_person_category(session, oebs_country_code=None, **kwargs):
    return ob.PersonCategoryBuilder(oebs_country_code=oebs_country_code or ob.generate_character_string(2),
                                    **kwargs).build(session).obj


@pytest.fixture
def export_obj(person):
    person.enqueue('OEBS_API', force=True)
    return person.exports['OEBS_API']


@pytest.fixture(name='firm')
def create_firm(session, country=None, w_firm_export=True, firm_id=None, **kwargs):
    if firm_id:
        return ob.Getter(mapper.Firm, firm_id).build(session).obj
    firm = ob.FirmBuilder(country=country or create_country(session), title=str(ob.get_big_number()) + '_firm',
                          **kwargs).build(session).obj
    if w_firm_export:
        oebs_org_id = ob.get_big_number()
        session.execute(
            '''INSERT INTO BO.T_FIRM_EXPORT (FIRM_ID, EXPORT_TYPE, OEBS_ORG_ID)
            VALUES (:firm_id, 'OEBS', :oebs_org_id)''',
            {'firm_id': firm.id, 'oebs_org_id': oebs_org_id})

    return firm


@pytest.fixture(name='country')
def create_country(session):
    return ob.CountryBuilder().build(session).obj


@pytest.fixture
def act(session):
    session.clear_cache()
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Act': 1, 'Invoice': 1}
    invoice = ob.InvoiceBuilder.construct(session)
    order = invoice.invoice_orders[0].order
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows(on_dt=invoice.dt)
    order.calculate_consumption(NOW, {order.shipment_type: 10})
    act, = invoice.generate_act(backdate=NOW, force=1)

    session.flush()
    return act


@pytest.fixture(autouse=True)
def oebs_config():
    def get_component_cfg_mocked(cfg, component_id, should_expand=True, expand_data=None, resolvers=None):
        if component_id == 'oebs_api':
            return {
                'Url': 'https://oebsapi-billing.testing.oebs.yandex-team.ru/oebsapi/rest/',
                'CloudUrl': 'https://oebsapi-test.mba.yandex-team.ru/rest/',
                'Timeout': 60,
                'Token': 'aaaaaaaaa',
            }
        return get_component_cfg(cfg, component_id, should_expand, expand_data, resolvers)

    with mock.patch('butils.application.plugins.components_cfg.get_component_cfg', get_component_cfg_mocked):
        yield
