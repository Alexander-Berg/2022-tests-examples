# -*- coding: utf-8 -*-

import pytest
import httpretty
import mock
import copy
import json
import datetime
import urlparse
import hamcrest
import http.client as http
from itertools import chain
from balance import constants as cst, exc, mapper, muzzle_util as mut
from balance.api.yadoc import YaDocApi
from tests import object_builder as ob


NOW = mut.trunc_date(datetime.datetime.now())
YEAR_AGO = NOW - datetime.timedelta(days=3650)

EMPTY_RESPONSE = {
    "content": [],
    "pageable": {
        "sort": {
            "unsorted": True,
            "sorted": False,
            "empty": True
        },
        "offset": 0,
        "pageNumber": 0,
        "pageSize": 20,
        "paged": True,
        "unpaged": False
    },
    "size": 20,
    "numberOfElements": 0,
    "totalElements": 0,
    "totalPages": 0,
    "last": True,
    "number": 0,
    "sort": {
        "unsorted": True,
        "sorted": False,
        "empty": True
    },
    "first": True,
    "empty": True
}

YADOC_URL = "https://yadoc-test.mba.yandex-team.ru/public/api/"
YADOC_URL_DOCUMENTS = YADOC_URL + "v1/documents"
YADOC_URL_DOWNLOAD = YADOC_URL + "v1/documents/download"
YADOC_URL_ORGANIZATIONS = YADOC_URL + "organizations"
YADOC_URL_PERIODS = YADOC_URL + "getAPClosedPeriod"


@pytest.fixture(autouse=True)
def patching():
    with mock.patch('balance.api.yadoc.YaDocApi._get_tvm_ticket', return_value='666'), \
         mock.patch('butils.application.plugins.components_cfg.get_component_cfg',
                    return_value={'Url': YADOC_URL, 'PageSize': 20}):
        yield


@pytest.fixture
def client(session):
    return ob.ClientBuilder.construct(session, is_agency=1)


@pytest.fixture
def person(session, client):
    return ob.PersonBuilder.construct(session, client=client, type='ur')


@pytest.fixture
def contract(session, client, person):
    return ob.ContractBuilder.construct(session, client=client, person=person)


@pytest.fixture
def act(session):
    invoice = ob.InvoiceBuilder.construct(session)
    order = invoice.invoice_orders[0].order
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows(on_dt=invoice.dt)
    order.calculate_consumption(NOW, {order.shipment_type: 10})
    act, = invoice.generate_act(backdate=NOW, force=1)
    return act


class MockResponse():

    def __init__(self, page_number=0, last_page=True, total_elements=0):
        self.page = copy.deepcopy(EMPTY_RESPONSE)
        self.content = self.page["content"]
        self.page["number"] = page_number
        self.page["last"] = last_page
        self.page["totalElements"] = total_elements

    def add_person(self, person_id, w_edo=False):
        person = [person_info for person_info in self.content if person_info["party_id"] == person_id]
        assert len(person) == 0, 'duplicate person'
        person_data = {"party_id": person_id,
                       "documents": [],
                       "contracts": []}
        if w_edo:
            person_data["edo_flag"] = True

        self.content.append(person_data)
        return self

    def add_contract(self, contract_id, person_id, indv_documents_flag):
        person = [person_info for person_info in self.content if person_info["party_id"] == person_id]
        assert len(person) > 0, 'add person before'
        person = person[0]
        contracts = [contract_info for contract_info in person["contracts"] if
                     contract_info["contract_id"] == contract_id]
        assert len(contracts) == 0, 'duplicate contract'
        contract_data = {"contract_id": contract_id,
                         "documents": []}
        if indv_documents_flag is not None:
            contract_data['indv_documents_flag'] = indv_documents_flag
        person["contracts"].append(contract_data)

        return self

    def add_document(self, document_id, document_type, person_id, document_number, amount=10, currency_code='RUB',
                     contract_id=None, w_edo=False, document_dt=NOW, invoice_id=None):
        person = [person_info for person_info in self.content if person_info["party_id"] == person_id]
        assert len(person) > 0, 'add person before'
        person = person[0]

        contract_documents = []
        if contract_id:
            contracts = [contract_info for contract_info in person["contracts"] if
                         contract_info["contract_id"] == contract_id]
            assert len(contracts) > 0, 'add contract before'
            assert len(contracts) == 1, 'duplicate_contract'
            contract_documents = contracts[0]["documents"]

        person_documents = person["documents"]

        documents = [document for document in chain(person_documents, contract_documents) if
                     document["doc_type"] == document_type and document["doc_id"] == document_id]
        assert len(documents) == 0, 'duplicate document'

        document_info = {"doc_type": document_type,
                         "doc_id": document_id,
                         "doc_number": document_number,
                         "edo_enabled_flag": w_edo,
                         "amount": amount,
                         "doc_date": document_dt.strftime("%Y-%m-%d"),
                         "currency_code": currency_code}

        if contract_id:
            contract_documents.append(document_info)
        else:
            person_documents.append(document_info)

        if document_type == 'INV_ADV':
            document_info["bill_number"] = invoice_id
        return self

    def get_response(self):
        return self.page


