import datetime
import hamcrest
import pytest

from balance import balance_steps as steps
import btestlib.utils as utils
import balance.balance_db as db

dt = datetime.datetime.now()

SERVICE_ID = 11
PRODUCT_ID = 2136


def pay_test_1():
    param_list = [{'person_category': 'yt', 'paysys_id': 11101014, 'region_id': 149}
        , {'person_category': 'yt', 'paysys_id': 11101014, 'region_id': 167}
        , {'person_category': 'yt', 'paysys_id': 11101014, 'region_id': 168}
        , {'person_category': 'yt', 'paysys_id': 11101014, 'region_id': 169}
        , {'person_category': 'yt', 'paysys_id': 11101014, 'region_id': 170}
        , {'person_category': 'yt', 'paysys_id': 11101014, 'region_id': 171}
        , {'person_category': 'yt', 'paysys_id': 11101014, 'region_id': 207}
        , {'person_category': 'yt', 'paysys_id': 11101014, 'region_id': 208}
        , {'person_category': 'yt', 'paysys_id': 11101014, 'region_id': 209}
        , {'person_category': 'yt', 'paysys_id': 11101014, 'region_id': 29386}]

    for param in param_list:
        client_id = steps.ClientSteps.create({'REGION_ID': param['region_id']})
        person_id = steps.PersonSteps.create(client_id, param['person_category'])
        service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
        steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id)
        orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 500, 'BeginDT': dt}
        ]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=param['paysys_id'],
                                                     credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id)


# pay_test_1()


def pay_test_2():
    region_id = 149
    param_list = [
        {'person_category': 'by_ytph', 'paysys_id': 1075, 'region_id': 149}
        , {'person_category': 'by_ytph', 'paysys_id': 1075, 'region_id': 167}
        , {'person_category': 'by_ytph', 'paysys_id': 1075, 'region_id': 168}
        , {'person_category': 'by_ytph', 'paysys_id': 1075, 'region_id': 169}
        , {'person_category': 'by_ytph', 'paysys_id': 1075, 'region_id': 170}
        , {'person_category': 'by_ytph', 'paysys_id': 1075, 'region_id': 171}
        , {'person_category': 'by_ytph', 'paysys_id': 1075, 'region_id': 207}
        , {'person_category': 'by_ytph', 'paysys_id': 1075, 'region_id': 208}
        , {'person_category': 'by_ytph', 'paysys_id': 1075, 'region_id': 209}
        , {'person_category': 'by_ytph', 'paysys_id': 1075, 'region_id': 29386}
    ]
    for param in param_list:
        client_id = steps.ClientSteps.create({'REGION_ID': param['region_id']})
        person_id = steps.PersonSteps.create(client_id, param['person_category'])
        service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
        steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id)
        orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 500, 'BeginDT': dt}
        ]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=param['paysys_id'],
                                                     credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id)


# pay_test_2()


def pay_test_3():
    param_list = [
        {'person_category': 'yt', 'paysys_id': 11101014, 'region_id': 29387}
        , {'person_category': 'ytph', 'paysys_id': 11101014, 'region_id': 29387}
    ]

    for param in param_list:
        client_id = steps.ClientSteps.create({'REGION_ID': param['region_id']})
        person_id = steps.PersonSteps.create(client_id, param['person_category'])
        service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
        steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id)
        orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 500, 'BeginDT': dt}
        ]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=param['paysys_id'],
                                                     credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id)


pay_test_3()
