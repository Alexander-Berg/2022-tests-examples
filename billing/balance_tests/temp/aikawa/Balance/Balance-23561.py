import datetime

from balance import balance_steps as steps

dt = datetime.datetime.now()
previous_day = dt - datetime.timedelta(days=1)
PERSON_TYPE = 'ur'
SERVICE_ID = 7
PRODUCT_ID = 1475
PAYSYS_ID = 1003


def create_order(client_id, product_id, service_id):
    service_order_id = steps.OrderSteps.next_id(service_id=service_id)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=product_id, service_id=service_id,
                                       service_order_id=service_order_id)
    return service_order_id, order_id


def create_invoice(service_order_id, person_id):
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    return invoice_id


client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
child_service_order_id, child_order = create_order(client_id, PRODUCT_ID, SERVICE_ID)
_, parent_order = create_order(client_id, PRODUCT_ID, SERVICE_ID)
invoice_id = create_invoice(child_service_order_id, person_id)
steps.CampaignsSteps.do_campaigns(SERVICE_ID, child_service_order_id, {'Bucks': 50}, 0, previous_day)
steps.CampaignsSteps.do_campaigns(SERVICE_ID, child_service_order_id, {'Bucks': 70}, 0, dt)
steps.OrderSteps.merge(parent_order, [child_order])
steps.OrderSteps.make_optimized(parent_order)
steps.OrderSteps.ua_enqueue([client_id])
query = "select input from t_export where type = 'UA_TRANSFER' and classname = 'Client' and object_id =  5946133"
input = steps.CommonSteps.get_pickled_value(query, 'input')
print input
steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
