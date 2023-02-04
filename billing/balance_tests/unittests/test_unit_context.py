# coding: utf-8

from decimal import Decimal as D

import pytest

from balance.balance_objects import Order, Contract, Line, Request, Invoice, Context, EmptyObject
from balance.balance_templates import Clients, Persons
from btestlib import constants as c
from btestlib import utils

STUB = 'STUBBBO'
CLIENT_TEMPLATE = Clients.NON_CURRENCY.new()
AGENCY_TEMPLATE = Clients.NON_CURRENCY.new(is_agency=True)
PERSON_TEMPLATE = Persons.UR
CONTRACT_TEMPLATE = Contract().new(Dynamic=STUB)  # todo-igogor заменить на реальный темплейт договора
ORDER_TEMPLATE = Order().new(Dynamic=STUB)

BASE_CONTEXT = Context().new(name='TEMP_CLIENT_CONTEXT',
                             service=c.Services.DIRECT,
                             paysys=c.Paysyses.BANK_UR_RUB,
                             product=c.Products.DIRECT_FISH,
                             price=D('30.0'),
                             manager=c.Managers.SOME_MANAGER)


def test_context_client_and_agency():
    client_context = BASE_CONTEXT.new(client_template=CLIENT_TEMPLATE)
    assert client_context.agency == EmptyObject()
    assert client_context.client == CLIENT_TEMPLATE

    agency_context = client_context.new(agency_template=AGENCY_TEMPLATE)
    assert agency_context.agency == AGENCY_TEMPLATE
    assert agency_context.client == CLIENT_TEMPLATE.new(agency=AGENCY_TEMPLATE)


@pytest.mark.parametrize('context, client', [
    [BASE_CONTEXT.new(client_template=CLIENT_TEMPLATE, person_template=PERSON_TEMPLATE), CLIENT_TEMPLATE],
    [BASE_CONTEXT.new(agency_template=AGENCY_TEMPLATE, client_template=CLIENT_TEMPLATE,
                      person_template=PERSON_TEMPLATE), AGENCY_TEMPLATE],
])
def test_context_person(context, client):
    assert context.person == PERSON_TEMPLATE.new(client=client)
    assert context.invoice.person == PERSON_TEMPLATE.new(client=EmptyObject())
    assert context.act.invoice.person == PERSON_TEMPLATE.new(client=EmptyObject())


@pytest.mark.parametrize('context, client', [
    [BASE_CONTEXT.new(client_template=CLIENT_TEMPLATE, person_template=PERSON_TEMPLATE), CLIENT_TEMPLATE],
    [BASE_CONTEXT.new(agency_template=AGENCY_TEMPLATE, client_template=CLIENT_TEMPLATE,
                      person_template=PERSON_TEMPLATE), AGENCY_TEMPLATE],
])
def test_context_contract(context, client):
    assert context.contract == EmptyObject()
    contract_context = context.new(contract_template=CONTRACT_TEMPLATE)
    assert contract_context.contract == CONTRACT_TEMPLATE.new(client=client,
                                                              person=PERSON_TEMPLATE.new(client=EmptyObject()))


@pytest.mark.parametrize('context, client, agency', [
    [BASE_CONTEXT.new(client_template=CLIENT_TEMPLATE), CLIENT_TEMPLATE, EmptyObject()],
    [BASE_CONTEXT.new(agency_template=AGENCY_TEMPLATE, client_template=CLIENT_TEMPLATE),
     CLIENT_TEMPLATE, AGENCY_TEMPLATE],
])
def test_context_order(context, client, agency):
    order = Order().new(service=context.service, product=context.product, manager=context.manager,
                        client=client.new(agency=EmptyObject()), agency=agency)
    assert context.order == order
    order_context = context.new(order_template=Order().new(Dynamic=STUB, service=STUB))
    assert order_context.order == order.new(Dynamic=STUB, service=context.service)


@pytest.mark.parametrize('context, order_client', [
    (BASE_CONTEXT.new(client_template=CLIENT_TEMPLATE), EmptyObject()),
    (BASE_CONTEXT.new(agency_template=AGENCY_TEMPLATE, client_template=CLIENT_TEMPLATE), CLIENT_TEMPLATE)
])
def test_context_request_lines(context, order_client):
    order = Order().new(service=context.service, product=context.product, manager=context.manager)
    assert context.lines == [Line(order=order.new(client=order_client, agency=EmptyObject()), qty=context.lines_qty[0])]


@pytest.mark.parametrize('context', [
    (BASE_CONTEXT.new(client_template=CLIENT_TEMPLATE, person_template=PERSON_TEMPLATE))
])
def test_context_invoice(context):
    order = Order().new(service=context.service, product=context.product, manager=context.manager)
    invoice = Invoice().new(request=Request().new(client=EmptyObject(), lines=[
        [Line(order=order.new(client=order.client, agency=EmptyObject()), qty=context.lines_qty[0])]]))


@pytest.mark.parametrize('context, attrname, message_part', [
    [BASE_CONTEXT.new(), 'client', 'client_template'],
    [BASE_CONTEXT.new(), 'person', 'person_template'],
    [BASE_CONTEXT.new(), 'contract', 'client_template'],
    [BASE_CONTEXT.new(), 'order', 'client_template'],
    [BASE_CONTEXT.new(), 'request', 'client_template'],
    [BASE_CONTEXT.new(), 'invoice', 'client_template'],
    [BASE_CONTEXT.new(), 'act', 'client_template'],
    [BASE_CONTEXT.new(client_template=CLIENT_TEMPLATE), 'contract', 'person_template'],
    [BASE_CONTEXT.new(client_template=CLIENT_TEMPLATE), 'invoice', 'person_template'],
    [BASE_CONTEXT.new(client_template=CLIENT_TEMPLATE), 'act', 'person_template'],
    [BASE_CONTEXT.new(person_template=PERSON_TEMPLATE), 'person', 'client_template'],
    [BASE_CONTEXT.new(person_template=PERSON_TEMPLATE), 'contract', 'client_template'],
    [BASE_CONTEXT.new(person_template=PERSON_TEMPLATE), 'order', 'client_template'],
    [BASE_CONTEXT.new(person_template=PERSON_TEMPLATE), 'request', 'client_template'],
    [BASE_CONTEXT.new(person_template=PERSON_TEMPLATE), 'invoice', 'client_template'],
    [BASE_CONTEXT.new(person_template=PERSON_TEMPLATE), 'act', 'client_template']
])
def test_context_template_absence(context, attrname, message_part):
    with pytest.raises(utils.TestsError) as exc_info:
        getattr(context, attrname)
    assert message_part in str(exc_info.value)