ERROR_ANSWER = {
    "timestamp": "2022-01-17T15:08:43.416+00:00",
    "status": 400,
    "error": "Bad Request",
    "errors": [
        {
            "codes": [
                "NotNull.findDocumentsRequest.dateTo",
                "NotNull.dateTo",
                "NotNull.java.time.LocalDate",
                "NotNull"
            ],
            "arguments": [
                {
                    "codes": [
                        "findDocumentsRequest.dateTo",
                        "dateTo"
                    ],
                    "arguments": None,
                    "defaultMessage": "dateTo",
                    "code": "dateTo"
                }
            ],
            "defaultMessage": u"должно быть задано",
            "objectName": "findDocumentsRequest",
            "field": "dateTo",
            "rejectedValue": None,
            "bindingFailure": True,
            "code": "NotNull"
        }
    ],
    "message": "Bad Request",
    "path": "/public/api/v1/documents"
}


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_empty_response_inv(session):
    httpretty.register_uri(
        httpretty.POST,
        YADOC_URL_DOCUMENTS,
        json.dumps(EMPTY_RESPONSE),
        status=200)
    invoice = ob.InvoiceBuilder.construct(session)
    response = YaDocApi(session).get_invoices_documents([invoice.external_id])
    hamcrest.assert_that(
        response,
        hamcrest.has_entries(
            items=hamcrest.has_length(0)))


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_non_valid_response(session):
    session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'INVOICE': ["INV_ADV", "BILL"]}
    httpretty.register_uri(
        httpretty.POST,
        YADOC_URL_DOCUMENTS,
        '[',
        status=200)

    invoice = ob.InvoiceBuilder.construct(session)
    with pytest.raises(exc.YADOC_API_CALL_ERROR) as exc_info:
        YaDocApi(session).get_invoices_documents([invoice.external_id])
    assert exc_info.value.msg == 'Error while calling api: invalid answer format - ['


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_non_200_response(session):
    session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'INVOICE': ["INV_ADV", "BILL"]}

    def request_callback(request, uri, response_headers):
        return [400, [], json.dumps(ERROR_ANSWER)]

    httpretty.register_uri(
        httpretty.POST,
        YADOC_URL_DOCUMENTS,
        body=request_callback)

    invoice = ob.InvoiceBuilder.construct(session)
    with pytest.raises(exc.YADOC_API_CALL_ERROR) as exc_info:
        YaDocApi(session).get_invoices_documents([invoice.external_id])
    assert exc_info.value.msg == 'Error while calling api: yadoc HttpError 400 on https://yadoc-test.mba.yandex-team.ru/public/api/v1/documents?page=0&size=20'


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_invoice_empty_result(session, person, contract):
    def request_callback(request, uri, response_headers):
        return [200, [], json.dumps(MockResponse().get_response())]

    httpretty.register_uri(
        httpretty.POST,
        YADOC_URL_DOCUMENTS,
        body=request_callback)

    invoice = ob.InvoiceBuilder.construct(session)
    response = YaDocApi(session).get_invoices_documents([invoice.external_id])
    hamcrest.assert_that(
        response,
        hamcrest.has_entries(
            items=hamcrest.has_length(0)))


@pytest.mark.parametrize('config', [
    {},
    {'INVOICE': []},
    {'INVOICE': ['INV_ADV']},
    {'INVOICE': ['BILL']},
])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_invoice_one_page_inv_adv(session, person, contract, config):
    def request_callback(request, uri, response_headers):
        request = json.loads(request.body)
        page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
        if page_num == 0 and "bill_number" in request:
            return [200, [], json.dumps((MockResponse()
                                         .add_person(person.id)
                                         .add_contract(contract.id, person.id, indv_documents_flag=True)
                                         .add_document(666, 'INV_ADV', person.id, 123123,
                                                       contract_id=contract.id, invoice_id=invoice.external_id,
                                                       document_dt=NOW)
                                         .add_document(232, 'INV_ADV', person.id, 12312, invoice_id=invoice.external_id,
                                                       document_dt=NOW)
                                         .get_response()))]

        if page_num == 0 and "doc_number" in request:
            return [200, [], json.dumps(MockResponse().get_response())]

    session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = config

    httpretty.register_uri(
        httpretty.POST,
        YADOC_URL_DOCUMENTS,
        body=request_callback)

    invoice = ob.InvoiceBuilder.construct(session)
    response = YaDocApi(session).get_invoices_documents([invoice.external_id])
    if 'INV_ADV' in config.get('INVOICE', []):
        hamcrest.assert_that(response,
                             hamcrest.has_entries(
                                 items=hamcrest.contains_inanyorder(
                                     hamcrest.has_entries(
                                         document=hamcrest.all_of(hamcrest.has_entries(amount=10,
                                                                                       currency='RUB',
                                                                                       doc_dt=NOW,
                                                                                       doc_id=666,
                                                                                       doc_number=123123,
                                                                                       doc_type=u'INV_ADV',
                                                                                       edo_enabled_flag=False,
                                                                                       indv_documents_flag=True),
                                                                  hamcrest.has_length(8)),
                                         invoice=hamcrest.has_entries(external_id=invoice.external_id)),
                                     hamcrest.has_entries(
                                         document=hamcrest.all_of(hamcrest.has_entries(amount=10,
                                                                                       currency='RUB',
                                                                                       doc_dt=NOW,
                                                                                       doc_id=232,
                                                                                       doc_number=12312,
                                                                                       doc_type=u'INV_ADV',
                                                                                       edo_enabled_flag=False,
                                                                                       indv_documents_flag=False),
                                                                  hamcrest.has_length(8)),
                                         invoice=hamcrest.has_entries(external_id=invoice.external_id))
                                 )))
    else:
        hamcrest.assert_that(
            response,
            hamcrest.has_entries(
                items=hamcrest.has_length(0)))


