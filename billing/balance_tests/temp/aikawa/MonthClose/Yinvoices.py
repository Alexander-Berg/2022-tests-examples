__author__ = 'aikawa'

# -*- coding: utf-8 -*-

import datetime

from balance import balance_steps as steps

SERVICE_ID = 7
PRODUCT_ID = 1475
PAYSYS_ID = 1003
QTY = 100
BASE_DT = datetime.datetime.now()

client_id = None or steps.ClientSteps.create()
agency_id = None
person_id = None or steps.PersonSteps.create(client_id, 'ur')
steps.ClientSteps.link(client_id, 'clientuid32')

contract_id, _ = steps.ContractSteps.create_contract('no_agency_post', {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                                        'DT': '2015-04-30T00:00:00',
                                                                        'FINISH_DT': '2016-06-30T00:00:00',
                                                                        'IS_SIGNED': '2015-01-01T00:00:00',
                                                                        'SERVICES': [7, 11],
                                                                        'COMMISSION_TYPE': 48,
                                                                        'NON_RESIDENT_CLIENTS': 0,
                                                                        # 'REPAYMENT_ON_CONSUME': 0,
                                                                        # 'PERSONAL_ACCOUNT': 1,
                                                                        # 'LIFT_CREDIT_ON_PAYMENT': 1,
                                                                        # 'PERSONAL_ACCOUNT_FICTIVE': 1
                                                                        })

service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
steps.OrderSteps.create(client_id, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID, params={'AgencyID': agency_id})
orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}]
request_id = steps.RequestSteps.create(client_id, orders_list)
invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=1, contract_id=contract_id,
                                             overdraft=0, endbuyer_id=None)
# steps.InvoiceSteps.pay(invoice_id)

#1----------------------------------------------------
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 30, 'Money': 0}, 0, BASE_DT)

#2----------------------------------------------------
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 0.000001, 'Money': 0}, 0, BASE_DT)

#3----------------------------------------------------
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 100, 'Money': 0}, 0, BASE_DT)

#4----------------------------------------------------
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 80, 'Money': 0}, 0, BASE_DT)
# # manual repayment invoice
# steps.ActsSteps.generate(agency_id, force=1, date=BASE_DT)
# steps.CampaignsSteps.do_campaigns(SERVICE_ID ,service_order_id, {'Bucks': 95, 'Money': 0}, 0, BASE_DT)

#5----------------------------------------------------
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 99.999999, 'Money': 0}, 0, BASE_DT)
# # manual repayment invoice
# steps.ActsSteps.generate(agency_id, force=1, date=BASE_DT)
# steps.CampaignsSteps.do_campaigns(SERVICE_ID ,service_order_id, {'Bucks': 100, 'Money': 0}, 0, BASE_DT)

#6----------------------------------------------------
steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 80, 'Money': 0}, 0, BASE_DT)
# manual repayment invoice
steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
steps.CampaignsSteps.do_campaigns(SERVICE_ID ,service_order_id, {'Bucks': 95, 'Money': 0}, 0, BASE_DT)