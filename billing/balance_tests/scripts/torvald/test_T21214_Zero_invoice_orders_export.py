# -*- coding: utf-8 -*-

import datetime

import balance.balance_api as api
import btestlib.reporter as reporter
from balance import balance_steps as steps
from btestlib.data import defaults

SERVICE_ID = 7
PRODUCT_ID = 503162
PAYSYS_ID = 1003
QTY = 100.1234
BASE_DT = datetime.datetime.now()

client_params = {'REGION_ID': '225', 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime(2000, 1, 1),
                 'SERVICE_ID': SERVICE_ID, 'IS_AGENCY': 0}
client_id = steps.ClientSteps.create(client_params)
person_id = steps.PersonSteps.create(client_id, 'ur')

contract_id, _ = steps.ContractSteps.create_contract('no_agency_post', {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                                        'DT': '2015-04-30T00:00:00',
                                                                        'FINISH_DT': '2016-06-30T00:00:00',
                                                                        'IS_SIGNED': '2015-01-01T00:00:00',
                                                                        'SERVICES': [7, 11],
                                                                        'NON_RESIDENT_CLIENTS': 0,
                                                                        'REPAYMENT_ON_CONSUME': 0,
                                                                        'PERSONAL_ACCOUNT': 1,
                                                                        'LIFT_CREDIT_ON_PAYMENT': 0,
                                                                        'PERSONAL_ACCOUNT_FICTIVE': 0
                                                                        })

service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
steps.OrderSteps.create(client_id, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID)
service_order_id2 = steps.OrderSteps.next_id(SERVICE_ID)
steps.OrderSteps.create(client_id, service_order_id2, service_id=SERVICE_ID, product_id=PRODUCT_ID)
service_order_id3 = steps.OrderSteps.next_id(SERVICE_ID)
steps.OrderSteps.create(client_id, service_order_id3, service_id=SERVICE_ID, product_id=PRODUCT_ID)
orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}]
request_id = steps.RequestSteps.create(client_id, orders_list)
invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=1, contract_id=contract_id,
                                             overdraft=0, endbuyer_id=None)
# steps.InvoiceSteps.pay(invoice_id)

response = api.medium().create_transfer_multiple(defaults.PASSPORT_UID,
                                                 [
                                                                      {"ServiceID": SERVICE_ID,
                                                                       "ServiceOrderID": service_order_id,
                                                                       "QtyOld": 100.1234, "QtyNew": 100.12}
                                                                  ],
                                                 [
                                                                      {"ServiceID": SERVICE_ID,
                                                                       "ServiceOrderID": service_order_id2,
                                                                       "QtyDelta": 1}
                                                                  ], 1, None)
reporter.log(response)
steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 0, 'Money': 100.12}, 0, BASE_DT)
steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id2, {'Bucks': 0, 'Money': 0.0034}, 0, BASE_DT)
steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)

pass
