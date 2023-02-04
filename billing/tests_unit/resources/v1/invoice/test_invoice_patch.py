# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import datetime

import http.client as http
import pytest
from hamcrest import assert_that, equal_to, is_not, has_entries

from balance.constants import PermissionCode

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.contract import create_general_contract
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
from yb_snout_api.tests_unit.fixtures.invoice import (
    create_invoice,
    create_custom_invoice,
)

from tests import object_builder as ob


@pytest.mark.smoke
class TestCaseInvoicePatchContract(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/patch/contract'

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'perms, res',
        [
            ([PermissionCode.PATCH_INVOICE_CONTRACT, PermissionCode.ADMIN_ACCESS], True),
            ([PermissionCode.PATCH_INVOICE_CONTRACT], False),
            ([PermissionCode.ADMIN_ACCESS], False),
        ],
        ids=lambda x: str(x),
    )
    def test_access(self, perms, res):
        role = ob.create_role(self.test_session, *perms)
        security.set_roles([role])

        invoice = create_invoice()
        general_contract = create_general_contract(
            client=invoice.client,
            person=invoice.person,
            services={invoice.service_id},
        )

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
                'contract_id': general_contract.id,
            },
        )
        if res:
            assert_that(response.status_code, equal_to(http.OK))
            assert_that(invoice.contract, equal_to(general_contract))
        else:
            assert_that(response.status_code, equal_to(http.FORBIDDEN))
            assert_that(invoice.contract, equal_to(None))


@pytest.mark.smoke
class TestCaseInvoicePatchDate(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/patch/date'

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'perms, res',
        [
            ([PermissionCode.ADMIN_ACCESS, PermissionCode.ALTER_INVOICE_DATE], True),
            ([PermissionCode.ADMIN_ACCESS], False),
            ([PermissionCode.ALTER_INVOICE_DATE], False),
        ],
        ids=lambda x: str(x),
    )
    def test_access(self, perms, res):
        role = ob.create_role(self.test_session, *perms)
        security.set_roles([role])

        invoice = create_invoice()
        old_dt = invoice.dt
        new_dt = (datetime.datetime.now() + datetime.timedelta(666)).replace(microsecond=0)

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
                'new_dt': new_dt.strftime('%Y-%m-%dT%H:%M:%S'),
            },
        )
        if res:
            assert_that(response.status_code, equal_to(http.OK))
            assert_that(invoice.dt, equal_to(new_dt))
        else:
            assert_that(response.status_code, equal_to(http.FORBIDDEN))
            assert_that(invoice.dt, equal_to(old_dt))


@pytest.mark.smoke
class TestCaseInvoicePatchSum(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/patch/sum'

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'perms, res',
        [
            ([PermissionCode.ADMIN_ACCESS, PermissionCode.ALTER_INVOICE_SUM], True),
            ([PermissionCode.ADMIN_ACCESS], False),
            ([PermissionCode.ALTER_INVOICE_DATE], False),
        ],
        ids=lambda x: str(x),
    )
    def test_access(self, perms, res, client):
        role = ob.create_role(self.test_session, *perms)
        security.set_roles([role])

        order = ob.OrderBuilder(client=client).build(self.test_session).obj
        invoice = create_custom_invoice({order: 10}, client=client)
        old_sum = invoice.total_sum
        new_sum = old_sum * 2

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
                'new_sum': new_sum,
            },
        )
        if res:
            assert_that(response.status_code, equal_to(http.OK))
            assert_that(invoice.total_sum, equal_to(new_sum))
        else:
            assert_that(response.status_code, equal_to(http.FORBIDDEN))
            assert_that(invoice.total_sum, equal_to(old_sum))
