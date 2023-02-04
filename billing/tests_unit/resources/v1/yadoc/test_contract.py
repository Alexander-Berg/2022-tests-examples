# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import datetime
import mock
import httpretty
import json
import hamcrest
import http.client as http
from balance import constants as cst
from brest.core.tests import utils as test_utils
from brest.core.tests import security

from tests import object_builder as ob
from tests.balance_tests.yadoc.test_yadoc import MockResponse, EMPTY_RESPONSE
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role
from yb_snout_api.tests_unit.fixtures.permissions import create_role


def trunc_date(date):
    return datetime.datetime.combine(date.date(), datetime.time(0))


NOW = trunc_date(datetime.datetime.now())
NOW_ISO = NOW.isoformat()
YEAR_AGO = NOW - datetime.timedelta(days=365)
YEAR_AGO_ISO = YEAR_AGO.isoformat()

YADOC_URL = "https://yadoc-test.mba.yandex-team.ru/public/api/"
YADOC_URL_DOCUMENTS = YADOC_URL + "v1/documents"
YADOC_URL_DOWNLOAD = YADOC_URL + "v1/documents/download"


@pytest.fixture(autouse=True)
def patching():
    with mock.patch('balance.api.yadoc.YaDocApi._get_tvm_ticket', return_value='666'), \
         mock.patch('butils.application.plugins.components_cfg.get_component_cfg',
                    return_value={'Url': YADOC_URL, 'PageSize': 20}):
        yield


@pytest.fixture(name='client')
def create_client(passport=None, **kwargs):
    session = test_utils.get_test_session()
    return ob.ClientBuilder.construct(session)


