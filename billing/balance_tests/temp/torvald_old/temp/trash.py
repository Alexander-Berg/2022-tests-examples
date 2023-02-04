# -*- coding: utf-8 -*-

from temp.MTestlib import MTestlib as mtl

rpc = mtl.rpc
test_rpc = mtl.test_rpc


def get_data():
    a = []
    global skip
    skip = []
    for num, item in enumerate(open('data.dat')):
        (sid, soid) = item.split('-')
        soid = soid.replace('\n', '')
        sql = 'select id, client_id from t_order where service_id = :service_id and service_order_id = :service_order_id'
        sql_params = {'service_id': int(sid), 'service_order_id': int(soid)}
        data = test_rpc.ExecuteSQL(sql, sql_params)
        order_id = None
        client_id = None
        if data and data[0].has_key('id'):
            order_id = test_rpc.ExecuteSQL(sql, sql_params)[0]['id']
        if data and data[0].has_key('client_id'):
            client_id = test_rpc.ExecuteSQL(sql, sql_params)[0]['client_id']
        if client_id is None or order_id is None:
            skip.append((sid, soid))
            yield 1
        else:
            print (num, item, sid, soid, order_id, client_id)
            yield client_id


if __name__ == '__main__':
    orders = []
    clients = []
    for item in get_data():
        clients.append(item)
    print(set(clients))
    print('Skippend list: {0}'.format(skip))
