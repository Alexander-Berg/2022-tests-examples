# -*- coding: utf-8 -*-
__author__ = 'aikawa'

import datetime
from decimal import Decimal as D

import pytest

import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features

dt = datetime.datetime.now() - datetime.timedelta(days=1)

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'


def counting_difference_between_funds_and_ship(order_id):
    order_info = db.balance().execute("select * from t_order where id = :order_id", {'order_id': order_id})[0]
    order_current_qty = D(order_info['consume_qty'])
    order_completion_qty = D(order_info['completion_qty'])
    return order_current_qty - order_completion_qty


def creating_order(order_params, client_id, person_id, service_id, product_id, msr):
    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=product_id,
                                       service_id=service_id)
    if 'with_payment' in order_params:
        orders_list = [
            {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': order_params['with_payment'],
             'BeginDT': dt}
        ]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                               additional_params=dict(InvoiceDesireDT=dt))
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                     credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
    if 'with_shipment' in order_params:
        reporter.log(steps.CampaignsSteps.do_campaigns(service_id, service_order_id,
                                                       {msr: order_params['with_shipment']}, 0, dt))
    return order_id


def creating_fish_order_group(order_params_list):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    for order_params in order_params_list:
        order_id = creating_order(order_params, client_id, person_id, SERVICE_ID, PRODUCT_ID, MSR)
        order_params['order_id'] = order_id
    for position, order_params in enumerate(order_params_list):
        if 'Parent' in order_params:
            parent_order_params = order_params
            steps.OrderSteps.make_optimized_force(parent_order_params['order_id'])
            break
    steps.OrderSteps.merge(parent_order_params['order_id'],
                           sub_orders_ids=[order['order_id'] for order in order_params_list if 'Parent' not in order],
                           group_without_transfer=1)
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    return order_params_list



children_is_overshipped_so_parent_overship_is_equal = [
    {'Parent': True, 'expected_difference_between_funds_and_ship': D('-40.123')}
    ,
    {'with_shipment': D('50.123'), 'with_payment': D('10'), 'expected_difference_between_funds_and_ship': D('-40.123')}
]

children_is_not_overshipped_so_free_funds_will_be_transferred = [
    {'Parent': True, 'with_payment': D('434.55'), 'expected_difference_between_funds_and_ship': D('511.12')}
    , {'with_shipment': D('323.43'), 'with_payment': D('400'), 'expected_difference_between_funds_and_ship': D('0')}
]

@pytest.mark.slow
@reporter.feature(Features.UNIFIED_ACCOUNT)
@pytest.mark.parametrize('order_params', [
    children_is_overshipped_so_parent_overship_is_equal
    , children_is_not_overshipped_so_free_funds_will_be_transferred
]
    , ids=[
        'children_is_overshipped_so_parent_overship_is_equal'
        , 'children_is_not_overshipped_so_free_funds_will_be_transferred'
    ])
def test_ua_in_fish_order(order_params):
    order_params_list = creating_fish_order_group(order_params)
    for order in order_params_list:
        assert counting_difference_between_funds_and_ship(order['order_id']) == order[
            'expected_difference_between_funds_and_ship']
        assert 1 == 1


if __name__ == "__main__":
    pytest.main("test_UA_FISH_ONLY_OPTIMIZED.py -v")
