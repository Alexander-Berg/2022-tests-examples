import datetime

import balance.balance_steps as steps
from temp.MTestlib import MTestlib as mtl

rpc = mtl.rpc
test_rpc = mtl.test_rpc

PERSON_TYPE = 'ur'
service_id = 98
product_id = 504939
QTY = 100
dt = datetime.datetime.now()


client_id = steps.ClientSteps.create()
agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
person_id = steps.PersonSteps.create(agency_id, PERSON_TYPE)
service_order_id = steps.OrderSteps.next_id(service_id)
steps.OrderSteps.create(client_id, service_order_id, product_id, service_id, {'AgencyID':agency_id})
orders_list = [
            {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt}
    ]
request_id = steps.RequestSteps.create(agency_id, orders_list)
contract_type = 'vertical_opt_agency_prem'
contract_id, _ = steps.ContractSteps.create_contract(contract_type, {'PERSON_ID': person_id, 'CLIENT_ID': agency_id,
                                                                     'SERVICES': [service_id]})
invoice_id,_ ,_ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=1201003,
                                                              credit=1, contract_id=contract_id, overdraft=0,endbuyer_id=None)
steps.InvoiceSteps.pay(invoice_id)