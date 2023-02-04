# -*- coding: utf-8 -*-

from __future__ import absolute_import
from __future__ import division

import datetime

from future import standard_library

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.person import create_person
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.reconciliation import create_reconciliation_request


@pytest.fixture(name='view_inv_role')
def create_view_inv_role():
    return create_role(
        (
            cst.PermissionCode.VIEW_INVOICES,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.mark.smoke
class TestRequests(TestCaseApiAppBase):
    BASE_API = '/v1/reconciliation/requests'

    @pytest.mark.parametrize(
        'pn, ps, start, end, reverse_',
        [
            pytest.param(1, 3, 0, 3, False, id='first page'),
            pytest.param(3, 2, 4, 6, False, id='last page'),
            pytest.param(4, 2, 6, 8, False, id='after last page'),
            pytest.param(1, 6, 0, 6, True, id='reversed'),
        ],
    )
    def test_create_request_mocked_api(self, client, pn, ps, start, end, reverse_):
        security.set_roles([])
        security.set_passport_client(client)

        start_dt = datetime.datetime(2020, 10, 1)
        person = create_person(client=client)

        requests = [
            create_reconciliation_request(
                client=client,
                person=person,
                firm_id=cst.FirmId.YANDEX_OOO,
                dt=start_dt + datetime.timedelta(days=delta),
            )
            for delta in range(6)
        ]

        response = self.test_client.get(
            self.BASE_API,
            {
                'pagination_pn': pn,
                'pagination_ps': ps,
                'sort_order': 'DESC' if reverse_ else 'ASC',
            },
            is_admin=False,
        )

        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        if reverse_:
            requests = list(reversed(requests))
        requests = requests[start:end]

        data = response.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'items': hm.contains(*[
                    hm.has_entries({'id': r.id})
                    for r in requests
                ]),
                'total_count': 6,
            }),
        )

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'w_role, match_client, w_firm, ans',
        [
            pytest.param(False, False, False, False, id='wo role'),
            pytest.param(True, True, False, False, id='wrong firm'),
            pytest.param(True, False, True, False, id='wrong client'),
            pytest.param(True, True, True, True, id='access'),
        ],
    )
    def test_permission(self, client, view_inv_role, w_role, match_client, w_firm, ans):
        roles = []
        if w_role:
            role_client = create_role_client(client=client if match_client else None)
            roles.append((
                view_inv_role,
                {
                    cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO if w_firm else cst.FirmId.MARKET,
                    cst.ConstraintTypes.client_batch_id: role_client.client_batch_id,
                },
            ))
        security.set_roles(roles)

        reconciliation_request = create_reconciliation_request(client=client, firm_id=cst.FirmId.YANDEX_OOO)
        res = self.test_client.get(
            self.BASE_API,
            {'client_id': client.id},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(
            res.get_json()['data'],
            hm.has_entries({
                'items': (
                    hm.contains(hm.has_entries({'id': reconciliation_request.id}))
                    if ans else
                    hm.empty()
                )
            }),
        )
