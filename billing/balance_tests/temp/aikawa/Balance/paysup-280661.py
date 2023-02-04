import datetime

from balance import balance_steps as steps

dt = datetime.datetime.now()
ORDER_DT = dt


def currency_case():
    PERSON_TYPE = 'yt_kzu'
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    PAYSYS_ID = 1003
    client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 500, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)


currency_case()
