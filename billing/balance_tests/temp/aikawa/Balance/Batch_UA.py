from balance import balance_steps as steps

# dt = datetime.datetime.now() - datetime.timedelta(days=1)
#
# SERVICE_ID = 7
# PRODUCT_ID = 1475
# PAYSYS_ID = 1003
#
# client_id = steps.ClientSteps.create()
#
# person_id = steps.PersonSteps.create(client_id, 'ur')
#
# order_ids_list = []
#
# service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
# order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
# orders_list = [
#     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}
# ]
# request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
# steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 50}, 0, dt)
# order_ids_list.append(order_id)
#
# service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
# parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
#
# steps.OrderSteps.merge(parent_order_id, order_ids_list)
# steps.CommonSteps.export('UA_TRANSFER', 'Client', 19346970)
print steps.CommonSteps.export('OEBS', 'ContractCollateral', 451582)


# coding: utf-8


# def return_clients_list():
#     data = open('/Users/aikawa/Work/SQL/04-10-16/clients.tsv', 'r')
#     clients_list = []
#     for line in data:
#         clients_list.append(line)
#     return clients_list
#
# clients_list = return_clients_list()
#
# for client in clients_list:
#     steps.CommonSteps.export('UA_TRANSFER', 'Client', client)
#     print client
