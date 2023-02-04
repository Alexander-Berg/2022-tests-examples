import datetime

import pytest

from temp.igogor.balance_objects import Contexts, Products

NOW = datetime.datetime.now()

DIRECT_FISH_RUB_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new()

MEDIA_DIRECT_FISH_PRODUCT = Products.MEDIA_DIRECT_FISH


@pytest.mark.parametrize('context', [DIRECT_FISH_RUB_CONTEXT])
def test0343(context):
    client_id = steps.ClientSteps.create()
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    order = steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                                    service_order_id=service_order_id, params={'AgencyID': None})
    service_order_id2 = steps.OrderSteps.next_id(service_id=context.service.id)
    order2 = steps.OrderSteps.create(client_id=client_id, product_id=MEDIA_DIRECT_FISH_PRODUCT.id,
                                     service_id=context.service.id, service_order_id=service_order_id2,
                                     params={'AgencyID': None})
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': NOW},
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id2, 'Qty': 100, 'BeginDT': NOW},
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params={'InvoiceDesireDT': NOW})


# -*- coding: utf-8 -*-
__author__ = 'aikawa'

import datetime
from decimal import Decimal as D

import pytest

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.matchers import contains_dicts_with_entries

dt = datetime.datetime.now() - datetime.timedelta(days=1)

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'


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

    steps.OrderSteps.merge(parent_order_params['order_id'],
                           sub_orders_ids=[order['order_id'] for order in order_params_list if 'Parent' not in order],
                           group_without_transfer=1)
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


@pytest.mark.slow
@reporter.feature(Features.UNIFIED_ACCOUNT)
@pytest.mark.parametrize('order_params', [
    from_parent_with_money_to_child_ehough_funds
    , from_parent_with_money_to_child_not_ehough_funds
    , from_parent_with_no_money_to_child
    , from_child_to_parent_with_money
]
    , ids=[
        'from_parent_with_money_to_child_ehough_funds'
        , 'from_parent_with_money_to_child_not_ehough_funds'
        , 'from_parent_with_no_money_to_child'
        , 'from_child_to_parent_with_money'
    ])
def test_ua_in_fish_order(order_params):
    order_params_list = creating_fish_order_group(order_params)
    for item in order_params_list:
        consumes = db.get_consumes_by_order(item['order_id'])
        utils.check_that(consumes, contains_dicts_with_entries(item['expected_cons_qty']))


if __name__ == "__main__":
    pytest.main("test_UA_FISH_ONLY.py -v")
