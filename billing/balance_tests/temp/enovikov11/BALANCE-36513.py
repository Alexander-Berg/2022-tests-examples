from balance import balance_steps as steps
from btestlib.constants import Services, PersonTypes, Managers
from datetime import datetime

TODAY = datetime(2020,7,27)

client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)

product = 507130
orders_list = []

for _ in xrange(1):
    service_order_id = steps.OrderSteps.next_id(42)
    steps.OrderSteps.create(client_id, service_order_id, service_id=42, product_id=507130,
                            params={'ManagerUID': Managers.SOME_MANAGER.uid})
    orders_list.append(
        {'ServiceID': 42, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': TODAY})

# Создаём риквест
request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=TODAY))

# Выставляем счёт
invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 12001003)
