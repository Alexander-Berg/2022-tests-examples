# -*- coding: utf-8 -*-
import pytest
import sqlalchemy as sa

from balance import constants as cst, mapper
from tests import object_builder as ob

PERMISSION = 'Permission'
WRONG_PERMISSION = 'WrongPermission'
ORDER_FIRM_IDS = [
    cst.FirmId.YANDEX_OOO,
    cst.FirmId.CLOUD,
]


class CheckType(cst.Enum):
    object = 'object'
    query = 'query'


def create_passport(session, roles, client=None, patch_session=True, **kwargs):
    passport = ob.create_passport(session, *roles, patch_session=patch_session, client=client, **kwargs)
    return passport


@pytest.fixture(name='wrong_role')
def create_wrong_role(session):
    return ob.create_role(session, (WRONG_PERMISSION, {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None}))


@pytest.fixture(name='right_role')
def create_right_role(session):
    return ob.create_role(session, (PERMISSION, {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None}))


@pytest.fixture(name='view_orders_role')
def create_view_orders_role(session):
    return ob.create_role(session, (cst.PermissionCode.VIEW_ORDERS, {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None}))


@pytest.fixture(name='yamoney_role')
def create_yamoney_role(session):
    return ob.create_role(session, (cst.PermissionCode.YAMONEY_SUPPORT_ACCESS, {cst.ConstraintTypes.firm_id: None}))


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder.construct(session)


@pytest.fixture(name='role_client')
def create_role_client(session, client=None):
    return ob.RoleClientBuilder.construct(
        session,
        client=client or create_client(session),
    )


@pytest.fixture(name='agency')
def create_agency(session):
    return ob.ClientBuilder.construct(session, is_agency=True)


@pytest.fixture(name='order')
def create_order(session, client=None, service_id=cst.ServiceId.DIRECT, agency=None):
    return ob.OrderBuilder.construct(
        session,
        agency=agency,
        client=client or create_client(session),
        service_id=service_id,
    )


@pytest.fixture(name='order_w_firms')
def create_order_w_firms(session, client=None, agency=None):
    order = ob.OrderBuilder.construct(
        session,
        agency=agency,
        client=client or ob.ClientBuilder()
    )
    for firm_id in ORDER_FIRM_IDS:
        order.add_firm(firm_id)
    session.flush()
    return order


def check_perm_constraints_by_check_type(session, obj, passport, check_type):
    if check_type == CheckType.object:
        return obj.check_perm_constraints(passport, PERMISSION)
    else:
        filters = mapper.Order.get_perm_constraints_query_filter(passport, PERMISSION)
        return (
            session.query(mapper.Order)
                .filter(mapper.Order.id == obj.id,
                        filters)
                .exists()
        )
