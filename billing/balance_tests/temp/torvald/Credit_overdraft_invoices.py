# -*- coding: utf-8 -*-

import datetime

from balance import balance_steps as steps

SERVICE_ID = 7
PRODUCT_ID = 1475
PAYSYS_ID = 1003
QTY = 118
BASE_DT = datetime.datetime.now()

client_id = None or steps.ClientSteps.create()
agency_id = None
steps.ClientSteps.link(client_id, 'clientuid32')

order_owner = client_id
invoice_owner = agency_id or client_id

person_id = None or steps.PersonSteps.create(invoice_owner, 'ur')

contract_id = None

steps.OverdraftSteps.set_force_overdraft(invoice_owner, SERVICE_ID, 10000)

orders_list = []
service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
# service_order_id =16704213
steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                        params={'AgencyID': agency_id})
orders_list.append({'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT})

request_id = steps.RequestSteps.create(invoice_owner, orders_list)
invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                             overdraft=1, endbuyer_id=None)

# steps.InvoiceSteps.pay(invoice_id)
steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 99.99, 'Money': 0}, 0, BASE_DT)

steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)