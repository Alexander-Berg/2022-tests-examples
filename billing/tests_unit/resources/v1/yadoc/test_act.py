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
import urlparse
import hamcrest as hm
import http.client as http
from decimal import Decimal as D
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


NOW = trunc_date(datetime.datetime.now()).isoformat()
YADOC_URL = "https://yadoc-test.mba.yandex-team.ru/public/api/"
YADOC_URL_DOCUMENTS = YADOC_URL + "v1/documents"
YADOC_URL_DOWNLOAD = YADOC_URL + "v1/documents/download"


def create_contract(session, client, person):
    return ob.ContractBuilder.construct(session, client=client, person=person)


def create_act(session, qty=D('50'), client=None, firm_id=cst.FirmId.YANDEX_OOO):
    session = test_utils.get_test_session()

    client = client or ob.ClientBuilder()
    order = ob.OrderBuilder(client=client).build(session).obj
    invoice = ob.InvoiceBuilder(
        person=ob.PersonBuilder(client=client, person_type='ur').build(session),
        request=ob.RequestBuilder(
            firm_id=firm_id,
            basket=ob.BasketBuilder(
                client=client,
                rows=[ob.BasketItemBuilder(order=order, quantity=qty)],
            ),
        ),
    ).build(session).obj
    invoice.turn_on_rows()
    order.calculate_consumption(
        dt=datetime.datetime.today() - datetime.timedelta(days=1),
        stop=0,
        shipment_info={'Bucks': qty},
    )

    acts = invoice.generate_act(force=True)
    return acts[0]


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


