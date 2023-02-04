import datetime

from balance import balance_steps as steps

dt = datetime.datetime.now() - datetime.timedelta(days=1)

SERVICE_ID = 7
PRODUCT_ID = 503162
PAYSYS_ID = 1003

client_id = steps.ClientSteps.create({'REGION_ID': '225'
                                             , 'CURRENCY': 'RUB'
                                             , 'SERVICE_ID': SERVICE_ID})

person_id = steps.PersonSteps.create(client_id, 'ur')

order_ids_list = []

service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 175.1125, 'BeginDT': dt}
        ]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                              credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Money': 175.112}, 0, dt)
order_ids_list.append(order_id)
#
service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 227.7404, 'BeginDT': dt}
        ]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                              credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Money': 227.74}, 0, dt)
order_ids_list.append(order_id)
#


service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
steps.OrderSteps.make_optimized(parent_order_id)

steps.OrderSteps.merge(parent_order_id, order_ids_list)
steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
# steps.OrderSteps.ua_enqueue([client_id])

# steps.CommonSteps.increase_priority('Client', object_id=client_id, type='UA_TRANSFER')
#
# query = "select state as val from T_EXPORT where type = 'UA_TRANSFER' and object_id = :client_id"
# sql_params = {'client_id': client_id}
# steps.CommonSteps.wait_for(query, sql_params)