# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest

import balance.balance_api as api
from balance import balance_steps as steps

DT = datetime.datetime.now()
PERSON_TYPE = 'ph'
PAYSYS_ID = 1001
SERVICE_ID = 7
NON_CURRENCY_PRODUCT_ID = 1475
MAIN_DT = datetime.datetime.now()
QTY = 100


def test_fair_overdraft_mv_client():

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    soid = []
    orders_list = []
    for s in range(3):
        service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
        order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID)
        soid.append(service_order_id)
        orders_list.append({'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': DT})

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                     credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, None, None)


    steps.CampaignsSteps.do_campaigns(SERVICE_ID, soid[1], {'Bucks': 100}, 0, campaigns_dt=DT)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, soid[2], {'Bucks': 100}, 0, campaigns_dt=DT)

