# -*- coding: utf-8 -*-

import datetime

from btestlib import balance_api as api
from btestlib import balance_db as db
from btestlib import balance_steps as steps
from btestlib.data import defaults

SERVICE_ID = 37
PRODUCT_ID = 502917
# PAYSYS_ID = 1601044
PAYSYS_ID = 1001
QTY = 100
BASE_DT = datetime.datetime.now()

client_id = None or steps.ClientSteps.create()
agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
order_owner = client_id
invoice_owner = agency_id or client_id
person_id = None or steps.PersonSteps.create(invoice_owner, 'ph')

contract_id, _ = steps.ContractSteps.create('opt_ag_post',{'client_id': invoice_owner, 'person_id': person_id,
                                                   'dt'       : '2015-04-30T00:00:00',
                                                   'FINISH_DT': '2016-06-30T00:00:00',
                                                   'is_signed': '2015-01-01T00:00:00',
                                                   # 'is_signed': None,
                                                   'SERVICES': [37],
                                                   'FIRM': 1
                                                   # 'COMMISSION_TYPE': 48,
                                                   # 'NON_RESIDENT_CLIENTS': 0
                                                   # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
                                                   # 'REPAYMENT_ON_CONSUME': 0,
                                                   # 'PERSONAL_ACCOUNT': 1,
                                                   # 'LIFT_CREDIT_ON_PAYMENT': 1,
                                                   # 'PERSONAL_ACCOUNT_FICTIVE': 1
                                                   })

service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                        params={'AgencyID': agency_id})
service_order_id2 = steps.OrderSteps.next_id(SERVICE_ID)
steps.OrderSteps.create(order_owner, service_order_id2, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                        params={'AgencyID': agency_id})

orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT},
      {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': QTY, 'BeginDT': BASE_DT}
]

request_id = steps.RequestSteps.create(invoice_owner, orders_list)
invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=1, contract_id=contract_id,
                                             overdraft=0, endbuyer_id=None)
