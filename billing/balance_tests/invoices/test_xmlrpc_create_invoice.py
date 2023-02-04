# -*- coding: utf-8 -*-

import xmlrpclib

import pytest

import tests.object_builder as ob

VALID_INN_1 = '1000000002'
PAYSYS_ID = 1001  # Yandex.money


class TestInvoice(object):
    def test_ok(self, session, xmlrpcserver):
        passport_id = ob.PassportBuilder().build(session).obj.passport_id
        request = ob.RequestBuilder().build(session).obj
        person = ob.PersonBuilder(
            inn=VALID_INN_1,
            type='ph',
            client=request.client,
            operator_uid=passport_id
        ).build(session).obj

        params = {
            'RequestID': request.id,
            'PaysysID': PAYSYS_ID,
            'PersonID': person.id,
        }

        xmlrpcserver.CreateInvoice(passport_id, params)

    @pytest.mark.parametrize(
        'init_params, error',
        [
            pytest.param(
                {'RequestID': 0, 'PaysysID': PAYSYS_ID, 'PersonID': None},
                'REQUEST_NOT_FOUND',
                id='REQUEST_NOT_FOUND'
            ),
            pytest.param(
                {'RequestID': None, 'PaysysID': PAYSYS_ID, 'PersonID': 0},
                'PERSON_NOT_FOUND',
                id='PERSON_NOT_FOUND'
            ),
            pytest.param(
                {'RequestID': None, 'PaysysID': 0, 'PersonID': None},
                'PAYSYS_NOT_FOUND',
                id='PAYSYS_NOT_FOUND'
            ),
        ]
    )
    def test_not_found(self, session, xmlrpcserver, init_params, error):
        passport_id = ob.PassportBuilder().build(session).obj.passport_id
        request = ob.RequestBuilder().build(session).obj
        person = ob.PersonBuilder(
            inn=VALID_INN_1,
            type='ph',
            client=request.client,
            operator_uid=passport_id
        ).build(session).obj

        created_params = {
            'RequestID': request.id,
            'PersonID': person.id,
        }
        real_params = {
            k: created_params[k] if v is None else v
            for k, v in init_params.iteritems()
        }

        with pytest.raises(xmlrpclib.Fault) as exc_info:
            xmlrpcserver.CreateInvoice(passport_id, real_params)

        assert error in exc_info.value.faultString

    def test_wrong_person(self, session, xmlrpcserver):
        passport_id = ob.PassportBuilder().build(session).obj.passport_id
        request = ob.RequestBuilder().build(session).obj
        ob.PersonBuilder(
            inn=VALID_INN_1,
            type='ph',
            client=request.client,
            operator_uid=passport_id
        ).build(session)
        wrong_person = ob.PersonBuilder().build(session).obj

        params = {
            'RequestID': request.id,
            'PaysysID': PAYSYS_ID,
            'PersonID': wrong_person.id,
        }

        with pytest.raises(xmlrpclib.Fault) as exc_info:
            xmlrpcserver.CreateInvoice(passport_id, params)

        assert 'Incompatible invoice params' in exc_info.value.faultString
