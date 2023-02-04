# -*- coding: utf-8 -*-

import datetime

import pytest
import hamcrest

from balance.constants import (
    ServiceId,
    DIRECT_PRODUCT_ID,
    DIRECT_PRODUCT_RUB_ID,
    UAChildType,
    OrderLogTariffState,
)

from tests import object_builder as ob


def test_ok(xmlrpcserver, session):
    order_direct = ob.OrderBuilder.construct(
        session,
        service_id=ServiceId.DIRECT,
        product_id=DIRECT_PRODUCT_ID
    )
    order_media = ob.OrderBuilder.construct(
        session,
        service_id=ServiceId.MEDIA_SELLING,
        product_id=DIRECT_PRODUCT_RUB_ID
    )

    res = xmlrpcserver.UpdateCampaigns([
        {
            'ServiceID': order_direct.service_id,
            'ServiceOrderID': order_direct.service_order_id,
            'dt': datetime.datetime.now(),
            'stop': 0,
            'Bucks': 666,
        },
        {
            'ServiceID': order_media.service_id,
            'ServiceOrderID': order_media.service_order_id,
            'dt': datetime.datetime.now(),
            'stop': 0,
            'Money': 666,
        },
    ])
    session.expire_all()

    assert res == {
        str(ServiceId.DIRECT): {str(order_direct.service_order_id): 1},
        str(ServiceId.MEDIA_SELLING): {str(order_media.service_order_id): 1},
    }

    hamcrest.assert_that(
        order_direct.shipment,
        hamcrest.has_properties(
            consumption=666,
            days=-1,
            bucks=666,
            shows=-1,
            clicks=-1,
            units=-1,
            money=0,
        )
    )
    hamcrest.assert_that(
        order_media.shipment,
        hamcrest.has_properties(
            consumption=666,
            days=-1,
            bucks=-1,
            shows=-1,
            clicks=-1,
            units=-1,
            money=666,
        )
    )


def test_deny_shipment(xmlrpcserver, session):
    order = ob.OrderBuilder.construct(
        session,
        service_id=ServiceId.DIRECT,
        product_id=DIRECT_PRODUCT_ID
    )
    order.shipment.deny_shipment = 1
    session.flush()

    res = xmlrpcserver.UpdateCampaigns([
        {
            'ServiceID': order.service_id,
            'ServiceOrderID': order.service_order_id,
            'dt': datetime.datetime.now(),
            'stop': 0,
            'Bucks': 666,
        },
    ])
    session.expire_all()

    assert res == {
        str(ServiceId.DIRECT): {str(order.service_order_id): 0},
    }

    hamcrest.assert_that(
        order.shipment,
        hamcrest.has_properties(
            consumption=0,
            days=None,
            bucks=None,
            shows=None,
            clicks=None,
            units=None,
            money=None,
        )
    )


@pytest.mark.parametrize(
    'is_log_tariff, child_ua_type, is_ok',
    [
        pytest.param(OrderLogTariffState.OFF, None, False, id='off'),
        pytest.param(OrderLogTariffState.INIT, None, True, id='main_init'),
        pytest.param(OrderLogTariffState.MIGRATED, None, True, id='main_migrated'),
        pytest.param(OrderLogTariffState.OFF, UAChildType.TRANSFERS, False, id='child_transfers'),
        pytest.param(OrderLogTariffState.OFF, UAChildType.OPTIMIZED, False, id='child_optimized'),
        pytest.param(OrderLogTariffState.OFF, UAChildType.LOG_TARIFF, True, id='child_log_tariff'),
    ]
)
def test_is_log_tariff(xmlrpcserver, session, is_log_tariff, child_ua_type, is_ok):
    client = ob.ClientBuilder.construct(session)
    client.set_currency(ServiceId.DIRECT, 'RUB', datetime.datetime(2000, 1, 1), None)
    order = ob.OrderBuilder.construct(
        session,
        client=client,
        service_id=ServiceId.DIRECT,
        product_id=DIRECT_PRODUCT_RUB_ID,
    )
    order.shipment.deny_shipment = 666
    if is_log_tariff is not None:
        order._is_log_tariff = is_log_tariff
    if child_ua_type is not None:
        order.child_ua_type = child_ua_type
    session.flush()

    res = xmlrpcserver.UpdateCampaigns([
        {
            'ServiceID': order.service_id,
            'ServiceOrderID': order.service_order_id,
            'dt': datetime.datetime.now(),
            'stop': 0,
            'Money': 666,
        },
    ])
    session.expire_all()

    assert res == {
        str(ServiceId.DIRECT): {str(order.service_order_id): 1 if is_ok else 0},
    }

    hamcrest.assert_that(
        order.shipment,
        hamcrest.has_properties(
            consumption=0,
            days=None,
            bucks=None,
            shows=None,
            clicks=None,
            units=None,
            money=None,
        )
    )


def test_future(xmlrpcserver, session):
    order1 = ob.OrderBuilder.construct(
        session,
        service_id=ServiceId.DIRECT,
        product_id=DIRECT_PRODUCT_ID
    )
    order2 = ob.OrderBuilder.construct(
        session,
        service_id=ServiceId.DIRECT,
        product_id=DIRECT_PRODUCT_ID
    )

    res = xmlrpcserver.UpdateCampaigns([
        {
            'ServiceID': order1.service_id,
            'ServiceOrderID': order1.service_order_id,
            'dt': datetime.datetime.now() + datetime.timedelta(minutes=1),
            'stop': 0,
            'Bucks': 666,
        },
        {
            'ServiceID': order2.service_id,
            'ServiceOrderID': order2.service_order_id,
            'dt': datetime.datetime.now() + datetime.timedelta(minutes=1),
            'stop': 0,
            'Bucks': 666,
        },
    ])

    assert res == {
        str(ServiceId.DIRECT): {
            str(order1.service_order_id): 0,
            str(order2.service_order_id): 0
        },
    }
