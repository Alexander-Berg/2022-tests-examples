# -*- coding: utf-8 -*-

"test_reconciliation"

from __future__ import unicode_literals

import datetime as dt
from decimal import Decimal

import sqlalchemy as sa

import balance.mapper as mapper
import balance.muzzle_util as ut

import requests
import pytest
import mock
import hamcrest
import httpretty
import copy
import json
import urlparse
from urllib3.exceptions import MaxRetryError
from itertools import chain
from balance import exc
from balance.api.reconciliation_report import ReconciliationReportApi
from balance.application import getApplication
from tests import object_builder as ob


NOW = dt.datetime.now()

def URL():
    res = getApplication().get_component_cfg('reconciliation_report')['Url']
    return res


@pytest.fixture
def client(session):
    return ob.ClientBuilder.construct(session)


@pytest.fixture
def person(session, client):
    return ob.PersonBuilder.construct(session, client=client, type='ur')


@pytest.fixture
def firm(session):
    _firm = ob.FirmBuilder.construct(session)
    oebs_org_id = ob.get_big_number()
    session.execute(
        '''insert into bo.t_firm_export (firm_id, export_type, oebs_org_id) values (:firm_id, 'OEBS', :oebs_org_id)''',
        {'firm_id': _firm.id, 'oebs_org_id': oebs_org_id},
    )
    session.config.__dict__['RECONCILIATION_REPORT_FIRMS'] = [_firm.id]
    return _firm


@pytest.fixture
def contract(session, client, person, firm):
    return ob.ContractBuilder.construct(session, client=client, person=person, firm=firm.id)


@pytest.fixture
def reconciliation_request(session, client, person, contract, firm, request_id='super_reconciliation_request_id_3000'):
     reconciliation_request = mapper.ReconciliationRequest(
        external_id=request_id,
        client_id=client.id,
        dt=dt.datetime(2020, 10, 10),
        contract_id=contract.id,
        person_id=person.id,
        firm_id=firm.id,
        dt_from=dt.datetime(2020, 10, 1),
        dt_to=dt.datetime(2020, 10, 31),
        email='ho4y_email@o4en.net',
        status='COMPLETED',
        error='',
     )
     session.add(reconciliation_request)
     session.flush()
     return reconciliation_request


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_create_request_wo_error(session, client, person, contract, firm):
    request_id = 'super_reconciliation_request_id_3000'
    req = {
        'firm_id': firm.id,
        'dt_from': dt.datetime(2020, 10, 1),
        'dt_to': dt.datetime(2020, 10, 31),
        'email': 'ho4y_email@o4en.net',
        'person_id': person.id,
        'contract_id': contract.id
    }
    httpretty.register_uri(
        httpretty.POST,
        urlparse.urljoin(URL(), 'report'),
        request_id,
        status=200,
        content_type='application/json'
    )

    with mock.patch(
        'balance.api.reconciliation_report.ReconciliationReportApi._get_tvm_ticket',
        return_value='666'
    ):
        result = ReconciliationReportApi(session).create_request(**req)

    session.flush()

    request = session.query(mapper.ReconciliationRequest).getone(external_id=request_id)

    hamcrest.assert_that(request, hamcrest.is_(result))
    post_request, = httpretty.latest_requests()
    hamcrest.assert_that(
        json.loads(post_request.body),
        hamcrest.has_entries(
                organization_id=firm.firm_exports['OEBS'].oebs_org_id,
                period_from='2020-10-01',
                period_to='2020-10-31',
                email='ho4y_email@o4en.net',
                person_id=person.id,
        )
    )
    hamcrest.assert_that(
        request,
        hamcrest.has_properties(
            external_id=request_id,
            client_id=client.id,
            person_id=person.id,
            contract_id=contract.id,
            status='NEW',
            error=None
        )
    )
    hamcrest.assert_that(request.client, hamcrest.is_(client))
    hamcrest.assert_that(request.person, hamcrest.is_(person))
    hamcrest.assert_that(request.contract, hamcrest.is_(contract))


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_create_request_wo_obligatory_params(session, person):
    request_id = 'super_reconciliation_request_id_3000'
    req = {
        'firm_id': '',
        'dt_from': dt.datetime(2020, 10, 1),
        'dt_to': dt.datetime(2020, 10, 31),
        'email': 'ho4y_email@o4en.net',
        'person_id': person.id,
    }

    with mock.patch(
        'balance.api.reconciliation_report.ReconciliationReportApi._get_tvm_ticket',
        return_value='666',
    ), pytest.raises(exc.RECONCILIATION_REPORT_BAD_REQUEST_ERROR) as e:
        ReconciliationReportApi(session).create_request(**req)

    assert e.value.msg == 'Bad request error while calling api: {}'.format(
        'it is required to fill in either person_id and firm_id, or contract_id'
    )


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_check_status_wo_error(session, reconciliation_request):
    request_id = 'super_reconciliation_request_id_3000'
    httpretty.register_uri(
        httpretty.GET,
        urlparse.urljoin(URL(), 'report/{}'.format(request_id)),
        json.dumps({'status': 'COMPLETED', 'message': ''}),
        status=200,
        content_type='application/json'
    )

    with mock.patch(
        'balance.api.reconciliation_report.ReconciliationReportApi._get_tvm_ticket',
        return_value='666',
    ):
        result = ReconciliationReportApi(session).check_and_update_status(reconciliation_request.id)

    hamcrest.assert_that(
        result,
        hamcrest.has_properties(
            external_id=reconciliation_request.external_id,
            status='COMPLETED',
            error=''
        )
    )

    get_request, = httpretty.latest_requests()

    hamcrest.assert_that(
        get_request.path,
        'report/{}'.format(request_id)
    )


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_check_status_w_error(session, reconciliation_request):
    request_id = 'super_reconciliation_request_id_3000'
    httpretty.register_uri(
        httpretty.GET,
        urlparse.urljoin(URL(), 'report/{}'.format(request_id)),
        json.dumps({'status': 'FAILED', 'message': 'some error'}),
        status=200,
        content_type='application/json'
    )

    with mock.patch(
        'balance.api.reconciliation_report.ReconciliationReportApi._get_tvm_ticket',
        return_value='666',
    ):
        result = ReconciliationReportApi(session).check_and_update_status(reconciliation_request.id)

    hamcrest.assert_that(
        result,
        hamcrest.has_properties(
            external_id=reconciliation_request.external_id,
            status='FAILED',
            error='some error'
        )
    )


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_check_status_w_error_unexisting_request_id(session, reconciliation_request):
    request_id = 666666666666
    httpretty.register_uri(
        httpretty.GET,
        urlparse.urljoin(URL(), 'report/{}'.format(request_id)),
        json.dumps({'status': 'FAILED', 'message': 'some error'}),
        status=200,
        content_type='application/json'
    )

    with mock.patch(
        'balance.api.reconciliation_report.ReconciliationReportApi._get_tvm_ticket',
        return_value='666',
    ), pytest.raises(exc.RECONCILIATION_REPORT_BAD_REQUEST_ERROR) as e:
        result = ReconciliationReportApi(session).check_and_update_status(request_id)

    assert e.value.msg == 'Bad request error while calling api: Reconciliation reuest {} not found'.format(request_id)


