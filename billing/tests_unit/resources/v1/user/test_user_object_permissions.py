# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import http.client as http
from hamcrest import (
    assert_that,
    equal_to,
    contains_inanyorder,
    has_entry,
    has_entries,
)

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.resources import enums
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice


@pytest.fixture(name='view_invoices_role')
def create_view_invoices_role(firm_id=None):
    return create_role((cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: firm_id}))


@pytest.fixture(name='transfer_role')
def create_transfer_role(firm_id=None):
    return create_role((cst.PermissionCode.TRANSFER_FROM_INVOICE, {cst.ConstraintTypes.firm_id: firm_id}))


@pytest.fixture(name='send_role')
def create_send_role(firm_id=None):
    return create_role((cst.PermissionCode.SEND_INVOICES, {cst.ConstraintTypes.firm_id: firm_id}))


@pytest.mark.permissions
class TestCaseUserObjectPermissions(TestCaseApiAppBase):
    BASE_URL = '/v1/user/object-permissions'

    def test_get_object_permissions(self, admin_role, view_invoices_role, transfer_role, send_role):
        roles = [
            admin_role,
            (view_invoices_role, {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO}),
            (transfer_role, {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO}),
            (transfer_role, {cst.ConstraintTypes.firm_id: cst.FirmId.CLOUD}),
            (send_role, {cst.ConstraintTypes.firm_id: cst.FirmId.CLOUD}),
        ]
        security.set_roles(roles)

        invoice = create_invoice(firm_id=cst.FirmId.YANDEX_OOO)
        response = self.test_client.get(self.BASE_URL, {'classname': 'Invoice', 'object_id': invoice.id})
        assert_that(response.status_code, equal_to(http.OK))

        required_perms = [
            cst.PermissionCode.ADMIN_ACCESS,
            cst.PermissionCode.VIEW_INVOICES,
            cst.PermissionCode.TRANSFER_FROM_INVOICE,
        ]
        assert_that(
            response.get_json()['data'],
            contains_inanyorder(*[
                has_entry('code', perm)
                for perm in required_perms
            ]),
        )

    @pytest.mark.parametrize(
        'classname, res, description',
        [
            ('FailClassName', http.INTERNAL_SERVER_ERROR,
             'Invalid parameter for function: Wrong classname=FailClassName for checking permission'),
            ('Invoice', http.NOT_FOUND, 'Invoice with ID -1 not found in DB'),
        ],
        ids=['wrong_classname', 'wrong_object_id'],
    )
    def test_wrong_object_params(self, admin_role, classname, res, description):
        params = {
            'classname': classname,
            'object_id': -1,
        }
        security.set_roles([admin_role])
        response = self.test_client.get(self.BASE_URL, params)
        assert_that(response.status_code, equal_to(res))
        assert_that(
            response.get_json(),
            has_entries({
                'description': description,
            }),
        )
