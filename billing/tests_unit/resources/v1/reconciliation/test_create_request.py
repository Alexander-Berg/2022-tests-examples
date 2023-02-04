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
import json

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
class TestCreateRequest(TestCaseApiAppBase):
    BASE_API = '/v1/reconciliation/create-request'

    def test_create_new_request_by_person_and_firm(self, client):
        self.test_session.config.__dict__['RECONCILIATION_REPORT_FIRMS'] = [cst.FirmId.YANDEX_OOO]
        security.set_roles([])
        security.set_passport_client(client)

        person = create_person(client=client)
        external_id = make_yadoc_api_response()
        email = 'james@bond.com'

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'person_id': person.id,
                'firm_id': cst.FirmId.YANDEX_OOO,
                'dt_from': '2020-10-01T00:00:00',
                'dt_to': '2020-10-31T00:00:00',
                'email': email,
            },
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        data = response.get_json()['data']
        reconciliation_request = (
            self.test_session
            .query(mapper.ReconciliationRequest)
            .filter(mapper.ReconciliationRequest.external_id == external_id)
            .one()
        )
        hm.assert_that(
            data,
            hm.has_entries({
                'id': reconciliation_request.id,
                'dt': hm.not_none(),
                'contract': None,
                'person': hm.has_entries({'id': person.id}),
                'firm': hm.has_entries({'id': cst.FirmId.YANDEX_OOO}),
                'dt_range': hm.has_entries({'dt_from': '2020-10-01T00:00:00', 'dt_to': '2020-10-31T00:00:00'}),
                'email': email,
                'status': cst.ReconciliationRequestStatus.NEW,
                'hidden': None,
            }),
        )

        last_request = httpretty.last_request()
        hm.assert_that(
            json.loads(last_request.parsed_body),
            hm.has_entries({
                'person_id': person.id,
                'organization_id': 121,  # это ООО Яндекс у oebs
                'email': email,
                'period_from': '2020-10-01',
                'period_to': '2020-10-31',
            }),
        )


    def test_create_new_request_by_contract(self, client):
        security.set_roles([])
        security.set_passport_client(client)

        contract = create_general_contract(client=client)
        external_id = make_yadoc_api_response()
        email = 'james@bond.com'
        self.test_session.config.__dict__['RECONCILIATION_REPORT_FIRMS'] = [contract.firm.id]

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'contract_id': contract.id,
                'dt_from': '2020-10-01T00:00:00',
                'dt_to': '2020-10-31T00:00:00',
                'email': email,
            },
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        data = response.get_json()['data']
        reconciliation_request = (
            self.test_session
            .query(mapper.ReconciliationRequest)
            .filter(mapper.ReconciliationRequest.external_id == external_id)
            .one()
        )
        hm.assert_that(
            data,
            hm.has_entries({
                'id': reconciliation_request.id,
                'dt': hm.not_none(),
                'contract': hm.has_entries({'id': contract.id, 'external_id': contract.external_id}),
                'person': hm.has_entries({'id': contract.person_id}),
                'firm': hm.has_entries({'id': contract.firm.id}),
                'dt_range': hm.has_entries({'dt_from': '2020-10-01T00:00:00', 'dt_to': '2020-10-31T00:00:00'}),
                'email': email,
                'status': cst.ReconciliationRequestStatus.NEW,
                'hidden': None,
            }),
        )

    def test_invalid_contract_params(self, general_contract, person):
        self.test_session.config.__dict__['RECONCILIATION_REPORT_FIRMS'] = [cst.FirmId.MARKET]
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'contract_id': general_contract.id,
                'person_id': person.id,
                'firm_id': cst.FirmId.MARKET,
                'dt_from': '2020-10-01T00:00:00',
                'dt_to': '2020-10-31T00:00:00',
                'email': 'woody@ya.ru',
            },
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.BAD_REQUEST))
        hm.assert_that(
            response.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'form_errors': hm.has_entries({
                    'contract_id': hm.contains(
                        hm.has_entries({
                            'error': 'PERSON_CONTRACT_DATA_VALIDATION_ERROR',
                            'description': 'Data from contract doesn`t match contract_id and organization_id fields.',
                        }),
                    ),
                }),
            }),
        )

    def test_invalid_response_from_api(self, person):
        self.test_session.config.__dict__['RECONCILIATION_REPORT_FIRMS'] = [cst.FirmId.YANDEX_OOO]
        external_id = make_yadoc_api_response(http.BAD_GATEWAY)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'person_id': person.id,
                'firm_id': cst.FirmId.YANDEX_OOO,
                'dt_from': '2020-10-01T00:00:00',
                'dt_to': '2020-10-31T00:00:00',
                'email': 'james@bond.com',
            },
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.INTERNAL_SERVER_ERROR))
        hm.assert_that(
            response.get_json(),
            hm.has_entries({
                'error': 'RECONCILIATION_REPORT_CONNECTION_ERROR',
            }),
        )

    def test_firm_is_not_in_config(self, person):
        self.test_session.config.__dict__['RECONCILIATION_REPORT_FIRMS'] = [cst.FirmId.MARKET]
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'person_id': person.id,
                'firm_id': cst.FirmId.YANDEX_OOO,
                'dt_from': '2020-10-01T00:00:00',
                'dt_to': '2020-10-31T00:00:00',
                'email': 'james@bond.com',
            },
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.BAD_REQUEST))
        hm.assert_that(
            response.get_json(),
            hm.has_entries({
                'error': 'RECONCILIATION_REPORT_BAD_REQUEST_ERROR',
            })
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

        external_id = make_yadoc_api_response()
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'person_id': create_person(client=client).id,
                'firm_id': cst.FirmId.YANDEX_OOO,
                'dt_from': '2020-10-01T00:00:00',
                'dt_to': '2020-10-31T00:00:00',
                'email': 'james@bond.com',
            },
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK if can_issue_inv else http.FORBIDDEN))
