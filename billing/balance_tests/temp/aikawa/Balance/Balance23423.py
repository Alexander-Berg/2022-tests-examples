__author__ = 'aikawa'
import datetime

from balance import balance_steps as steps

dt = datetime.datetime.now()

PERSON_TYPE = 'ur'
PAYSYS_ID = 1014

SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'


def test5():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'yt', {'early_docs': '1'})
    print steps.CommonSteps.get_extprops('Person', person_id, 'early_docs')

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


test5()


def test6():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'yt')

    print steps.CommonSteps.get_extprops('Person', person_id, 'early_docs')

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


test6()
