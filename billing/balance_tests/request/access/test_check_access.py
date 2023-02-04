# -*- coding: utf-8 -*-
import pytest

from balance import exc
from balance.mapper import Service, RoleClientPassport
from balance.constants import ConstraintTypes, FirmId, ServiceId, RoleType
from tests.balance_tests.request.request_common import (create_request,
                              create_passport,
                              create_role,
                              create_role_client,
                              create_manager,
                              create_client,
                              create_order,
                              create_firm,
                              ISSUE_INVOICES,
                              VIEW_ALL_INVOICES,
                              VIEW_INVOICES,
                              YAMONEY_SUPPORT_ACCESS)

from tests import object_builder as ob

pytestmark = [
    pytest.mark.permissions,
]


def test_nodoby(session):
    passport = create_passport(session, create_role(session), patch_session=False, client=None)
    request = create_request(session)

    with pytest.raises(exc.PERMISSION_DENIED):
        request.check_access(passport)


def test_client(session):
    request = create_request(session)
    passport = create_passport(session, create_role(session), patch_session=False,
                               client=request.client)
    session.flush()
    request.check_access(passport)


def test_simple_client(session):
    request = create_request(session)
    passport = create_passport(session, create_role(session), patch_session=False,
                               client=None, simple_client=request.client)
    session.flush()

    request.check_access(passport)


def test_service_client(session):
    request = create_request(session)
    request.client.service_id = 7
    passport = create_passport(session, create_role(session), patch_session=False,
                               client=None, simple_client=request.client)
    service = session.query(Service).get(7)
    passport.link_to_client(request.client, service)
    session.flush()

    request.check_access(passport)


def test_perm(session):
    request = create_request(session)
    role = create_role(session, ISSUE_INVOICES)
    passport = create_passport(session, role, patch_session=False)

    request.check_access(passport)


def test_perm_firm(session):
    request = create_request(session, None)
    role = create_role(session, (ISSUE_INVOICES, {ConstraintTypes.firm_id: None}))
    passport = create_passport(session, (role, FirmId.YANDEX_OOO), patch_session=False)

    request.check_access(passport)


def test_perm_wrong_firm(session):
    request = create_request(session, firm_id=FirmId.YANDEX_OOO)
    role = create_role(session, (ISSUE_INVOICES, {ConstraintTypes.firm_id: None}))
    passport = create_passport(session, (role, FirmId.TAXI), patch_session=False)

    with pytest.raises(exc.PERMISSION_DENIED):
        request.check_access(passport)


def test_perm_manager(session):
    role = create_role(session, (ISSUE_INVOICES, {ConstraintTypes.manager: 1, ConstraintTypes.firm_id: None}))
    passport = create_passport(session, (role, FirmId.YANDEX_OOO), patch_session=False)
    manager = create_manager(session, passport)
    client = create_client(session)
    request = create_request(session, firm_id=None, client=client,
                             orders=[create_order(session, client=client, manager=manager)])

    request.check_access(passport)


def test_perm_wrong_manager(session):
    role = create_role(session, (ISSUE_INVOICES, {ConstraintTypes.manager: 1, ConstraintTypes.firm_id: None}))
    passport = create_passport(session, (role, FirmId.YANDEX_OOO), patch_session=False)
    create_manager(session, passport)
    wrong_manager = create_manager(session)
    client = create_client(session)
    request = create_request(session, firm_id=None, client=client,
                             orders=[create_order(session, client=client, manager=wrong_manager)])

    with pytest.raises(exc.PERMISSION_DENIED):
        request.check_access(passport)


@pytest.mark.parametrize(
    'perm',
    [
        VIEW_INVOICES,
        VIEW_ALL_INVOICES,
        YAMONEY_SUPPORT_ACCESS,
    ]
)
def test_wrong_perms(session, perm):
    request = create_request(session)
    role = create_role(session, perm)
    passport = create_passport(session, role, patch_session=False)

    with pytest.raises(exc.PERMISSION_DENIED):
        request.check_access(passport)


def test_direct_limited_error(session, client):
    request = create_request(session, firm_id=None, client=client,
                             orders=[create_order(session, client=client,
                                                  service=session.query(Service).getone(ServiceId.DIRECT))])
    representative = create_client(session)

    passport = create_passport(session, create_role(session), patch_session=False,
                               client=request.client)
    passport._passport_client_roles = [
        RoleClientPassport(
            passport=passport,
            client_id=representative.id,
            role_id=RoleType.DIRECT_LIMITED
        )
    ]
    session.flush()

    with pytest.raises(exc.PERMISSION_DENIED_DIRECT_LIMITED) as exc_info:
        request.check_access(passport)
    assert exc_info.value.msg == 'User {uid} has no rights to access request {request}' \
                                 ' due to DirectLimited perm.'.format(uid=passport.passport_id, request=request.id)


