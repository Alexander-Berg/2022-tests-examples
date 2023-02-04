# -*- coding: utf-8 -*-

import datetime

from temp.MTestlib import MTestlib as mtl

rpc = mtl.rpc
test_rpc = mtl.test_rpc

service_id = 7

migrate_to_currency_time = datetime.datetime.now() + datetime.timedelta(seconds=15)

params = {'REGION_ID': '225'
    , 'CURRENCY': 'RUB'
    , 'SERVICE_ID': service_id
}

client_id = mtl.create_client(params)


print mtl.create_client(params)
