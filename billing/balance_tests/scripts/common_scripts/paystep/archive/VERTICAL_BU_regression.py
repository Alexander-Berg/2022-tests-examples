# -*- coding: utf-8 -*-
__author__ = 'aikawa'

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils

pytestmark = [
    pytest.mark.priority('mid'),
    reporter.feature(Features.ACT, Features.INVOICE, Features.COMMON, Features.CONTRACT)
]

dt = datetime.datetime.now()

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
# NOW = datetime.datetime(2016,10,10)
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))

PERSON_TYPE = 'ur'
QTY = 25
PAYSYS_ID = 1201003

# TODO: service was removed. Will not be used in parametrization
auto_service_data = {
    'service_id': '97',
    'product_id': '503794'
}

tours_service_data = {
    'service_id': '98',
    'product_id': '504939'
}

realty_service_data = {
    'service_id': '81',
    'product_id': '503937'
}


def from_client_to_payment(service_id, product_id, contract_type, credit, client_id, agency_id, person_id):
    service_order_id = steps.OrderSteps.next_id(service_id)
    reporter.log((client_id, service_order_id, product_id, service_id, agency_id))
    steps.OrderSteps.create(client_id, service_order_id, product_id, service_id, {'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(agency_id, orders_list)
    reporter.log(request_id)
    if contract_type:
        # TODO remove static date
        contract_id, _ = steps.ContractSteps.create_contract(contract_type, {'PERSON_ID': person_id,
                                                                             'CLIENT_ID': agency_id,
                                                                             'SERVICES': [service_id],
                                                                             'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO})
    else:
        contract_id = None
        credit = 0
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=credit, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 10, 'Days': 0, 'Money': 0}, 0, dt)
    steps.ActsSteps.create(invoice_id, dt)


def create_invoice_new_firm(service_data, contract_type, credit, client_id, agency_id, person_id):
    from_client_to_payment(service_id=service_data['service_id'], product_id=service_data['product_id'], contract_type=contract_type, credit=credit, client_id=client_id, agency_id=agency_id, person_id=person_id)


@pytest.fixture
def client_id():
    return steps.ClientSteps.create()


@pytest.fixture
def agency_id():
    return steps.ClientSteps.create({'IS_AGENCY': 1})


@pytest.fixture
def person_id(agency_id):
    return steps.PersonSteps.create(agency_id, PERSON_TYPE)


@pytest.mark.smoke
@pytest.mark.parametrize('service_data', [
    # tours_service_data,
    realty_service_data
])
@pytest.mark.parametrize('contract_type, credit', [
    ('vertical_comm_post', True),
    # ('vertical_comm_post', False),

    # Отключили по просьбе бекофиса
    # (None, None)
])
def test_create_invoice_new_firm(service_data, contract_type, credit, client_id, agency_id, person_id):
    create_invoice_new_firm(service_data, contract_type, credit, client_id, agency_id, person_id)
    assert 1 == 1


if __name__ == '__main__':
    pytest.main('-v')