@pytest.mark.smoke
@pytest.mark.usefixtures('httpretty_enabled_fixture')
class TestYadocActDocuments(TestCaseApiAppBase):
    BASE_API = u'/v1/yadoc/act/docs'

    @pytest.mark.permissions
    @pytest.mark.parametrize('passport_w_client', [
        True,
        False
    ])
    def test_client_forbidden(self, passport_w_client, client):
        security.set_roles([])
        if passport_w_client:
            security.set_passport_client(client)

        act = create_act(self.test_session, client=client)
        response = self.test_client.get(
            self.BASE_API,
            {'external_id': act.external_id},
            is_admin=False,
        )
        if passport_w_client:
            hm.assert_that(response.status_code, hm.equal_to(http.OK),
                                 'Response code must be OK')
        else:
            hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN),
                                 'Response code must be FORBIDDEN')

    def test_act_not_exist(self):

        def request_callback(request, uri, response_headers):
            return [200, [], json.dumps(EMPTY_RESPONSE)]

        httpretty.register_uri(
            httpretty.POST,
            YADOC_URL_DOCUMENTS,
            body=request_callback)

        response = self.test_client.get(
            self.BASE_API,
            params={'external_id': '4566'}
        )

        hm.assert_that(response.status_code, hm.equal_to(http.NOT_FOUND),
                             'Response code must be 404(NOT_FOUND)')

    @pytest.mark.permissions
    @pytest.mark.parametrize('w_perm', [
        True,
        False
    ])
    def test_perm_forbidden(self, w_perm, client):
        if w_perm:
            role = create_role([cst.PermissionCode.VIEW_INVOICES,
                                {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None}])
            security.set_roles([role])
        else:
            security.set_roles([])

        act = create_act(self.test_session, client=client)
        response = self.test_client.get(self.BASE_API,
                                        {'external_id': act.external_id},
                                        is_admin=False)
        if w_perm:
            hm.assert_that(response.status_code, hm.equal_to(http.OK),
                                 'Response code must be OK')
        else:
            hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN),
                                 'Response code must be FORBIDDEN')

    def test_no_documents(self):
        act = create_act(self.test_session)
        params = {'external_id': act.external_id}

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

        assert res.json['data'] == {u'items': []}

    def test_multiple_documents(self):
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'ACT': ["ACT", "INV"]}
        act = create_act(self.test_session)
        act.factura = ob.get_big_number()
        params = {'external_id': act.external_id}

        def request_callback(request, uri, response_headers):
            request = json.loads(request.body)

            if 'ACT' in request["doc_type"]:
                assert request["doc_number"][0] == act.external_id
                return [200, [], json.dumps((MockResponse()
                                             .add_person(act.invoice.person.id)
                                             .add_document(666, 'ACT', act.invoice.person.id, 12414)
                                             .get_response()))]

            if 'INV' in request["doc_type"]:
                assert request["doc_number"][0] == act.factura
                return [200, [], json.dumps((MockResponse()
                                             .add_person(act.invoice.person.id)
                                             .add_document(9888, 'INV', act.invoice.person.id, 34343)
                                             .get_response()))]

        httpretty.register_uri(
            httpretty.POST,
            YADOC_URL_DOCUMENTS,
            body=request_callback)

        response = self.test_client.get(
            self.BASE_API,
            params=params
        )

        hm.assert_that(
            response.json['data'],
            hm.has_entries(
                items=hm.contains_inanyorder(
                    hm.has_entries(
                        document=hm.all_of(hm.has_entries(amount='10.00',
                                                                      currency='RUB',
                                                                      doc_dt=NOW,
                                                                      doc_id=666,
                                                                      doc_number='12414',
                                                                      doc_type=u'ACT',
                                                                      edo_enabled_flag=False,
                                                                      indv_documents_flag=False),
                                                 hm.has_length(8)),
                        act=hm.has_entries(external_id='12414')),
                    hm.has_entries(
                        document=hm.all_of(hm.has_entries(amount='10.00',
                                                                      currency='RUB',
                                                                      doc_dt=NOW,
                                                                      doc_id=9888,
                                                                      doc_number='34343',
                                                                      doc_type=u'INV',
                                                                      edo_enabled_flag=False,
                                                                      indv_documents_flag=False),
                                                 hm.has_length(8)),
                        act=hm.has_entries(external_id=None))
                )))

    @pytest.mark.parametrize('edo_enabled_flag', [
        True,
        False
    ])
    @pytest.mark.parametrize('indv_documents_flag', [
        True,
        False])
    def test_w_contract_check_flags(self, edo_enabled_flag, indv_documents_flag):
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'ACT': ["ACT", "INV"]}
        act = create_act(self.test_session)
        contract = create_contract(self.test_session, act.invoice.client, act.invoice.person)
        act.factura = ob.get_big_number()
        params = {'external_id': act.external_id}

        def request_callback(request, uri, response_headers):
            request = json.loads(request.body)

            if 'ACT' in request["doc_type"]:
                assert request["doc_number"][0] == act.external_id
                return [200, [], json.dumps((MockResponse()
                                             .add_person(act.invoice.person.id)
                                             .add_contract(contract.id,
                                                           act.invoice.person.id,
                                                           indv_documents_flag=indv_documents_flag)
                                             .add_document(666, 'ACT', act.invoice.person.id, 12414,
                                                           w_edo=edo_enabled_flag,
                                                           contract_id=contract.id)
                                             .get_response()))]

            if 'INV' in request["doc_type"]:
                assert request["doc_number"][0] == act.factura
                return [200, [], json.dumps((MockResponse()
                                             .add_person(act.invoice.person.id)
                                             .add_contract(contract.id,
                                                           act.invoice.person.id,
                                                           indv_documents_flag=indv_documents_flag)
                                             .add_document(9888, 'INV', act.invoice.person.id, 34343,
                                                           w_edo=edo_enabled_flag,
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

        hm.assert_that(
            response.json['data'],
            hm.has_entries(
                items=hm.contains_inanyorder(
                    hm.has_entries(
                        document=hm.all_of(hm.has_entries(amount='10.00',
                                                                      currency='RUB',
                                                                      doc_dt=NOW,
                                                                      doc_id=666,
                                                                      doc_number='12414',
                                                                      doc_type=u'ACT',
                                                                      edo_enabled_flag=edo_enabled_flag,
                                                                      indv_documents_flag=indv_documents_flag),
                                                 hm.has_length(8)),
                        act=hm.has_entries(external_id='12414')),
                    hm.has_entries(
                        document=hm.all_of(hm.has_entries(amount='10.00',
                                                                      currency='RUB',
                                                                      doc_dt=NOW,
                                                                      doc_id=9888,
                                                                      doc_number='34343',
                                                                      doc_type=u'INV',
                                                                      edo_enabled_flag=edo_enabled_flag,
                                                                      indv_documents_flag=indv_documents_flag),
                                                 hm.has_length(8)),
                        act=hm.has_entries(external_id=None))
                )))


@pytest.mark.smoke
@pytest.mark.usefixtures('httpretty_enabled_fixture')
class TestYadocActDocumentsBatch(TestCaseApiAppBase):
    BASE_API = u'/v1/yadoc/act/download-docs-batch'

    @pytest.mark.permissions
    @pytest.mark.parametrize('passport_w_client', [
        True,
        False
    ])
    def test_client_forbidden(self, passport_w_client, client):
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'ACT': ['INV', 'ACT']}
        security.set_roles([])
        if passport_w_client:
            security.set_passport_client(client)
        act = create_act(self.test_session, client=client)
        params = {'external_ids': act.external_id}

        def request_callback(request, uri, response_headers):
            if request.method == 'POST':
                request = json.loads(request.body)
                page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
                if request["doc_type"] == ['ACT']:
                    return [200, [], json.dumps(MockResponse()
                                                .add_person(act.invoice.person.id)
                                                .add_document(666, 'ACT', act.invoice.person.id, 12414)
                                                .get_response())]

                if request["doc_type"] == ['INV']:
                    return [200, [], json.dumps(MockResponse().get_response())]
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
            params=params,
            is_admin=False,
        )

        if passport_w_client:
            hm.assert_that(response.status_code, hm.equal_to(http.OK),
                                 'Response code must be OK')
        else:
            hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN),
                                 'Response code must be FORBIDDEN')

    @pytest.mark.permissions
    @pytest.mark.parametrize('w_perm', [
        True,
        False
    ])
    def test_perm_forbidden(self, w_perm, client):
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'ACT': ['INV', 'ACT']}
        if w_perm:
            role = create_role([cst.PermissionCode.VIEW_INVOICES,
                                {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None}])
            security.set_roles([role])
        else:
            security.set_roles([])
        act = create_act(self.test_session, client=client)
        params = {'external_ids': act.external_id}

        def request_callback(request, uri, response_headers):
            if request.method == 'POST':
                request = json.loads(request.body)
                page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
                if request["doc_type"] == ['ACT']:
                    return [200, [], json.dumps(MockResponse()
                                                .add_person(act.invoice.person.id)
                                                .add_document(666, 'ACT', act.invoice.person.id, 12414)
                                                .get_response())]

                if request["doc_type"] == ['INV']:
                    return [200, [], json.dumps(MockResponse().get_response())]
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
            params=params,
            is_admin=False,
        )

        if w_perm:
            hm.assert_that(response.status_code, hm.equal_to(http.OK),
                                 'Response code must be OK')
        else:
            hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN),
                                 'Response code must be FORBIDDEN')

    @pytest.mark.parametrize('content_type, extension', [['application/pdf;charset=utf-8', 'pdf'],
                                                         ['application/octet-stream;charset=utf-8', 'zip']])
    def test_download_docs(self, content_type, extension, client):
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'ACT': ['INV', 'ACT']}
        act = create_act(self.test_session, client=client)
        params = {'external_ids': act.external_id}

        def request_callback(request, uri, response_headers):
            if request.method == 'POST':
                request = json.loads(request.body)
                page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
                if request["doc_type"] == ['ACT']:
                    return [200, [], json.dumps(MockResponse()
                                                .add_person(act.invoice.person.id)
                                                .add_document(666, 'ACT', act.invoice.person.id, 12414)
                                                .get_response())]

                if request["doc_type"] == ['INV']:
                    return [200, [], json.dumps(MockResponse().get_response())]
            else:
                return [200, {"Content-Type": content_type}, 'testtest']

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

        filename = 'documents_{}'.format(datetime.datetime.now().strftime("%Y%m%d"))
        assert response.headers['access-control-expose-headers'] == 'Content-Disposition'
        assert response.headers['Content-Disposition'] == 'attachment; filename={}.{}'.format(filename,
                                                                                              extension)

    @pytest.mark.parametrize('edo_enabled_flag', [
        True,
        False
    ])
    @pytest.mark.parametrize('indv_documents_flag', [
        True,
        False])
    def test_download_docs_check_flags(self, client, indv_documents_flag,
                                       edo_enabled_flag):
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'ACT': ['INV', 'ACT']}
        act = create_act(self.test_session, client=client)
        contract = create_contract(self.test_session, client, act.invoice.person)
        params = {'external_ids': act.external_id}

        def request_callback(request, uri, response_headers):
            if request.method == 'POST':
                request = json.loads(request.body)
                page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
                if request["doc_type"] == ['ACT']:
                    return [200, [], json.dumps(MockResponse()
                                                .add_person(act.invoice.person.id)
                                                .add_contract(contract.id, act.invoice.person.id,
                                                              indv_documents_flag=indv_documents_flag)
                                                .add_document(666, 'ACT', act.invoice.person.id, 12414,
                                                              contract_id=contract.id,
                                                              w_edo=edo_enabled_flag)
                                                .get_response())]

                if request["doc_type"] == ['INV']:
                    return [200, [], json.dumps(MockResponse().get_response())]
            else:
                return [200, {"Content-Type": 'application/pdf;charset=utf-8'}, 'testtest']

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

        if not indv_documents_flag and not edo_enabled_flag:
            assert response.status_code == 200
        else:
            assert response.status_code == 404
            assert response.json['error'] == 'YADOC_DOCUMENTS_NOT_FOUND_FILTERED'



