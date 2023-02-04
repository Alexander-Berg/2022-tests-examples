# -*- coding: utf-8 -*-
import pytest
import mock

from balance import mapper
from balance import exc
from balance.constants import (
    ConstraintTypes,
    FirmId
)

from tests import object_builder as ob

from tests.balance_tests.invoices.access.access_common import (
    create_invoice,
    create_firm_id,
    create_passport,
    create_role,
    create_role_client,
    create_manager,
    create_client,
    VIEW_ALL_INVOICES,
    VIEW_INVOICES,
    YAMONEY_SUPPORT_ACCESS,
)

pytestmark = [
    pytest.mark.permissions,
]


def test_nobody(session):
    """У пользователя без прав нет доступа к счету"""
    passport = create_passport(session, [create_role(session)])
    invoice = create_invoice(session)

    with pytest.raises(exc.PERMISSION_DENIED):
        invoice.check_access(passport)


def test_client(session):
    """Если пользователь - представитель владельца счета, есть доступ к счету"""
    invoice = create_invoice(session)
    passport = create_passport(session, [create_role(session)], client=invoice.client)
    session.flush()

    invoice.check_access(passport)


def test_wrong_client(session):
    """Если пользователь - представитель владельца счета, есть доступ к счету"""
    invoice = create_invoice(session)
    passport = create_passport(session, [create_role(session)], client=create_client(session))
    session.flush()

    with pytest.raises(exc.PERMISSION_DENIED):
        invoice.check_access(passport)


def test_simple_client(session):
    """Если пользователь - представитель владельца счета в персональных сервисах, есть доступ к счету"""
    invoice = create_invoice(session)
    passport = create_passport(session, [create_role(session)], simple_client=invoice.client)
    session.flush()

    invoice.check_access(passport)


def test_service_client(session):
    """Если пользователь - представитель владельца счета в разрезе сервиса, есть доступ к счету"""
    invoice = create_invoice(session)
    invoice.client.service_id = 7
    passport = create_passport(session, [create_role(session)])
    service = session.query(mapper.Service).get(7)
    passport.link_to_client(invoice.client, service)
    session.flush()

    invoice.check_access(passport)


def test_yamoney_support_simple(session):
    """если все заказы в реквесте принадлежат простым сервисам и пользователь - саппорт ЯД, есть доступ к счету"""
    invoice = create_invoice(session)
    role = create_role(session, YAMONEY_SUPPORT_ACCESS)
    passport = create_passport(session, [role])

    with mock.patch('balance.mapper.invoices.Request.is_simple', True):
        invoice.check_access(passport)


def test_yamoney_support_common(session):
    """если  заказы в реквесте не принадлежат простым сервисам даже у саппорта ЯД нет доступа к счету"""
    invoice = create_invoice(session)
    role = create_role(session, YAMONEY_SUPPORT_ACCESS)
    passport = create_passport(session, [role])

    with pytest.raises(exc.PERMISSION_DENIED):
        invoice.check_access(passport)


def test_perm(session):
    """Пользователь с правом ViewInvoices, ни роль, ни право не ограничены по фирме/клиенту -> есть доступ к счету"""
    invoice = create_invoice(session)
    role = create_role(session, VIEW_INVOICES)
    passport = create_passport(session, [role])

    invoice.check_access(passport)


def test_perm_firm(session):
    """Пользователь с правом ViewInvoices, право ограничено по фирме, но фирма не указана,
     роль ограничена по фирме, фирма роли совпадает с фирмой счета -> есть доступ к счету"""
    invoice = create_invoice(session)
    role = create_role(session, (VIEW_INVOICES, {ConstraintTypes.firm_id: None}))
    passport = create_passport(session, [(role, FirmId.YANDEX_OOO)])

    invoice.check_access(passport)


def test_perm_wrong_firm(session):
    """Пользователь с правом ViewInvoices, право ограничено по фирме, но фирма не указана,
     роль ограничена по фирме, фирма роли не совпадает с фирмой счета -> нет доступа к счету"""
    invoice = create_invoice(session)
    role = create_role(session, (VIEW_INVOICES, {ConstraintTypes.firm_id: None}))
    passport = create_passport(session, [(role, FirmId.TAXI)])

    with pytest.raises(exc.PERMISSION_DENIED):
        invoice.check_access(passport)


def test_perm_manager(session):
    """Менеджер с правом ViewInvoices,
    право ограничено по фирме, но фирма не указана,
    менеджерская роль ограничена по фирме,
    фирма роли совпадает с фирмой счета,
    менеджер указан в заказе -> есть доступ к счету"""
    role = create_role(
        session,
        (VIEW_INVOICES,
         {ConstraintTypes.manager: 1, ConstraintTypes.firm_id: None})
    )
    passport = create_passport(session, [(role, FirmId.YANDEX_OOO)])
    manager = create_manager(session, passport)
    invoice = create_invoice(session, manager=manager)

    invoice.check_access(passport)


