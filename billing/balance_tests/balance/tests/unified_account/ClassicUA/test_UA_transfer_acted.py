import datetime

import hamcrest
import pytest

import balance.balance_db as db
from balance import balance_steps as steps
from btestlib import utils as utils

dt = datetime.datetime.now() - datetime.timedelta(days=1)
act_dt = datetime.datetime.now()
ORDER_DT = dt


def generate_data_test_transfer2main_acted():
    return [
        {'person_type': 'ur', 'paysys_id': 1003, 'cons_count_on_child': 1, 'child_cons_expected': {
            'current_sum': 15000,
            'consume_sum': 15000,
            'current_qty': 500,
            'consume_qty': 500,
            'act_qty': 500,
            'act_sum': 15000,
            'completion_sum': 6000,
            'completion_qty': 200
        },
         'cons_count_on_parent': 0, 'parent_cons_expected': None},
        {'person_type': 'ph', 'paysys_id': 1001, 'cons_count_on_child': 1, 'child_cons_expected': {
            'current_sum': 6000,
            'consume_sum': 15000,
            'current_qty': 200,
            'consume_qty': 500,
            'act_qty': 500,
            'act_sum': 15000,
            'completion_sum': 6000,
            'completion_qty': 200
        },
         'cons_count_on_parent': 1, 'parent_cons_expected': {
            'current_sum': 9000,
            'consume_sum': 9000,
            'current_qty': 300,
            'consume_qty': 300,
            'act_qty': 0,
            'act_sum': 0,
            'completion_sum': 0,
            'completion_qty': 0
        }}
    ]


@pytest.mark.parametrize('data',
                         generate_data_test_transfer2main_acted()
                         )
def test_transfer2main_acted(data):
    PERSON_TYPE = data['person_type']
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    PAYSYS_ID = data['paysys_id']
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 500, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 500}, 0, dt)
    steps.ActsSteps.generate(client_id, force=1, date=act_dt)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, dt)

    parent_service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    parent_order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                              service_order_id=parent_service_order_id)

    steps.OrderSteps.merge(parent_order_id, [order_id])

    steps.OrderSteps.ua_enqueue([client_id])
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    child_consume = db.get_consumes_by_order(order_id)
    assert len(child_consume) == data['cons_count_on_child']
    utils.check_that(child_consume[0], hamcrest.has_entries(data['child_cons_expected']))
    parent_consume = db.get_consumes_by_order(parent_order_id)
    assert len(parent_consume) == data['cons_count_on_parent']
    if len(parent_consume) > 0:
        utils.check_that(parent_consume[0], hamcrest.has_entries(data['parent_cons_expected']))


def generate_data_test_transfer2child_acted():
    return [
        {'person_type': 'ur', 'paysys_id': 1003, 'cons_count_on_child': 1, 'child_cons_expected': {
            'current_sum': 15000,
            'consume_sum': 15000,
            'current_qty': 500,
            'consume_qty': 500,
            'act_qty': 500,
            'act_sum': 15000,
            'completion_sum': 6000,
            'completion_qty': 200
        },
         'cons_count_on_parent': 0, 'parent_cons_expected': None},
        {'person_type': 'ph', 'paysys_id': 1001, 'cons_count_on_child': 1, 'child_cons_expected': {
            'current_sum': 6000,
            'consume_sum': 15000,
            'current_qty': 200,
            'consume_qty': 500,
            'act_qty': 500,
            'act_sum': 15000,
            'completion_sum': 6000,
            'completion_qty': 200
        },
         'cons_count_on_parent': 1, 'parent_cons_expected': {
            'current_sum': 9000,
            'consume_sum': 9000,
            'current_qty': 300,
            'consume_qty': 300,
            'act_qty': 0,
            'act_sum': 0,
            'completion_sum': 0,
            'completion_qty': 0
        }}
    ]


@pytest.mark.parametrize('data',
                         generate_data_test_transfer2child_acted()
                         )
def test_transfer2child_acted(data):
    PERSON_TYPE = data['person_type']
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    PAYSYS_ID = data['paysys_id']
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    parent_service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    parent_order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                              service_order_id=parent_service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': parent_service_order_id, 'Qty': 500, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, parent_service_order_id, {'Bucks': 500}, 0, dt)
    steps.ActsSteps.generate(client_id, force=1, date=act_dt)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, parent_service_order_id, {'Bucks': 200}, 0, dt)

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 300}, 0, dt)

    steps.OrderSteps.merge(parent_order_id, [order_id])

    steps.OrderSteps.ua_enqueue([client_id])
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    child_consume = db.get_consumes_by_order(order_id)
    assert len(child_consume) == 1
    utils.check_that(child_consume[0], hamcrest.has_entries({
        'current_sum': 9000,
        'consume_sum': 9000,
        'current_qty': 300,
        'act_qty': 0,
        'completion_sum': 9000,
        'completion_qty': 300,
        'act_sum': 0,
        'consume_qty': 300
    }))
    parent_consume = db.get_consumes_by_order(parent_order_id)
    assert len(parent_consume) == 1
    utils.check_that(parent_consume[0], hamcrest.has_entries({
        'current_sum': 6000,
        'consume_sum': 15000,
        'current_qty': 200,
        'act_qty': 500,
        'completion_sum': 6000,
        'completion_qty': 200,
        'act_sum': 15000,
        'consume_qty': 500
    }))
