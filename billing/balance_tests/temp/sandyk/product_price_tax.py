# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import balance.balance_db as db


MAIN_PRODUCT = 501401


def next_id(table):
    select = 'select max(id) as id from ' + table
    return int(db.balance().execute(select)[0]['id']) + 1


def next_product_id(table):
    select = 'select max(id) as id from ' + table
    val = int(db.balance().execute(select)[0]['id'])
    if val >= 10000000:
        return val + 1
    else:
        return 10000000


mapper = {'t_product': 'id',
          't_tax': 'product_id',
          't_price': 'product_id'}


def generate_insert(table, product_id, product_id_new=None):
    select = 'select * from ' + table + ' where ' + mapper[table] + '=' + str(product_id)
    print select
    data = db.balance().execute(select)
    new_id = next_id(table)
    i = 0
    insert = []
    for p in data:
        fields = ''
        values = ''
        if table == 't_product':
            data[i]['id'] = next_product_id(table)
        else:
            data[i]['id'] = new_id
        i += 1
        new_id += 1
        for k in p.keys():
            fields += ',' + k
            if p[k] is None:
                p[k] = 'Null'
            if (k == 'dt') or (k == 'update_dt'):
                p[k] = 'to_date(\'' + str(p[k]) + '\',\'YYYY-MM-DD HH24:MI:SS\')'
            if product_id_new is not None and k == 'product_id':
                p[k] = product_id_new
            if (k == 'price') or (k == 'price2') or (k == 'nsp_pct'):
                p[k] = str(p[k]).replace('.', ',')
            value = p[k]
            if ( k != 'fullname') and ( k != 'name'):
                if (p[k] != 'Null') and (k != 'dt') and (k != 'update_dt'):
                    value = '\'' + str(p[k]) + '\''
            else:
                value = '\'' + ( p[k]) + '\''
            values += ',' + value
        insert.append('Insert into ' + table + ' (' + fields[1:] + ') values (' + values[1:] + ')')

    return insert, data[0]['id']


insert1, product_id_new = generate_insert('t_product', MAIN_PRODUCT)
insert2, product_id2 = generate_insert('t_tax', MAIN_PRODUCT, product_id_new)
insert3, product_id2 = generate_insert('t_price', MAIN_PRODUCT, product_id_new)

print (insert1[0])+';'
for i in insert2:
    print (i)+';'
for i in insert3:
    print (i)+';'


# product = db.balance().execute('select * from t_product where id = :product_id', {'product_id': product_id})
# fields = ''
# values = ''
# for p in product:
# product[0]['id'] = next_id ('t_product')
# for k in p.keys():
# fields += ',' + k
#         if  p[k] is None :
#             p[k] =  'Null'
#         value = p[k]
#         if ( k != 'fullname') and  ( k !='name'):
#             if p[k] != 'Null':
#                 value = '\''+str( p[k])+'\''
#         else:
#             value = '\''+( p[k])+'\''
#         values += ',' +value
#
# insert = 'Insert into T_PRODUCT (' + fields[1:] + ') values (' + values[1:] + ')'
# print insert

