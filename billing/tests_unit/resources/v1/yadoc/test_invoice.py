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
import hamcrest
import http.client as http
from balance import constants as cst
from brest.core.tests import utils as test_utils
from brest.core.tests import security

from tests import object_builder as ob
from tests.balance_tests.yadoc.test_yadoc import MockResponse, EMPTY_RESPONSE, ERROR_ANSWER
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role
from yb_snout_api.tests_unit.fixtures.permissions import create_role


def trunc_date(date):
    return datetime.datetime.combine(date.date(), datetime.time(0))


NOW = datetime.datetime.now()
NOW_ISO = trunc_date(NOW).isoformat()
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


@pytest.mark.smoke
@pytest.mark.usefixtures('httpretty_enabled_fixture')
class TestYadocInvoiceDocuments(TestCaseApiAppBase):
    BASE_API = u'/v1/yadoc/invoice/docs'

    @pytest.mark.permissions
    @pytest.mark.parametrize('passport_w_client', [
        True,
        False
    ])
    def test_client_forbidden(self, passport_w_client, client):
        security.set_roles([])
        if passport_w_client:
            security.set_passport_client(client)

        invoice = ob.InvoiceBuilder.construct(self.test_session, client=client)
        self.test_session.flush()

        response = self.test_client.get(
            self.BASE_API,
            {'external_id': invoice.external_id},
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
            role = create_role([cst.PermissionCode.VIEW_INVOICES,
                                {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None}])
            security.set_roles([role])
        else:
            security.set_roles([])

        invoice = ob.InvoiceBuilder.construct(self.test_session, client=client)
        self.test_session.flush()
        response = self.test_client.get(
            self.BASE_API,
            {'external_id': invoice.external_id},
            is_admin=False,
        )
        if w_perm:
            hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK),
                                 'Response code must be OK')
        else:
            hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.FORBIDDEN),
                                 'Response code must be FORBIDDEN')

    def test_no_documents(self):
        invoice = ob.InvoiceBuilder.construct(self.test_session)
        security.set_passport_client(invoice.client)
        params = {'external_id': invoice.external_id}

        def request_callback(request, uri, response_headers):
            return [200, [], json.dumps(EMPTY_RESPONSE)]

        httpretty.register_uri(
            httpretty.POST,
            YADOC_URL_DOCUMENTS,
            body=request_callback)

        res = self.test_client.get(
            self.BASE_API,
            params=params,
            is_admin=False,
        )

        assert res.json['data'] == {u'items': []}

    def test_invoice_not_exist(self):
        params = {'external_id': '4566'}

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

    def test_connection_error(self):
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'INVOICE': ["INV_ADV", "BILL"]}
        invoice = ob.InvoiceBuilder.construct(self.test_session)

        def request_callback(request, uri, response_headers):
            return [400, [], json.dumps(ERROR_ANSWER)]

        httpretty.register_uri(
            httpretty.POST,
            YADOC_URL_DOCUMENTS,
            body=request_callback)

        response = self.test_client.get(
            self.BASE_API,
            params={'external_id': invoice.external_id}
        )

        hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.INTERNAL_SERVER_ERROR),
                             'Response code must be 500(INTERNAL_SERVER_ERROR)')

    def test_multiple_documents(self):
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'INVOICE': ['INV_ADV', 'BILL']}
        invoice = ob.InvoiceBuilder.construct(self.test_session)
        params = {'external_id': invoice.external_id}

        def request_callback(request, uri, response_headers):
            request = json.loads(request.body)
            page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
            if page_num == 0 and "bill_number" in request:
                return [200, [], json.dumps((MockResponse()
                                             .add_person(invoice.person.id)
                                             .add_document(666, 'INV_ADV', invoice.person.id, 123123,
                                                           invoice_id=invoice.external_id)
                                             .add_document(232, 'INV_ADV', invoice.person.id, 12312,
                                                           invoice_id=invoice.external_id)
                                             .get_response()))]

            if page_num == 0 and "doc_number" in request:
                return [200, [], json.dumps(MockResponse().get_response())]

        httpretty.register_uri(
            httpretty.POST,
            YADOC_URL_DOCUMENTS,
            body=request_callback)

        response = self.test_client.get(
            self.BASE_API,
            params=params
        )

        hamcrest.assert_that(
            response.json['data'],
            hamcrest.has_entries(
                items=hamcrest.contains_inanyorder(
                    hamcrest.has_entries(
                        document=hamcrest.all_of(hamcrest.has_entries(amount='10.00',
                                                                      currency='RUB',
                                                                      doc_dt=NOW_ISO,
                                                                      doc_id=666,
                                                                      doc_number='123123',
                                                                      doc_type=u'INV_ADV',
                                                                      edo_enabled_flag=False,
                                                                      indv_documents_flag=False),
                                                 hamcrest.has_length(8)),
                        invoice=hamcrest.has_entries(external_id=invoice.external_id)),
                    hamcrest.has_entries(
                        document=hamcrest.all_of(hamcrest.has_entries(amount='10.00',
                                                                      currency='RUB',
                                                                      doc_dt=NOW_ISO,
                                                                      doc_id=232,
                                                                      doc_number='12312',
                                                                      doc_type=u'INV_ADV',
                                                                      edo_enabled_flag=False,
                                                                      indv_documents_flag=False),
                                                 hamcrest.has_length(8)),
                        invoice=hamcrest.has_entries(external_id=invoice.external_id))
                )))


