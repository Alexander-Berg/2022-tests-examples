# -*- coding: utf-8 -*-

import sys
import datetime
import xmlrpclib

import pytest
from mock import patch

from balance.constants import *
from balance.mapper import Client

from tests import object_builder as ob
from tests import tutils as tut

NOW = datetime.datetime.now()

CODE_SUCCESS = 0


def create_client_service_data(client, service_id=ServiceId.DIRECT, **kwargs):
    client_service_data = ob.create_client_service_data(service_id=service_id, **kwargs)
    client.service_data[service_id] = client_service_data
    return client_service_data


@pytest.mark.parametrize('params', [{}, {'CLIENT_ID': ''}])
def test_defaults(xmlrpcserver, session, params):
    res = xmlrpcserver.CreateClient(session.oper_id, params)
    assert len(res) == 3
    assert (res[0], res[1]) == (CODE_SUCCESS, 'SUCCESS')
    client_id = res[2]
    client = session.query(Client).get(client_id)
    assert client.is_docs_detailed == 0
    assert client.is_docs_separated == 0
    assert client.class_id == client_id
    assert client.client_type_id == 0
    assert client.currency_payment is None
    assert client.domain_check_comment == ''
    assert client.domain_check_status == 0
    assert client.email is None
    assert client.fax is None
    assert client.fraud_status is None
    assert client.full_repayment == 1
    assert client.internal == 0
    assert client.is_agency == 0
    assert client.is_wholesaler is None
    assert client.iso_currency_payment is None
    assert client.manual_suspect == 0
    assert client.manual_suspect_comment is None
    assert client.name is None
    assert client.overdraft_ban == 0
    assert client.phone is None
    assert client.region_id is None
    assert client.reliable_cc_payer == 0
    assert client.subregion_id is None
    assert client.url is None


def test_settable(xmlrpcserver, session):
    res = xmlrpcserver.CreateClient(session.oper_id, {'CLIENT_TYPE_ID': 1,
                                                      'NAME': 'name',
                                                      'EMAIL': 'new@email.ru',
                                                      'SUBREGION_ID': 225,
                                                      'FAX': "555-1234",
                                                      'PHONE': "555-4321",
                                                      'URL': "http://yandex.ru",
                                                      'INTERNAL': 1,
                                                      'IS_WHOLESALER': 1,
                                                      'FULL_REPAYMENT': 1,
                                                      'MANUAL_SUSPECT': 1,
                                                      })
    assert len(res) == 3
    assert (res[0], res[1]) == (CODE_SUCCESS, 'SUCCESS')
    client_id = res[2]
    client = session.query(Client).get(client_id)
    assert client.is_docs_detailed == 0
    assert client.is_docs_separated == 0
    assert client.class_id == client_id
    assert client.client_type_id == 1
    assert client.currency_payment is None
    assert client.domain_check_comment == ''
    assert client.domain_check_status == 0
    assert client.email == 'new@email.ru'
    assert client.fax == '555-1234'
    assert client.fraud_status is None
    assert client.full_repayment == 1
    assert client.internal == 0
    assert client.is_agency == 0
    assert client.is_wholesaler is None
    assert client.iso_currency_payment is None
    assert client.manual_suspect == 0
    assert client.manual_suspect_comment is None
    assert client.name == 'name'
    assert client.overdraft_ban == 0
    assert client.phone == '555-4321'
    assert client.region_id is None
    assert client.reliable_cc_payer == 0
    assert client.subregion_id == 225
    assert client.url == 'http://yandex.ru'


@pytest.mark.parametrize('old_region', [
    149,
    None,
    225
])
@pytest.mark.parametrize('new_region', [
    225,
    None,
    187])
@pytest.mark.parametrize('w_order', [True,
                                     # False
                                     ])
@pytest.mark.parametrize('client_only', [0,
                                         # 1
                                         ])
@pytest.mark.parametrize('iso_currency', ['RUB',
                                          # None
                                          ])
@pytest.mark.parametrize('migrate_to_currency_dt', [
    NOW - datetime.timedelta(days=1),
    NOW + datetime.timedelta(days=1),
    # None,

])
def test_region(medium_xmlrpc, session, old_region, new_region, w_order, client_only,
                iso_currency, migrate_to_currency_dt):
    client = ob.ClientBuilder.construct(session, region_id=old_region)
    service = ob.ServiceBuilder.construct(session, client_only=client_only)
    create_client_service_data(client,
                               migrate_to_currency_dt=migrate_to_currency_dt,
                               service_id=service.id,
                               currency=iso_currency)
    if w_order:
        ob.OrderBuilder.construct(session, client=client, service=service)
    session.flush()
    if (w_order and
        iso_currency and
        client_only == 0 and
        old_region != new_region and
        old_region and
        new_region and
        migrate_to_currency_dt == NOW - datetime.timedelta(days=1) and
        not (old_region == 149 and new_region == 225)):
        should_pass = 0
    else:
        should_pass = 1

    try:
        medium_xmlrpc.CreateClient(session.oper_id, {'CLIENT_TYPE_ID': 1,
                                                     'NAME': 'name',
                                                     'CLIENT_ID': client.id,
                                                     'EMAIL': 'new@email.ru',
                                                     'REGION_ID': new_region,
                                                     'FAX': "555-1234",
                                                     'PHONE': "555-4321",
                                                     'URL': "http://yandex.ru",
                                                     'INTERNAL': 1,
                                                     'IS_WHOLESALER': 1,
                                                     'FULL_REPAYMENT': 1,
                                                     'MANUAL_SUSPECT': 1,
                                                     })
        assert should_pass == 1
    except Exception as exc_info:
        assert should_pass == 0
        assert tut.get_exception_code(exc_info, 'code') == 'INVALID_PARAM'
        assert tut.get_exception_code(exc_info,
                                      'msg') == 'Invalid parameter for function: Can not change region_id from {} to {}'.format(
            old_region, new_region)


