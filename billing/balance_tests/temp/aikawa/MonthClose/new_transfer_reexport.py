from balance import balance_steps as steps
from balance import balance_db as db
from balance import balance_api as api
import datetime

# result =  api.test_balance().ExecuteSQL('cmp', "select order_id from BUA_CMP_DATA bcd where CMP_ID = 2479 and JIRA_ID is null")
# orders_list = [diff['order_id'] for diff in result]
# print orders_list
#
# for order_id in orders_list:
#     print order_id
#     order = db.get_order_by_id(order_id)[0]
#     service_order_id = order['service_order_id']
#     service_id = order['service_id']
#     api.test_balance().Campaigns({'service_id': service_id, 'service_order_id': service_order_id, 'use_current_shipment': True})




order = db.get_order_by_id(15286287)[0]
service_order_id = order['service_order_id']
service_id = order['service_id']
api.test_balance().Campaigns({'service_id': service_id, 'service_order_id': service_order_id, 'use_current_shipment': True})