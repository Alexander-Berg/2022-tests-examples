import datetime

import balance.balance_db as db
from balance import balance_steps as steps

dt = datetime.datetime.now() - datetime.timedelta(days=1)

SERVICE_ID = 7
PRODUCT_ID = 1475
PAYSYS_ID = 1003

client_id = steps.ClientSteps.create()

another_client_id = steps.ClientSteps.create()

person_id = steps.PersonSteps.create(client_id, 'ur')

order_ids_list = []

service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
# orders_list = [
#             {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 175.1125, 'BeginDT': dt}
#         ]
# request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
# invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                                               credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
# steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 100}, 0, dt)



# query = ("update t_shipment set consumption = 100 where service_id =:service_id and service_order_id = :service_order_id")
# sql_params = {'service_id': SERVICE_ID, 'service_order_id':service_order_id}
# db.balance().execute(query, sql_params)

order_ids_list.append(order_id)
#
# service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
# order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
# orders_list = [
#             {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 227.7404, 'BeginDT': dt}
#         ]
# request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
# invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                                               credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
# steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 100}, 0, dt)
# order_ids_list.append(order_id)
#


service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
parent_order_id = steps.OrderSteps.create(client_id=another_client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
steps.OrderSteps.make_optimized(parent_order_id)

steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Money': 50}, 0, dt)

query = ("update t_shipment set shipment_type = 'Money' where service_id =:service_id and service_order_id = :service_order_id")
sql_params = {'service_id': SERVICE_ID, 'service_order_id':service_order_id}
print db.balance().execute(query, sql_params)

query = ("select shipment_type from t_shipment where service_id =:service_id and service_order_id = :service_order_id")
sql_params = {'service_id': SERVICE_ID, 'service_order_id':service_order_id}
print db.balance().execute(query, sql_params)

query = ("update t_order set group_order_id = :parent_order_id where id =:order_id")
sql_params = {'parent_order_id': parent_order_id, 'order_id':order_ids_list[0]}
print db.balance().execute(query, sql_params)


# steps.OrderSteps.merge(parent_order_id, order_ids_list)

steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
#
steps.CommonSteps.increase_priority('Client', object_id=client_id, type='UA_TRANSFER')
#
query = "select state as val from T_EXPORT where type = 'UA_TRANSFER' and object_id = :client_id"
sql_params = {'client_id': client_id}
steps.CommonSteps.wait_for(query, sql_params, service_order_id = service_order_id)
