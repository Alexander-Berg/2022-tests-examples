# -*- coding: utf-8 -*-
__author__ = 'aikawa'

import datetime

from balance import balance_steps as steps

dt = datetime.datetime.now() - datetime.timedelta(days=1)

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
CURRENCY_PRODUCT_ID = 503162
NON_CURRENCY_PRODUCT_ID = 1475
NON_CURRENCY_MSR = 'Bucks'
CURRENCY_MSR = 'Money'

client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID)
orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}
    ]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
steps.InvoiceSteps.pay(invoice_id)
steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {NON_CURRENCY_MSR: 100}, 0, dt)
steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='MODIFY', dt=dt)
steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {CURRENCY_MSR: 29}, 0, dt)
