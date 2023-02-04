# coding: utf-8
from decimal import Decimal as D

from balance import balance_steps2 as steps
from balance.balance_objects import Order, Contract, Context
from balance.balance_templates import Clients, Persons
from btestlib import constants as c

STUB = 'STUBBBO'
CLIENT_TEMPLATE = Clients.NON_CURRENCY.new()
AGENCY_TEMPLATE = Clients.NON_CURRENCY.new(is_agency=True)
PERSON_TEMPLATE = Persons.UR
CONTRACT_TEMPLATE = Contract().new(Dynamic=STUB)  # todo-igogor заменить на реальный темплейт договора
ORDER_TEMPLATE = Order().new(Dynamic=STUB)

BASE_CONTEXT = Context(name='TEMP_CLIENT_CONTEXT',
                       service=c.Services.DIRECT,
                       paysys=c.Paysyses.BANK_UR_RUB,
                       product=c.Products.DIRECT_FISH,
                       price=D('30.0'),
                       manager=c.Managers.SOME_MANAGER)


def test_prepare_client():
    context = BASE_CONTEXT.new(client_template=CLIENT_TEMPLATE)
    created_client = steps.prepare(context.client)
    assert created_client.new(id=None) == context.client
    assert isinstance(created_client.id, int)


def test_prepare_agency():
    context = BASE_CONTEXT.new(agency_template=AGENCY_TEMPLATE, client_template=CLIENT_TEMPLATE)
    created_subclient = steps.prepare(context.client)
    assert created_subclient.new(agency=None, id=None) == context.client.new(agency=None)
    assert isinstance(created_subclient.id, int)

    created_agency = created_subclient.agency
    assert created_agency.new(id=None) == context.agency
    assert isinstance(created_agency.id, int)


def test_prepare_person():
    context = BASE_CONTEXT.new(client_template=CLIENT_TEMPLATE, person_template=PERSON_TEMPLATE)
    created_person = steps.prepare(context.person)
    assert created_person.new(client=None, id=None) == context.person.new(client=None)
    assert isinstance(created_person.id, int)

    created_client = created_person.client
    assert created_client.new(id=None) == context.client
    assert isinstance(created_client.id, int)


def test_prepare_contract():
    context = BASE_CONTEXT.new(client_template=CLIENT_TEMPLATE, person_template=PERSON_TEMPLATE,
                               contract_template=CONTRACT_TEMPLATE)
    created_contract = steps.prepare(context.contract)
    assert created_contract.new(id=None, client=None, person=None) == context.contract.new(client=None, person=None)
    assert isinstance(created_contract.id, int)

    created_client = created_contract.client
    assert created_client.new(id=None) == context.client
    assert isinstance(created_client.id, int)

    created_person = created_contract.person
    assert created_person.new(client=None, id=None) == context.person.new(client=None)
    assert isinstance(created_person.id, int)

    assert created_person.client == created_contract.client
