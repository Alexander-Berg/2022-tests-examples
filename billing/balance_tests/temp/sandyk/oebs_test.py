# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

from btestlib import balance_steps as steps

SERVICE_ID = 111
PRODUCT_ID = 503352
PERSON_TYPE = 'ph'
PAYSYS_ID = 1001
QTY = 100
BASE_DT = datetime.datetime(2016,4,11)

client_id = None or steps.ClientSteps.create()
agency_id = None
order_owner = client_id
invoice_owner =  client_id
person_id = None or steps.PersonSteps.create(invoice_owner, PERSON_TYPE)
contract_id = None
service_order_id = '20000000005732'
steps.OrderSteps.create(7781165, service_order_id, service_id=111, product_id=503352,
                        params={'AgencyID': None})
# service_order_id2 = steps.OrderSteps.next_id(SERVICE_ID)
# steps.OrderSteps.create(order_owner, service_order_id2, service_id=SERVICE_ID, product_id=PRODUCT_ID,
#                         params={'AgencyID': agency_id})

orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': datetime.datetime(2016,4,1)}
     # , {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': QTY+10, 'BeginDT': BASE_DT}
]

request_id = steps.RequestSteps.create(7781165, orders_list)
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
#                                              overdraft=0, endbuyer_id=None)
# steps.InvoiceSteps.pay(invoice_id, None, None)
steps.CampaignsSteps.do_campaigns(SERVICE_ID ,service_order_id, {'Bucks': 10000000, 'Money': 10000000}, 0, campaigns_dt = datetime.datetime(2016,4,1) )
# steps.ActsSteps.create(invoice_id, BASE_DT)
# steps.ActsSteps.enqueue([client_id], force=0, date = BASE_DT)
# steps.ActsSteps.generate(invoice_owner, force=0, date=BASE_DT)