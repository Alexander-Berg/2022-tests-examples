# -*- coding: utf-8 -*-

import datetime

from balance import balance_steps as steps

dt = datetime.datetime.now()


# плательщик - юрик, платит способом оплаты "Рублями со счета в банке", у клиента по этому плательщику нет менеджера и он не является субклиентом.
def test_1():
    person_type = 'ur'
    paysys_id = 1003
    service_id = 7
    product_id = 1475

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, person_type)

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)
    steps.OrderSteps.create(client_id=client_id, product_id=product_id, service_id=service_id,
                            service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=paysys_id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)


# test_1()

def test_2():
    person_type = 'ur'
    paysys_id = 1033
    service_id = 7
    product_id = 1475

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, person_type)

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)
    steps.OrderSteps.create(client_id=client_id, product_id=product_id, service_id=service_id,
                            service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=paysys_id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)


test_2()