@pytest.mark.smoke
@pytest.mark.usefixtures('httpretty_enabled_fixture')
class TestYadocInvoiceDocumentsBatch(TestCaseApiAppBase):
    BASE_API = u'/v1/yadoc/invoice/download-docs-batch'

    @pytest.mark.permissions
    @pytest.mark.parametrize('passport_w_client', [
        True,
        False
    ])
    def test_client_forbidden(self, passport_w_client, client):
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'INVOICE': ['INV_ADV', 'BILL']}
        security.set_roles([])
        if passport_w_client:
            security.set_passport_client(client)
        invoice = ob.InvoiceBuilder.construct(self.test_session, client=client)
        params = {'external_ids': invoice.external_id}

        def request_callback(request, uri, response_headers):
            if request.method == 'POST':
                request = json.loads(request.body)
                page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
                if page_num == 0 and "bill_number" in request:
                    return [200, [], json.dumps((MockResponse()
                                                 .add_person(invoice.person.id)
                                                 .add_document(666, 'INV_ADV', invoice.person.id, 123123,
                                                               invoice_id=invoice.external_id,
                                                               document_dt=NOW)
                                                 .get_response()))]

                if page_num == 0 and "doc_number" in request:
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
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'INVOICE': ['INV_ADV', 'BILL']}
        if w_perm:
            role = create_role([cst.PermissionCode.VIEW_INVOICES,
                                {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None}])
            security.set_roles([role])
        else:
            security.set_roles([])
        invoice = ob.InvoiceBuilder.construct(self.test_session, client=client)
        params = {'external_ids': invoice.external_id}

        def request_callback(request, uri, response_headers):
            if request.method == 'POST':
                request = json.loads(request.body)
                page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
                if page_num == 0 and "bill_number" in request:
                    return [200, [], json.dumps((MockResponse()
                                                 .add_person(invoice.person.id)
                                                 .add_document(666, 'INV_ADV', invoice.person.id, 123123,
                                                               invoice_id=invoice.external_id,
                                                               document_dt=NOW)
                                                 .get_response()))]

                if page_num == 0 and "doc_number" in request:
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
            hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK),
                                 'Response code must be OK')
        else:
            hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.FORBIDDEN),
                                 'Response code must be FORBIDDEN')

    @pytest.mark.parametrize('content_type, extension', [['application/pdf;charset=utf-8', 'pdf'],
                                                         ['application/octet-stream;charset=utf-8', 'zip']])
    def test_download_docs(self, content_type, extension):
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'INVOICE': ['INV_ADV', 'BILL']}
        invoice = ob.InvoiceBuilder.construct(self.test_session)
        params = {'external_ids': invoice.external_id}

        def request_callback(request, uri, response_headers):
            if request.method == 'POST':
                request = json.loads(request.body)
                page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
                if page_num == 0 and "bill_number" in request:
                    return [200, [], json.dumps((MockResponse()
                                                 .add_person(invoice.person.id)
                                                 .add_document(666, 'INV_ADV', invoice.person.id, 123123,
                                                               invoice_id=invoice.external_id,
                                                               document_dt=NOW)
                                                 .get_response()))]

                if page_num == 0 and "doc_number" in request:
                    return [200, [], json.dumps(MockResponse().get_response())]
            else:
                qs = urlparse.parse_qs(urlparse.urlparse(request.path).query)
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


