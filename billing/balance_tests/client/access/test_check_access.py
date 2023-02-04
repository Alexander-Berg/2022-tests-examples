# -*- coding: utf-8 -*-
import pytest

from balance import exc, mapper
from balance.constants import PermissionCode

from tests import object_builder as ob

from tests.balance_tests.client.access.conftest import create_client, create_role_client

pytestmark = [
    pytest.mark.permissions,
]


def test_nobody(session, client, wrong_role):
    """У пользователя без прав нет доступа к клиенту"""
    passport = ob.create_passport(session, wrong_role)
    with pytest.raises(exc.PERMISSION_DENIED):
        client.check_access(passport)


def test_client(session, client, wrong_role):
    """Можно посмотреть информацию по своему клиенту"""
    passport = ob.create_passport(session, wrong_role, client=client)
    session.flush()
    client.check_access(passport)


def test_repr_client(session, client):
    repr_role = ob.create_role(session, PermissionCode.REPRESENT_CLIENT)
    passport = ob.create_passport(session, repr_role)
    session.add(mapper.RoleClientPassport(passport=passport, role=repr_role, client=client))
    session.flush()
    client.check_access(passport)


def test_simple_client(session, client, wrong_role):
    """Представители клиента могут просматривать информацию по клиенту"""
    passport = ob.create_passport(session, wrong_role, simple_client=client)
    session.flush()
    client.check_access(passport)


def test_wrong_client(session, client, wrong_role):
    """У паспорта есть клиент, но не тот"""
    passport = ob.create_passport(session, wrong_role, client=create_client(session))
    session.flush()
    with pytest.raises(exc.PERMISSION_DENIED):
        client.check_access(passport)


def test_perm(session, client):
    """Пользователь с правом ViewClients, ни роль, ни право не ограничены по фирме -> есть доступ к клиенту"""
    role = ob.create_role(session, PermissionCode.VIEW_CLIENTS)
    passport = ob.create_passport(session, role)
    client.check_access(passport)


def test_perm_w_clients(session, role_client, view_clients_role):
    """Пользователь с правом ViewClients, право ограничено по клиентам, но id клиентов не указаны,
     роль ограничена по клиентам, клиент есть в этом списке -> есть доступ к клиенту"""
    passport = ob.create_passport(session, (view_clients_role, None, role_client.client_batch_id))
    role_client.client.check_access(passport)


def test_perm_w_clients_2(session, role_client, view_clients_role):
    passport = ob.create_passport(
        session,
        (view_clients_role, None, role_client.client_batch_id),
        (view_clients_role, None, create_role_client(session).client_batch_id),
    )
    role_client.client.check_access(passport)


def test_perm_wrong_client(session, role_client, view_clients_role):
    """Для ограниченной роли с правом ViewClients указан другой клиент"""
    passport = ob.create_passport(session, (view_clients_role, None, create_role_client(session).client_batch_id))
    with pytest.raises(exc.PERMISSION_DENIED):
        role_client.client.check_access(passport)