@pytest.mark.parametrize("indv_documents_flag", [True, False])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_indv_documents_flag(session, person, contract, indv_documents_flag):
    def request_callback(request, uri, response_headers):
        request = json.loads(request.body)
        page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
        if page_num == 0 and "bill_number" in request:
            return [200, [], json.dumps((MockResponse()
                                         .add_person(person.id)
                                         .add_contract(contract.id, person.id, indv_documents_flag=indv_documents_flag)
                                         .add_document(666, 'INV_ADV', person.id, 123123,
                                                       contract_id=contract.id, invoice_id=invoice.external_id,
                                                       document_dt=NOW)
                                         .add_document(232, 'INV_ADV', person.id, 12312, invoice_id=invoice.external_id,
                                                       document_dt=NOW)
                                         .get_response()))]

        if page_num == 0 and "doc_number" in request:
            return [200, [], json.dumps(MockResponse().get_response())]

    session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'INVOICE': ['INV_ADV']}

    httpretty.register_uri(
        httpretty.POST,
        YADOC_URL_DOCUMENTS,
        body=request_callback)

    invoice = ob.InvoiceBuilder.construct(session)
    response = YaDocApi(session).get_invoices_documents([invoice.external_id])
    hamcrest.assert_that(response,
                         hamcrest.has_entries(
                             items=hamcrest.contains_inanyorder(
                                 hamcrest.has_entries(
                                     document=hamcrest.all_of(hamcrest.has_entries(amount=10,
                                                                                   currency='RUB',
                                                                                   doc_dt=NOW,
                                                                                   doc_id=666,
                                                                                   doc_number=123123,
                                                                                   doc_type=u'INV_ADV',
                                                                                   edo_enabled_flag=False,
                                                                                   indv_documents_flag=indv_documents_flag),
                                                              hamcrest.has_length(8)),
                                     invoice=hamcrest.has_entries(external_id=invoice.external_id)),
                                 hamcrest.has_entries(
                                     document=hamcrest.all_of(hamcrest.has_entries(amount=10,
                                                                                   currency='RUB',
                                                                                   doc_dt=NOW,
                                                                                   doc_id=232,
                                                                                   doc_number=12312,
                                                                                   doc_type=u'INV_ADV',
                                                                                   edo_enabled_flag=False,
                                                                                   indv_documents_flag=False),
                                                              hamcrest.has_length(8)),
                                     invoice=hamcrest.has_entries(external_id=invoice.external_id))
                             )))


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_download_documents_batch_invalid_classname(session, person, contract):
    def request_callback(request, uri, response_headers):
        pass

    session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {}

    httpretty.register_uri(
        httpretty.POST,
        YADOC_URL_DOCUMENTS,
        body=request_callback)

    invoice = ob.InvoiceBuilder.construct(session)
    with pytest.raises(exc.YADOC_DOCUMENTS_NOT_FOUND) as exc_info:
        response = YaDocApi(session).download_documents_batch('INVOICE', [invoice.external_id])
    assert exc_info.value.msg == 'Documents not found, reason is: unknown classname INVOICE'


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_download_documents_no_documents_when_download(session, person, contract):
    def request_callback(request, uri, response_headers):
        if request.method == 'POST':
            request = json.loads(request.body)
            page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
            if page_num == 0 and "bill_number" in request:
                return [200, [], json.dumps((MockResponse()
                                             .add_person(person.id)
                                             .add_contract(contract.id, person.id, indv_documents_flag=False)
                                             .add_document(document_id=666,
                                                           document_type='INV_ADV',
                                                           person_id=person.id,
                                                           document_number=123123,
                                                           contract_id=contract.id,
                                                           invoice_id=invoice.external_id,
                                                           document_dt=NOW)
                                             .get_response()))]

            if page_num == 0 and "doc_number" in request:
                return [200, [], json.dumps(MockResponse().get_response())]
        else:
            qs = urlparse.parse_qs(urlparse.urlparse(request.path).query)
            return [404, [], 'testtest']

    session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'INVOICE': ['BILL', 'INV_ADV']}

    httpretty.register_uri(
        httpretty.POST,
        YADOC_URL_DOCUMENTS,
        body=request_callback)

    httpretty.register_uri(
        httpretty.GET,
        YADOC_URL_DOWNLOAD,
        body=request_callback)

    invoice = ob.InvoiceBuilder.construct(session)
    with pytest.raises(exc.YADOC_DOCUMENTS_NOT_FOUND) as exc_info:
        response = YaDocApi(session).download_documents_batch('INVOICE', [invoice.external_id])
    assert exc_info.value.msg == 'Documents not found, reason is: Yadoc has no documents with ids 666'


@pytest.mark.parametrize('config', [
    {'INVOICE': []},
    {'INVOICE': ['INV_ADV']},
    {'INVOICE': ['BILL']},
])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_download_documents_batch_invoice(session, person, contract, config):
    def request_callback(request, uri, response_headers):
        if request.method == 'POST':
            request = json.loads(request.body)
            page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
            if page_num == 0 and "bill_number" in request:
                return [200, [], json.dumps((MockResponse()
                                             .add_person(person.id)
                                             .add_contract(contract.id, person.id, indv_documents_flag=False)
                                             .add_document(666, 'INV_ADV', person.id, 123123,
                                                           contract_id=contract.id, invoice_id=invoice.external_id,
                                                           document_dt=NOW)
                                             .add_document(232, 'INV_ADV', person.id, 12312,
                                                           invoice_id=invoice.external_id,
                                                           document_dt=NOW)
                                             .get_response()))]

            if page_num == 0 and "doc_number" in request:
                return [200, [], json.dumps(MockResponse().get_response())]
        else:
            qs = urlparse.parse_qs(urlparse.urlparse(request.path).query)
            return [200, [], 'testtest']

    session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = config

    httpretty.register_uri(
        httpretty.POST,
        YADOC_URL_DOCUMENTS,
        body=request_callback)

    httpretty.register_uri(
        httpretty.GET,
        YADOC_URL_DOWNLOAD,
        body=request_callback)

    invoice = ob.InvoiceBuilder.construct(session)
    if 'INV_ADV' in config.get('INVOICE', []):
        YaDocApi(session).download_documents_batch('INVOICE', [invoice.external_id])
    else:
        with pytest.raises(exc.YADOC_DOCUMENTS_NOT_FOUND) as exc_info:
            YaDocApi(session).download_documents_batch('INVOICE', [invoice.external_id])
        assert exc_info.value.msg == u'Documents not found, reason is: Yadoc has no documents for INVOICE {}'.format(
            invoice.external_id)


