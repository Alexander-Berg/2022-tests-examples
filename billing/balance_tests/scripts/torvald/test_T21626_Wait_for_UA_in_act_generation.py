# -*- coding: utf-8 -*-

import datetime

import btestlib.reporter as reporter
from balance import balance_steps as steps

DIRECT = 7
DIRECT_PRODUCT = 1475

PAYSYS_ID = 1003
QTY = 100
FIRM_ID = 1
FICTIVE_PERSONAL_ACCOUNT_COLLATERAL_ID = 1033
BASE_DT = datetime.datetime.now()
COMPLETION_DT = datetime.datetime(2015,11,20)


def test_1():
    service_id = DIRECT
    product_id = DIRECT_PRODUCT

    client_id = None or steps.ClientSteps.create()
    agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
    order_owner = client_id
    invoice_owner = agency_id or client_id
    person_id = None or steps.PersonSteps.create(invoice_owner, 'ur')
    contract_id = None

    service_order_id = steps.OrderSteps.next_id(service_id)
    group_order_id = steps.OrderSteps.create(order_owner, service_order_id, service_id=service_id, product_id=product_id,
                            params={'AgencyID': agency_id})
    service_order_id2 = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(order_owner, service_order_id2, service_id=service_id, product_id=product_id,
                            params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
      , {'ServiceID': service_id, 'ServiceOrderID': service_order_id2, 'Qty': QTY, 'BeginDT': BASE_DT}
    ]

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    service_order_ids = []
    order_ids = []
    orders_list = []
    for i in xrange(3):
        reporter.log(('--------------------------{}-------------------------'.format(i)))
        service_order_ids.append(steps.OrderSteps.next_id(service_id))
        order_ids.append(
            steps.OrderSteps.create(order_owner, service_order_ids[i], service_id=service_id, product_id=product_id,
                                    params={'AgencyID': agency_id}))
        steps.CampaignsSteps.do_campaigns(service_id, service_order_ids[i], {'Bucks': QTY, 'Money': 0}, 0, COMPLETION_DT)

    steps.OrderSteps.merge(group_order_id, order_ids)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id2, {'Bucks': QTY, 'Money': 0}, 0, COMPLETION_DT)
    # steps.CampaignsSteps.do_campaigns(service_id, service_order_ids[2], {'Bucks': QTY, 'Money': 0}, 0, COMPLETION_DT)

    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)

    steps.ActsSteps.enqueue([invoice_owner], BASE_DT, 1)
    steps.CommonSteps.export('MONTH_PROC', 'Client', invoice_owner)

    return 1

if __name__ == "__main__":
    test_1()