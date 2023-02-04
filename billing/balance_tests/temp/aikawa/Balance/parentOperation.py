import datetime

import balance.balance_db as db
from balance import balance_steps as steps

dt = datetime.datetime.now()

SERVICE_ID = 7
PRODUCT_ID = 1475
PAYSYS_ID = 1003
PERSON_TYPE = 'ur'
QTY = 100

client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt}
]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                             credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
steps.InvoiceSteps.pay(invoice_id)

steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': QTY}, 0, dt)

steps.ActsSteps.generate(client_id, force=0, date=dt)

steps.CommonSteps.increase_priority('Client', object_id=client_id, type='MONTH_PROC')

query = "select state as val from T_EXPORT where type = 'MONTH_PROC' and object_id = :client_id"
sql_params = {'client_id': client_id}
steps.CommonSteps.wait_for(query, sql_params)

query = "select operation_id from t_act where client_id = :client_id"
sql_params = {'client_id': client_id}
operation_id = db.balance().execute(query, sql_params)[0]['operation_id']
print operation_id

query = 'select result from t_operation where id = {0}'.format(operation_id)
print query
result = steps.CommonSteps.get_pickled_value(query, 'result')
print result