@pytest.mark.parametrize('edo_enabled_flag', [True, False])
@pytest.mark.parametrize('indv_documents_flag', [True, False])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_download_documents_depend_on_flags(session, person, contract, indv_documents_flag, edo_enabled_flag):
    def request_callback(request, uri, response_headers):
        if request.method == 'POST':
            request = json.loads(request.body)
            page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
            if page_num == 0 and "bill_number" in request:
                return [200, [], json.dumps((MockResponse()
                                             .add_person(person.id)
                                             .add_contract(contract.id, person.id,
                                                           indv_documents_flag=indv_documents_flag)
                                             .add_document(666, 'INV_ADV', person.id, 123123,
                                                           contract_id=contract.id, invoice_id=invoice.external_id,
                                                           document_dt=NOW, w_edo=edo_enabled_flag)
                                             .get_response()))]

            if page_num == 0 and "doc_number" in request:
                return [200, [], json.dumps(MockResponse().get_response())]
        else:
            qs = urlparse.parse_qs(urlparse.urlparse(request.path).query)
            return [200, [], 'testtest']

    session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'INVOICE': ['INV_ADV']}

    httpretty.register_uri(
        httpretty.POST,
        YADOC_URL_DOCUMENTS,
        body=request_callback)

    httpretty.register_uri(
        httpretty.GET,
        YADOC_URL_DOWNLOAD,
        body=request_callback)

    invoice = ob.InvoiceBuilder.construct(session)
    if not edo_enabled_flag and not indv_documents_flag:
        YaDocApi(session).download_documents_batch('INVOICE', [invoice.external_id])
    else:
        with pytest.raises(exc.YADOC_DOCUMENTS_NOT_FOUND_FILTERED) as exc_info:
            YaDocApi(session).download_documents_batch('INVOICE', [invoice.external_id])
        assert exc_info.value.msg == u'Documents were found, but filtered by: edo_enabled_flag or indv_documents_flag'.format(
            invoice.external_id)


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_filter_documents_depend_on_flags(session, person, contract):
    def request_callback(request, uri, response_headers):
        if request.method == 'POST':
            request = json.loads(request.body)
            page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
            if page_num == 0 and "bill_number" in request:
                return [200, [], json.dumps((MockResponse()
                                             .add_person(person.id)
                                             .add_document(666, 'INV_ADV', person.id, 123123,
                                                           invoice_id=invoice.external_id,
                                                           document_dt=NOW, w_edo=True)
                                             .add_document(66666, 'INV_ADV', person.id, 123123,
                                                           invoice_id=invoice.external_id,
                                                           document_dt=NOW, w_edo=False)
                                             .get_response()))]

            if page_num == 0 and "doc_number" in request:
                return [200, [], json.dumps(MockResponse().get_response())]
        else:
            qs = urlparse.parse_qs(urlparse.urlparse(request.path).query)
            assert qs['document_ids'] == ['66666']
            return [200, [], 'testtest']

    session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'INVOICE': ['INV_ADV']}

    httpretty.register_uri(
        httpretty.POST,
        YADOC_URL_DOCUMENTS,
        body=request_callback)

    httpretty.register_uri(
        httpretty.GET,
        YADOC_URL_DOWNLOAD,
        body=request_callback)

    invoice = ob.InvoiceBuilder.construct(session)
    YaDocApi(session).download_documents_batch('INVOICE', [invoice.external_id])


@pytest.mark.usefixtures('httpretty_enabled_fixture')
@pytest.mark.parametrize('document_id', [666, 777])
def test_download_documents_batch_contract(session, person, contract, document_id):
    def request_callback(request, uri, response_headers):
        if request.method == 'POST':
            request = json.loads(request.body)
            assert set(request["doc_type"]) == {'PARTNER_INV', 'PARTNER_ACT'}
            return [200, [], json.dumps((MockResponse(total_elements=2)
                                         .add_person(person.id)
                                         .add_contract(contract.id, person.id, indv_documents_flag=False)
                                         .add_document(666, 'PARTNER_INV', person.id, '2312',
                                                       contract_id=contract.id,
                                                       document_dt=NOW)
                                         .get_response()))]
        else:
            return [200, [], 'testtest']

    session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'CONTRACT': ["PARTNER_ACT", "PARTNER_INV"]}

    httpretty.register_uri(
        httpretty.POST,
        YADOC_URL_DOCUMENTS,
        body=request_callback)

    httpretty.register_uri(
        httpretty.GET,
        YADOC_URL_DOWNLOAD,
        body=request_callback)

    if document_id == 666:
        YaDocApi(session).download_documents_batch('CONTRACT',
                                                   [contract.id],
                                                   document_ids=[document_id])
    else:
        with pytest.raises(exc.YADOC_DOCUMENTS_NOT_FOUND) as exc_info:
            YaDocApi(session).download_documents_batch('CONTRACT',
                                                       [contract.id],
                                                       document_ids=[document_id])
        assert exc_info.value.msg == u'Documents not found, reason is: Yadoc documents with ids 777 not related with ' \
                                     u'CONTRACT {}'.format(contract.id)


