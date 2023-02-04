# -*- coding: utf-8 -*-
import datetime

from balance import balance_steps as steps
from temp.MTestlib import MTestlib as mtl

rpc = mtl.rpc
test_rpc = mtl.test_rpc

sql_query = "SELECT input FROM t_export WHERE type = 'UA_TRANSFER' AND classname = 'Client' AND object_id =  56205204"
input = steps.CommonSteps.get_pickled_value(sql_query, 'input')

print input

input['for_dt'] = datetime.datetime(2017, 11, 28, 23, 59, 59)
print mtl.set_input_value(input)
from balance import balance_db as db

# print (pickle.dumps(input))
a = db.balance().execute(
    "update t_export set input = :input where state = '0' and type = 'UA_TRANSFER' and classname = 'Client' and object_id = 56205204", {'input': mtl.set_input_value(input)})
# test_rpc.ExecuteSQL("commit")
# print a
print input
steps.CommonSteps.export('UA_TRANSFER', 'Client', 56205204, input_=input)


import balance.balance_api as api

print api.test_balance().GetNotification(1, 363040206)
