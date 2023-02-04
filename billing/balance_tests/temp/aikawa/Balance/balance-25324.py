# -*- coding: utf-8 -*-
__author__ = 'aikawa'

import datetime
from decimal import Decimal as D

import pytest

from balance import balance_steps as steps

dt = datetime.datetime.now() - datetime.timedelta(days=1)

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'


def creating_order(order_params, client_id, person_id, service_id, product_id, msr):
    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=product_id, service_id=service_id)
    if 'with_payment' in order_params:
        orders_list = [
            {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': order_params['with_payment'], 'BeginDT': dt}
        ]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                     credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
    if 'with_shipment' in order_params:
        steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {msr: order_params['with_shipment']}, 0, dt)
    return order_id


def creating_fish_order_group(order_params_list):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    for order_params in order_params_list:
        order_id = creating_order(order_params, client_id, person_id, SERVICE_ID, PRODUCT_ID, MSR)
        order_params['order_id'] = order_id
    # pull out parent order params from order params list
    for position, order_params in enumerate(order_params_list):
        if 'Parent' in order_params:
            parent_order_params = order_params
            break

    steps.OrderSteps.merge(parent_order_params['order_id'], sub_orders_ids=[order['order_id'] for order in order_params_list if 'Parent' not in order], group_without_transfer=1)
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    return order_params_list


from_parent_with_money_to_child_ehough_funds = [
    {'Parent': True, 'with_payment': D('100.4545'), 'expected_cons_qty': [{'current_qty': D('0')}]}
    , {'with_shipment': D('50.123'), 'expected_cons_qty': [{'current_qty': D('50.123')}]}
    , {'with_shipment': D('50.3315'), 'expected_cons_qty': [{'current_qty': D('50.3315')}]}
]

from_parent_with_money_to_child_not_ehough_funds = [
    {'Parent': True, 'with_payment': D('434.55'), 'expected_cons_qty': [{'current_qty': D('0')}]}
    , {'with_shipment': D('323.43'), 'expected_cons_qty': [{'current_qty': D('323.273775')}]}
    , {'with_shipment': D('111.33'), 'expected_cons_qty': [{'current_qty': D('111.276225')}]}
]

from_parent_with_no_money_to_child = [
    {'Parent': True, 'expected_cons_qty': []}
    , {'with_shipment': D('4545.445455'), 'expected_cons_qty': []}
    , {'with_shipment': D('54.545553'), 'expected_cons_qty': []}
]

from_child_to_parent_with_money = [
    {'Parent': True, 'with_payment': D('100.33'), 'expected_cons_qty': [
        {'current_qty': D('100.33')}
        , {'current_qty': D('4545.445455')}
        , {'current_qty': D('54.545553')}
    ]
     }
    , {'with_payment': D('4545.445455'), 'expected_cons_qty': [{'current_qty': D('0')}]}
    , {'with_payment': D('54.545553'), 'expected_cons_qty': [{'current_qty': D('0')}]}
]


def test_ua_in_fish_order():
    service_id = 11
    product_id_fish = 2136
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(service_id)
    parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=product_id_fish, service_id=service_id)
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)

    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=product_id_fish, service_id=service_id)
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}]
    # request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 50}, 0, dt)

    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id_2 = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=608, service_id=service_id)
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}]
    # request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Shows': 50}, 0, dt)

    steps.OrderSteps.merge(parent_order_id, sub_orders_ids=[order_id], group_without_transfer=1)
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    #
    #
    #
    # if 'with_shipment' in order_params:
    #     steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {msr: order_params['with_shipment']}, 0, dt)


if __name__ == "__main__":
    pytest.main("test_UA_FISH_ONLY.py -v")
