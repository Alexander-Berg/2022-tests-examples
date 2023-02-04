# -*- coding: utf-8 -*-
__author__ = 'aikawa'

import datetime

import pytest
from hamcrest import equal_to, has_entries

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils

dt = datetime.datetime.now() - datetime.timedelta(days=1)
contract_dt = datetime.datetime.now() + datetime.timedelta(days=7)

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'
QTY = 100
CONTRACT_TYPE = 'no_agency_post'


@pytest.fixture
def client_id():
    return steps.ClientSteps.create()


@pytest.fixture
def person_id(client_id):
    return steps.PersonSteps.create(client_id, PERSON_TYPE)


@pytest.fixture
def parent_order_id(client_id):
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
    return parent_order_id


def order_id(client_id):
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
    return order_id, service_order_id


def main(client_id, person_id, child_service_order_id, invoice_params):
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': child_service_order_id, 'Qty': QTY, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
    if invoice_params['with_contract']:
        contract_id, _ = steps.ContractSteps.create_contract(CONTRACT_TYPE,
                                                             {'PERSON_ID': person_id, 'CLIENT_ID': client_id,
                                                              'SERVICES': [SERVICE_ID], 'FINISH_DT': contract_dt})
    else:
        contract_id = None
    if invoice_params['overdraft']:
        steps.OverdraftSteps.set_force_overdraft(client_id, SERVICE_ID, 100000)
        overdraft = 1
    else:
        overdraft = 0
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=invoice_params['credit'], contract_id=contract_id, overdraft=overdraft, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
    return invoice_id


without_contract = dict(with_contract=False, credit=False, overdraft=False)
with_contract_non_credit = dict(with_contract=True, credit=False, overdraft=False)
with_contract_with_credit = dict(with_contract=True, credit=True, overdraft=False)
with_overdraft = dict(with_contract=False, credit=False, overdraft=True)


@pytest.mark.parametrize('invoice_params', [
    without_contract
    , with_contract_non_credit
    , with_contract_with_credit
    , with_overdraft
]
    , ids=[
        'without_contract'
        , 'with_contract_non_credit'
        , 'with_contract_with_credit'
        , 'with_overdraft'
    ])
def test_UA_payment_on_daughter(client_id, person_id, parent_order_id, invoice_params):
    child_order_id, child_service_order_id = order_id(client_id)
    steps.OrderSteps.merge(parent_order_id, [child_order_id])
    invoice_id = main(client_id, person_id, child_service_order_id, invoice_params)
    # check if consume has been created but now is transferred fully to parent_order
    child_consume = db.get_consumes_by_order(child_order_id)[0]
    utils.check_that(child_consume['current_qty'], equal_to(0))
    # they are connected via invoice
    parent_consume = db.get_consumes_by_order(parent_order_id)[0]
    utils.check_that(parent_consume, has_entries({'invoice_id': invoice_id, 'current_qty': QTY}))


@pytest.mark.parametrize('invoice_params', [
    without_contract
]
    , ids=[
        'without_contract'
    ])
def test_UA_transfer_to_daughter(client_id, person_id, parent_order_id, invoice_params):
    dpt_order_id, dpt_service_order_id = order_id(client_id)
    main(client_id, person_id, dpt_service_order_id, invoice_params)
    child_order_id, child_service_order_id = order_id(client_id)
    steps.OrderSteps.merge(parent_order_id, [child_order_id])
    steps.OrderSteps.transfer(
        [
            {'order_id': dpt_order_id, 'qty_old': QTY, 'qty_new': QTY / 2, 'all_qty': 0}
        ],
        [
            {'order_id': child_order_id, 'qty_delta': 1}
        ]
    )
    # check that consume created by transfer wasn't not transferred to parent_order
    child_consume = db.get_consumes_by_order(child_order_id)[0]
    utils.check_that(child_consume['current_qty'], equal_to(QTY/2))

if __name__ == "__main__":
    pytest.main("test_UA_payment_on_daughter.py -k")
