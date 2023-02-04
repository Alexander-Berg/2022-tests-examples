# -*- coding: utf-8 -*-
__author__ = 'sandyk'
import xmlrpclib

TEST_XML_RPC_ADDR = "http://xmlrpc.balance.greed-tm1f.yandex.ru:8002/xmlrpc"

server = xmlrpclib.ServerProxy(TEST_XML_RPC_ADDR, use_datetime=True)

table = ['t_product', 't_product_unit', 't_product_type', 't_price']
where = 't_product.engine_id = 50 and t_product.hidden = 0 and t_price.hidden = 0'
res = server.Balance.QueryCatalog(table, where)

def test_rit_price():
    product_prices = []
    for row in res['result']:
        result = dict(zip(res['columns'], row))
        product_prices.append((u'product id: {}, product_name: {}, product_type: {}, price: {}, dt: {:%Y-%m-%d}'.format(
            result['t_product.id'], result['t_product.fullname'], result['t_product_type.cc'], result['t_price.price'], result['t_price.dt'])))

    assert len(product_prices) >20

if __name__ == "__main__":
    test_rit_price()