@pytest.mark.usefixtures('httpretty_enabled_fixture')
@pytest.mark.parametrize('edo_enabled_flag', [
    True,
    False
])
@pytest.mark.parametrize('indv_documents_flag', [
    True,
    False])
def test_download_documents_batch_contract_flags_check(session, person, contract, edo_enabled_flag,
                                                       indv_documents_flag):
    def request_callback(request, uri, response_headers):
        if request.method == 'POST':
            request = json.loads(request.body)
            assert set(request["doc_type"]) == {'PARTNER_INV', 'PARTNER_ACT'}
            return [200, [], json.dumps((MockResponse(total_elements=2)
                                         .add_person(person.id)
                                         .add_contract(contract.id, person.id, indv_documents_flag=indv_documents_flag)
                                         .add_document(666,
                                                       'PARTNER_INV',
                                                       person.id,
                                                       '2312',
                                                       contract_id=contract.id,
                                                       document_dt=NOW,
                                                       w_edo=edo_enabled_flag)
                                         .get_response()))]
        else:
            return [200, [], 'testtest']

    session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'CONTRACT': ["PARTNER_ACT", "PARTNER_INV"]}

    httpretty.register_uri(
        httpretty.POST,
        YADOC_URL_DOCUMENTS,
        body=request_callback)

    httpretty.register_uri(
        httpretty.GET,
        YADOC_URL_DOWNLOAD,
        body=request_callback)

    if not edo_enabled_flag and not indv_documents_flag:
        YaDocApi(session).download_documents_batch('CONTRACT',
                                                   [contract.id],
                                                   document_ids=[666])
    else:
        with pytest.raises(exc.YADOC_DOCUMENTS_NOT_FOUND_FILTERED) as exc_info:
            YaDocApi(session).download_documents_batch('CONTRACT',
                                                       [contract.id],
                                                       document_ids=[666])

        assert exc_info.value.msg == u'Documents were found, but filtered by: edo_enabled_flag or indv_documents_flag'


