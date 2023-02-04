# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import datetime
import pytest
import allure
from decimal import Decimal as D

from balance.actions.invoice_turnon import InvoiceTurnOn
from balance.actions import single_account
from balance.constants import (
    FirmId,
    InvoiceTransferStatus,
    ServiceId,
    OebsOperationType,
)
from balance.mapper.common import Service
from balance.providers.personal_acc_manager import PersonalAccountManager
import tests.object_builder as ob

from brest.core.tests import utils as test_utils
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.person import create_person

DEFAULT_ORDER_QTY = 10
DEFAULT_OVERDRAFT_DELAY = 30


def create_custom_invoice(
        orders_qty_map=None,
        client=None,
        firm_id=FirmId.YANDEX_OOO,
        service_id=ServiceId.DIRECT,
        **kwargs  # noqa: C816
):
    session = test_utils.get_test_session()
    client = client or ob.ClientBuilder().build(session).obj
    orders_qty_map = (
        orders_qty_map
        or {ob.OrderBuilder(service_id=service_id, client=client).build(session).obj: D('1')}
    )
    rows = [
        ob.BasketItemBuilder(order=order, quantity=qty)
        for order, qty in orders_qty_map.items()
    ]
    basket = ob.BasketBuilder(
        client=client,
        rows=rows,
    )
    request = ob.RequestBuilder(basket=basket, firm_id=firm_id)
    person = kwargs.pop('person', ob.PersonBuilder(client=client))
    invoice = ob.InvoiceBuilder(
        request=request,
        person=person,
        **kwargs  # noqa: C815
    ).build(session).obj
    return invoice


def create_cash_payment_fact(invoice, amount, operation_type=OebsOperationType.ONLINE, dt=None):
    cpf = ob.OebsCashPaymentFactBuilder(
        amount=amount,
        invoice=invoice,
        operation_type=operation_type,
        dt=dt
    ).build(invoice.session).obj
    invoice.session.expire_all()  # триггер
    return cpf


def create_correction_payment(invoice):
    ob.create_correction_payment(invoice)


@pytest.fixture(name=u'invoice')
@allure.step(u'create invoice')
def create_invoice(
    client=None,
    firm_id=FirmId.YANDEX_OOO,
    service_id=ServiceId.DIRECT,
    order_count=3,
    turn_on=False,
    **kwargs
):
    session = test_utils.get_test_session()
    service = ob.Getter(Service, service_id)
    client = client or ob.ClientBuilder().build(session).obj
    orders = [ob.OrderBuilder(service=service, client=client).build(session).obj for _i in range(order_count)]
    invoice = create_custom_invoice(
        orders_qty_map={order: DEFAULT_ORDER_QTY for order in orders},
        firm_id=firm_id,
        client=client,
        service_id=service_id,
        **kwargs  # noqa: C815
    )
    if turn_on:
        invoice.turn_on_rows()
    return invoice


@pytest.fixture(name=u'invoice_with_endbuyer')
@allure.step(u'create invoice with endbuyer')
def create_invoice_with_endbuyer():
    session = test_utils.get_test_session()
    order = ob.OrderBuilder(generate_id=True).build(session).obj
    person = ob.PersonBuilder().build(session).obj
    request = ob.RequestBuilder(
        basket=ob.BasketBuilder(rows=[ob.BasketItemBuilder(order=order, quantity=1)]),
    )
    invoice = ob.InvoiceBuilder(
        request=request,
        endbuyer_id=person.id,
    ).build(session).obj
    InvoiceTurnOn(invoice, invoice.effective_sum, manual=True).do()

    return invoice


@pytest.fixture(name=u'overdraft_invoice')
@allure.step(u'create overdraft invoice')
def create_overdraft_invoice(client=None, person=None, firm_id=FirmId.YANDEX_OOO):
    session = test_utils.get_test_session()
    client = client or create_client()
    person = person or create_person(client=person)

    now = datetime.datetime.now()
    dt = now - datetime.timedelta(days=DEFAULT_OVERDRAFT_DELAY)

    order = ob.OrderBuilder.construct(session, client=client)
    rows = [
        ob.BasketItemBuilder(order=order, quantity=10)
    ]
    basket = ob.BasketBuilder(rows=rows, client=client)
    request = ob.RequestBuilder.construct(session, basket=basket, firm_id=firm_id)
    invoice = ob.InvoiceBuilder.construct(
        session,
        request=request,
        person=person,
        overdraft=True,
        dt=dt,
    )

    return invoice


@pytest.fixture(name='personal_account')
@allure.step('create personal account')
def create_personal_account(client=None):
    session = test_utils.get_test_session()
    person_type = list(single_account.availability.ALLOWED_LEGAL_ENTITY_PERSON_CATEGORIES)[0]
    client = client or ob.ClientBuilder(with_single_account=True).build(session).obj
    person = ob.PersonBuilder(
        client=client,
        name='Sponge Bob',
        email='s.bob@nickelodeon.com',
        type=person_type,
    ).build(session).obj

    single_account.prepare.process_client(client)
    personal_account = (
        PersonalAccountManager(session)
        .for_person(person)
        .for_single_account(client.single_account_number)
        .get(auto_create=False)
    )

    return personal_account


@pytest.fixture(name=u'request_')
@allure.step(u'create request')
def create_request(client=None, firm_id=FirmId.YANDEX_OOO, order_qty=None, agency=None):
    session = test_utils.get_test_session()
    client = client or ob.ClientBuilder().build(session).obj
    order_qty = order_qty or [D(DEFAULT_ORDER_QTY)] * 3
    orders = [ob.OrderBuilder(agency=agency, client=client).build(session).obj for _i in range(len(order_qty))]
    rows = [ob.BasketItemBuilder(order=order, quantity=qty) for order, qty in zip(orders, order_qty)]
    basket = ob.BasketBuilder(client=agency or client, rows=rows)
    return ob.RequestBuilder(basket=basket, firm_id=firm_id).build(session).obj


@pytest.fixture(name=u'invoice_transfer')
def create_invoice_transfer(src_invoice, dst_invoice, amount, status=InvoiceTransferStatus.exported):
    session = test_utils.get_test_session()
    invoice_transfer = ob.InvoiceTransferBuilder(
        src_invoice=src_invoice,
        dst_invoice=dst_invoice,
        amount=amount
    ).build(session).obj
    invoice_transfer.set_status(status)
    session.flush()
    return invoice_transfer