@pytest.mark.usefixtures('httpretty_enabled_fixture')
class TestYadocPartnerContracts(TestCaseApiAppBase):
    BASE_API = u'/v1/yadoc/contract/partner-contracts'

    @pytest.mark.permissions
    @pytest.mark.parametrize('call_w_client', [True, False])
    @pytest.mark.parametrize('passport_w_client', [True, False])
    def test_client_forbidden(self, passport_w_client, client, call_w_client):

        security.set_roles([])
        if passport_w_client:
            security.set_passport_client(client)

        contract = ob.ContractBuilder.construct(self.test_session, client=client, ctype='PARTNERS')

        if call_w_client:
            response = self.test_client.get(self.BASE_API, {'client_id': contract.client_id}, is_admin=False)
        else:
            response = self.test_client.get(self.BASE_API, is_admin=False)

        if passport_w_client:
            hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK),
                                 'Response code must be OK')
        else:
            if call_w_client:
                hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.FORBIDDEN),
                                     'Response code must be FORBIDDEN')
            else:
                hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.NOT_FOUND),
                                     'Response code must be NOT_FOUND')

    @pytest.mark.permissions
    @pytest.mark.parametrize('w_perm', [
        True,
        False
    ])
    def test_perm_forbidden(self, w_perm, client):
        if w_perm:
            role = create_role([cst.PermissionCode.VIEW_CLIENTS,
                                {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None}])
            security.set_roles([role])
        else:
            security.set_roles([])

        contract = ob.ContractBuilder.construct(self.test_session, client=client)
        response = self.test_client.get(self.BASE_API, {'client_id': contract.client_id},
                                        is_admin=False)
        if w_perm:
            hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK),
                                 'Response code must be OK')
        else:
            hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.FORBIDDEN),
                                 'Response code must be FORBIDDEN')

    def test_empty_response(self, client):
        response = self.test_client.get(self.BASE_API, {'client_id': client.id})
        hamcrest.assert_that(
            response.json['data'],
            hamcrest.has_entries(items=hamcrest.empty()))

    @pytest.mark.parametrize('is_agency', [
        True,
        False])
    @pytest.mark.parametrize('agent_scheme', [
        1,
        0
    ])
    @pytest.mark.parametrize('ctype', ['GENERAL', 'SPENDABLE', 'PARTNERS', 'DISTRIBUTION'])
    def test_contracts_depend_on_ctype(self, client, ctype, is_agency, agent_scheme):
        client.is_agency = is_agency
        service = ob.ServiceBuilder.construct(self.test_session)
        ob.ThirdpartyServiceBuilder.construct(self.test_session, id=service.id, agent_scheme=agent_scheme)
        self.test_session.flush()
        contract = ob.ContractBuilder.construct(self.test_session, client=client, ctype=ctype, dt=NOW,
                                                finish_dt=None, services=[service.id])
        response = self.test_client.get(self.BASE_API, {'client_id': client.id})
        if ctype == 'GENERAL' and not is_agency and not agent_scheme:
            hamcrest.assert_that(
                response.json['data'],
                hamcrest.has_entries(items=hamcrest.empty()))

        else:
            hamcrest.assert_that(
                response.json['data'],
                hamcrest.has_entries(items=hamcrest.contains(hamcrest.has_entries(external_id=contract.external_id,
                                                                                  id=contract.id,
                                                                                  type=contract.ctype.type,
                                                                                  dt=NOW_ISO,
                                                                                  end_dt=None,
                                                                                  is_signed=True))))

    @pytest.mark.parametrize('is_signed', [None, YEAR_AGO])
    def test_contracts_depend_on_signed(self, client, is_signed):
        contract = ob.ContractBuilder.construct(self.test_session, client=client, ctype='SPENDABLE',
                                                is_signed=is_signed, end_dt=NOW, dt=is_signed)
        response = self.test_client.get(self.BASE_API, {'client_id': client.id})
        if is_signed:
            hamcrest.assert_that(
                response.json['data'],
                hamcrest.has_entries(items=hamcrest.contains(hamcrest.has_entries(external_id=contract.external_id,
                                                                                  id=contract.id,
                                                                                  type=contract.ctype.type,
                                                                                  dt=YEAR_AGO_ISO,
                                                                                  end_dt=NOW_ISO,
                                                                                  is_signed=True))))
        else:
            hamcrest.assert_that(
                response.json['data'],
                hamcrest.has_entries(items=hamcrest.empty()))

    @pytest.mark.parametrize('is_cancelled', [None, YEAR_AGO])
    def test_contracts_depend_on_cancelled(self, client, is_cancelled):
        contract = ob.ContractBuilder.construct(self.test_session, client=client, ctype='SPENDABLE',
                                                is_signed=YEAR_AGO, end_dt=NOW, dt=YEAR_AGO,
                                                is_cancelled=is_cancelled)
        response = self.test_client.get(self.BASE_API, {'client_id': client.id})
        if not is_cancelled:
            hamcrest.assert_that(
                response.json['data'],
                hamcrest.has_entries(items=hamcrest.contains(hamcrest.has_entries(external_id=contract.external_id,
                                                                                  id=contract.id,
                                                                                  type=contract.ctype.type,
                                                                                  dt=YEAR_AGO_ISO,
                                                                                  end_dt=NOW_ISO,
                                                                                  is_signed=True))))
        else:
            hamcrest.assert_that(
                response.json['data'],
                hamcrest.has_entries(items=hamcrest.empty()))


