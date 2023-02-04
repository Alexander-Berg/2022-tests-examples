from balance import balance_steps as steps
import datetime
from btestlib.constants import PersonTypes, Paysyses
import balance.balance_db as db

client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
orders_list = []
service_order_id = steps.OrderSteps.next_id(1000)
steps.OrderSteps.create(client_id, service_order_id, service_id=1000, product_id=512352)
orders_list.append(
    {'ServiceID': 1000, 'ServiceOrderID': service_order_id, 'Qty': 55, 'BeginDT': datetime.datetime.now()})
request_id = steps.RequestSteps.create(client_id, orders_list, {'InvoiceDesireDT': datetime.datetime.now()})
invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.BANK_UR_RUB.id)
