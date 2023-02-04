# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
from hamcrest import assert_that, equal_to

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.resources.v1.invoice import enums

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice

from tests import object_builder as ob


@pytest.mark.smoke
class TestCaseInvoicePreliminaryAction(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/preliminary-action'

    @pytest.mark.parametrize(
        'action_type, field, value',
        [
            (enums.PreliminaryAction.CONFIRM, 'status_id', cst.InvoiceStatusId.CONFIRMED),
            (enums.PreliminaryAction.DECLINE, 'hidden', 2),  # никто не знает...
        ],
    )
    def test_actions(
            self,
            invoice,
            action_type,
            field,
            value,
    ):
        invoice.status_id = cst.InvoiceStatusId.PRELIMINARY

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'action_type': action_type.name,
                'invoice_id': invoice.id,
            },
        )
        assert_that(response.status_code, equal_to(http.OK), u'Response code must be OK')

        assert_that(
            getattr(invoice, field, value),
            equal_to(value),
        )

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'perms, res',
        [
            ([cst.PermissionCode.ADMIN_ACCESS, cst.PermissionCode.CHANGE_REPAYMENTS_STATUS], True),
            ([cst.PermissionCode.ADMIN_ACCESS], False),
            ([cst.PermissionCode.CHANGE_REPAYMENTS_STATUS], False),
        ],
        ids=lambda x: str(x),
    )
    def test_access(self, perms, res):
        role = ob.create_role(self.test_session, *perms)
        security.set_roles([role])

        invoice = create_invoice()
        invoice.status_id = cst.InvoiceStatusId.PRELIMINARY

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'action_type': enums.PreliminaryAction.CONFIRM.name,
                'invoice_id': invoice.id,
            },
        )

        if res:
            assert_that(response.status_code, equal_to(http.OK))
            assert_that(invoice.status_id, equal_to(cst.InvoiceStatusId.CONFIRMED))
        else:
            assert_that(response.status_code, equal_to(http.FORBIDDEN))
            assert_that(invoice.status_id, equal_to(cst.InvoiceStatusId.PRELIMINARY))