@pytest.mark.smoke
@pytest.mark.usefixtures('httpretty_enabled_fixture')
class TestYadocActDocumentsByType(TestCaseApiAppBase):
    BASE_API = u'/v1/yadoc/act/download-docs-by-type'

    @pytest.mark.parametrize('request_document_types, config_document_types', [
        (['INV', 'ACT'], ['INV', 'ACT']),
        (['INV', 'ACT'], []),
        (['INV'], ['ACT'])
    ])
    def test_download_docs_by_type(self, request_document_types, config_document_types):
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'ACT': config_document_types}
        act = create_act(self.test_session)
        params = [('external_id', act.external_id)]

        for request_document_type in request_document_types:
            params.append(('document_types', request_document_type))

        def request_callback(request, uri, response_headers):
            if request.method == 'POST':
                request = json.loads(request.body)
                page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
                if request["doc_type"] == ['ACT']:
                    return [200, [], json.dumps(MockResponse()
                                                .add_person(act.invoice.person.id)
                                                .add_document(666, 'ACT', act.invoice.person.id, 12414)
                                                .get_response())]

                if request["doc_type"] == ['INV']:
                    return [200, [], json.dumps(MockResponse().get_response())]
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

        filename = 'documents_{}'.format(datetime.datetime.now().strftime("%Y%m%d"))
        if request_document_types == config_document_types:
            assert response.headers['access-control-expose-headers'] == 'Content-Disposition'
            assert response.headers['Content-Disposition'] == 'attachment; filename={}.zip'.format(filename)
        else:
            assert response.status_code == 404

    @pytest.mark.parametrize('edo_enabled_flag', [True, False])
    @pytest.mark.parametrize('indv_documents_flag', [True, False])
    def test_download_docs_by_type_check_flags(self, edo_enabled_flag, indv_documents_flag):
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'ACT': ['INV', 'ACT']}
        act = create_act(self.test_session)
        contract = create_contract(self.test_session, act.invoice.client, act.invoice.person)
        params = [('external_id', act.external_id)]

        for request_document_type in ['INV', 'ACT']:
            params.append(('document_types', request_document_type))

        def request_callback(request, uri, response_headers):
            if request.method == 'POST':
                request = json.loads(request.body)
                page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
                if request["doc_type"] == ['ACT']:
                    return [200, [], json.dumps(MockResponse()
                                                .add_person(act.invoice.person.id)
                    .add_contract(contract.id, act.invoice.person.id, indv_documents_flag=indv_documents_flag)
                                                .add_document(666, 'ACT', act.invoice.person.id, 12414,
                                                              contract_id=contract.id,
                                                              w_edo=edo_enabled_flag)
                                                .get_response())]

                if request["doc_type"] == ['INV']:
                    return [200, [], json.dumps(MockResponse().get_response())]
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

        filename = 'documents_{}'.format(datetime.datetime.now().strftime("%Y%m%d"))
        if not indv_documents_flag and not edo_enabled_flag:
            assert response.status_code == 200
        else:
            assert response.status_code == 404
            assert response.json['error'] == 'YADOC_DOCUMENTS_NOT_FOUND_FILTERED'

    @pytest.mark.permissions
    @pytest.mark.parametrize('passport_w_client', [
        True,
        False
    ])
    def test_client_forbidden(self, passport_w_client, client):
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'ACT': ['INV', 'ACT']}
        security.set_roles([])
        if passport_w_client:
            security.set_passport_client(client)
        act = create_act(self.test_session, client=client)
        params = {'external_id': act.external_id, 'document_types': 'ACT'}

        def request_callback(request, uri, response_headers):
            if request.method == 'POST':
                request = json.loads(request.body)
                page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
                if request["doc_type"] == ['ACT']:
                    return [200, [], json.dumps(MockResponse()
                                                .add_person(act.invoice.person.id)
                                                .add_document(666, 'ACT', act.invoice.person.id, 12414)
                                                .get_response())]

                if request["doc_type"] == ['INV']:
                    return [200, [], json.dumps(MockResponse().get_response())]
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
            params=params,
            is_admin=False,
        )

        if passport_w_client:
            hm.assert_that(response.status_code, hm.equal_to(http.OK), 'Response code must be OK')
        else:
            hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN), 'Response code must be FORBIDDEN')

    @pytest.mark.permissions
    @pytest.mark.parametrize('w_perm', [
        True,
        False
    ])
    def test_perm_forbidden(self, w_perm, client):
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'ACT': ['INV', 'ACT']}
        if w_perm:
            role = create_role([cst.PermissionCode.VIEW_INVOICES,
                                {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None}])
            security.set_roles([role])
        else:
            security.set_roles([])
        act = create_act(self.test_session, client=client)
        params = {'external_id': act.external_id, 'document_types': 'ACT'}

        def request_callback(request, uri, response_headers):
            if request.method == 'POST':
                request = json.loads(request.body)
                page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
                if request["doc_type"] == ['ACT']:
                    return [200, [], json.dumps(MockResponse()
                                                .add_person(act.invoice.person.id)
                                                .add_document(666, 'ACT', act.invoice.person.id, 12414)
                                                .get_response())]

                if request["doc_type"] == ['INV']:
                    return [200, [], json.dumps(MockResponse().get_response())]
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
            params=params,
            is_admin=False,
        )

        if w_perm:
            hm.assert_that(response.status_code, hm.equal_to(http.OK),
                                 'Response code must be OK')
        else:
            hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN),
                                 'Response code must be FORBIDDEN')


