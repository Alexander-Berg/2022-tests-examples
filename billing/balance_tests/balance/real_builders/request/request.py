# -*- coding: utf-8 -*-

from balance import balance_steps as steps
import datetime

# Создание недовыставленного счета без оператора и с клиентом без названия
def test_empty_request():
    client_id = steps.ClientSteps.create({'NAME': ''})
    orders_list = []
    service_order_id = steps.OrderSteps.next_id(7)
    steps.OrderSteps.create(client_id, service_order_id, service_id=7, product_id=1475)
    orders_list.append(
        {'ServiceID': 7, 'ServiceOrderID': service_order_id, 'Qty': 55, 'BeginDT': datetime.datetime.now()})
    request_id = steps.RequestSteps.create(client_id, orders_list, passport_uid=None)
    return client_id, request_id