def test_perm_wrong_manager(session):
    """Менеджер с правом ViewInvoices,
    право ограничено по фирме, но фирма не указана,
    менеджерская роль ограничена по фирме,
    фирма роли совпадает с фирмой счета,
    в заказе указан другой менеджер -> нет доступа к счету"""
    role = create_role(
        session,
        (VIEW_INVOICES,
         {ConstraintTypes.manager: 1, ConstraintTypes.firm_id: None})
    )
    passport = create_passport(session, [(role, FirmId.YANDEX_OOO)])
    create_manager(session, passport)
    wrong_manager = create_manager(session)
    invoice = create_invoice(session, manager=wrong_manager)

    with pytest.raises(exc.PERMISSION_DENIED):
        invoice.check_access(passport)


def test_wrong_perm(session):
    invoice = create_invoice(session)
    role = create_role(session, VIEW_ALL_INVOICES)
    passport = create_passport(session, [role])

    with pytest.raises(exc.PERMISSION_DENIED):
        invoice.check_access(passport)


def test_perm_w_clients(session, role_client):
    """Пользователь с правом ViewInvoices, право ограничено по клиентам, но id клиентов не указаны,
     роль ограничена по клиентам, клиент есть в этом списке -> есть доступ к клиенту"""
    role = create_role(
        session,
        (VIEW_INVOICES,
         {ConstraintTypes.client_batch_id: None, ConstraintTypes.firm_id: None})
    )
    invoice = create_invoice(session, client=role_client.client)
    passport = ob.create_passport(session, (role, None, role_client.client_batch_id))
    invoice.check_access(passport)


def test_perm_w_clients_w_firm(session, role_client, firm_id):
    """Фирма и клиент указаны в одной записи роль-паспорт"""
    role = create_role(
        session,
        (VIEW_INVOICES,
         {ConstraintTypes.client_batch_id: None, ConstraintTypes.firm_id: None})
    )
    passport = ob.create_passport(
        session,
        (role, firm_id, role_client.client_batch_id),
        (role, None, create_role_client(session).client_batch_id),
    )
    invoice = create_invoice(session, firm_id=firm_id, client=role_client.client)
    invoice.check_access(passport)


def test_perm_w_clients_w_firm_in_different_roles_access(session, role_client, firm_id):
    """У роли есть ограничения и по фирме и по клиентам.
    А к паспорту эта роль привязана дважды (в одном случае с firm_id, в другом с client_id)
    => есть доступа к счету, т.к. ограничения выглядят как (клиент и любая фирма) and (любой клиент и фирма).
    """
    role = create_role(
        session,
        (VIEW_INVOICES,
         {ConstraintTypes.client_batch_id: None, ConstraintTypes.firm_id: None})
    )
    passport = ob.create_passport(
        session,
        (role, None, role_client.client_batch_id),
        (role, firm_id, None),
    )
    invoice = create_invoice(session, firm_id=firm_id, client=role_client.client)
    invoice.check_access(passport)


def test_perm_w_clients_w_firm_in_different_roles_failed(session, role_client, firm_id):
    """У роли есть ограничения и по фирме и по клиентам.
    А к паспорту эта роль привязана дважды
    (в одном случае с firm_id и неправильным клиентом, в другом с правильным client_id и неправильной фирмой)
    => нет доступа к счету, т.к. ограничения не выполняются.
    """
    role = create_role(
        session,
        (VIEW_INVOICES,
         {ConstraintTypes.client_batch_id: None, ConstraintTypes.firm_id: None})
    )
    passport = ob.create_passport(
        session,
        (role, create_firm_id(session), role_client.client_batch_id),
        (role, firm_id, ob.get_big_number()),
    )
    invoice = create_invoice(session, firm_id=firm_id, client=role_client.client)
    with pytest.raises(exc.PERMISSION_DENIED):
        invoice.check_access(passport)


def test_perm_wrong_client(session, role_client):
    """Для ограниченной роли с правом ViewInvoices указан другой клиент"""
    role = create_role(session, (VIEW_INVOICES, {ConstraintTypes.client_batch_id: None}))
    passport = ob.create_passport(session, (role, None, create_role_client(session).client_batch_id))
    invoice = create_invoice(session, client=role_client.client)
    with pytest.raises(exc.PERMISSION_DENIED):
        invoice.check_access(passport)
