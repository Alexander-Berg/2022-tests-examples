# -*- coding: utf-8 -*-
__author__ = 'sandyk'

from collections import OrderedDict

import balance.balance_db as db
import btestlib.reporter as reporter

field_with_product_id_mapper = {'t_product': 'id',
                                't_tax': 'product_id',
                                't_price': 'product_id',
                                't_product_markup': 'product_id',
                                't_prod_season_coeff': 'target_id',
                                't_product_name': 'product_id',
                                }

sequence_mapper = {'t_tax': 'bo.s_tax_id.nextval',
                   't_price': 'bo.s_price_id.nextval',
                   't_product_markup': 'bo.s_product_markup_id.nextval',
                   't_prod_season_coeff': 'bo.s_prod_season_coeff_id.nextval'}

DT_FIELDS = ['dt', 'update_dt', 'activ_dt', 'finish_dt']
NUMBER_FIELDS = ['price', 'price2', 'nsp_pct']


def next_test_product_id(delta=1):
    query = 'SELECT max(id) AS id FROM t_product'
    val = int(db.meta().execute(query, single_row=True)['id'])
    if val >= 10000000:
        return val + delta
    else:
        return 10000000



def modify_value(table, field, value, product_id_new):
    if value is None:
        value = u'null'
    if field == field_with_product_id_mapper[table]:
        value = product_id_new
    if field == 'id' and table != 't_product':
        value = sequence_mapper[table]
    if field in DT_FIELDS:
        value = u"to_date('{}','YYYY-MM-DD HH24:MI:SS')".format(value)
    if field in NUMBER_FIELDS:
        value = str(value).replace('.', ',')
    value = value.decode('utf-8') if isinstance(value, str) else unicode(value)
    if (value != u'null') and field != 'id' and field not in DT_FIELDS:
        value = value.replace("'", "''")  # escape single quotes for sql
        value = u"'{}'".format(value)
    return value


def generate_inserts(table, product_id, product_id_new):
    query = 'select * from {table} where {field_with_product_id} = {product_id}'.format(
        table=table,
        field_with_product_id=field_with_product_id_mapper[table],
        product_id=str(product_id)
    )
    reporter.log(query)
    data = db.meta().execute(query)
    insert_queries = []
    for row in data:
        row_modified = {field: modify_value(table, field, value, product_id_new) for field, value in row.iteritems()}
        row_modified = OrderedDict(sorted(row_modified.iteritems()))
        insert_queries.append(u'insert into bo.{table} ({fields}) values ({vals});'.format(
            table=table,
            fields=u','.join(row_modified.keys()),
            vals=u','.join(row_modified.values())))

    return insert_queries


# Генерация запросов для создания нового продукта полной копии PRODUCT_ID_TO_COPY
if __name__ == "__main__":
    PRODUCT_ID_TO_COPY = [1475]
    for index, product in enumerate(PRODUCT_ID_TO_COPY):
        product_id_new = next_test_product_id(index + 1)

        inserts = []
        for table in ['t_product', 't_tax', 't_price', 't_product_markup', 't_prod_season_coeff', 't_product_name']:
            inserts.extend(generate_inserts(table, product, product_id_new))

        print u'\n-- Запросы для создания тестового продукта {} (копия продукта {}):'.format(product_id_new,
                                                                                             product)
        for insert in inserts:
            print insert
