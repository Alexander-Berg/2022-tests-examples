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


@pytest.fixture(name='view_persons_role')
def create_view_persons_role(session):
    return ob.create_role(session, (cst.PermissionCode.VIEW_PERSONS, {cst.ConstraintTypes.client_batch_id: None}))


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder.construct(session)


@pytest.fixture(name='role_client')
def create_role_client(session, client=None):
    return ob.RoleClientBuilder.construct(
        session,
        client=client or create_client(session),
    )


@pytest.fixture(name='person')
def create_person(session, client):
    return ob.PersonBuilder.construct(session, client=client)


def check_perm_constraints_by_check_type(session, obj, passport, check_type):
    if check_type == CheckType.object:
        return obj.check_perm_constraints(passport, PERMISSION)
    else:
        filters = mapper.Person.get_perm_constraints_query_filter(passport, PERMISSION)
        return (
            session.query(mapper.Person)
            .filter(
                mapper.Person.id == obj.id,
                filters,
            )
            .exists()
        )
