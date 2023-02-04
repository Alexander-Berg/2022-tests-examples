# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

from btestlib import balance_steps as steps

SERVICE_ID = 7
PRODUCT_ID = 1475
# PAYSYS_ID = 1601044
PAYSYS_ID = 1001
QTY = 100
BASE_DT = datetime.datetime.now()

client_id = None or steps.ClientSteps.create()
agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
order_owner = client_id
invoice_owner = agency_id or client_id
person_id = None or steps.PersonSteps.create(invoice_owner, 'ph')

contract_id, _ = steps.ContractSteps.create('opt_prem',{'client_id': invoice_owner, 'person_id': person_id,
                                                   'dt'       : '2015-04-30T00:00:00',
                                                   'FINISH_DT': '2016-06-30T00:00:00',
                                                   'is_signed': '2015-01-01T00:00:00',
                                                   # 'is_signed': None,
                                                   'SERVICES': [7],
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
      {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': QTY+10, 'BeginDT': BASE_DT}
]

request_id = steps.RequestSteps.create(invoice_owner, orders_list)
invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                             overdraft=0, endbuyer_id=None)

steps.InvoiceSteps.pay(invoice_id)
steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 70, 'Money': 0}, 0, BASE_DT)
steps.CampaignsSteps.do_campaigns(SERVICE_ID ,service_order_id2, {'Bucks': 50, 'Money': 0}, 0, BASE_DT)


# t = db.balance().execute('select  value_json, qqq from t_config where item = \'CONSUMPTION_NEGATIVE_REVERSE_ALLOWED_PARTIAL\'')
# print t[0]['qqq']


# steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)
# api.medium().CreateTransferMultiple(defaults.PASSPORT_UID,
#                                                    [
#                                                                           {"ServiceID": SERVICE_ID,
#                                                                            "ServiceOrderID": service_order_id,
#                                                                            "QtyOld": patched_qty, "QtyNew": QTY_NEW[0]}
#                                                                       ],
#                                                    [
#                                                                           {"ServiceID": SERVICE_ID,
#                                                                            "ServiceOrderID": service_order_id2,
#                                                                            "QtyDelta": 1}
#                                                                           , {"ServiceID": SERVICE_ID,
#                                                                              "ServiceOrderID": service_order_ids[2],
#                                                                              "QtyDelta": 2}
#                                                                       ], result_return, operation_id)