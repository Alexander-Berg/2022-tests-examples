# -*- coding: utf-8 -*-

import datetime

import btestlib.reporter as reporter
from balance import balance_steps as steps

SERVICE_ID = 114
PRODUCT_ID = 502981
PAYSYS_ID = 1017
QTY = 5
BASE_DT = datetime.datetime.now()

client_id = None or steps.ClientSteps.create()
# db.balance().execute('''Update t_client set FULLNAME = u'UL Nonres', CURRENCY_PAYMENT = 'USD', ISO_CURRENCY_PAYMENT = 'USD', IS_NON_RESIDENT = 1
#                     where ID = :client_id ''',
#         {'client_id':client_id})
# agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
agency_id = None
order_owner = client_id
invoice_owner = agency_id if agency_id is not None else client_id
person_id = None or steps.PersonSteps.create(invoice_owner, 'ua')
#
contract_id, _ = steps.ContractSteps.create_contract('ukr_opt_client',
                                                     {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                      'DT': '2015-04-30T00:00:00',
                                                      'FINISH_DT': '2016-06-30T00:00:00',
                                                      'IS_SIGNED': '2015-01-01T00:00:00',
                                                      'SERVICES': [114],
                                                      # 'COMMISSION_TYPE': 57,
                                                      # 'NON_RESIDENT_CLIENTS': 1,
                                                      # 'REPAYMENT_ON_CONSUME': 0,
                                                      # 'PERSONAL_ACCOUNT': 1,
                                                      # 'LIFT_CREDIT_ON_PAYMENT': 0,
                                                      # 'PERSONAL_ACCOUNT_FICTIVE': 0
                                                      })
steps.ContractSteps.create_collateral(1033, {'CONTRACT2_ID': contract_id, 'DT': '2015-04-30T00:00:00',
                                             'IS_SIGNED': '2015-01-01T00:00:00'})

for i in range(20):
    reporter.log(('--------------------------{}-------------------------'.format(i)))
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                        params={'AgencyID': agency_id})
# service_order_id2 = steps.OrderSteps.next_id(SERVICE_ID)
# steps.OrderSteps.create(client_id, service_order_id2, service_id=SERVICE_ID, product_id=PRODUCT_ID)
# service_order_id3 = steps.OrderSteps.next_id(SERVICE_ID)
# steps.OrderSteps.create(client_id, service_order_id3, service_id=SERVICE_ID, product_id=PRODUCT_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}]
    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=1, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)
# steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 1.000221, 'Money': 0}, 0, BASE_DT)
# steps.CampaignsSteps.do_campaigns(SERVICE_ID ,service_order_id2, {'Bucks': 0, 'Money': 0}, 0, BASE_DT)
steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)

pass