@pytest.mark.smoke
@pytest.mark.usefixtures('httpretty_enabled_fixture')
class TestYadocContractPartnerDocuments(TestCaseApiAppBase):
    BASE_API = u'/v1/yadoc/contract/docs'

    @pytest.mark.permissions
    @pytest.mark.parametrize('passport_w_client', [True, False])
    def test_client_forbidden(self, passport_w_client, client):
        security.set_roles([])
        if passport_w_client:
            security.set_passport_client(client)

        contract = ob.ContractBuilder.construct(self.test_session, client=client)

        response = self.test_client.get(
            self.BASE_API,
            {
                'contract_ids': contract.id,
                'dt_from': NOW_ISO,
                'pagination_pn': 1,
                'pagination_ps': 10,
                'document_types': 'PARTNER_ACT',
            },
            is_admin=False,
        )
        if passport_w_client:
            hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK),
                                 'Response code must be OK')
        else:
            hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.FORBIDDEN),
                                 'Response code must be FORBIDDEN')

    @pytest.mark.permissions
    @pytest.mark.parametrize('w_perm', [
        True,
        False
    ])
    def test_perm_forbidden(self, w_perm, client):
        if w_perm:
            role = create_role([cst.PermissionCode.VIEW_CONTRACTS,
                                {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None}])
            security.set_roles([role])
        else:
            security.set_roles([])

        contract = ob.ContractBuilder.construct(self.test_session, client=client)
        response = self.test_client.get(self.BASE_API, {'contract_ids': contract.id,
                                                        'dt_from': NOW_ISO,
                                                        'pagination_pn': 1,
                                                        'pagination_ps': 10,
                                                        'document_types': 'PARTNER_ACT',
                                                        },
                                        is_admin=False)
        if w_perm:
            hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK),
                                 'Response code must be OK')
        else:
            hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.FORBIDDEN),
                                 'Response code must be FORBIDDEN')

    def test_contract_not_exist(self):

        def request_callback(request, uri, response_headers):
            return [200, [], json.dumps(EMPTY_RESPONSE)]

        httpretty.register_uri(
            httpretty.POST,
            YADOC_URL_DOCUMENTS,
            body=request_callback)

        response = self.test_client.get(
            self.BASE_API,
            params={'contract_ids': 23234,
                    'dt_from': NOW_ISO,
                    'pagination_pn': 1,
                    'pagination_ps': 10,
                    'document_types': 'PARTNER_ACT',
                    }
        )

        hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.NOT_FOUND),
                             'Response code must be 404(NOT_FOUND)')

    def test_no_documents(self):
        contract = ob.ContractBuilder.construct(self.test_session)
        params = {'contract_ids': contract.id,
                  'dt_from': NOW_ISO,
                  'pagination_pn': 1,
                  'pagination_ps': 10,
                  'document_types': 'PARTNER_ACT',
                  }

        def request_callback(request, uri, response_headers):
            return [200, [], json.dumps(EMPTY_RESPONSE)]

        httpretty.register_uri(
            httpretty.POST,
            YADOC_URL_DOCUMENTS,
            body=request_callback)

        res = self.test_client.get(
            self.BASE_API,
            params=params
        )

        assert res.json['data'] == {'items': [],
                                    'request': {'pagination_ps': 10,
                                                'pagination_pn': 1},
                                    'totals': {'row_count': 0}}

    @pytest.mark.parametrize('request_document_types, config_document_types', [
        (['PARTNER_INV', 'PARTNER_ACT'], ['PARTNER_INV', 'PARTNER_ACT']),
        (['PARTNER_INV', 'PARTNER_ACT'], []),
        (['PARTNER_INV'], ['PARTNER_ACT'])
    ])
    def test_multiple_documents(self, request_document_types, config_document_types):
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {
            'CONTRACT': config_document_types}
        contract = ob.ContractBuilder.construct(self.test_session)
        params = [('contract_ids', contract.id),
                  ('dt_from', NOW_ISO),
                  ('dt_to', NOW_ISO),
                  ('pagination_pn', 1),
                  ('pagination_ps', 10)]

        for document_type in request_document_types:
            params.append(('document_types', document_type))

        def request_callback(request, uri, response_headers):
            request = json.loads(request.body)

            assert request["doc_type"] == ['PARTNER_INV', 'PARTNER_ACT']
            return [200, [], json.dumps((MockResponse(total_elements=2)
                                         .add_person(contract.person.id)
                                         .add_contract(contract.id, contract.person.id, indv_documents_flag=False)
                                         .add_document(666, 'PARTNER_INV', contract.person.id, '2312',
                                                       contract_id=contract.id)
                                         .add_document(777, 'PARTNER_ACT', contract.person.id, '23241', w_edo=True,
                                                       contract_id=contract.id)
                                         .get_response()))]

        httpretty.register_uri(
            httpretty.POST,
            YADOC_URL_DOCUMENTS,
            body=request_callback)

        response = self.test_client.get(
            self.BASE_API,
            params=params
        )
        if request_document_types == config_document_types:
            hamcrest.assert_that(
                response.json['data'],
                hamcrest.has_entries(
                    items=hamcrest.contains_inanyorder(
                        hamcrest.has_entries(
                            document=hamcrest.all_of(hamcrest.has_entries(amount='10.00',
                                                                          currency='RUB',
                                                                          doc_dt=NOW_ISO,
                                                                          doc_id=666,
                                                                          doc_number='2312',
                                                                          doc_type=u'PARTNER_INV',
                                                                          edo_enabled_flag=False,
                                                                          indv_documents_flag=False),
                                                     hamcrest.has_length(8)),
                            contract=hamcrest.has_entries(id=contract.id)),
                        hamcrest.has_entries(
                            document=hamcrest.all_of(hamcrest.has_entries(amount='10.00',
                                                                          currency='RUB',
                                                                          doc_dt=NOW_ISO,
                                                                          doc_id=777,
                                                                          doc_number='23241',
                                                                          doc_type=u'PARTNER_ACT',
                                                                          edo_enabled_flag=True,
                                                                          indv_documents_flag=False),
                                                     hamcrest.has_length(8)),
                            contract=hamcrest.has_entries(id=contract.id))
                    ),
                    totals=hamcrest.has_entries(row_count=2),
                    request=hamcrest.has_entries(pagination_pn=1,
                                                 pagination_ps=10)
                ))

        else:
            hamcrest.assert_that(
                response.json['data'],
                hamcrest.has_entries(
                    items=hamcrest.has_length(0),
                    totals=hamcrest.has_entries(row_count=0),
                    request=hamcrest.has_entries(pagination_pn=1,
                                                 pagination_ps=10)))