def test_too_long_passport(medium_xmlrpc, session):
    session.oper_id = 648804746

    def dd(*args, **kwargs):
        return {'uid': 2323,
                'fields': {'login': 'wer',
                           'fio': 'errereererrereerrereererrereerrereererrereerrereererrereerrereererrereerrereererrereerrereererrereerrereererrereerrereererrereerrereererrereerrereererrereerrereererrereerrereererrereerrereererrereerrereererrereerrereererrereerrereererrereerrereererrereerrereererrereerrereererrereerrereererrereerrereererrereerrereererrereerrereererrere'}}

    with patch('butils.passport.PassportBlackbox._call_api_once', dd), pytest.raises(xmlrpclib.Fault) as exc_info:
        medium_xmlrpc.CreateClient(session.oper_id, {'CLIENT_TYPE_ID': 1,
                                                     'NAME': 'name',
                                                     'EMAIL': 'new@email.ru',
                                                     'FAX': "555-1234",
                                                     'PHONE': "555-4321",
                                                     'URL': "http://yandex.ru",
                                                     'INTERNAL': 1,
                                                     'IS_WHOLESALER': 1,
                                                     'FULL_REPAYMENT': 1,
                                                     'MANUAL_SUSPECT': 1,
                                                     })
    assert tut.get_exception_code(exc_info.value, 'code') == 'PASSPORT_SAVE_FAILED_ERROR'
    assert tut.get_exception_code(exc_info.value, 'msg') == 'Failed to save passport 648804746 into DB'


@pytest.mark.parametrize(
    'cfg, params, req_flag',
    [
        pytest.param(0, {}, False, id='always_off'),
        pytest.param(1, {}, True, id='always_on'),
        pytest.param({'is_agency': 1}, {'IS_AGENCY': 1}, True, id='agency_ok'),
        pytest.param({'is_agency': 0}, {}, True, id='direct_ok'),
        pytest.param({'is_agency': 1}, {}, False, id='agency_fail'),
        pytest.param({'region_id': 225}, {'REGION_ID': 225}, True, id='region_ok'),
        pytest.param({'region_id': 226}, {'REGION_ID': 225}, False, id='region_fail'),
        pytest.param({'id_modulus': 1}, {}, True, id='modulus_ok'),
        pytest.param({'id_modulus': sys.maxint}, {}, False, id='modulus_fail'),
        pytest.param({'is_agency': 1, 'region_id': 225}, {'IS_AGENCY': 1, 'REGION_ID': 225}, True, id='and_ok'),
        pytest.param({'is_agency': 1, 'region_id': 226}, {'IS_AGENCY': 1, 'REGION_ID': 225}, False, id='and_fail'),
        pytest.param([{'is_agency': 1}, {'region_id': 225}], {'REGION_ID': 225}, True, id='or'),
    ]
)
def test_should_turn_on_log_tariff(medium_xmlrpc, session, cfg, params, req_flag):
    session.config.__dict__['CLIENTS_TURN_ON_LOG_TARIFF'] = cfg
    real_params = {
        'CLIENT_ID': ''
    }
    real_params.update(params)
    res = medium_xmlrpc.CreateClient(session.oper_id, real_params)
    client = session.query(Client).get(res[2])

    assert client.should_turn_on_log_tariff == req_flag


@pytest.mark.parametrize(
    'cfg, req_flag',
    [
        pytest.param({'is_agency': 0}, False, id='direct'),
        pytest.param({'is_agency': 1}, False, id='agency'),
        pytest.param(1, True, id='all'),
    ]
)
def test_should_turn_on_log_tariff_subclient(medium_xmlrpc, session, cfg, req_flag):
    agency = ob.ClientBuilder.construct(session=session, is_agency=1)
    session.config.__dict__['CLIENTS_TURN_ON_LOG_TARIFF'] = cfg

    res = medium_xmlrpc.CreateClient(session.oper_id, {'CLIENT_ID': '', 'AGENCY_ID': agency.id})
    client = session.query(Client).get(res[2])

    assert client.should_turn_on_log_tariff == req_flag
    assert client.agency_id == agency.id


def test_should_turn_on_log_tariff_existing_client(medium_xmlrpc, session):
    client = ob.ClientBuilder.construct(session=session)
    session.config.__dict__['CLIENTS_TURN_ON_LOG_TARIFF'] = 1

    medium_xmlrpc.CreateClient(session.oper_id, {'CLIENT_ID': client.id})

    assert client.should_turn_on_log_tariff is False