@pytest.mark.smoke
@pytest.mark.usefixtures('httpretty_enabled_fixture')
class TestYadocInvoiceDocumentsByType(TestCaseApiAppBase):
    BASE_API = u'/v1/yadoc/invoice/download-docs-by-type'

    @pytest.mark.parametrize('request_document_types, config_document_types', [
        (['INV_ADV', 'BILL'], ['INV_ADV', 'BILL']),
        (['INV_ADV', 'BILL'], []),
        (['INV_ADV'], ['BILL'])
    ])
    def test_download_docs_by_type(self, request_document_types, config_document_types):
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'INVOICE': config_document_types}
        invoice = ob.InvoiceBuilder.construct(self.test_session)

        params = [('external_id', invoice.external_id)]

        for request_document_type in request_document_types:
            params.append(('document_types', request_document_type))

        def request_callback(request, uri, response_headers):
            if request.method == 'POST':
                request = json.loads(request.body)
                page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
                if page_num == 0 and "bill_number" in request:
                    return [200, [], json.dumps((MockResponse()
                                                 .add_person(invoice.person.id)
                                                 .add_document(666, 'INV_ADV', invoice.person.id, 123123,
                                                               invoice_id=invoice.external_id,
                                                               document_dt=NOW)
                                                 .get_response()))]

                if page_num == 0 and "doc_number" in request:
                    return [200, [], json.dumps(MockResponse().get_response())]
            else:
                qs = urlparse.parse_qs(urlparse.urlparse(request.path).query)
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

    @pytest.mark.permissions
    @pytest.mark.parametrize('passport_w_client', [
        True,
        False
    ])
    def test_client_forbidden(self, passport_w_client, client):
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'INVOICE': ['INV_ADV', 'BILL']}
        security.set_roles([])
        if passport_w_client:
            security.set_passport_client(client)
        invoice = ob.InvoiceBuilder.construct(self.test_session, client=client)
        params = {'external_id': invoice.external_id, 'document_types': 'INV_ADV'}

        def request_callback(request, uri, response_headers):
            if request.method == 'POST':
                request = json.loads(request.body)
                page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
                if page_num == 0 and "bill_number" in request:
                    return [200, [], json.dumps((MockResponse()
                                                 .add_person(invoice.person.id)
                                                 .add_document(666, 'INV_ADV', invoice.person.id, 123123,
                                                               invoice_id=invoice.external_id,
                                                               document_dt=NOW)
                                                 .get_response()))]

                if page_num == 0 and "doc_number" in request:
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
        self.test_session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'INVOICE': ['INV_ADV', 'BILL']}
        if w_perm:
            role = create_role([cst.PermissionCode.VIEW_INVOICES,
                                {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None}])
            security.set_roles([role])
        else:
            security.set_roles([])
        invoice = ob.InvoiceBuilder.construct(self.test_session, client=client)
        params = {'external_id': invoice.external_id, 'document_types': 'INV_ADV'}

        def request_callback(request, uri, response_headers):
            if request.method == 'POST':
                request = json.loads(request.body)
                page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
                if page_num == 0 and "bill_number" in request:
                    return [200, [], json.dumps((MockResponse()
                                                 .add_person(invoice.person.id)
                                                 .add_document(666, 'INV_ADV', invoice.person.id, 123123,
                                                               invoice_id=invoice.external_id,
                                                               document_dt=NOW)
                                                 .get_response()))]

                if page_num == 0 and "doc_number" in request:
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
            hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK),
                                 'Response code must be OK')
        else:
            hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.FORBIDDEN),
                                 'Response code must be FORBIDDEN')
