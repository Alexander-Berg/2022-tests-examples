# -*- coding: utf-8 -*-

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features

DIRECT = 7
DIRECT_PRODUCT = 1475
PAYSYS_ID = 1003
QTY = 50
BASE_DT = datetime.datetime.now()

ORDERS_COUNT = 2


@pytest.mark.priority('low')
@reporter.feature(Features.UNIFIED_ACCOUNT)
@pytest.mark.tickets('BALANCE-21606')
def test_UA_priority():
    client_id = None or steps.ClientSteps.create()
    agency_id = None
    # agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
    order_owner = client_id
    invoice_owner = agency_id or client_id
    person_id = None or steps.PersonSteps.create(invoice_owner, 'ur')
    contract_id = None

    service_id = DIRECT
    product_id = DIRECT_PRODUCT

    service_order_id = steps.OrderSteps.next_id(service_id)
    group_order_id = steps.OrderSteps.create(order_owner, service_order_id, service_id=service_id,
                                             product_id=product_id,
                                             params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
    ]
    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0,
                                                 contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)

    service_order_ids = []
    order_ids = []
    orders_list = []
    for i in xrange(ORDERS_COUNT):
        reporter.log(('--------------------------{}-------------------------'.format(i)))
        service_order_ids.append(steps.OrderSteps.next_id(service_id))
        order_ids.append(
            steps.OrderSteps.create(order_owner, service_order_ids[i], service_id=service_id, product_id=product_id,
                                    params={'AgencyID': agency_id}))
        steps.CampaignsSteps.do_campaigns(service_id, service_order_ids[i], {'Bucks': QTY / 10 * i, 'Money': 0}, 0,
                                          datetime.datetime(2015, 11, 22))

    steps.OrderSteps.merge(group_order_id, order_ids)
    # increase_priority('{"Client_ids": [%s], "Orders_ids": []}' % (invoice_owner))
    with steps.OrderSteps.increase_priority(client_id):

        # We should enqueue object only, not process
        steps.OrderSteps.ua_enqueue([client_id])
        # steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)

    query = "select priority from t_export where type ='UA_TRANSFER' and classname = 'Client' and object_id = :client_id"
    query_params = {'client_id': client_id}
    result = db.balance().execute(query, query_params)
    assert result[0]['priority'] == -1


if __name__ == "__main__":
    pytest.main("test_UA_priority.py -v")
