# -*- coding: utf-8 -*-
import datetime as dt

import mock
import pytest

from balance import mapper
from balance import constants
from balance.actions.dcs.compare.auto_analysis import obb2

from tests import object_builder as ob
from tests.balance_tests.dcs.dcs_common import create_product, create_order


@pytest.fixture
def yt_client_mock():
    with mock.patch('yt.wrapper.YtClient') as m:
        yield m.return_value


@pytest.mark.parametrize(
    'product_unit_id, product_id, service_id, value',
    [
        pytest.param(constants.SHOWS_1000_UNIT_ID, None, constants.ServiceId.MEDIA_BANNERS, (100, 0, 0), id='shows'),
        pytest.param(constants.AUCTION_UNIT_ID, None, constants.ServiceId.MARKET, (0, 0, 100), id='bucks'),
        pytest.param(None, constants.DIRECT_PRODUCT_RUB_ID, constants.ServiceId.DIRECT, (0, 100, 0), id='money'),
        pytest.param(None, constants.DIRECT_PRODUCT_ID, constants.ServiceId.DIRECT, (0, 100, 100), id='direct_bucks'),
    ]
)
def test_match(session, yt_client_mock, product_unit_id, product_id, service_id, value):
    client = ob.ClientBuilder.construct(session)

    if product_unit_id is not None:
        product = create_product(session, product_unit_id)
    else:
        product = ob.Getter(mapper.Product, product_id)

    order = create_order(session, client, service_id=service_id, product=product)

    shows, money, bucks = value
    order.calculate_consumption(
        dt.datetime.now(),
        {'Shows': shows, 'Money': money, 'Bucks': bucks}
    )

    yt_client_mock.select_rows.return_value = [
        {
            'EngineID': order.service_id,
            'BillingExportID': order.service_order_id,
            'Days': 0,
            'Shows': shows,
            'Cost': 0,
            'CostCur': money * 10 ** 6,
            'CostFinal': bucks * 10 ** 6,
        }
    ]

    result = obb2.process(session, [{'service_id': order.service_id,
                                     'service_order_id': order.service_order_id}],
                          'hahn', 1, 1)
    expected = [{'service_id': order.service_id,
                 'service_order_id': order.service_order_id}]
    assert result == expected


def test_partial_match(session, yt_client_mock):
    client = ob.ClientBuilder.construct(session)
    service_id = constants.DIRECT_SERVICE_ID
    product = ob.Getter(mapper.Product, constants.DIRECT_PRODUCT_RUB_ID)

    orders = [
        create_order(session, client, service_id=service_id, product=product),
        create_order(session, client, service_id=service_id, product=product),
    ]

    rows = []
    yt_client_mock.select_rows.return_value = []

    for index, order in enumerate(orders, start=1):
        rows.append({
            'service_id': order.service_id,
            'service_order_id': order.service_order_id,
        })

        order.calculate_consumption(
            dt.datetime.now(),
            {order.shipment_type: 100},
        )

        yt_client_mock.select_rows.return_value.append(
            {
                'EngineID': order.service_id,
                'BillingExportID': order.service_order_id,
                'CostCur': index * 100 * 10 ** 6,
            },
        )

    result = obb2.process(session, rows, 'hahn', 1, 1)
    expected = [{'service_id': orders[0].service_id,
                 'service_order_id': orders[0].service_order_id}]
    assert result == expected


def test_passes(session, yt_client_mock):
    client = ob.ClientBuilder.construct(session)
    service_id = constants.DIRECT_SERVICE_ID
    product = ob.Getter(mapper.Product, constants.DIRECT_PRODUCT_RUB_ID)
    order = create_order(session, client, service_id=service_id, product=product)

    order.calculate_consumption(
        dt.datetime.now(),
        {order.shipment_type: 300},
    )

    side_effect = []
    for pass_ in xrange(1, 3 + 1):
        side_effect.append([{
            'EngineID': order.service_id,
            'BillingExportID': order.service_order_id,
            'CostCur': pass_ * 100 * 10 ** 6,
        }])
    yt_client_mock.select_rows.side_effect = side_effect

    with mock.patch('time.sleep'):
        result = obb2.process(session, [{'service_id': order.service_id,
                                         'service_order_id': order.service_order_id}],
                              'hahn', 3, 1)
    expected = [{'service_id': order.service_id,
                 'service_order_id': order.service_order_id}]
    assert result == expected
