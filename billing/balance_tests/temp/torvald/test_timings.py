# coding: utf-8
import datetime
import time

from balance import balance_steps as steps


def run():
    service_id = 7
    qty = 100
    paysys_id = 1003
    base_dt = datetime.datetime.now()

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    service_order_ids = []
    order_ids = []
    for i in range(3):
        service_order_id = steps.OrderSteps.next_id(service_id)
        service_order_ids.append(service_order_id)
        order_ids.append(steps.OrderSteps.create(client_id, service_order_id))

    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_ids[0], 'Qty': qty, 'BeginDT': base_dt}
    ]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0, contract_id=None,
                                                 overdraft=0, endbuyer_id=None)
    start = time.time()
    for i in range(10):
        steps.InvoiceSteps.pay(invoice_id)
    end = time.time()
    print start - end;


print profile.run('run()')
