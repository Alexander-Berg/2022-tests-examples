# -*- coding: utf-8 -*-

from __future__ import absolute_import
from __future__ import division

import datetime

from future import standard_library

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http
import httpretty

from balance.application import getApplication
from balance import constants as cst, mapper
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.person import create_person
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.contract import create_general_contract
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_issue_inv_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.reconciliation import create_reconciliation_request


def make_yadoc_api_response(status_code=http.OK, resp_text=None):
    url = getApplication().get_component_cfg('reconciliation_report')['Url']
    external_id = resp_text or 'agent-007-%s' % ob.get_big_number()
    httpretty.register_uri(
        httpretty.POST,
        '%sreport' % (url,),
        status=status_code,
        body=external_id,
    )
    return external_id


@pytest.mark.smoke
@pytest.mark.usefixtures('httpretty_enabled_fixture')
class TestCloneRequest(TestCaseApiAppBase):
    BASE_API = '/v1/reconciliation/clone-request'

    def test_clone(self, client):
        self.test_session.config.__dict__['RECONCILIATION_REPORT_FIRMS'] = [cst.FirmId.YANDEX_OOO]
        security.set_roles([])
        security.set_passport_client(client)

        reconciliation_request = create_reconciliation_request(client=client, firm_id=cst.FirmId.YANDEX_OOO)
        external_id = make_yadoc_api_response()

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'reconciliation_request_id': reconciliation_request.id,
            },
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        data = response.get_json()['data']
        reconciliation_request_2 = (
            self.test_session
            .query(mapper.ReconciliationRequest)
            .filter(mapper.ReconciliationRequest.external_id == external_id)
            .one()
        )
        hm.assert_that(
            data,
            hm.has_entries({
                'id': reconciliation_request_2.id,
            }),
        )
        hm.assert_that(
            reconciliation_request_2,
            hm.has_properties({
                'person_id': reconciliation_request.person_id,
                'firm_id': reconciliation_request.firm_id,
                'contract_id': reconciliation_request.contract_id,
                'hidden': reconciliation_request.hidden,
                'email': reconciliation_request.email,
                'dt_from': reconciliation_request.dt_from,
                'dt_to': reconciliation_request.dt_to,
                'dt': hm.greater_than_or_equal_to(reconciliation_request.dt),
            }),
        )

    def test_invalid_response_from_api(self, client):
        self.test_session.config.__dict__['RECONCILIATION_REPORT_FIRMS'] = [cst.FirmId.YANDEX_OOO]
        reconciliation_request = create_reconciliation_request(client=client, firm_id=cst.FirmId.YANDEX_OOO)
        make_yadoc_api_response(http.BAD_GATEWAY)

        response = self.test_client.secure_post(
            self.BASE_API,
            data={'reconciliation_request_id': reconciliation_request.id},
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.INTERNAL_SERVER_ERROR))
        hm.assert_that(
            response.get_json(),
            hm.has_entries({
                'error': 'RECONCILIATION_REPORT_CONNECTION_ERROR',
            }),
        )

    @pytest.mark.parametrize(
        'can_issue_inv',
        [True, False],
    )
    def test_permission(self, client, admin_role, issue_inv_role, can_issue_inv):
        """Создавать новые запросы можно только с правом IssueInvoices.
        """
        self.test_session.config.__dict__['RECONCILIATION_REPORT_FIRMS'] = [cst.FirmId.YANDEX_OOO]
        roles = [admin_role]
        if can_issue_inv:
            roles.append(issue_inv_role)
        security.set_roles(roles)

        reconciliation_request = create_reconciliation_request(client=client, firm_id=cst.FirmId.YANDEX_OOO)
        external_id = make_yadoc_api_response()

        response = self.test_client.secure_post(
            self.BASE_API,
            data={'reconciliation_request_id': reconciliation_request.id},
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK if can_issue_inv else http.FORBIDDEN))
