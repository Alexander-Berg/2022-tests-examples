import datetime

from balance import balance_steps as steps
from balance import balance_db as db
dt = datetime.datetime.now()
PERSON_TYPE = 'ph'
PAYSYS_ID = 1002
SERVICE_ID = 7
PRODUCT_ID = 1475

client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                   service_order_id=service_order_id, params={'AgencyID': None, 'Text': 'blabla'})
orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 1, 'BeginDT': dt}
]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
campaigns_list = [
    {'client_id': client_id, 'service_id': SERVICE_ID, 'product_id': PRODUCT_ID, 'qty': 100}
]
invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                              person_id=person_id,
                                                              campaigns_list=campaigns_list,
                                                              paysys_id=PAYSYS_ID,
                                                              contract_id=None)
steps.InvoiceSteps.pay(invoice_id)
payment_id = db.get_payments_by_invoice(invoice_id)[0]['id']

steps.CommonSteps.export('CASH_REGISTER', classname='Payment', object_id=payment_id)
