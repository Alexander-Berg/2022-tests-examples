__author__ = 'aikawa'
import datetime

import balance.balance_db as db
from balance import balance_steps as steps

dt = datetime.datetime.now()

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'


def test1():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    # db.balance().execute('update (select * from t_person where id = :person_id) set postaddress = Null', {'person_id': person_id})
    # db.balance().execute('update (select * from t_person where id = :person_id) set address = Null', {'person_id': person_id})
    # db.balance().execute('update (select * from t_person where id = :person_id) set legaladdress = Null', {'person_id': person_id})
    db.balance().execute('update (select * from t_person where id = :person_id) set kladr_code = 2323',
                         {'person_id': person_id})

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)


# test1()


def test2():
    client_id = 9774476
    person_id = 5142858

    db.balance().execute('update (select * from t_person where id = :person_id) set postaddress = Null',
                         {'person_id': person_id})

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)


# test2()
# test2()

def test3():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    db.balance().execute('update (select * from t_person where id = :person_id) set postaddress = Null',
                         {'person_id': person_id})
    db.balance().execute('update (select * from t_person where id = :person_id) set kladr_code = 7700000000000',
              {'person_id': person_id})

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)


# test3()
# test3()



# def test4():
#     client_id = 11376463
#     person_id = 5148420
#
#     db.balance().execute('update (select * from t_person where id = :person_id) set postaddress = Null', {'person_id': person_id})
#
#     service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
#     order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id)
#     orders_list = [
#                 {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
#             ]
#     request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
#
#     invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                                                   credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
#     steps.InvoiceSteps.pay(invoice_id)
#
#     steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)
#
# test4()


def test5():
    client_id = 11402089
    person_id = 5163636

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id)
    orders_list = [
                {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
            ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))

    invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                                  credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)

test5()