@pytest.mark.smoke
@pytest.mark.usefixtures('httpretty_enabled_fixture')
class TestYadocContractPartnerDocumentsDownload(TestCaseApiAppBase):
    BASE_API = u'/v1/yadoc/contract/download-docs-by-document-ids'

    @pytest.mark.permissions
    @pytest.mark.parametrize('passport_w_client', [True, False])
    def test_client_forbidden(self, passport_w_client, client):
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {
            'CONTRACT': ['PARTNER_INV', 'PARTNER_ACT']}
        security.set_roles([])

        if passport_w_client:
            security.set_passport_client(client)

        contract = ob.ContractBuilder.construct(self.test_session, client=client)

        def request_callback(request, uri, response_headers):
            if request.method == 'POST':
                request = json.loads(request.body)
                assert set(request["doc_type"]) == {'PARTNER_INV', 'PARTNER_ACT'}
                return [200, [], json.dumps((MockResponse(total_elements=2)
                                             .add_person(contract.person.id)
                                             .add_contract(contract.id, contract.person.id, indv_documents_flag=False)
                                             .add_document(666, 'PARTNER_INV', contract.person.id, '2312',
                                                           contract_id=contract.id,
                                                           document_dt=NOW)
                                             .get_response()))]
            else:
                return [200, {"Content-Type": 'application/octet-stream;charset=utf-8'}, 'testtest']

        httpretty.register_uri(
            httpretty.POST,
            YADOC_URL_DOCUMENTS,
            body=request_callback)

        httpretty.register_uri(
            httpretty.GET,
            YADOC_URL_DOWNLOAD,
            body=request_callback)

        response = self.test_client.get(
            self.BASE_API, {
                'contract_ids': contract.id,
                'document_ids': 666,
            },
            is_admin=False,
        )
        if passport_w_client:
            hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK),
                                 'Response code must be OK')
        else:
            hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.FORBIDDEN),
                                 'Response code must be FORBIDDEN')

    @pytest.mark.permissions
    @pytest.mark.parametrize('w_perm', [
        True,
        False
    ])
    def test_perm_forbidden(self, w_perm, client):
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {
            'CONTRACT': ['PARTNER_INV', 'PARTNER_ACT']}
        if w_perm:
            role = create_role([cst.PermissionCode.VIEW_CONTRACTS,
                                {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None}])
            security.set_roles([role])
        else:
            security.set_roles([])

        contract = ob.ContractBuilder.construct(self.test_session, client=client)

        def request_callback(request, uri, response_headers):
            if request.method == 'POST':
                request = json.loads(request.body)
                assert set(request["doc_type"]) == {'PARTNER_INV', 'PARTNER_ACT'}
                return [200, [], json.dumps((MockResponse(total_elements=2)
                                             .add_person(contract.person.id)
                                             .add_contract(contract.id, contract.person.id, indv_documents_flag=False)
                                             .add_document(666, 'PARTNER_INV', contract.person.id, '2312',
                                                           contract_id=contract.id,
                                                           document_dt=NOW)
                                             .get_response()))]
            else:
                return [200, {"Content-Type": 'application/octet-stream;charset=utf-8'}, 'testtest']

        httpretty.register_uri(
            httpretty.POST,
            YADOC_URL_DOCUMENTS,
            body=request_callback)

        httpretty.register_uri(
            httpretty.GET,
            YADOC_URL_DOWNLOAD,
            body=request_callback)

        response = self.test_client.get(self.BASE_API, {'contract_ids': contract.id,
                                                        'document_ids': 666
                                                        },
                                        is_admin=False)
        if w_perm:
            hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK),
                                 'Response code must be OK')
        else:
            hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.FORBIDDEN),
                                 'Response code must be FORBIDDEN')

    def test_contract_not_exist(self):
        response = self.test_client.get(
            self.BASE_API,
            params={'contract_ids': 23234,
                    'document_ids': 555
                    }
        )

        hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.NOT_FOUND),
                             'Response code must be 404(NOT_FOUND)')

    def test_no_documents(self):
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {
            'CONTRACT': ['PARTNER_INV', 'PARTNER_ACT']}
        contract = ob.ContractBuilder.construct(self.test_session)
        params = {'contract_ids': contract.id,
                  'document_ids': 444,
                  }

        def request_callback(request, uri, response_headers):
            return [200, [], json.dumps(EMPTY_RESPONSE)]

        httpretty.register_uri(
            httpretty.POST,
            YADOC_URL_DOCUMENTS,
            body=request_callback)

        response = self.test_client.get(
            self.BASE_API,
            params=params
        )

        hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.NOT_FOUND),
                             'Response code must be 404(NOT_FOUND)')
        hamcrest.assert_that(response.json["description"], hamcrest.equal_to(u'Documents not found, reason is:'
                                                                             u' Yadoc has no documents for CONTRACT {}'.format(
            contract.id)))

    @pytest.mark.parametrize('document_ids', [[666],
                                              [666, 888],
                                              [666, 777],
                                              [888]])
    def test_multiple_documents(self, document_ids):
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {
            'CONTRACT': ['PARTNER_INV', 'PARTNER_ACT']}
        contract = ob.ContractBuilder.construct(self.test_session)
        params = [('contract_ids', contract.id)]

        for doc_id in document_ids:
            params.append(('document_ids', doc_id))

        def request_callback(request, uri, response_headers):
            if request.method == 'POST':
                request = json.loads(request.body)
                assert set(request["doc_type"]) == {'PARTNER_INV', 'PARTNER_ACT'}
                response = (MockResponse(total_elements=2)
                            .add_person(contract.person.id)
                            .add_contract(contract.id, contract.person.id, indv_documents_flag=False))
                for doc_id in [666, 777]:
                    response.add_document(doc_id, 'PARTNER_INV', contract.person.id, '2312',
                                          contract_id=contract.id,
                                          document_dt=NOW)
                return [200, [], json.dumps((response
                                             .get_response()))]
            else:
                return [200, {"Content-Type": 'application/octet-stream;charset=utf-8'}, 'testtest']

        httpretty.register_uri(
            httpretty.POST,
            YADOC_URL_DOCUMENTS,
            body=request_callback)

        httpretty.register_uri(
            httpretty.GET,
            YADOC_URL_DOWNLOAD,
            body=request_callback)

        response = self.test_client.get(
            self.BASE_API,
            params=params
        )
        if document_ids == [666, 777]:

            hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK),
                                 'Response code must be OK')
        else:

            hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.NOT_FOUND),
                                 'Response code must be 404(NOT_FOUND)')
