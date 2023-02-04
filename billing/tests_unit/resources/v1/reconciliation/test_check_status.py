# -*- coding: utf-8 -*-

from __future__ import absolute_import
from __future__ import division

import datetime
import json

from future import standard_library

standard_library.install_aliases()

import pytest
import json
import httpretty
import hamcrest as hm
import http.client as http

from balance.application import getApplication
from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.utils import clean_dict
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.reconciliation import create_reconciliation_request


def make_yadoc_api_response(id_, status_code=http.OK, resp_json=None):
    url = getApplication().get_component_cfg('reconciliation_report')['Url']
    httpretty.register_uri(
        httpretty.GET,
        '%sreport/%s' % (url, str(id_)),
        status=status_code,
        body=json.dumps(resp_json),
    )


@pytest.fixture(name='view_inv_role')
def create_view_inv_role():
    return create_role(
        (
            cst.PermissionCode.VIEW_INVOICES,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.mark.smoke
@pytest.mark.usefixtures('httpretty_enabled_fixture')
class TestCheckStatus(TestCaseApiAppBase):
    BASE_API = '/v1/reconciliation/check-status'

    @pytest.mark.parametrize(
        'req_status, msg',
        [
            (cst.ReconciliationRequestStatus.IN_PROGRESS, None),
            (cst.ReconciliationRequestStatus.COMPLETED, None),
            (cst.ReconciliationRequestStatus.FAILED, 'smth goes wrong'),
        ],
        ids=lambda x, _: x,
    )
    def test_base(self, client, req_status, msg):
        security.set_roles([])
        security.set_passport_client(client)

        reconciliation_request = create_reconciliation_request(client=client)
        make_yadoc_api_response(
            reconciliation_request.external_id,
            http.OK,
            clean_dict({'status': req_status, 'message': msg}),
        )

        response = self.test_client.secure_post(
            self.BASE_API,
            data={'reconciliation_request_id': reconciliation_request.id},
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        data = response.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'id': reconciliation_request.id,
                'status': req_status,
            }),
        )

        self.test_session.refresh(reconciliation_request)
        assert reconciliation_request.status == req_status
        assert reconciliation_request.error == msg

    def test_wrong_status(self, reconciliation_request):
        make_yadoc_api_response(
            reconciliation_request.external_id,
            http.OK,
            {'status': 'NEW_STATUS'},
        )

        response = self.test_client.secure_post(
            self.BASE_API,
            data={'reconciliation_request_id': reconciliation_request.id},
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.INTERNAL_SERVER_ERROR))

        hm.assert_that(
            response.get_json(),
            hm.has_entries({
                'error': 'RECONCILIATION_REPORT_API_CALL_ERROR',
                'description': 'Error while calling api:'
                               ' unexpected reconciliation request status received, status: NEW_STATUS',
            }),
        )

    def test_problem_w_api(self, reconciliation_request):
        make_yadoc_api_response(
            reconciliation_request.external_id,
            http.BAD_GATEWAY,
        )
        response = self.test_client.secure_post(
            self.BASE_API,
            data={'reconciliation_request_id': reconciliation_request.id},
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.INTERNAL_SERVER_ERROR))

        hm.assert_that(
            response.get_json(),
            hm.has_entries({'error': 'RECONCILIATION_REPORT_CONNECTION_ERROR'}),
        )

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'w_role, w_firm, match_client, ans',
        [
            pytest.param(False, False, False, http.FORBIDDEN, id='wo_role'),
            pytest.param(True, False, True, http.FORBIDDEN, id='wo_firm'),
            pytest.param(True, True, False, http.FORBIDDEN, id='wo_client'),
            pytest.param(True, True, True, http.OK, id='master'),
        ],
    )
    def test_permission(self, view_inv_role, client, w_role, w_firm, match_client, ans):
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
        make_yadoc_api_response(
            reconciliation_request.external_id,
            http.OK,
            {'status': cst.ReconciliationRequestStatus.COMPLETED},
        )

        response = self.test_client.secure_post(
            self.BASE_API,
            data={'reconciliation_request_id': reconciliation_request.id},
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(ans))