@pytest.mark.smoke
class TestYadocLastClosedPeriods(TestCaseApiAppBase):
    BASE_API = u'/v1/yadoc/last-closed-periods'

    @pytest.mark.permissions
    @pytest.mark.parametrize('passport_w_client', [
        True,
        False
    ])
    def test_client(self, client, passport_w_client):
        another_client = create_client()
        security.set_roles([])
        if passport_w_client:
            security.set_passport_client(client)
        else:
            security.set_passport_client(another_client)
        ob.YadocFirmBuilder.construct(self.test_session, firm_id=cst.FirmId.YANDEX_OOO, last_closed_dt=datetime.datetime(2022, 1, 31))
        act = create_act(self.test_session, client=client, firm_id=cst.FirmId.YANDEX_OOO)

        response = self.test_client.get(self.BASE_API, params={}, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'Response code must be OK')
        items = response.get_json()['data']['items']
        hm.assert_that(items, hm.has_length(1 if passport_w_client else 0))
        if passport_w_client:
            hm.assert_that(
                items,
                hm.contains(
                    hm.has_entries(
                        firm_id=act.invoice.firm_id,
                        last_closed_dt='2022-01-31T00:00:00',
                    )
                )
            )

    @pytest.mark.permissions
    @pytest.mark.parametrize('perms, res', [
        [(cst.PermissionCode.ADMIN_ACCESS, cst.PermissionCode.VIEW_CLIENTS, cst.PermissionCode.VIEW_INVOICES), http.OK],
        [(cst.PermissionCode.ADMIN_ACCESS, cst.PermissionCode.VIEW_INVOICES), http.FORBIDDEN],
        [(cst.PermissionCode.ADMIN_ACCESS, cst.PermissionCode.VIEW_CLIENTS), http.FORBIDDEN],
    ])
    def test_admin(self, client, perms, res):
        security.set_roles([ob.create_role(self.test_session, *perms)])
        security.set_passport_client(None)
        ob.YadocFirmBuilder.construct(self.test_session, firm_id=cst.FirmId.YANDEX_OOO, last_closed_dt=datetime.datetime(2022, 1, 31))
        act = create_act(self.test_session, client=client, firm_id=cst.FirmId.YANDEX_OOO)

        response = self.test_client.get(self.BASE_API, params={'client_id': client.id}, is_admin=True)
        hm.assert_that(response.status_code, hm.equal_to(res))
        if res == http.OK:
            hm.assert_that(
                response.get_json()['data']['items'],
                hm.contains(
                    hm.has_entries(
                        firm_id=act.invoice.firm_id,
                        last_closed_dt='2022-01-31T00:00:00',
                    )
                )
            )

    def test_multiple_firms(self, client):
        security.set_passport_client(client)
        firm_dt_map = {
            cst.FirmId.YANDEX_OOO: (datetime.datetime(2022, 1, 31), '2022-01-31T00:00:00'),
            cst.FirmId.FOOD: (datetime.datetime(2022, 2, 28), '2022-02-28T00:00:00'),
            cst.FirmId.MARKET: (None, None)
        }
        ob.YadocFirmBuilder.construct(self.test_session, firm_id=cst.FirmId.YANDEX_OOO, last_closed_dt=firm_dt_map[cst.FirmId.YANDEX_OOO][0])
        ob.YadocFirmBuilder.construct(self.test_session, firm_id=cst.FirmId.FOOD, last_closed_dt=firm_dt_map[cst.FirmId.FOOD][0])
        ob.YadocFirmBuilder.construct(self.test_session, firm_id=cst.FirmId.MARKET, last_closed_dt=firm_dt_map[cst.FirmId.MARKET][0])

        firm_ids = [cst.FirmId.YANDEX_OOO, cst.FirmId.YANDEX_OOO, cst.FirmId.FOOD, cst.FirmId.MARKET]
        acts = [create_act(self.test_session, client=client, firm_id=firm_id) for firm_id in firm_ids]

        response = self.test_client.get(self.BASE_API, params={}, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(http.OK))
        items = response.get_json()['data']['items']
        hm.assert_that(items, hm.has_length(len(set(firm_ids))))
        hm.assert_that(
            items,
            hm.contains_inanyorder(*[
                hm.has_entries(
                    firm_id=firm_id,
                    last_closed_dt=firm_dt_map[firm_id][1],
                )
                for firm_id in set(firm_ids)
            ])
        )

    def test_no_firms(self, client):
        ob.YadocFirmBuilder.construct(self.test_session, firm_id=cst.FirmId.FOOD, last_closed_dt=datetime.datetime(2022, 1, 31))
        create_act(self.test_session, client=client, firm_id=cst.FirmId.CLOUD)
        create_act(self.test_session, client=create_client(), firm_id=cst.FirmId.FOOD)
        security.set_passport_client(client)
        response = self.test_client.get(self.BASE_API, params={}, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(http.OK))
        items = response.get_json()['data']['items']
        hm.assert_that(items, hm.has_length(0))
