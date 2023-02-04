# -*- coding: utf-8 -*-
__author__ = 'aikawa'
import datetime

import pytest
from hamcrest import equal_to

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils

dt = datetime.datetime.now()

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'


@pytest.fixture
def client_id():
    return steps.ClientSteps.create()

@pytest.fixture
def person_id(client_id):
    return steps.PersonSteps.create(client_id, PERSON_TYPE)

@pytest.fixture
def agency_id():
    return steps.ClientSteps.create({'IS_AGENCY': 1})

@pytest.fixture
def agency_person_id(agency_id):
    return steps.PersonSteps.create(client_id, PERSON_TYPE)

contract_types = ['no_agency']

def from_contract_creating_to_invoice(contract_type, client_id, person_id):
    print client_id
    print agency_id
    print contract_type
    contract_id, _ = steps.ContractSteps.create_contract(contract_type, {'PERSON_ID': person_id, 'CLIENT_ID': client_id,
                                                                         'SERVICES': [SERVICE_ID],
                                                                         'FINISH_DT': '2016-11-29T00:00:00'})
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
    orders_list = [
                {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
            ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))

    invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID, credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
    return contract_id


@pytest.mark.parametrize('contract_type', contract_types)
@pytest.mark.parametrize('client_and_person_from_contract', [{'client_id': client_id, 'person_id': person_id}, {'client_id': agency_id, 'person_id': agency_person_id}], ids=[
    'is_client'
    , 'is_agency'
])
def test_creating_contract(contract_type, client_and_person_from_contract):
    client_id = client_and_person_from_contract['client_id']
    person_id = client_and_person_from_contract['person_id']
    contract_id = from_contract_creating_to_invoice(contract_type, client_id, person_id)
    # a-vasin: ооочень странная проверка
    client = db.get_contracts_by_client(client_id)
    utils.check_that(client['client_id'], equal_to(client_id))

if __name__ == "__main__":
    pytest.main("Contract_test.py -v")
