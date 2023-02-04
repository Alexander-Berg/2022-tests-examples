import datetime

from balance import balance_steps as steps
from btestlib import utils as ut
from balance import balance_db as db

def test_1():
    PERSON_TYPE = 'ur'
    PAYSYS_ID_LIST = [11101003,  11101033]
    SERVICE_ID = 11
    PRODUCT_ID = 2136
    MSR = 'Bucks'

    dt = datetime.datetime.now()

    for PAYSYS_ID in PAYSYS_ID_LIST:
        client_id = steps.ClientSteps.create()
        person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

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

def test_2():
    PERSON_TYPE = 'ph'
    PAYSYS_ID_LIST = [11101002, 11101052, 11101000]
    SERVICE_ID = 11
    PRODUCT_ID = 2136
    MSR = 'Bucks'

    dt = datetime.datetime.now()

    for PAYSYS_ID in PAYSYS_ID_LIST:
        client_id = steps.ClientSteps.create()
        person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

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