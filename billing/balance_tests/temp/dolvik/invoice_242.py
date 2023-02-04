from btestlib.constants import *
from balance import balance_steps as steps
INVOICE_DT = datetime.now()


client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
service_order_id = steps.OrderSteps.next_id(242)
steps.OrderSteps.create(client_id, service_order_id, service_id=242, product_id=511480)
orders_list = [{'ServiceID': 242, 'ServiceOrderID': service_order_id, 'Qty': 50, 'BeginDT': INVOICE_DT}]
request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=INVOICE_DT))
invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.BANK_UR_RUB.id)