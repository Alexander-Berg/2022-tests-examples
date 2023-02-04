# -*- coding: utf-8 -*-
import pytest

from balance import constants as cst, mapper
from tests import object_builder as ob

PERMISSION = 'Permission'
WRONG_PERMISSION = 'WrongPermission'


class CheckType(cst.Enum):
    object = 'object'
    query = 'query'


def create_passport(session, roles, client=None, patch_session=True, **kwargs):
    passport = ob.create_passport(session, *roles, patch_session=patch_session, client=client, **kwargs)
    return passport


@pytest.fixture(name='wrong_role')
def create_wrong_role(session):
    return ob.create_role(
        session,
        (WRONG_PERMISSION, {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None}),
    )


@pytest.fixture(name='right_role')
def create_right_role(session):
    return ob.create_role(
        session,
        (PERMISSION, {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None}),
    )


@pytest.fixture(name='view_contract_role')
def create_view_contract_role(session):
    return ob.create_role(
        session,
        (
            cst.PermissionCode.VIEW_CONTRACTS,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.fixture(name='direct_limited_role')
def create_direct_limited_role(session):
    return ob.create_role(session, cst.PermissionCode.DIRECT_LIMITED)


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder.construct(session)


@pytest.fixture(name='role_client')
def create_role_client(session, client=None):
    return ob.RoleClientBuilder.construct(
        session,
        client=client or create_client(session),
    )


@pytest.fixture(name='firm_id')
def create_firm_id(session):
    return ob.FirmBuilder.construct(session).id


@pytest.fixture(name='contract')
def create_contract(session, client=None, firm=cst.FirmId.YANDEX_OOO):
    client = client or ob.ClientBuilder.construct(session)
    contract = ob.ContractBuilder.construct(
        session,
        client=client,
        firm=firm,
    )
    session.flush()
    return contract


def create_role(session, *permissions):
    return ob.create_role(session, *permissions)


def check_perm_constraints_by_check_type(session, obj, passport, check_type):
    if check_type == CheckType.object:
        return obj.check_perm_constraints(passport, PERMISSION)
    else:
        filters = mapper.Contract.get_perm_constraints_query_filter(passport, PERMISSION)
        return (
            session.query(mapper.Contract)
            .filter(mapper.Contract.id == obj.id, filters)
            .exists()
        )
