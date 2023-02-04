# -*- coding: utf-8 -*-
import pytest
from balance import mapper
from balance import constants as cst

from tests import object_builder as ob

PERMISSION = 'Permission'
WRONG_PERMISSION = 'WrongPermission'


class CheckType(cst.Enum):
    object = 'object'
    query = 'query'


@pytest.fixture(name='wrong_role')
def create_wrong_role(session):
    return ob.create_role(session, (WRONG_PERMISSION, {cst.ConstraintTypes.client_batch_id: None}))


@pytest.fixture(name='view_clients_role')
def create_view_clients_role(session):
    return ob.create_role(session, (cst.PermissionCode.VIEW_CLIENTS, {cst.ConstraintTypes.client_batch_id: None}))


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder.construct(session)


@pytest.fixture(name='role_client')
def create_role_client(session, client=None):
    return ob.RoleClientBuilder.construct(
        session,
        client=client or create_client(session),
    )


def check_perm_constraints_by_check_type(session, obj, passport, check_type):
    if check_type == CheckType.object:
        return obj.check_perm_constraints(passport, PERMISSION)
    else:
        filters = mapper.Client.get_perm_constraints_query_filter(passport, PERMISSION)
        return (
            session.query(mapper.Client)
            .filter(
                mapper.Client.id == obj.id,
                filters,
            )
            .exists()
        )
