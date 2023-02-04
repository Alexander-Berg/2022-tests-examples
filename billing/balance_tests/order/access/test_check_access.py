# -*- coding: utf-8 -*-
import pytest

from balance import (
    constants as cst,
    exc,
)
from tests import object_builder as ob

# noinspection PyUnresolvedReferences
from tests.balance_tests.order.access.access_common import (
    create_agency,
    create_order,
    create_order_w_firms,
    create_wrong_role,
    create_client,
    create_role_client,
    create_passport,
    create_view_orders_role,
    create_yamoney_role,
    ORDER_FIRM_IDS,
)

pytestmark = [
    pytest.mark.permissions,
]


@pytest.mark.parametrize(
    'order_func',
    [
        pytest.param(create_order, id='order wo firms'),
        pytest.param(create_order_w_firms, id='order w firms'),
    ],
)
def test_nobody(session, order_func, wrong_role):
    """У пользователя без прав нет доступа к заказу"""
    passport = create_passport(session, [wrong_role])
    with pytest.raises(exc.PERMISSION_DENIED):
        order_func(session).check_access(passport)


def test_client(session, order, wrong_role):
    """Есть пользователь - представитель владельца заказа, есть доступ к заказу"""
    passport = create_passport(session, [wrong_role], client=order.client)
    order.check_access(passport)


def test_agency(session, agency, wrong_role):
    """Есть агенство - представитель владельца заказа, есть доступ к заказу"""
    order = ob.OrderBuilder.construct(
        session,
        agency=agency,
    )
    passport = create_passport(session, [wrong_role], client=agency)
    order.check_access(passport)


def test_agency_direct_limited(session, agency, client, wrong_role):
    """Есть агенство - представитель владельца заказа и клиента заказа direct limited, нет доступа к заказу"""
    order = ob.OrderBuilder.construct(
        session,
        client=client,
        agency=agency,
    )
    passport = create_passport(session, [wrong_role], client=agency)
    passport.update_limited([agency.id])
    session.flush()
    with pytest.raises(exc.PERMISSION_DENIED_DIRECT_LIMITED):
        order.check_access(passport)


def test_wrong_client(session, order, wrong_role):
    """Есть пользователь - он не представитель владельца заказа, нет доступа к заказу"""
    passport = create_passport(session, [wrong_role], client=create_client(session))
    with pytest.raises(exc.PERMISSION_DENIED):
        order.check_access(passport)


def test_simple_client(session, order, wrong_role):
    """Если пользователь - представитель владельца заказы в персональных сервисах, есть доступ к заказу"""
    passport = create_passport(session, [wrong_role], simple_client=order.client)
    order.check_access(passport)


def test_yamoney_support(session, yamoney_role):
    """С правом YaMoneySupportAccess можно получить доступ к простому заказу"""
    passport = create_passport(session, [yamoney_role])
    simple_order = create_order(session, service_id=cst.ServiceId.YANDEX_STORE)
    simple_order.check_access(passport)


def test_perm(session, order_w_firms, view_orders_role):
    """Пользователь с правом ViewOrders, роль не ограничены по фирме -> есть доступ к заказу"""
    passport = create_passport(session, [view_orders_role])
    order_w_firms.check_access(passport)


@pytest.mark.parametrize(
    'role_firms, has_access',
    [
        pytest.param(ORDER_FIRM_IDS, True, id='role firms == order firms'),
        pytest.param([ORDER_FIRM_IDS[0]], False, id='role firms < order firms'),
        pytest.param(ORDER_FIRM_IDS + [cst.FirmId.DRIVE], True, id='role firms > order firms'),
        pytest.param([cst.FirmId.DRIVE, cst.FirmId.BUS], False, id='role firms != order firms'),
    ]
)
def test_perm_firm(session, order_w_firms, view_orders_role, role_firms, has_access):
    """Пользователь с правом ViewOrders, право ограничено по фирме, но фирма не указана,
     роль ограничена по фирмам, чтобы получить доступ к заказу у роли должны быть все фирмы из заказа"""
    passport = create_passport(
        session,
        [
            (view_orders_role, firm_id)
            for firm_id in role_firms
        ],
    )
    if has_access:
        order_w_firms.check_access(passport)
    else:
        with pytest.raises(exc.PERMISSION_DENIED):
            order_w_firms.check_access(passport)


def test_perm_w_clients(session, role_client, view_orders_role):
    order = create_order_w_firms(session, client=role_client.client)
    passport = create_passport(
        session,
        [
            (view_orders_role, firm_id, role_client.client_batch_id)
            for firm_id in ORDER_FIRM_IDS
        ],
    )
    order.check_access(passport)


def test_perm_w_clients_w_firm_in_different_roles_access(session, role_client, view_orders_role):
    passport = create_passport(
        session,
        [
            (view_orders_role, None, role_client.client_batch_id),
            (view_orders_role, ORDER_FIRM_IDS[0], None),
            (view_orders_role, ORDER_FIRM_IDS[1], None),
        ],
    )
    order = create_order_w_firms(session, client=role_client.client)
    order.check_access(passport)


def test_perm_w_clients_w_firm_in_different_roles_failed(session, role_client, view_orders_role):
    passport = create_passport(
        session,
        [
            (view_orders_role, ORDER_FIRM_IDS[0], role_client.client_batch_id),
            (view_orders_role, ORDER_FIRM_IDS[1], create_role_client(session).client_batch_id),
        ],
    )
    order = create_order_w_firms(session, client=role_client.client)
    with pytest.raises(exc.PERMISSION_DENIED):
        order.check_access(passport)


def test_perm_wrong_client(session, role_client, view_orders_role):
    passport = create_passport(session, [(view_orders_role, None, create_role_client(session).client_batch_id)])
    order = create_order(session, client=role_client.client)
    with pytest.raises(exc.PERMISSION_DENIED):
        order.check_access(passport)


def test_perm_unexisting_client_batch(session, role_client, view_orders_role):
    passport = create_passport(session, [(view_orders_role, None, -666)])
    order = create_order(session, client=role_client.client)
    with pytest.raises(exc.PERMISSION_DENIED):
        order.check_access(passport)


def test_perm_wrong_firm(session, role_client, view_orders_role):
    passport = create_passport(
        session,
        [
            (view_orders_role, cst.FirmId.DRIVE, role_client.client_batch_id),
            (view_orders_role, cst.FirmId.MARKET, role_client.client_batch_id),
        ],
    )
    order = create_order_w_firms(session, client=role_client.client)
    with pytest.raises(exc.PERMISSION_DENIED):
        order.check_access(passport)


def test_perm_wrong_role_firm_order_wo_firms(session, role_client, view_orders_role):
    passport = create_passport(
        session,
        [
            (view_orders_role, cst.FirmId.DRIVE, role_client.client_batch_id),
            (view_orders_role, cst.FirmId.MARKET, role_client.client_batch_id),
        ],
    )
    order = create_order(session, client=role_client.client)
    order.check_access(passport)


@pytest.mark.parametrize(
    'is_agency',
    [
        pytest.param(True, id='agency'),
        pytest.param(False, id='client'),
    ],
)
def test_access_for_agency(session, client, agency, view_orders_role, is_agency):
    client_batch_id = create_role_client(session, client=agency if is_agency else client).client_batch_id
    passport = create_passport(
        session,
        [
            (view_orders_role, None, client_batch_id),
        ],
    )
    order = create_order(session, client=client, agency=agency)

    if is_agency:
        order.check_access(passport)

    else:
        with pytest.raises(exc.PERMISSION_DENIED):
            order.check_access(passport)
