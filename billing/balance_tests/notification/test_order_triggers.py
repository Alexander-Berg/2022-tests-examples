# -*- coding: utf-8 -*-
import datetime
import pytest
from decimal import Decimal

from tests import object_builder as ob
from balance.constants import *

NOW = datetime.datetime.now()
HOUR_AFTER = NOW + datetime.timedelta(hours=1)
HOUR_BEFORE = NOW - datetime.timedelta(hours=1)
CODE_SUCCESS = 0


@pytest.fixture
def client(session, **attrs):
    return ob.ClientBuilder(**attrs).build(session).obj


def order(session, client, parent_group_order=None, **attrs):
    return ob.OrderBuilder(client=client, group_order_id=parent_group_order and parent_group_order.id, **attrs).build(
        session).obj


@pytest.fixture
def product(session, **attrs):
    return ob.ProductBuilder(**attrs).build(session).obj


@pytest.fixture
def service_order_id():
    return ob.get_big_number()


def test_order_simple_group(session, xmlrpcserver, client, product, service_order_id):
    # простое объединение в группу для заказов разрешенных сервисов
    parent_order = order(session, client=client)
    res = session.execute("select * from bo.ton t where t.user_data.order_id = :oid",
                          {'oid': parent_order.id + NOTIFY_ORDER_OPCODE / Decimal(1000)})
    assert len(res.fetchall()) == 0
    xmlrpcserver.CreateOrUpdateOrdersBatch(session.oper_id,
                                           [{'ClientID': client.id,
                                             'ProductID': product.id,
                                             'ServiceOrderID': service_order_id,
                                             'ServiceID': parent_order.service.id,
                                             'GroupServiceOrderID': parent_order.service_order_id}])
    res = session.execute("select * from bo.ton t where t.user_data.order_id = :oid",
                          {'oid': parent_order.id + NOTIFY_ORDER_OPCODE / Decimal(1000)})
    assert len(res.fetchall())
