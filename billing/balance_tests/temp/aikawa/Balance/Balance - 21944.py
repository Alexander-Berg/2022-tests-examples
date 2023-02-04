import datetime

from balance import balance_steps as steps

PERSON_TYPE = 'yt'
PAYSYS_ID = 1014
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'

dt = datetime.datetime.now()
client_id = steps.ClientSteps.create({'REGION_ID': '149'})
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

# contract_type = 'opt_agency'
# contract_id, _ = steps.ContractSteps.create(contract_type, {'person_id': person_id, 'client_id': client_id, 'SERVICES': [SERVICE_ID], 'FINISH_DT': '2016-11-29T00:00:00'})

service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
        ]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))

invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                              credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
steps.InvoiceSteps.pay(invoice_id)
