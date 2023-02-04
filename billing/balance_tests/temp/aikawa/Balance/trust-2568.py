import datetime

from balance import balance_steps as steps

dt = datetime.datetime.now()

PERSON_TYPE = 'ur'
SERVICE_ID = 7
PRODUCT_ID = 1475

client_id = steps.ClientSteps.create()
steps.ClientSteps.link(client_id, 'yndx-tst-role-3')
steps.ClientSteps.deny_cc(client_id)
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                   service_order_id=service_order_id)
orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
print request_id


# https://trust-2568.greed-dev2e.yandex.ru/paypreview.xml?request_id=133563793,
