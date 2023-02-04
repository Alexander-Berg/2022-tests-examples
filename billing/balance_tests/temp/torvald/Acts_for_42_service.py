# -*- coding: utf-8 -*-

import datetime

from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib.data import defaults

SERVICE_ID = 42
PRODUCT_ID = 506619
# PAYSYS_ID = 1601044
PAYSYS_ID = 1601047
QTY = 100
BASE_DT = datetime.datetime.now()

client_id = None or steps.ClientSteps.create()
# steps.ClientSteps.link(client_id, 'yndx-toloka')

agency_id = None
order_owner = client_id
invoice_owner = agency_id or client_id

# person_id = None or steps.PersonSteps.create(invoice_owner, 'sw_yt', params = {'name': 'YANDEX LLC',
#                                                                                'phone': '(495) 739-70-00',
#                                                                                'email': 'toloka-billing@yandex-team.ru',
#                                                                                'postaddress': '16 Lva Tolstogo st., Moscow, 119021, Russia',
#                                                                                'postcode': '119021'})

# person_id = None or steps.PersonSteps.create(invoice_owner, 'sw_yt')
person_id = None or steps.PersonSteps.create(invoice_owner, 'sw_yt', params={'region': '142734'})
# person_id = None or steps.PersonSteps.create(invoice_owner, 'sw_yt', params={'region': '225'})

# contract_id, _ = steps.ContractSteps.create('sw_opt_client_pre_16',{'client_id': invoice_owner, 'person_id': person_id,
#                                                    'dt'       : '2015-04-30T00:00:00',
#                                                    'FINISH_DT': '2016-06-30T00:00:00',
#                                                    'is_signed': '2015-01-01T00:00:00',
#                                                    # 'is_signed': None,
#                                                    'SERVICES': [42],
#                                                    'FIRM': 16,
#                                                    # 'COMMISSION_TYPE': 48,
#                                                    # 'NON_RESIDENT_CLIENTS': 0
#                                                    # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
#                                                    # 'REPAYMENT_ON_CONSUME': 0,
#                                                    # 'PERSONAL_ACCOUNT': 1,
#                                                    # 'CREDIT_LIMIT_SINGLE': 500000
#                                                    # 'LIFT_CREDIT_ON_PAYMENT': 1,
#                                                    # 'PERSONAL_ACCOUNT_FICTIVE': 1
#                                                    })
contract_id = None

service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                        params={'AgencyID': agency_id})
service_order_id2 = steps.OrderSteps.next_id(SERVICE_ID)
steps.OrderSteps.create(order_owner, service_order_id2, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                        params={'AgencyID': agency_id})
service_order_id3 = steps.OrderSteps.next_id(SERVICE_ID)
steps.OrderSteps.create(order_owner, service_order_id3, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                        params={'AgencyID': agency_id})
orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
]

request_id = steps.RequestSteps.create(invoice_owner, orders_list)
invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                             overdraft=0, endbuyer_id=None)

orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT},
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': QTY, 'BeginDT': BASE_DT},
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id3, 'Qty': QTY, 'BeginDT': BASE_DT}
]

# steps.CommonSteps.export('OEBS', 'Person', person_id)
# steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)
steps.InvoiceSteps.pay(invoice_id)

steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 100, 'Money': 0}, 0, BASE_DT)
# steps.CampaignsSteps.do_campaigns(SERVICE_ID ,service_order_id2, {'Bucks': 0, 'Money': 0}, 0, BASE_DT)
steps.CommonSteps.export('MONTH_PROC', 'Client', invoice_owner)
steps.ActsSteps.generate(client_id, force=0, date=BASE_DT)
acts = db.get_acts_by_invoice(invoice_id)

api.medium().CreateTransferMultiple(defaults.PASSPORT_UID,
                                    [
                                                                          {"ServiceID": SERVICE_ID,
                                                                           "ServiceOrderID": orders_list[0]['ServiceOrderID'],
                                                                           "QtyOld": 12, "QtyNew": 1}
                                                                      ],
                                    [
                                                                          {"ServiceID": SERVICE_ID,
                                                                           "ServiceOrderID": orders_list[1]['ServiceOrderID'],
                                                                           "QtyDelta": 1}
                                                                          , {"ServiceID": SERVICE_ID,
                                                                             "ServiceOrderID": orders_list[2]['ServiceOrderID'],
                                                                             "QtyDelta": 2}
                                                                      ], 1, None)

steps.CommonSteps.export('OEBS', 'Person', person_id)
steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)
steps.CommonSteps.export('OEBS', 'Act', acts[0]['id'])
