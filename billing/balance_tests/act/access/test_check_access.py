# -*- coding: utf-8 -*-
import pytest
import mock

from balance import constants as cst, exc, mapper
from tests import object_builder as ob

from tests.balance_tests.act.access.access_common import (
    create_invoice,
    create_passport,
    create_manager,
    create_client,
    create_act,
    create_role,
    create_role_client,
    create_wrong_role,
    create_right_role,
    create_yamoney_role,
    create_view_invoices_role,
    create_view_invoices_role_2,
    INVOICE_FIRM_ID,
)

pytestmark = [
    pytest.mark.permissions,
]


def test_nobody(session, wrong_role, act):
    """У пользователя без прав нет доступа к акту"""
    passport = create_passport(session, [wrong_role])
    with pytest.raises(exc.PERMISSION_DENIED):
        act.check_access(passport)


def test_client(session, act, wrong_role):
    """Если пользователь - представитель владельца счета, есть доступ к акту"""
    passport = create_passport(session, [wrong_role], client=act.client)
    session.flush()
    act.check_access(passport)


def test_wrong_client(session, act, wrong_role):
    """Если пользователь - представитель владельца счета, есть доступ к акту"""
    passport = create_passport(session, [wrong_role], client=create_client(session))
    session.flush()
    with pytest.raises(exc.PERMISSION_DENIED):
        act.check_access(passport)


def test_simple_client(session, act, wrong_role):
    """Если пользователь - представитель владельца счета в персональных сервисах, есть доступ к акту"""
    passport = create_passport(session, [wrong_role], simple_client=act.client)
    session.flush()
    act.check_access(passport)


def test_service_client(session, act, wrong_role):
    """Если пользователь - представитель владельца счета в разрезе сервиса, есть доступ к акту"""
    act.client.service_id = cst.ServiceId.DIRECT
    passport = create_passport(session, [wrong_role])
    service = session.query(mapper.Service).get(cst.ServiceId.DIRECT)
    passport.link_to_client(act.client, service)
    session.flush()
    act.check_access(passport)


def test_yamoney_support_simple(session, act, yamoney_role):
    """если все заказы в реквесте принадлежат простым сервисам и пользователь - саппорт ЯД, есть доступ к акту"""
    passport = create_passport(session, [yamoney_role])
    with mock.patch('balance.mapper.invoices.Request.is_simple', True):
        act.check_access(passport)


def test_yamoney_support_common(session, act, yamoney_role):
    """если  заказы в реквесте не принадлежат простым сервисам даже у саппорта ЯД нет доступа к акту"""
    passport = create_passport(session, [yamoney_role])
    with pytest.raises(exc.PERMISSION_DENIED):
        act.check_access(passport)


def test_perm(session, act, view_invoices_role):
    """Пользователь с правом ViewInvoices, ни роль, ни право не ограничены по фирме -> есть доступ к акту"""
    passport = create_passport(session, [view_invoices_role])
    act.check_access(passport)


def test_perm_firm(session, act, view_invoices_role):
    """Пользователь с правом ViewInvoices, право ограничено по фирме, но фирма не указана,
     роль ограничена по фирме, фирма роли совпадает с фирмой счета -> есть доступ к акту"""
    passport = create_passport(session, [(view_invoices_role, INVOICE_FIRM_ID)])
    act.check_access(passport)


def test_perm_wrong_firm(session, act, view_invoices_role):
    """Пользователь с правом ViewInvoices, право ограничено по фирме, но фирма не указана,
     роль ограничена по фирме, фирма роли не совпадает с фирмой счета -> нет доступа к акту"""
    passport = create_passport(session, [(view_invoices_role, cst.FirmId.TAXI)])
    with pytest.raises(exc.PERMISSION_DENIED):
        act.check_access(passport)


def test_perm_manager(session, view_invoices_role_2):
    """Менеджер с правом ViewInvoices,
    право ограничено по фирме, но фирма не указана,
    менеджерская роль ограничена по фирме,
    фирма роли совпадает с фирмой счета,
    менеджер указан в заказе -> есть доступ к акту"""
    passport = create_passport(session, [(view_invoices_role_2, INVOICE_FIRM_ID)])
    manager = create_manager(session, passport)
    act = create_act(session, manager=manager)
    session.flush()
    act.check_access(passport)