def test_hide_request(session, reconciliation_request):
    request_id = 'super_reconciliation_request_id_3000'
    with mock.patch(
        'balance.api.reconciliation_report.ReconciliationReportApi._get_tvm_ticket',
        return_value='666',
    ):
        result = ReconciliationReportApi(session).hide_request(reconciliation_request.id)

    assert reconciliation_request.hidden == 1


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_download(session, reconciliation_request):
    request_id = 'super_reconciliation_request_id_3000'

    httpretty.register_uri(
        httpretty.GET,
        urlparse.urljoin(URL(), 'report/{}/pdf'.format(reconciliation_request.external_id)),
        'https://example.net/url_for_pdf',
        status=200,
        content_type='text/plain'
    )

    httpretty.register_uri(
        httpretty.GET,
        'https://example.net/url_for_pdf',
        'pdf',
        status=200,
    )

    with mock.patch(
        'balance.api.reconciliation_report.ReconciliationReportApi._get_tvm_ticket',
        return_value='666',
    ):
        result = ReconciliationReportApi(session).download(reconciliation_request.id)

    hamcrest.assert_that(
        result,
        hamcrest.has_items(
            reconciliation_request,
            'pdf'
        )
    )


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_create_request_w_connection_error(session, firm, person):
    url = urlparse.urljoin(URL(), 'report')
    dt_format = '%d.%m.%Y'
    request_id = 'super_reconciliation_request_id_3000'
    req = {
        'firm_id': firm.id,
        'dt_from': dt.datetime(2020, 10, 1),
        'dt_to': dt.datetime(2020, 10, 31),
        'email': 'ho4y_email@o4en.net',
        'person_id': person.id,
    }

    def f_w_exc(req, url, resp_headers):
        raise requests.ConnectionError(
            MaxRetryError(mock.MagicMock(), url, '666'), request=req
        )

    httpretty.register_uri(httpretty.POST, url, body=f_w_exc)

    with mock.patch(
        'balance.api.reconciliation_report.ReconciliationReportApi._get_tvm_ticket',
        return_value='666'), pytest.raises(exc.RECONCILIATION_REPORT_CONNECTION_ERROR) as e:
        ReconciliationReportApi(session).create_request(**req)
    assert e.value.msg == 'Connection error on {}'.format(url)


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_create_request_status_code_not_200(session, firm, person):
    url = urlparse.urljoin(URL(), 'report')
    dt_format = '%d.%m.%Y'
    request_id = 'super_reconciliation_request_id_3000'
    req = {
        'firm_id': firm.id,
        'dt_from': dt.datetime(2020, 10, 1),
        'dt_to': dt.datetime(2020, 10, 31),
        'email': 'ho4y_email@o4en.net',
        'person_id': person.id,
    }

    httpretty.register_uri(httpretty.POST, url, status=500)

    with mock.patch(
        'balance.api.reconciliation_report.ReconciliationReportApi._get_tvm_ticket',
        return_value='666'), pytest.raises(exc.RECONCILIATION_REPORT_CONNECTION_ERROR) as e:
        ReconciliationReportApi(session).create_request(**req)
    assert e.value.msg == 'Connection error on {}'.format(url)
