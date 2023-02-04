# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import pytest
import allure

from balance.constants import PermissionCode, RoleName, ConstraintTypes
from tests import object_builder as ob
import balance.mapper as mapper

from brest.core.tests import utils as test_utils


def get_role(role_id):
    session = test_utils.get_test_session()
    return ob.Getter(mapper.Role, role_id).build(session).obj


def create_role(*permissions, **kwargs):
    session = test_utils.get_test_session()
    return ob.create_role(session, *permissions, **kwargs)


@pytest.fixture(name='passport')
def create_passport(roles=None, **kwargs):
    session = test_utils.get_test_session()
    roles = roles or []
    return ob.create_passport(session, *roles, **kwargs)


@pytest.fixture(name='representative')
def create_representative(**kwargs):
    session = test_utils.get_test_session()
    return ob.RoleClientPassportBuilder.construct(session, **kwargs)


@pytest.fixture(name='admin_role')
@allure.step('create admin role')
def create_admin_role():
    return create_role(PermissionCode.ADMIN_ACCESS)


@pytest.fixture(name='support_role')
@allure.step('create admin role')
def create_support_role():
    return create_role(PermissionCode.ADMIN_ACCESS, PermissionCode.BILLING_SUPPORT)

@pytest.fixture(name='issue_inv_role')
def create_issue_inv_role():
    return create_role(
        (
            PermissionCode.ISSUE_INVOICES,
            {ConstraintTypes.firm_id: None, ConstraintTypes.client_batch_id: None},
        ),
    )

@pytest.fixture(name='client_role')
@allure.step('get client role')
def get_client_role():
    session = test_utils.get_test_session()
    return ob.Getter(mapper.Role, RoleName.CLIENT).build(session).obj


@pytest.fixture(name='agency_role')
@allure.step('get agency role')
def get_agency_role():
    session = test_utils.get_test_session()
    return ob.Getter(mapper.Role, RoleName.AGENCY).build(session).obj


@pytest.fixture(name='view_client_role')
def create_view_client_role():
    return create_role(
        (
            PermissionCode.VIEW_CLIENTS,
            {ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.fixture(name='view_inv_role')
def create_view_inv_role():
    return create_role(
        (
            PermissionCode.VIEW_INVOICES,
            {ConstraintTypes.firm_id: None, ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.fixture(name='do_inv_trans_role')
def create_do_inv_trans_role():
    return create_role(
        (
            PermissionCode.DO_INVOICE_TRANSFER,
            {ConstraintTypes.firm_id: None, ConstraintTypes.client_batch_id: None},
        ),
    )
