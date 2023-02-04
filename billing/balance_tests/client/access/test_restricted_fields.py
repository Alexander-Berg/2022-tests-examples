# -*- coding: utf-8 -*-
import pytest

from balance import (
    core,
    constants as cst,
    exc,
)
from tests import object_builder as ob

pytestmark = [
    pytest.mark.permissions,
]


class PermCheckType(object):
    wo_role = 0
    w_role = 1
    w_right_batch = 2
    w_wrong_batch = 3


@pytest.fixture(name='add_func_role')
def create_add_func_role(session):
    return ob.create_role(session, (cst.PermissionCode.ADDITIONAL_FUNCTIONS, {cst.ConstraintTypes.client_batch_id: None}))


def _set_role(session, check_type, client, role):
    roles = []
    if check_type not in (PermCheckType.wo_role,):
        if check_type not in (PermCheckType.w_role,):
            client_batch_id = ob.RoleClientBuilder.construct(
                session,
                client=client if check_type == PermCheckType.w_right_batch else None,
            ).client_batch_id
            role = (role, {cst.ConstraintTypes.client_batch_id: client_batch_id})
        roles = [role]
    ob.set_roles(
        session,
        session.passport,
        roles,
    )


@pytest.mark.parametrize(
    'check_type, success',
    [
        (PermCheckType.wo_role, False),
        (PermCheckType.w_role, True),
        (PermCheckType.w_right_batch, True),
        (PermCheckType.w_wrong_batch, False),
    ],
)
def test_check_restricted_fields(session, client, add_func_role, check_type, success):
    client.manual_suspect = 0
    _set_role(session, check_type, client, add_func_role)
    core_ = core.Core(session)

    if success:
        client = core_.create_or_update_client(client.id, {'manual_suspect': '1'})
        assert client.manual_suspect == 1
    else:
        with pytest.raises(exc.PERMISSION_DENIED):
            core_.create_or_update_client(client.id, {'manual_suspect': '1'})


def test_general_field(session, client):
    client.name = 'Garry'
    _set_role(session, PermCheckType.wo_role, client, None)
    core_ = core.Core(session)

    client = core_.create_or_update_client(client.id, {'name': 'Hermione'})
    assert client.name == 'Hermione'


def test_same_restricted_field(session, client):
    client.manual_suspect = 0
    _set_role(session, PermCheckType.wo_role, client, None)
    core_ = core.Core(session)

    client = core_.create_or_update_client(client.id, {'manual_suspect': '0'})
    assert client.manual_suspect == 0
