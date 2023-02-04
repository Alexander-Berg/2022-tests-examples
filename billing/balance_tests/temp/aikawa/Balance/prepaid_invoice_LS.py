import datetime

import balance.balance_steps as steps

dt = datetime.datetime.now()
invoice_dt = (dt.replace(day=1) - datetime.timedelta(days=1)).replace(hour=0, minute=0, second=0, microsecond=0)

SERVICE_ID = 7
PRODUCT_ID = 503354

PAYSYS_ID = 1055
PERSON_TYPE = 'tru'
QTY = 100
contract_type = 'tr_comm_post'

client_id = steps.ClientSteps.create()
steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',dt = invoice_dt - datetime.timedelta(hours=1), region_id = 983, service_id=SERVICE_ID, currency= 'TRY' )

person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

contract_id, _ = steps.ContractSteps.create_contract(contract_type, {
    'PERSON_ID': person_id
    , 'CLIENT_ID': client_id
    , 'SERVICES': [SERVICE_ID]
    , 'FINISH_DT': '2016-11-29T00:00:00'
    , 'PERSONAL_ACCOUNT': 1
    , 'CREDIT_LIMIT': 100
    , 'CURRENCY': 'TRY'
})

service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)

orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 50, 'BeginDT': dt}
]

request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=invoice_dt))

invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                             credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)

steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id,{'Bucks': 0, 'Days': 0, 'Money': 0}, 0, dt)

# invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
#
# steps.InvoiceSteps.pay(invoice_id)
