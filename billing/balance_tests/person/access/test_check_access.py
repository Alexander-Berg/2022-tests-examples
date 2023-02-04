# -*- coding: utf-8 -*-
import pytest

from balance import exc, mapper
from balance.constants import PermissionCode

from tests import object_builder as ob

from tests.balance_tests.person.access.conftest import create_client, create_role_client, create_person

pytestmark = [
    pytest.mark.permissions,
]


def test_nobody(session, person, wrong_role):
    """У пользователя без прав нет доступа к плательщику"""
    passport = ob.create_passport(session, wrong_role)
    with pytest.raises(exc.PERMISSION_DENIED):
        person.check_access(passport)


def test_person(session, person, wrong_role):
    """Можно посмотреть информацию по своему плательщику"""
    passport = ob.create_passport(session, wrong_role, client=person.client)
    session.flush()
    person.check_access(passport)


def test_repr_client(session, client, person):
    repr_role = ob.create_role(session, PermissionCode.REPRESENT_CLIENT)
    passport = ob.create_passport(session, repr_role)
    session.add(mapper.RoleClientPassport(passport=passport, role=repr_role, client=client))
    session.flush()
    person.check_access(passport)


def test_simple_client(session, client, person, wrong_role):
    """Представители клиента могут просматривать информацию по плательщику"""
    passport = ob.create_passport(session, wrong_role, simple_client=client)
    session.flush()
    person.check_access(passport)


def test_wrong_client(session, person, wrong_role):
    passport = ob.create_passport(session, wrong_role, client=create_client(session))
    session.flush()
    with pytest.raises(exc.PERMISSION_DENIED):
        person.check_access(passport)


def test_perm(session, person):
    """Пользователь с правом ViewPersons, ни роль, ни право не ограничены по фирме -> есть доступ к плательщику"""
    role = ob.create_role(session, PermissionCode.VIEW_PERSONS)
    passport = ob.create_passport(session, role)
    person.check_access(passport)


def test_perm_w_clients(session, role_client, view_persons_role):
    """Пользователь с правом ViewPersons, право ограничено по клиентам, но id клиентов не указаны,
     роль ограничена по клиентам, клиент есть в этом списке -> есть доступ к плательщику"""
    passport = ob.create_passport(session, (view_persons_role, None, role_client.client_batch_id))
    person = create_person(session, role_client.client)
    person.check_access(passport)


def test_perm_w_clients_2(session, role_client, view_persons_role):
    passport = ob.create_passport(
        session,
        (view_persons_role, None, role_client.client_batch_id),
        (view_persons_role, None, create_role_client(session).client_batch_id),
    )
    person = create_person(session, role_client.client)
    person.check_access(passport)


def test_perm_wrong_client(session, role_client, view_persons_role):
    """Для ограниченной роли с правом ViewPersons указан неправильный клиент"""
    passport = ob.create_passport(session, (view_persons_role, None, create_role_client(session).client_batch_id))
    person = create_person(session, role_client.client)
    with pytest.raises(exc.PERMISSION_DENIED):
        person.check_access(passport)
