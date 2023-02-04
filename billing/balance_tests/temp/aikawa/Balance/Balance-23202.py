import datetime

from balance import balance_steps as steps

dt = datetime.datetime.now()
compl_dt_prev_day = dt - datetime.timedelta(days=1)
compl_dt_prev_prev_day = dt - datetime.timedelta(days=2)
next_day = dt + datetime.timedelta(days=1)
ORDER_DT = dt

PERSON_TYPE = 'ur'
SERVICE_ID = 7
PRODUCT_ID = 1475
PAYSYS_ID = 1003


def create_order(client_id, product_id, service_id):
    service_order_id = steps.OrderSteps.next_id(service_id=service_id)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=product_id, service_id=service_id,
                                       service_order_id=service_order_id)
    return service_order_id, order_id


def create_invoice(service_order_id, qty):
    orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': dt}]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    return invoice_id


client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
parent_service_order_id, parent_order_id = create_order(client_id, PRODUCT_ID, SERVICE_ID)
child_service_order_id, child_order_id = create_order(client_id, PRODUCT_ID, SERVICE_ID)
parent_invoice_id = create_invoice(parent_service_order_id, 100)
child_invoice_id = create_invoice(child_service_order_id, 50)
steps.InvoiceSteps.pay(parent_invoice_id)
steps.InvoiceSteps.pay(child_invoice_id)
steps.CampaignsSteps.do_campaigns(SERVICE_ID, child_service_order_id, {'Bucks': 120}, 0, compl_dt_prev_prev_day)
steps.ClientSteps.migrate_to_currency(client_id, 'COPY')
steos
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, child_service_order_id, {'Bucks': CompletionFixedQty}, 0, dt)
