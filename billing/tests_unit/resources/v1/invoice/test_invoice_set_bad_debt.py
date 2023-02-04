# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import mock
import http.client as http
import pytest
from decimal import Decimal as D
from hamcrest import assert_that, equal_to, has_entries

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.utils import context_managers as ctx_util
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.act import create_act
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_role, create_admin_role


@pytest.mark.smoke
class TestCaseInvoiceSetBadDebt(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/set-bad-debt'

    def test_set_bad_debt(self, admin_role):
        session = self.test_session

        role = create_role((cst.PermissionCode.MANAGE_BAD_DEBT, {cst.ConstraintTypes.firm_id: None}))
        roles = [
            admin_role,
            (role, {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO}),
        ]
        security.set_roles(roles)

        act = create_act(firm_id=cst.FirmId.YANDEX_OOO)
        act.amount = act.paid_amount + D(10)  # создаем долг
        session.flush()

        commentary = 'Плохой долг счёта (Bad debt for invoice).'
        our_fault = True

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': act.invoice.id,
                'commentary': commentary,
                'our_fault': our_fault,
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        assert_that(len(act.bad_debt_acts), equal_to(1))

        bad_debt = act.bad_debt_acts[0]

        assert_that(bad_debt.commentary, equal_to(commentary))
        assert_that(bad_debt.our_fault, equal_to(our_fault))

    @pytest.mark.permissions
    @mock.patch('yb_snout_api.utils.context_managers._new_transactional_session', ctx_util.new_rollback_session)
    def test_permission_denied(self, admin_role):
        role = create_role((cst.PermissionCode.MANAGE_BAD_DEBT, {cst.ConstraintTypes.firm_id: None}))
        roles = [
            admin_role,
            (role, {cst.ConstraintTypes.firm_id: cst.FirmId.DRIVE}),
        ]
        security.set_roles(roles)

        act = create_act(firm_id=cst.FirmId.YANDEX_OOO)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': act.invoice.id,
                'commentary': 'commentary',
                'our_fault': True,
            },
        )

        assert_that(response.status_code, equal_to(http.FORBIDDEN), 'response code must be FORBIDDEN')
        assert_that(
            response.get_json(),
            has_entries({
                'error': 'PERMISSION_DENIED',
                'description': 'User {} has no permission {}.'.format(
                    self.test_session.passport.passport_id,
                    cst.PermissionCode.MANAGE_BAD_DEBT,
                ),
            }),
        )
