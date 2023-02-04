import datetime
import time
from temp.MTestlib import MTestlib as mtl

service_id = 116
service_order_id = 266863360
invoice_id = 46348320

mtl.rpc.Balance.UpdateCampaigns([{"ServiceID": service_id, "ServiceOrderID": service_order_id, "dt": datetime.datetime.now(), "stop": 0, "Bucks": 0.49, "Money": 0}])
time.sleep(15)
sql = "select state as val from T_EXPORT where type = 'PROCESS_COMPLETION' and object_id = :order_id"
sql_params = {'order_id': 21804584}
mtl.wait_for(sql, sql_params, value = 1)