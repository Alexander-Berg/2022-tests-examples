# coding: utf-8
import datetime

from balance import balance_steps as steps

dt = datetime.datetime.now() - datetime.timedelta(days=1)

SERVICE_ID = 7
PRODUCT_ID = 1475
PAYSYS_ID = 1001

client_id = steps.ClientSteps.create()

person_id = steps.PersonSteps.create(client_id, 'ph')

order_ids_list = []

# дочерний
service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID,
                                   service_id=SERVICE_ID)
order_ids_list.append(order_id)

# родительский
service_order_id_parent = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id_parent = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_parent, product_id=PRODUCT_ID,
                                   service_id=SERVICE_ID)
order_ids_list.append(order_id_parent)
orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt},
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id_parent, 'Qty': 100, 'BeginDT': dt}
]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                       additional_params=dict(InvoiceDesireDT=dt))

invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                             credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)


# steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)


steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 50}, 0, dt)

act_id = steps.ActsSteps.generate(client_id, force=1, date=dt)[0]
steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 30}, 0, dt)
steps.OrderSteps.merge(order_id_parent, [order_id])
steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
# order_ids_list.append(order_id)
# #
# service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
# order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID,
#                                    service_id=SERVICE_ID)
# orders_list = [
#     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 150, 'BeginDT': dt}
# ]
# request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
#                                        additional_params=dict(InvoiceDesireDT=dt))
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
# steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Money': 10}, 0, dt)
# order_ids_list.append(order_id)
# #
#
#
# service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
# parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID,
#                                           service_id=SERVICE_ID)
# steps.OrderSteps.make_optimized(parent_order_id)
#
# steps.OrderSteps.merge(parent_order_id, order_ids_list)
# steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
