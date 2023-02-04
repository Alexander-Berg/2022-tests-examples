# -*- coding: utf-8 -*-

import datetime

import hamcrest
import pytest

import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features

DT = datetime.datetime.now()


@reporter.feature(Features.TO_UNIT)
def test_transfer_proportional_to_tax():
    PERSON_TYPE = 'ph'
    PAYSYS_ID = 1001
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    parent_order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 90, 'BeginDT': DT}
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID)
    steps.OrderSteps.transfer([{'order_id': parent_order_id, 'qty_old': 90, 'qty_new': 50, 'all_qty': 0}],
                              [{'order_id': order_id, 'qty_delta': 1}])
    consume_id = db.get_consumes_by_order(parent_order_id)[0]['id']
    db.balance().execute('''update t_consume set tax_policy_pct_id = 1 where id = :consume_id''',
                         {'consume_id': consume_id})
    steps.OrderSteps.transfer([{'order_id': order_id, 'qty_old': 40, 'qty_new': 0, 'all_qty': 0}],
                              [{'order_id': parent_order_id, 'qty_delta': 1}])

    service_order_id_1 = steps.OrderSteps.next_id(SERVICE_ID)
    child_order_id_1 = steps.OrderSteps.create(client_id, service_order_id_1, PRODUCT_ID, SERVICE_ID)

    service_order_id_2 = steps.OrderSteps.next_id(SERVICE_ID)
    child_order_id_2 = steps.OrderSteps.create(client_id, service_order_id_2, PRODUCT_ID, SERVICE_ID)
    steps.OrderSteps.merge(parent_order_id, [child_order_id_1, child_order_id_2])

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id_1, {'Bucks': 60}, 0, DT)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id_2, {'Bucks': 120}, 0, DT)
    steps.CommonSteps.export(queue_='UA_TRANSFER', object_id=client_id, classname='Client', input_={'for_dt': DT})