def test_perm_wrong_manager(session, view_invoices_role_2):
    """Менеджер с правом ViewInvoices,
    право ограничено по фирме, но фирма не указана,
    менеджерская роль ограничена по фирме,
    фирма роли совпадает с фирмой счета,
    в заказе указан другой менеджер -> нет доступа к акту"""
    passport = create_passport(session, [(view_invoices_role_2, INVOICE_FIRM_ID)])
    create_manager(session, passport)
    wrong_manager = create_manager(session)
    act = create_act(session, manager=wrong_manager)
    with pytest.raises(exc.PERMISSION_DENIED):
        act.check_access(passport)


@pytest.mark.parametrize(
    'perm',
    [
        cst.PermissionCode.ADMIN_ACCESS,
        cst.PermissionCode.YAMONEY_SUPPORT_ACCESS,
        'ViewAllInvoices',
    ]
)
def test_wrong_perms(session, perm):
    """У пользователя нет права просматривать акты"""
    act = create_act(session)
    role = ob.create_role(session, perm)
    passport = ob.create_passport(session, role)

    with pytest.raises(exc.PERMISSION_DENIED):
        act.check_access(passport)


def test_perm_w_clients(session, role_client):
    """Пользователь с правом ViewInvoices, право ограничено по клиентам, но id клиентов не указаны,
     роль ограничена по клиентам, клиент есть в этом списке -> есть доступ к акту"""
    role = create_role(
        session,
        (cst.PermissionCode.VIEW_INVOICES,
         {cst.ConstraintTypes.client_batch_id: None, cst.ConstraintTypes.firm_id: None})
    )
    act = create_act(session, client=role_client.client)
    passport = ob.create_passport(session, (role, None, role_client.client_batch_id))
    act.check_access(passport)


def test_perm_w_clients_w_firm(session, role_client):
    """Фирма и клиент указаны в одной записи роль-паспорт"""
    role = create_role(
        session,
        (cst.PermissionCode.VIEW_INVOICES,
         {cst.ConstraintTypes.client_batch_id: None, cst.ConstraintTypes.firm_id: None})
    )
    passport = ob.create_passport(
        session,
        (role, INVOICE_FIRM_ID, role_client.client_batch_id),
        (role, None, create_role_client(session).client_batch_id),
    )
    act = create_act(session, firm_id=INVOICE_FIRM_ID, client=role_client.client)
    act.check_access(passport)


def test_perm_w_clients_w_firm_in_different_roles_access(session, role_client):
    """У роли есть ограничения и по фирме и по клиентам.
    А к паспорту эта роль привязана дважды (в одном случае с firm_id, в другом с client_id)
    => есть доступа к акту, т.к. ограничения выглядят как (клиент и любая фирма) and (любой клиент и фирма).
    """
    role = create_role(
        session,
        (cst.PermissionCode.VIEW_INVOICES,
         {cst.ConstraintTypes.client_batch_id: None, cst.ConstraintTypes.firm_id: None})
    )
    passport = ob.create_passport(
        session,
        (role, None, role_client.client_batch_id),
        (role, INVOICE_FIRM_ID, None),
    )
    act = create_act(session, firm_id=INVOICE_FIRM_ID, client=role_client.client)
    act.check_access(passport)


def test_perm_w_clients_w_firm_in_different_roles_failed(session, role_client):
    """У роли есть ограничения и по фирме и по клиентам.
    А к паспорту эта роль привязана дважды
    (в одном случае с firm_id и неправильным клиентом, в другом с правильным client_id и неправильной фирмой)
    => нет доступа к акту, т.к. ограничения не выполняются.
    """
    role = create_role(
        session,
        (cst.PermissionCode.VIEW_INVOICES,
         {cst.ConstraintTypes.client_batch_id: None, cst.ConstraintTypes.firm_id: None})
    )
    passport = ob.create_passport(
        session,
        (role, cst.FirmId.TAXI, role_client.client_batch_id),
        (role, INVOICE_FIRM_ID, ob.get_big_number()),
    )
    act = create_act(session, firm_id=INVOICE_FIRM_ID, client=role_client.client)
    with pytest.raises(exc.PERMISSION_DENIED):
        act.check_access(passport)


def test_perm_wrong_client(session, role_client):
    """Для ограниченной роли с правом ViewInvoices указан другой клиент"""
    role = create_role(session, (cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.client_batch_id: None}))
    passport = ob.create_passport(session, (role, None, create_role_client(session).client_batch_id))
    act = create_act(session, client=role_client.client)
    with pytest.raises(exc.PERMISSION_DENIED):
        act.check_access(passport)