@pytest.mark.parametrize('w_factura', [True, False])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_act_one_page_inv(session, act, w_factura):
    session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'ACT': ["ACT", "INV"]}
    person = act.invoice.person
    if w_factura:
        act.factura = ob.get_big_number()
    else:
        act.factura = None

    session.flush()

    def request_callback(request, uri, response_headers):
        request = json.loads(request.body)

        if 'ACT' in request["doc_type"]:
            assert request["doc_number"][0] == act.external_id
            return [200, [], json.dumps((MockResponse()
                                         .add_person(person.id)
                                         .add_document(666, 'ACT', person.id, act.external_id,
                                                       document_dt=NOW)
                                         .get_response()))]

        if 'INV' in request["doc_type"]:
            assert request["doc_number"][0] == act.factura
            return [200, [], json.dumps((MockResponse()
                                         .add_person(person.id)
                                         .add_document(9888, 'INV', person.id, 34343, document_dt=NOW)
                                         .get_response()))]

    httpretty.register_uri(
        httpretty.POST,
        YADOC_URL_DOCUMENTS,
        body=request_callback)

    response = YaDocApi(session).get_acts_documents([act.external_id])

    if w_factura:
        hamcrest.assert_that(
            response,
            hamcrest.has_entries(
                items=hamcrest.all_of(hamcrest.has_length(2),
                                      hamcrest.contains_inanyorder(
                                          hamcrest.has_entries(
                                              document=hamcrest.all_of(hamcrest.has_entries(amount=10,
                                                                                            currency='RUB',
                                                                                            doc_dt=NOW,
                                                                                            doc_id=666,
                                                                                            doc_number=act.external_id,
                                                                                            doc_type=u'ACT',
                                                                                            edo_enabled_flag=False,
                                                                                            indv_documents_flag=False),
                                                                       hamcrest.has_length(8)),
                                              act=hamcrest.has_entries(external_id=act.external_id)),
                                          hamcrest.has_entries(
                                              document=hamcrest.all_of(hamcrest.has_entries(amount=10,
                                                                                            currency='RUB',
                                                                                            doc_dt=NOW,
                                                                                            doc_id=9888,
                                                                                            doc_number=34343,
                                                                                            doc_type=u'INV',
                                                                                            edo_enabled_flag=False,
                                                                                            indv_documents_flag=False),
                                                                       hamcrest.has_length(8)),
                                              act=hamcrest.has_entries(external_id=None))
                                      ))))
    else:
        hamcrest.assert_that(
            response,
            hamcrest.has_entries(
                items=hamcrest.all_of(hamcrest.has_length(1),
                                      hamcrest.contains_inanyorder(
                                          hamcrest.has_entries(
                                              document=hamcrest.all_of(hamcrest.has_entries(amount=10,
                                                                                            currency='RUB',
                                                                                            doc_dt=NOW,
                                                                                            doc_id=666,
                                                                                            doc_number=act.external_id,
                                                                                            doc_type=u'ACT',
                                                                                            edo_enabled_flag=False,
                                                                                            indv_documents_flag=False),
                                                                       hamcrest.has_length(8)),
                                              act=hamcrest.has_entries(external_id=act.external_id))))))


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_contract_empty_ans(session, contract, person):
    session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'CONTRACT': ['PARTNER_INV', 'PARTNER_ACT']}

    def request_callback(request, uri, response_headers):
        request = json.loads(request.body)

        assert request["doc_type"] == ['PARTNER_INV', 'PARTNER_ACT']
        return [200, [], json.dumps((MockResponse()
                                     .get_response()))]

    httpretty.register_uri(
        httpretty.POST,
        YADOC_URL_DOCUMENTS,
        body=request_callback)
    response = YaDocApi(session).get_contracts_documents([contract.id],
                                                         YEAR_AGO,
                                                         NOW,
                                                         page_number=0,
                                                         page_size=20)
    hamcrest.assert_that(
        response,
        hamcrest.has_entries(
            items=hamcrest.has_length(0),
            request=hamcrest.has_entries(pagination_pn=0,
                                         pagination_ps=20),
            totals=hamcrest.has_entries(row_count=0)))


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_contract_w_ans(session, contract, person):
    session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'CONTRACT': ['PARTNER_INV', 'PARTNER_ACT']}

    def request_callback(request, uri, response_headers):
        request = json.loads(request.body)

        assert request["doc_type"] == ['PARTNER_INV', 'PARTNER_ACT']
        return [200, [], json.dumps((MockResponse(total_elements=2)
                                     .add_person(person.id)
                                     .add_contract(contract.id, person.id, indv_documents_flag=True)
                                     .add_document(666, 'PARTNER_INV', person.id, '2312', contract_id=contract.id,
                                                   document_dt=NOW)
                                     .add_document(777, 'PARTNER_ACT', person.id, '23241', w_edo=True,
                                                   contract_id=contract.id, document_dt=NOW)
                                     .get_response()))]

    httpretty.register_uri(
        httpretty.POST,
        YADOC_URL_DOCUMENTS,
        body=request_callback)

    response = YaDocApi(session).get_contracts_documents([contract.id
                                                          ],
                                                         YEAR_AGO,
                                                         NOW,
                                                         page_number=0,
                                                         page_size=20)
    hamcrest.assert_that(
        response,
        hamcrest.has_entries(
            items=hamcrest.all_of(hamcrest.has_length(2),
                                  hamcrest.contains_inanyorder(
                                      hamcrest.has_entries(
                                          document=hamcrest.all_of(hamcrest.has_entries(amount=10,
                                                                                        currency='RUB',
                                                                                        doc_dt=NOW,
                                                                                        doc_id=666,
                                                                                        doc_number='2312',
                                                                                        doc_type=u'PARTNER_INV',
                                                                                        edo_enabled_flag=False,
                                                                                        indv_documents_flag=True),
                                                                   hamcrest.has_length(8)),
                                          contract=hamcrest.has_entries(id=contract.id)),
                                      hamcrest.has_entries(
                                          document=hamcrest.all_of(hamcrest.has_entries(amount=10,
                                                                                        currency='RUB',
                                                                                        doc_dt=NOW,
                                                                                        doc_id=777,
                                                                                        doc_number='23241',
                                                                                        doc_type=u'PARTNER_ACT',
                                                                                        edo_enabled_flag=True,
                                                                                        indv_documents_flag=True),
                                                                   hamcrest.has_length(8)),
                                          contract=hamcrest.has_entries(id=contract.id))
                                  )),
            request=hamcrest.has_entries(pagination_pn=0,
                                         pagination_ps=20),
            totals=hamcrest.has_entries(row_count=2)))


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_contract_one_page_act(session, contract, person):
    session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'CONTRACT': ['PARTNER_INV', 'PARTNER_ACT']}

    def request_callback(request, uri, response_headers):
        request = json.loads(request.body)

        assert request["contract_id"] == [contract.id]
        return [200, [], json.dumps((MockResponse()
                                     .add_person(person.id)
                                     .add_contract(contract.id, person.id, indv_documents_flag=False)
                                     .add_document(777, 'PARTNER_ACT', person.id, '23241', w_edo=True,
                                                   contract_id=contract.id, document_dt=NOW)
                                     .get_response()))]

    httpretty.register_uri(
        httpretty.POST,
        YADOC_URL_DOCUMENTS,
        body=request_callback)
    response = YaDocApi(session).get_contracts_documents([contract.id],
                                                         YEAR_AGO,
                                                         NOW,
                                                         page_number=0,
                                                         page_size=20)

    hamcrest.assert_that(
        response,
        hamcrest.has_entries(
            items=hamcrest.all_of(hamcrest.has_length(1),
                                  hamcrest.contains_inanyorder(
                                      hamcrest.has_entries(
                                          document=hamcrest.all_of(hamcrest.has_entries(amount=10,
                                                                                        currency='RUB',
                                                                                        doc_dt=NOW,
                                                                                        doc_id=777,
                                                                                        doc_number='23241',
                                                                                        doc_type=u'PARTNER_ACT',
                                                                                        edo_enabled_flag=True,
                                                                                        indv_documents_flag=False),
                                                                   hamcrest.has_length(8)),
                                          contract=hamcrest.has_entries(id=contract.id)))),
            request=hamcrest.has_entries(pagination_pn=0,
                                         pagination_ps=20),
            totals=hamcrest.has_entries(row_count=1)))


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_invoice_one_page_inv_adv_wo_contract(session, person):
    session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'INVOICE': ["INV_ADV", "BILL"]}

    def request_callback(request, uri, response_headers):
        request = json.loads(request.body)
        page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
        if page_num == 0 and "bill_number" in request:
            return [200, [], json.dumps((MockResponse()
                                         .add_person(person.id)
                                         .add_document(232, 'INV_ADV', person.id, 12312, invoice_id=invoice.external_id,
                                                       document_dt=NOW)
                                         .get_response()))]

        if page_num == 0 and "doc_number" in request:
            return [200, [], json.dumps(MockResponse().get_response())]

    httpretty.register_uri(
        httpretty.POST,
        YADOC_URL_DOCUMENTS,
        body=request_callback)

    invoice = ob.InvoiceBuilder.construct(session)
    response = YaDocApi(session).get_invoices_documents([invoice.external_id])
    hamcrest.assert_that(
        response,
        hamcrest.has_entries(
            items=hamcrest.all_of(hamcrest.has_length(1),
                                  hamcrest.contains_inanyorder(
                                      hamcrest.has_entries(
                                          document=hamcrest.all_of(hamcrest.has_entries(amount=10,
                                                                                        currency='RUB',
                                                                                        doc_dt=NOW,
                                                                                        doc_id=232,
                                                                                        doc_number=12312,
                                                                                        doc_type=u'INV_ADV',
                                                                                        edo_enabled_flag=False,
                                                                                        indv_documents_flag=False),
                                                                   hamcrest.has_length(8)),
                                          invoice=hamcrest.has_entries(external_id=invoice.external_id))))))


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_multiple_pages_same_type(session, person, contract):
    session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'INVOICE': ["INV_ADV", "BILL"]}

    def request_callback(request, uri, response_headers):
        request = json.loads(request.body)
        page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
        if page_num == 0 and "bill_number" in request:
            return [200, [], json.dumps((MockResponse(page_number=0, last_page=False)
                                         .add_person(person.id)
                                         .add_contract(contract.id, person.id, indv_documents_flag=False)
                                         .add_document(232, 'INV_ADV', person.id, 12312, invoice_id=invoice.external_id,
                                                       contract_id=contract.id, document_dt=NOW)
                                         .get_response()))]

        if page_num == 1 and "bill_number" in request:
            return [200, [], json.dumps((MockResponse(page_number=1, last_page=True)
                                         .add_person(person.id)
                                         .add_document(23, 'BILL', person.id, invoice.external_id, invoice_id='3242rr',
                                                       document_dt=NOW)
                                         .get_response()))]

        if page_num == 0 and "doc_number" in request:
            return [200, [], json.dumps(MockResponse().get_response())]

    httpretty.register_uri(
        httpretty.POST,
        YADOC_URL_DOCUMENTS,
        body=request_callback)

    invoice = ob.InvoiceBuilder.construct(session)
    response = YaDocApi(session).get_invoices_documents([invoice.external_id])

    hamcrest.assert_that(
        response,
        hamcrest.has_entries(
            items=hamcrest.all_of(hamcrest.has_length(2),
                                  hamcrest.contains_inanyorder(
                                      hamcrest.has_entries(
                                          document=hamcrest.all_of(hamcrest.has_entries(amount=10,
                                                                                        currency='RUB',
                                                                                        doc_dt=NOW,
                                                                                        doc_id=232,
                                                                                        doc_number=12312,
                                                                                        doc_type=u'INV_ADV',
                                                                                        edo_enabled_flag=False,
                                                                                        indv_documents_flag=False),
                                                                   hamcrest.has_length(8)),
                                          invoice=hamcrest.has_entries(external_id=invoice.external_id)),
                                      hamcrest.has_entries(
                                          document=hamcrest.all_of(hamcrest.has_entries(amount=10,
                                                                                        currency='RUB',
                                                                                        doc_dt=NOW,
                                                                                        doc_id=23,
                                                                                        doc_number=invoice.external_id,
                                                                                        doc_type=u'BILL',
                                                                                        edo_enabled_flag=False,
                                                                                        indv_documents_flag=False),
                                                                   hamcrest.has_length(8)),
                                          invoice=hamcrest.has_entries(external_id=invoice.external_id))
                                  ))))


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_multiple_pages_diff_types(session, person, contract):
    session.config.__dict__['YADOC_AVAILABLE_DOCUMENT_TYPES'] = {'INVOICE': ["INV_ADV", "BILL"]}

    def request_callback(request, uri, response_headers):
        request = json.loads(request.body)
        page_num = int(urlparse.parse_qs(urlparse.urlparse(uri).query)["page"][0])
        if page_num == 0 and "bill_number" in request:
            return [200, [], json.dumps((MockResponse()
                                         .add_person(person.id)
                                         .add_contract(contract.id, person.id, indv_documents_flag=False)
                                         .add_document(666, 'INV_ADV', person.id, 234, contract_id=contract.id,
                                                       document_dt=NOW)
                                         .get_response()))]

        if page_num == 0 and "doc_number" in request:
            return [200, [], json.dumps((MockResponse()
                                         .add_person(person.id)
                                         .add_contract(contract.id, person.id, indv_documents_flag=False)
                                         .add_document(23, 'BILL', person.id, invoice.external_id, invoice_id='3242rr',
                                                       document_dt=NOW)
                                         .get_response()))]

    httpretty.register_uri(
        httpretty.POST,
        YADOC_URL_DOCUMENTS,
        body=request_callback)

    invoice = ob.InvoiceBuilder.construct(session)
    response = YaDocApi(session).get_invoices_documents([invoice.external_id])
    hamcrest.assert_that(
        response,
        hamcrest.has_entries(
            items=hamcrest.all_of(hamcrest.has_length(2),
                                  hamcrest.contains_inanyorder(
                                      hamcrest.has_entries(
                                          document=hamcrest.all_of(hamcrest.has_entries(amount=10,
                                                                                        currency='RUB',
                                                                                        doc_dt=NOW,
                                                                                        doc_id=666,
                                                                                        doc_number=234,
                                                                                        doc_type=u'INV_ADV',
                                                                                        edo_enabled_flag=False,
                                                                                        indv_documents_flag=False),
                                                                   hamcrest.has_length(8)),
                                          invoice=hamcrest.has_entries(external_id=None)),
                                      hamcrest.has_entries(
                                          document=hamcrest.all_of(hamcrest.has_entries(amount=10,
                                                                                        currency='RUB',
                                                                                        doc_dt=NOW,
                                                                                        doc_id=23,
                                                                                        doc_number=invoice.external_id,
                                                                                        doc_type=u'BILL',
                                                                                        edo_enabled_flag=False,
                                                                                        indv_documents_flag=False),
                                                                   hamcrest.has_length(8)),
                                          invoice=hamcrest.has_entries(external_id=invoice.external_id))
                                  ))))


