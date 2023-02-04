# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
import mock
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    is_,
)

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.utils import context_managers as ctx_util
from yb_snout_api.resources.v1.invoice import enums
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice


@pytest.fixture(name='view_inv_role')
def create_view_inv_role():
    return create_role((cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None}))


@pytest.fixture(name='unused_funds_lock_role')
def create_unused_funds_lock_role():
    return create_role((cst.PermissionCode.CHANGE_UNUSED_FUNDS_LOCK, {cst.ConstraintTypes.firm_id: None}))


@pytest.mark.smoke
class TestCaseInvoiceSetUnusedFundsLock(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/set-unused-funds-lock'

    @pytest.mark.parametrize(
        'status',
        [
            enums.UnusedFundsLockType.TRANSFER,
            enums.UnusedFundsLockType.OFF,
        ],
    )
    def test_set_unused_funds_lock(self, invoice, status):
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
                'unused_funds_lock': status.name,
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        response_data = response.json['data']

        assert_that(response_data['unused_funds_lock'], equal_to(status.name))
        assert_that(invoice.unused_funds_lock, equal_to(status.value))

    @pytest.mark.permission
    @pytest.mark.parametrize(
        'checking_perm',
        [
            cst.PermissionCode.VIEW_INVOICES,
            cst.PermissionCode.CHANGE_UNUSED_FUNDS_LOCK,
        ],
    )
    @pytest.mark.parametrize(
        'role_firm_id, res',
        [
            pytest.param(cst.SENTINEL, http.FORBIDDEN,
                         id='wo role'),
            pytest.param(None, http.OK,
                         id='role wo constraints'),
            pytest.param(cst.FirmId.YANDEX_OOO, http.OK,
                         id='role constraints matches firm_id'),
            pytest.param(cst.FirmId.CLOUD, http.FORBIDDEN,
                         id='role constraints don\'t matches firm_id'),
        ],
    )
    @mock.patch('yb_snout_api.utils.context_managers._new_transactional_session', ctx_util.new_rollback_session)
    def test_permission(
            self,
            client,
            admin_role,
            view_inv_role,
            unused_funds_lock_role,
            checking_perm,
            role_firm_id,
            res,
    ):
        from yb_snout_api.resources.v1.invoice import enums

        passport_id = self.test_session.passport.passport_id
        status = enums.UnusedFundsLockType.OFF
        role_perm_map = {
            cst.PermissionCode.VIEW_INVOICES: view_inv_role,
            cst.PermissionCode.CHANGE_UNUSED_FUNDS_LOCK: unused_funds_lock_role,
        }

        roles = [admin_role]
        for perm, role in role_perm_map.items():
            if perm == checking_perm:
                if role_firm_id is not cst.SENTINEL:
                    roles.append((role, {cst.ConstraintTypes.firm_id: role_firm_id}))
            else:
                roles.append(role)

        security.set_roles(roles)

        invoice = create_invoice(client=client)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
                'unused_funds_lock': status.name,
            },
        )
        assert_that(response.status_code, equal_to(res))
        if res is http.FORBIDDEN:
            assert_that(
                response.get_json(),
                has_entries({
                    'error': 'PERMISSION_DENIED',
                    'description': 'User %s has no permission %s.' % (passport_id, checking_perm),
                }),
            )
