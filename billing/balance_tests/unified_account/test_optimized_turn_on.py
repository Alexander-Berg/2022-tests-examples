# -*- coding: utf-8 -*-
import datetime
import pytest
import sqlalchemy as sa

from balance.mapper import ClientServiceData, Order
from balance.constants import *
from tests import object_builder as ob
from tests.tutils import get_exception_code

NOW = datetime.datetime.now()
HOUR_AFTER = NOW + datetime.timedelta(hours=1)
HOUR_BEFORE = NOW - datetime.timedelta(hours=1)
CODE_SUCCESS = 0


@pytest.fixture
def order(session, **attrs):
    return ob.OrderBuilder(**attrs).build(session).obj


@pytest.fixture
def client(session, **attrs):
    return ob.ClientBuilder(**attrs).build(session).obj


@pytest.fixture
def product(session, **attrs):
    return ob.ProductBuilder(**attrs).build(session).obj


@pytest.fixture
def service_order_id():
    return ob.get_big_number()


def add_client_service_data_to_client(client, service_id, migrate_to_currency, convert_type):
    client.service_data[service_id] = ClientServiceData(service_id)
    client.service_data[service_id].iso_currency = 'RUB'
    client.service_data[service_id].migrate_to_currency = migrate_to_currency
    client.service_data[service_id].convert_type = convert_type


def test_turn_on_optimized_ua_create_order(session, xmlrpcserver, client, product, service_order_id):
    """ можно включить неотключаемый ЕС при создании заказа"""
    add_client_service_data_to_client(client=client, service_id=ServiceId.DIRECT, migrate_to_currency=HOUR_BEFORE,
                                      convert_type=None)
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id,
                                                 [{'ClientID': client.id,
                                                   'ProductID': product.id,
                                                   'ServiceOrderID': service_order_id,
                                                   'ServiceID': ServiceId.DIRECT,
                                                   'is_ua_optimize': 1}])
    assert res == [[CODE_SUCCESS, 'Success']]
    orders = session.query(Order).filter(sa.and_(Order.service_order_id == service_order_id,
                                                 Order.service_id == ServiceId.DIRECT)).all()
    assert orders[0].is_ua_optimize == 1


@pytest.mark.parametrize('is_ua_optimize_before', [None, 0])
def test_turn_on_optimized_ua_update_order(session, xmlrpcserver, order, is_ua_optimize_before):
    """ можно включить неотключаемый ЕС при редактировании заказа"""
    order.is_ua_optimize = is_ua_optimize_before
    add_client_service_data_to_client(client=order.client, service_id=ServiceId.DIRECT,
                                      migrate_to_currency=HOUR_BEFORE,
                                      convert_type=None)
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id,
                                                 [{'ClientID': order.client.id,
                                                   'ProductID': order.product.id,
                                                   'ServiceOrderID': order.service_order_id,
                                                   'ServiceID': ServiceId.DIRECT,
                                                   'is_ua_optimize': 1}])
    assert res == [[CODE_SUCCESS, 'Success']]
    orders = session.query(Order).filter(sa.and_(Order.service_order_id == order.service_order_id,
                                                 Order.service_id == ServiceId.DIRECT)).all()
    assert orders[0].is_ua_optimize == 1


def test_turn_off_optimized_ua(session, xmlrpcserver, order):
    """ нельзя выключить неотключаемый ЕС"""
    order.is_ua_optimize = 1
    res = xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id,
                                                 [{'ClientID': order.client.id,
                                                   'ProductID': order.product.id,
                                                   'ServiceOrderID': order.service_order_id,
                                                   'ServiceID': ServiceId.DIRECT,
                                                   'is_ua_optimize': 0}])
    assert res[0][0] == -1
    assert get_exception_code(res[0][1]) == 'ORDER_UA_OPTIMIZE_IMMUTABLE'
    assert order.is_ua_optimize == 1


def test_check_turn_on_optimize_currency_client(order):
    """ можно включить неотключаемый ЕС изначально валютному клиенту"""
    order.is_ua_optimize = 0
    add_client_service_data_to_client(client=order.client, service_id=order.service.id, migrate_to_currency=HOUR_BEFORE,
                                      convert_type=None)
    assert order.check_turn_on_optimize(NOW) is True


@pytest.mark.parametrize('convert_type_value', [CONVERT_TYPE_COPY, CONVERT_TYPE_MODIFY])
def test_check_turn_on_optimize_success_state(order, convert_type_value):
    """ можно включить неотключаемый ЕС успешно смигрированному клиенту"""
    order.is_ua_optimize = 0

    order.client.enqueue('MIGRATE_TO_CURRENCY')
    order.client.exports['MIGRATE_TO_CURRENCY'].export_dt = NOW
    order.client.exports['MIGRATE_TO_CURRENCY'].state = ExportState.exported

    add_client_service_data_to_client(
        client=order.client,
        service_id=order.service.id,
        migrate_to_currency=HOUR_BEFORE,
        convert_type=convert_type_value,
    )
    assert order.check_turn_on_optimize(NOW) is True


@pytest.mark.parametrize('state_value', [ExportState.enqueued, ExportState.failed])
@pytest.mark.parametrize('convert_type_value', [CONVERT_TYPE_COPY, CONVERT_TYPE_MODIFY])
def test_check_turn_on_optimize_non_success_state(order, convert_type_value, state_value):
    """ нельзя включить неотключаемый ЕС неуспешно смигрированному клиенту или находящемуся в процессе миграции"""
    order.is_ua_optimize = 0

    order.client.enqueue('MIGRATE_TO_CURRENCY')
    order.client.exports['MIGRATE_TO_CURRENCY'].export_dt = NOW
    order.client.exports['MIGRATE_TO_CURRENCY'].state = state_value

    add_client_service_data_to_client(
        client=order.client,
        service_id=order.service.id,
        migrate_to_currency=HOUR_BEFORE,
        convert_type=convert_type_value,
    )
    assert order.check_turn_on_optimize(NOW) is False


@pytest.mark.parametrize('convert_type_value', [CONVERT_TYPE_COPY, CONVERT_TYPE_MODIFY])
def test_check_turn_on_optimize_export_dt_is_null(order, convert_type_value):
    """
    нельзя включить неотключаемый ЕС валютному клиенту,
     у которого нет даты обработки в очереди MIGRATE_TO_CURRENCY
     (например, только что созданный клиент)
    """
    order.is_ua_optimize = 0

    add_client_service_data_to_client(
        client=order.client,
        service_id=order.service.id,
        migrate_to_currency=HOUR_BEFORE,
        convert_type=convert_type_value,
    )
    assert order.check_turn_on_optimize(NOW) is False


@pytest.mark.parametrize('migrate_to_currency_value', [None, HOUR_AFTER])
def test_check_turn_on_optimize_migration_dt_is_null(order, migrate_to_currency_value):
    """ нельзя включить неотключаемый ЕС клиенту без даты миграции или с датой миграции в будущем"""
    order.is_ua_optimize = 0
    add_client_service_data_to_client(client=order.client, service_id=order.service.id,
                                      migrate_to_currency=migrate_to_currency_value,
                                      convert_type=CONVERT_TYPE_COPY)
    assert order.check_turn_on_optimize(NOW) is False


def test_check_turn_on_optimize_non_migrated_client(order):
    """ нельзя включить неотключаемый ЕС фишечному клиенту"""
    order.is_ua_optimize = 0
    assert order.check_turn_on_optimize(NOW) is False
