# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance import balance_web as web
from balance.features import Features

LOGIN = 'clientuid45'
QUANT = 100
MAIN_DT = datetime.datetime.now()

pytestmark = [pytest.mark.priority('mid')
    , pytest.mark.tickets('BALANCE-23841')
    , reporter.feature(Features.UI, Features.PAYSTEP, Features.REQUEST)
              ]


@pytest.mark.parametrize('params', [
    {'region_id': 225, 'service_id': 7, 'product_id': 1475, 'is_visible': 1}
    , {'region_id': None, 'service_id': 7, 'product_id': 1475, 'is_visible': 1}
    , {'region_id': 225, 'service_id': 11, 'product_id': 2136, 'is_visible': 0}
    , {'region_id': None, 'service_id': 11, 'product_id': 2136, 'is_visible': 0}
]
    , ids=lambda x: 'region_id_{0}_service_id_{1}'.format(x['region_id'], x['service_id']))
def test_direct_payment_link_on_paystep(params):
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0, 'REGION_ID': params['region_id']})
    steps.ClientSteps.link(client_id, LOGIN)
    service_order_id = steps.OrderSteps.next_id(params['service_id'])
    order_id = steps.OrderSteps.create(client_id, service_order_id, params['product_id'], params['service_id'],
                                       {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
    orders_list = [
        {'ServiceID': params['service_id'], 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    ]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': MAIN_DT})

    my_driver = web.Driver()
    with my_driver as driver:
        web.Paychoose(driver)
        web.Paychoose.open(driver, request_id)
        person_type = driver.find_element(*web.Paychoose.PERSON_TYPE_PH)
        person_type.click()
        if params['is_visible'] == 1:
            driver.find_element(*web.Paychoose.DIRECT_PAYMENT_LINK)
            return 'The link has been found'
        elif params['is_visible'] == 0:
            try:
                driver.find_element(*web.Paychoose.DIRECT_PAYMENT_LINK)
            except Exception as e:
                if 'Unable to locate element' in str(e):
                    return 'The direct_payment link is absent'
                else:
                    raise Exception('The text of exception doesn`t matched with expected one')


if __name__ == "__main__":
    pytest.main("BALANCE-23841.py -v")
