# coding: utf-8

from decimal import Decimal as D

from balance import balance_steps2 as steps
from balance.balance_objects import RequestOrder, Context
from balance.balance_templates import Clients, Persons, Contracts, Collaterals
from btestlib import constants as c
from btestlib import utils


def test_design_prepare():
    context = Context(name='TEMP_CLIENT_CONTEXT',
                      service=c.Services.DIRECT,
                      paysys=c.Paysyses.BANK_UR_RUB,
                      product=c.Products.DIRECT_FISH,
                      price=D('30.0'),
                      manager=c.Managers.SOME_MANAGER,

                      client_template=Clients.NON_CURRENCY,
                      person_template=Persons.UR,
                      request_orders_templates=[RequestOrder(qty=D('100.0')), RequestOrder(qty=D('200.0'))]
                      )

    agency = context.agency
    # prepared_agency = steps.prepare(agency)
    client = context.client
    prepared_client = steps.prepare(client)
    person = context.person
    prepared_person = steps.prepare(person)
    contract = context.contract
    order = context.order
    prepared_order = steps.prepare(order)
    request = context.request
    prepared_request = steps.prepare(request)
    invoice = context.invoice
    prepared_invoice = steps.prepare(invoice)
    act = context.act
    prepared_act = steps.prepare(act)

    pass


def test_prepare_contract():
    context = Context(name='TEMP_CLIENT_CONTEXT',
                      service=c.Services.DIRECT,
                      manager=c.Managers.SOME_MANAGER,

                      client_template=Clients.NON_CURRENCY,
                      person_template=Persons.UR,
                      contract_template=Contracts.DEFAULT.new(
                          type=c.ContractCommissionType.NO_AGENCY,
                          start_dt=utils.Date.shift(months=-6),
                          finish_dt=utils.Date.shift(months=6),
                          signed_dt=utils.Date.shift(months=-5),
                          cancelled_dt=utils.Date.shift(days=-10),
                          dict_params={'print-template': 'torro'},
                          MANAGER_BO_CODE=21463,
                          FIRM=111,
                          collaterals=[
                              Collaterals.by_id(collateral_type_id=80).new(start_dt=utils.Date.shift(months=-1),
                                                                           finish_dt=utils.Date.shift(months=9),
                                                                           signed_dt=utils.Date.shift()
                                                                           )]))

    contract = steps.prepare(context.contract)


def test_contract_creation_explicit():
    client = steps.create_or_update_client(Clients.NON_CURRENCY)
    person = steps.create_or_update_person(Persons.UR.new(client=client))
    contract = steps.create_or_update_contract(Contracts.DEFAULT.new(type=c.ContractCommissionType.NO_AGENCY,
                                                                     client=client, person=person,
                                                                     start_dt=utils.Date.shift(months=-6),
                                                                     finish_dt=utils.Date.shift(months=6),
                                                                     signed_dt=utils.Date.shift(months=-5),
                                                                     dict_params={'print-template': 'torro',
                                                                                  'manager-bo-code': 21463},
                                                                     FIRM=111
                                                                     ))
    collateral = steps.create_or_update_collateral(
        Collaterals.by_id(collateral_type_id=80).new(contract=contract,
                                                     finish_dt=utils.Date.shift(months=9),
                                                     start_dt=utils.Date.shift(months=-1)))
    annuled_collateral = steps.create_or_update_collateral(collateral.new(signed_dt=utils.Date.shift()))
    annuled_contract = steps.create_or_update_contract(contract.new(cancelled_dt=utils.Date.shift(days=-10)))
    pass
