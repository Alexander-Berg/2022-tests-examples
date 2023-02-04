import datetime

import balance.balance_steps as steps

dt = datetime.datetime.now()
invoice_dt = datetime.datetime(2016, 7, 01, 0, 0, 0)

SERVICE_ID = 7
PRODUCT_ID = 1475

PAYSYS_ID = 1003
PERSON_TYPE = 'ur'
QTY = 100
contract_type = 'opt_agency_post'

client_id = steps.ClientSteps.create({'IS_AGENCY': 1})

person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

contract_id, _ = steps.ContractSteps.create_contract(contract_type, {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                                     'DT': '2015-04-30T00:00:00',
                                                                     'FINISH_DT': '2017-08-30T00:00:00',
                                                                     'IS_SIGNED': '2015-01-01T00:00:00',
                                                                     'SERVICES': [7],
                                                                     'FIRM': '1',
                                                                     'PERSONAL_ACCOUNT_FICTIVE': 0})

service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID,
                                   service_id=SERVICE_ID)

orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 50, 'BeginDT': dt}
]

request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                             credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)

steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 50, 'Days': 0, 'Money': 0}, 0, invoice_dt)

print steps.ActsSteps.generate(client_id, force=1, date=invoice_dt)
# request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=invoice_dt))
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                              credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)
# steps.CommonSteps.export('OEBS', 'Act', act_id)
