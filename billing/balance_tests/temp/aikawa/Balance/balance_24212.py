import datetime

from balance import balance_steps as steps

dt = datetime.datetime.now()

SERVICE_ID = 7
PRODUCT_ID = 1475
PAYSYS_ID = 1003

client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
person_id = steps.PersonSteps.create(client_id, 'ur')

service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                   service_order_id=service_order_id, params={'AgencyID': None})

service_order_id2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id2 = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                    service_order_id=service_order_id2, params={'AgencyID': None})

orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 11.8, 'BeginDT': dt},
    # {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': 10, 'BeginDT': dt},
]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                       additional_params={'InvoiceDesireDT': dt})

invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                             credit=0, contract_id=None, overdraft=0)

steps.InvoiceSteps.pay(invoice_id)

steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 9.953679}, 0, dt)
steps.InvoiceSteps.make_rollback_ai(invoice_id=invoice_id)
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 9.9656}, 0, dt)
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id2, {'Bucks': 11}, 0, dt)

# consume_id = db.get_consumes_by_order(order_id2)[0]['id']
# db.balance().execute('''update t_consume set completion_sum = 300.01 where id = :consume_id''', {'consume_id': consume_id})
