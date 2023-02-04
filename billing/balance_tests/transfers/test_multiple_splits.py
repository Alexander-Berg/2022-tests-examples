# -*- coding: utf-8 -*-

import decimal

import pytest

from balance.actions.transfers_qty.interface import (
    TransferMultiple,
)

from tests import object_builder as ob
from tests.balance_tests.transfers.common import (
    create_src,
    create_dst,
)

D = decimal.Decimal


@pytest.fixture
def product(request, session):
    return ob.ProductBuilder(price=request.param, currency='RUR').build(session).obj


_SRC_PARTS_1 = [
    [D('0.5'), D('0.5')],
    [D('0'), D('0.5')],
    [D('0.5'), D('0')],
]

_SRC_PARTS_2 = [
    [D('0.5'), D('0.5')],
    [D('0.01'), D('0.39')],
    [D('0.59'), D('0.01')],
]

_SRC_PARTS_3 = [
    D('5.48'), D('4.09'), D('9.08'), D('3.66'), D('0.98'), D('2.63'),
    D('4.79'), D('1.49'), D('0.52'), D('3.66'), D('2.77'), D('14.44'),
    D('0.75'), D('9.87'), D('3.49'), D('13.07'), D('13.32'), D('0.29'),
    D('0.53'), D('0.19'), D('0.07'), D('0.54'), D('0.18'), D('0.84'),
    D('0.13'), D('0.21'), D('0.18'), D('0.13'), D('0.19'), D('0.09'),
    D('0.70'), D('0.66'), D('0.31'), D('0.24'), D('0.85'), D('0.17'),
    D('0.71'), D('3.44'), D('0.24'), D('0.96'), D('0.53'), D('0.68'),
    D('0.13'), D('1.09'), D('0.24'), D('1.13'), D('0.37'), D('0.320000'),
    D('1.01'), D('0.37'), D('3.010000'), D('0.21'), D('1.08'), D('0.11'),
    D('0.53'), D('3.34'), D('1.40'), D('1.53'), D('0.34'), D('1.04'),
    D('0.22'), D('0.34'), D('0.34'), D('0.14'), D('0.26'), D('0.05'),
    D('0.20'), D('9.99'), D('0.44'), D('1.89'), D('2.12'), D('0.42'),
    D('0.86'), D('2.28'), D('0.24'), D('2.10'), D('0.11'), D('0.50'),
    D('0.74'), D('1.84'), D('0.46'), D('2.47'), D('0.99'), D('1.63'),
    D('0.54'), D('3.03'), D('2.39'), D('2.38'), D('1.80'), D('3.92'),
    D('2.67'), D('12.18'), D('0.17'), D('5.93'), D('2.52'), D('0.83'),
    D('0.96'), D('0.07'), D('1.02'), D('0.38'), D('0.51'), D('0.35'),
    D('3.41'), D('0.57'), D('3.37'), D('0.30'), D('2.10'), D('3.44'),
    D('3.26'), D('2.86'), D('4.87'), D('0.80'), D('4.53'), D('0.74'),
    D('11.11'), D('3.66'), D('0.13'), D('1.23'), D('0.67'), D('0.55'),
    D('0.88'), D('0.4'), D('0.820000'), D('80.55'), D('0.10'), D('37.28'),
    D('41.88'), D('1.38'), D('0.71')
]

TEST_PARAMS = [
    [D('12.0'), [D('2')], [D('2')]],
    [D('12.0'), [D('2')], [D('1'), D('1')]],
    [D('12.0'), [D('1'), D('1')], [D('2')]],
    [D('12.0'), [D('1'), D('1')], [D('1'), D('1')]],
    [D('12.0'), [D('2')], [D('1.5'), D('0.5')]],
    [D('12.0'), [D('1.5'), D('0.5')], [D('2')]],
    [D('12.0'), [D('1'), D('1')], [D('1.5'), D('0.5')]],
    [D('12.0'), [D('2')], [D('1')] + [D('0.01')] * 100],
    [D('12.0'), [D('1')] + [D('0.01')] * 100, [D('2')]],
    [D('12.0'), [D('1')] + [D('0.05')] * 20, [D('0.06')] * 25 + [D('0.5')]],
    [D('12.0'), _SRC_PARTS_1, [D('2')]],
    [D('12.0'), _SRC_PARTS_1, [D('0.75'), D('0.5'), D('0.75')]],
    [D('12.0'), _SRC_PARTS_1, [D('0.5')] * 4],
    [D('12.0'), _SRC_PARTS_1, [D('0.4')] * 5],
    [D('12.0'), _SRC_PARTS_2, [D('2')]],
    [D('12.0'), _SRC_PARTS_2, [D('0.75'), D('0.5'), D('0.75')]],
    [D('12.0'), _SRC_PARTS_2, [D('0.5')] * 4],
    [D('12.0'), _SRC_PARTS_2, [D('0.4')] * 5],
    [D('1.0'), _SRC_PARTS_3, [sum(_SRC_PARTS_3)]]
]


@pytest.mark.parametrize(
    ['product', 'src_parts', 'dst_parts'],
    TEST_PARAMS,
    indirect=['product'],
    ids=[str(id_) for id_, _ in enumerate(TEST_PARAMS)]
)
def test(session, client, product, src_parts, dst_parts):
    src_result_parts = [D(0) for _ in src_parts]
    src_orders, src_list = create_src(client, product, src_parts, src_result_parts)
    dst_orders, dst_list = create_dst(client, product, dst_parts)

    TransferMultiple(session, src_list, dst_list).do()

    for qty, order in zip(src_result_parts, src_orders):
        assert order.consume_qty == qty

    for qty, order in zip(dst_parts, dst_orders):
        assert order.consume_qty == qty
        assert sum(cons.current_qty for cons in order.consumes) == qty

    get_ratio = lambda cons: (cons.current_sum / cons.current_qty)

    price, = product.prices
    ratio = price.price.as_decimal()
    assert all(get_ratio(order.consumes[0]) == ratio for order in dst_orders) is True
