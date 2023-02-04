# -*- coding: utf-8 -*-

__author__ = 'sandyk'

import pytest

import btestlib.reporter as reporter
from balance.features import Features
import balance.balance_api as api

table = ['t_product', 't_product_unit', 't_product_type', 't_price']
where = 't_product.engine_id = 50 and t_product.hidden = 0 and t_price.hidden = 0'


@reporter.feature(Features.XMLRPC)
@pytest.mark.tickets('BALANCE-21969')
def test_rit_price():
    res = api.medium().QueryCatalog(table, where)
    product_prices = []
    for row in res['result']:
        result = dict(zip(res['columns'], row))
        product_prices.append((u'product id: {}, product_name: {}, product_type: {}, price: {}, dt: {:%Y-%m-%d}'.format(
            result['t_product.id'], result['t_product.fullname'], result['t_product_type.cc'], result['t_price.price'],
            result['t_price.dt'])))
    assert len(product_prices) > 20
