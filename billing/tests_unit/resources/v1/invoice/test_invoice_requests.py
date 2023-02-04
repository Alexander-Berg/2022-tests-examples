# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
import hamcrest as hm

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.resources import enums
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_request
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_role_client, create_client


@pytest.fixture(name='issue_request_role')
def create_issue_request_role():
    return create_role(
        (
            cst.PermissionCode.ISSUE_INVOICES,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.mark.smoke
class TestCaseRequests(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/requests'

    @pytest.mark.slow
    def test_requests(self, request_):
        from yb_snout_api.resources.v1.invoice import enums as invoice_enums

        params = {
            'client_id': request_.client_id,
            'request_id': request_.id,
            'sort_key': invoice_enums.RequestsSortKeyType.DT.name,
            'sort_order': enums.SortOrderType.ASC.name,
            'pagination_pn': 1,
            'pagination_ps': 10,
        }

        response = self.test_client.get(self.BASE_API, params)
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        hm.assert_that(
            data,
            hm.has_entries({
                'total_row_count': 1,
                'items': hm.contains(
                    hm.has_entries({
                        'client_id': request_.client.id,
                        'oper_id': request_.passport_id,
                        'request_id': request_.id,
                    }),
                ),
            }),
        )

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'match_client',
        [True, False, None],
    )
    def test_permissions_client(self, admin_role, issue_request_role, client, match_client):
        roles = [admin_role]
        if match_client is not None:
            client_batch_id = create_role_client(client=client if match_client else None).client_batch_id
            roles.append(
                (issue_request_role, {cst.ConstraintTypes.client_batch_id: client_batch_id}),
            )
        security.set_roles(roles)

        request = create_request(client=client)
        res = self.test_client.get(self.BASE_API, {'request_id': request.id})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data', {})
        items_match = hm.contains(hm.has_entries({'request_id': request.id})) if match_client else hm.empty()
        hm.assert_that(
            data,
            hm.has_entries({
                'total_row_count': 1 if match_client else 0,
                'items': items_match,
            }),
        )

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'request_firm_id, ans',
        [
            (None, True),
            (cst.FirmId.YANDEX_OOO, True),
            (cst.FirmId.DRIVE, False),
        ],
    )
    def test_permissions_firm(self, admin_role, issue_request_role, client, request_firm_id, ans):
        roles = [
            admin_role,
            (issue_request_role, {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO}),
        ]
        security.set_roles(roles)

        request = create_request(client=client, firm_id=request_firm_id)

        res = self.test_client.get(self.BASE_API, {'request_id': request.id})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data', {})
        items_match = hm.contains(hm.has_entries({'request_id': request.id})) if ans else hm.empty()
        hm.assert_that(
            data,
            hm.has_entries({
                'total_row_count': 1 if ans else 0,
                'items': items_match,
            }),
        )
