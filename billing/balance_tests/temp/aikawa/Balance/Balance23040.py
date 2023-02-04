import datetime
import hamcrest
import pytest

from balance import balance_steps as steps
import balance.balance_db as db
import btestlib.utils as utils

dt = datetime.datetime.now()

PERSON_TYPE = 'ur'
SERVICE_ID = 7
PRODUCT_ID = 1475
CURRENCY_PRODUCT_ID = 503162
ORDER_DT = dt
PAYSYS_ID = 1003

def currency_case():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 500}, 0, dt)
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY')
    # steps.ClientSteps.create_multicurrency({'CLIENT_ID': client_id})
    service_order_id2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    steps.OrderSteps.create(client_id=client_id, product_id=CURRENCY_PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id2)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 500, 'BeginDT': dt},
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': 500, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)


    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id2, {'Money': 500}, 0, dt)
    print steps.ActsSteps.generate(client_id, force=1, date=dt)

# currency_case()




def currency_case2():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id)

    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY')

    service_order_id2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    steps.OrderSteps.create(client_id=client_id, product_id=CURRENCY_PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id2)

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 500, 'BeginDT': dt},
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': 500, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)


    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Money': 15000}, 0, datetime.datetime.now())
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id2, {'Money': 500}, 0, datetime.datetime.now())
    print steps.ActsSteps.generate(client_id, force=1, date=dt)

currency_case2()
# steps.ClientSteps.link(50537, 'aikawa-test-0')