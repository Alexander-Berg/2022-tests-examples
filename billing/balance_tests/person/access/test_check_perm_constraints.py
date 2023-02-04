# -*- coding: utf-8 -*-
import pytest

from balance.constants import ConstraintTypes
from tests import object_builder as ob

from tests.balance_tests.person.access.conftest import (
    CheckType,
    check_perm_constraints_by_check_type,
    PERMISSION,
    create_role_client,
    create_person,
) 

pytestmark = [
    pytest.mark.permissions,
    pytest.mark.parametrize('check_type', [CheckType.object, CheckType.query]),
]


def test_wo_permission(session, check_type, person):
    role = ob.create_role(session)
    passport = ob.create_passport(session, role)
    assert check_perm_constraints_by_check_type(session, person, passport, check_type) is False


def test_w_permission_wo_constraints(session, check_type, person):
    role = ob.create_role(session, PERMISSION)
    passport = ob.create_passport(session, role)
    assert check_perm_constraints_by_check_type(session, person, passport, check_type) is True


def test_w_permission_w_constraints_w_right_client_id(session, check_type, role_client):
    role = ob.create_role(session, (PERMISSION, {ConstraintTypes.client_batch_id: None}))
    passport = ob.create_passport(session, (role, None, role_client.client_batch_id))
    person = create_person(session, role_client.client)
    assert check_perm_constraints_by_check_type(session, person, passport, check_type) is True


def test_clients_in_different_role_user(session, check_type, role_client):
    role = ob.create_role(session, (PERMISSION, {ConstraintTypes.client_batch_id: None}))
    passport = ob.create_passport(
        session,
        (role, None, role_client.client_batch_id),
        (role, None, create_role_client(session).client_batch_id),
    )
    person = create_person(session, role_client.client)
    assert check_perm_constraints_by_check_type(session, person, passport, check_type) is True


def test_wrong_client(session, check_type, role_client, person):
    role = ob.create_role(session, (PERMISSION, {ConstraintTypes.client_batch_id: None}))
    passport = ob.create_passport(
        session,
        (role, None, role_client.client_batch_id),
    )
    assert check_perm_constraints_by_check_type(session, person, passport, check_type) is False


def test_nested_empty_clients(session, check_type, role_client):
    role1 = ob.create_role(session, PERMISSION)
    role2 = ob.create_role(session, (PERMISSION, {ConstraintTypes.client_batch_id: None}))
    passport = ob.create_passport(
        session,
        role1,
        (role2, None, role_client.client_batch_id),
    )
    person = create_person(session, role_client.client)
    assert check_perm_constraints_by_check_type(session, person, passport, check_type) is True


def test_nested_clients(session, check_type, role_client):
    role1 = ob.create_role(session, (PERMISSION, {ConstraintTypes.client_batch_id: None}))
    role2 = ob.create_role(session, (PERMISSION, {ConstraintTypes.client_batch_id: None}))
    passport = ob.create_passport(
        session,
        (role1, None, role_client.client_batch_id),
        (role2, None, create_role_client(session).client_batch_id),
    )
    person = create_person(session, role_client.client)
    assert check_perm_constraints_by_check_type(session, person, passport, check_type) is True


def test_hidden_role_client(session, check_type, role_client):
    role = ob.create_role(session, (PERMISSION, {ConstraintTypes.client_batch_id: None}))
    role_client.hidden = True
    passport = ob.create_passport(
        session,
        (role, None, role_client.client_batch_id),
    )
    person = create_person(session, role_client.client)
    assert check_perm_constraints_by_check_type(session, person, passport, check_type) is False
