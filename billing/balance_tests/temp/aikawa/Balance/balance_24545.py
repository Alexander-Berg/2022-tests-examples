import datetime

from balance import balance_steps as steps

dt = datetime.datetime.now()

PERSON_TYPE = 'ur'
# SERVICE_ID = 26
SERVICE_ID = 99
# SERVICE_ID = 7
# PRODUCT_ID = 1475
# PRODUCT_ID = 508287
PRODUCT_ID = 506994
PAYSYS_ID = 1003
# PRODUCT_ID = 2136
# MSR = 'Bucks'
# # MSR = 'Money'
# CONTRACT_ID = None
#               # or 719276
#
for x in range(1):
    client_id = steps.ClientSteps.create()
    # client_id = 34041366
    # person_id = 5232847
    # agency_id = 7320375
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id,
                                       params={'AgencyID': None, 'ActText': 'blabla'})

    service_order_id2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id2 = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                        service_order_id=service_order_id2, params={'AgencyID': None, 'ActText': 'bla'})

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt},
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': 100, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay_fair(invoice_id)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, dt)
    #
    steps.ActsSteps.enqueue([client_id], force=1, date=dt)
    act_id = steps.ActsSteps.generate(client_id, force=1, date=dt)[0]
    steps.CommonSteps.export('OEBS', 'Product', PRODUCT_ID)
    steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)
    steps.CommonSteps.export('OEBS', 'Act', act_id)
