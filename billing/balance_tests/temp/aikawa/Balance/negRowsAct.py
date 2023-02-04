import datetime

import balance.balance_steps as steps

dt = datetime.datetime.now()


def test_1():
    SERVICE_ID = 7
    PRODUCT_ID = 1475

    PAYSYS_ID = 1003
    PERSON_TYPE = 'ur'
    QTY = 100
    contract_type = 'opt_agency_post'

    client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    steps.ClientSteps.link(client_id, 'aikawa-test-0')

    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID,
                                       service_id=SERVICE_ID)

    service_order_id2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id2 = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id2, product_id=PRODUCT_ID,
                                        service_id=SERVICE_ID)

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}
        , {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': 100, 'BeginDT': dt}
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 50, 'Days': 0, 'Money': 0}, 0, dt)

    print steps.ActsSteps.generate(client_id, force=1, date=dt)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 20, 'Days': 0, 'Money': 0}, 0, dt)
    steps.OrderSteps.transfer(
        [
            {'order_id': order_id, 'qty_old': QTY, 'qty_new': 20, 'all_qty': 1}
        ],
        [
            {'order_id': order_id2, 'qty_delta': 1}
        ]
    )
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id2, {'Bucks': 70, 'Days': 0, 'Money': 0}, 0, dt)
    print steps.ActsSteps.generate(client_id, force=1, date=dt)

# request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=invoice_dt))
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                              credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)
# steps.CommonSteps.export('OEBS', 'Act', act_id)
