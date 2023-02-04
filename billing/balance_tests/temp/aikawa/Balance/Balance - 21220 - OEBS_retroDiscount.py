__author__ = 'aikawa'

import datetime

from balance import balance_steps as steps

dt = datetime.datetime.now()

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'
contract_type = 'no_agency_post'


client_id = steps.ClientSteps.create({'IS_AGENCY':'1'})
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)


service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id)
orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
        ]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=previous_month_not_the_last_day))

contract_id, _ = steps.ContractSteps.create_contract(contract_type, {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                                     'DT': '2015-04-30T00:00:00',
                                                                     'FINISH_DT': '2016-06-30T00:00:00',
                                                                     'IS_SIGNED': '2015-01-01T00:00:00',
                                                                     'SERVICES': [132]})
# 'CURRRENCY': 'USD',
# 'FIRM': '16',
# 'CREDIT_CURRENCY_LIMIT': 853
#
# invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                                               credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
# steps.InvoiceSteps.pay(invoice_id)
#
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, previous_month_not_the_last_day)
#
# # print steps.ActsSteps.generate(client_id, force=1, date=dt)

steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)