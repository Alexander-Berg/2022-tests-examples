__author__ = 'aikawa'

import datetime

from balance import balance_api as api
from balance import balance_steps as steps

dt = datetime.datetime.now()
SERVICE_ID = 7
PAYSYS_ID  = 1003
PRODUCT_ID = 1475

client_id = steps.ClientSteps.create()
person_id =  steps.PersonSteps.create(client_id, 'ur')
service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
        ]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                              credit=0, contract_id=None, overdraft=0,endbuyer_id=None)

steps.InvoiceSteps.pay(invoice_id)

print steps.CommonSteps.get_extprops(classname='Person', object_id=person_id, attrname='invalid_bankprops')
print steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)

# query = "select state as val from T_EXPORT where type = 'OEBS' and object_id = :person_id and classname = 'Person'"
# sql_params = {'person_id': person_id}
# steps.CommonSteps.wait_for(query, sql_params)
# print db.balance().execute("select update_dt from T_EXPORT where type = 'OEBS' and object_id = :person_id and classname = 'Person'", {'person_id':person_id})[0]
#
# query = "select hold_bill_flag from apps.hz_cust_accounts rc where account_number = :account_number"
# sql_params = {'account_number':'P'+str(person_id)}
# print api.test_balance().ExecuteSQL('oebs', query, sql_params)
#
# print api.medium().InvalidatePersonBankProps(person_id)

# query = 'update (select  * from t_export where  CLASSNAME = :export_type and   type=\'OEBS\' and OBJECT_ID = :object_id ) set priority = -1'
# query_params = {'export_type': 'Person', 'object_id': person_id}
# db.balance().execute(query, query_params)
# state = 0
#
# query = 'select state as val from t_export where  CLASSNAME = :export_type and   type=\'OEBS\' and OBJECT_ID = :object_id '
# query_params = {'export_type': 'Person', 'object_id': person_id}
# steps.CommonSteps.wait_for(query, query_params)

print steps.CommonSteps.export('OEBS', 'Person', person_id)

query = "select hold_bill_flag from apps.hz_cust_accounts rc where account_number = :account_number"
sql_params = {'account_number':'P'+str(person_id)}
print api.test_balance().ExecuteSQL('oebs', query, sql_params)