def test_direct_limited_market_order(session, client):
    request = create_request(session, firm_id=None, client=client,
                             orders=[create_order(session, client=client,
                                                  service=session.query(Service).getone(ServiceId.MARKET))])
    representative = create_client(session)

    passport = create_passport(session, create_role(session), patch_session=False,
                               client=request.client)
    passport._passport_client_roles = [
        RoleClientPassport(
            passport=passport,
            client_id=representative.id,
            role_id=RoleType.DIRECT_LIMITED
        )
    ]
    session.flush()

    request.check_access(passport)


def test_direct_limited_access_by_limited(session):
    request = create_request(session)
    representative = create_client(session)
    order = request.request_orders[0].order
    order.client = representative
    order.agency = request.client

    passport = create_passport(session, create_role(session), patch_session=False,
                               client=request.client)

    passport._passport_client_roles = [
        RoleClientPassport(
            passport=passport,
            client_id=representative.id,
            role_id=RoleType.DIRECT_LIMITED
        )
    ]
    session.flush()

    request.check_access(passport)


def test_perm_w_clients(session, role_client):
    """Пользователь с правом ViewInvoices, право ограничено по клиентам, но id клиентов не указаны,
     роль ограничена по клиентам, клиент есть в этом списке -> есть доступ к реквесту"""
    role = create_role(
        session,
        (ISSUE_INVOICES,
         {ConstraintTypes.client_batch_id: None, ConstraintTypes.firm_id: None})
    )
    request = create_request(session, client=role_client.client)
    passport = ob.create_passport(session, (role, None, role_client.client_batch_id))
    request.check_access(passport)


def test_perm_w_clients_w_firm(session, role_client, firm):
    """Фирма и клиент указаны в одной записи роль-паспорт"""
    role = create_role(
        session,
        (ISSUE_INVOICES,
         {ConstraintTypes.client_batch_id: None, ConstraintTypes.firm_id: None})
    )
    passport = ob.create_passport(
        session,
        (role, firm.id, role_client.client_batch_id),
        (role, None, create_role_client(session).client_batch_id),
    )
    request = create_request(session, firm_id=firm.id, client=role_client.client)
    request.check_access(passport)


def test_perm_w_clients_w_firm_in_different_roles_access(session, role_client, firm):
    """У роли есть ограничения и по фирме и по клиентам.
    А к паспорту эта роль привязана дважды (в одном случае с firm_id, в другом с client_id)
    => есть доступ к реквесте, т.к. ограничения выглядят как (клиент и любая фирма) and (любой клиент и фирма).
    """
    role = create_role(
        session,
        (ISSUE_INVOICES,
         {ConstraintTypes.client_batch_id: None, ConstraintTypes.firm_id: None})
    )
    passport = ob.create_passport(
        session,
        (role, None, role_client.client_batch_id),
        (role, firm.id, None),
    )
    request = create_request(session, firm_id=firm.id, client=role_client.client)
    request.check_access(passport)


def test_perm_w_clients_w_firm_in_different_roles_failed(session, role_client, firm):
    """У роли есть ограничения и по фирме и по клиентам.
    А к паспорту эта роль привязана дважды
    (в одном случае с firm_id и неправильным клиентом, в другом с правильным client_id и неправильной фирмой)
    => нет доступа к реквесту, т.к. ограничения не выполняются.
    """
    role = create_role(
        session,
        (ISSUE_INVOICES,
         {ConstraintTypes.client_batch_id: None, ConstraintTypes.firm_id: None})
    )
    passport = ob.create_passport(
        session,
        (role, create_firm(session).id, role_client.client_batch_id),
        (role, firm.id, create_role_client(session).client_batch_id),
    )
    request = create_request(session, firm_id=firm.id, client=role_client.client)
    with pytest.raises(exc.PERMISSION_DENIED):
        request.check_access(passport)


def test_perm_wrong_client(session, role_client):
    """Для ограниченной роли с правом IssueInvoices указан другой клиент"""
    role = create_role(session, (ISSUE_INVOICES, {ConstraintTypes.client_batch_id: None}))
    passport = ob.create_passport(session, (role, None, create_role_client(session).client_batch_id))
    request = create_request(session, client=role_client.client)
    with pytest.raises(exc.PERMISSION_DENIED):
        request.check_access(passport)