@pytest.mark.usefixtures('httpretty_enabled_fixture')
@pytest.mark.parametrize('response', [
    (
        http.OK,
        [
            {"short_code": "YMAR", "org_name": "ООО \"Яндекс.Маркет\"", "source_system": "OEBS", "source_entity_id": "64554"},
            {"short_code": "YACL", "org_name": "ООО \"Яндекс.Облако\"", "source_system": "OEBS", "source_entity_id": "145543"},
            {"short_code": "YASH", "org_name": "АНО ДПО ШАД", "source_system": "OEBS", "source_entity_id": "55562"},
            {"short_code": "YARU", "org_name": "ООО \"ЯНДЕКС\"", "source_system": "OEBS", "source_entity_id": "121"},
            {"short_code": "YTAX", "org_name": "ООО \"Яндекс.Такси\"", "source_system": "OEBS", "source_entity_id": "64552"},
            {"short_code": "UBRU", "org_name": "ООО \"Яндекс.Логистика\"", "source_system": "OEBS", "source_entity_id": "111012"},
            {"short_code": "YDRV", "org_name": "ООО \"Яндекс.Драйв\"", "source_system": "OEBS", "source_entity_id": "118724"},
            {"short_code": "YFUL", "org_name": "ООО \"Яндекс.Заправки\"", "source_system": "OEBS", "source_entity_id": "150405"},
            {"short_code": "YEDA", "org_name": "ООО \"Яндекс.Еда\"", "source_system": "OEBS", "source_entity_id": "127556"},
            {"short_code": "BYAD", "org_name": "ООО Яндекс Реклама", "source_system": "OEBS", "source_entity_id": "107441"},
            {"short_code": "UBKZ", "org_name": "ТОО \"Яндекс.Такси Корп\"", "source_system": "OEBS", "source_entity_id": "114867"},
            {"short_code": "GOIL", "org_name": "Yango.Taxi Ltd", "source_system": "OEBS", "source_entity_id": "6586046"},
            {"short_code": "YAKZ", "org_name": "ТОО Яндекс.Казахстан", "source_system": "OEBS", "source_entity_id": "97308"},
            {"short_code": "YKZT", "org_name": "ТОО Яндекс.Еда Казахстан", "source_system": "OEBS", "source_entity_id": "94969"},
            {"short_code": "UMBN", "org_name": "Uber ML B.V.", "source_system": "OEBS", "source_entity_id": "281118"},
            {"short_code": "YVER", "org_name": "ООО \"Яндекс.Вертикали\"", "source_system": "OEBS", "source_entity_id": "64553"},
            {"short_code": "YZDR", "org_name": "ООО \"Клиника Яндекс.Здоровье\"", "source_system": "OEBS", "source_entity_id": "119318"},
            {"short_code": "KZCL", "org_name": "ТОО Яндекс.Облако Казахстан", "source_system": "OEBS", "source_entity_id": "272037"}
        ]
    ),
    (http.FORBIDDEN, {"error": "This client does not have any active IDM roles for YaDoc"}),
    (http.INTERNAL_SERVER_ERROR, None)

])
def test_organizations(session, response):
    httpretty.register_uri(
        httpretty.GET,
        YADOC_URL_ORGANIZATIONS,
        json.dumps(response[1]),
        status=response[0],
        content_type='application/json',
    )

    if response[0] == http.OK:
        res = YaDocApi(session).get_available_organizations()
        hamcrest.assert_that(res, hamcrest.has_length(18))
    else:
        with pytest.raises(exc.YADOC_API_CALL_ERROR) as e:
            YaDocApi(session).get_available_organizations()
        hamcrest.assert_that(e.value.msg, hamcrest.contains_string(str(response[0])))


@pytest.mark.usefixtures('httpretty_enabled_fixture')
@pytest.mark.parametrize('response', [
    (http.OK, {"close_date": "31.01.2022", "status": "SUCCESS"}),
    (http.OK, {"close_date": None, "status": "SUCCESS"}),
    (http.NOT_FOUND, {}),
    (http.BAD_REQUEST, {"errors": ["описание ошибки от OEBS"]}),
    (http.INTERNAL_SERVER_ERROR, {"status": "ERROR", "message": "OEBS raised error during DB call."}),

])
def test_periods(session, response):
    httpretty.register_uri(
        httpretty.POST,
        YADOC_URL_PERIODS,
        json.dumps(response[1]),
        status=response[0],
        content_type='application/json',
    )

    if response[0] == http.OK:
        res = YaDocApi(session).get_ap_closed_period('121')
        hamcrest.assert_that(
            res,
            hamcrest.equal_to(
                datetime.datetime(2022, 1, 31) if response[1]['close_date'] is not None else None
            )
        )
    else:
        with pytest.raises(exc.YADOC_API_CALL_ERROR):
            YaDocApi(session).get_available_organizations()
