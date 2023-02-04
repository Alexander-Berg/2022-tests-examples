# -*- coding: utf-8 -*-
import pytest
import decimal
import datetime

from hamcrest import assert_that, has_entries

from balance import constants as cst
from balance import mapper
from tests import object_builder as ob
import tests.tutils as tut
import xmlrpclib

D = decimal.Decimal

PAYMENT_METHOD_CC = 'card'
ISO_CURRENCY = 'RUB'
OVERDRAFT_LIMIT = 100
CLIENT_OVERDRAFT_LIMIT = 80
SERVICE_ID = cst.ServiceId.DIRECT
FIRM_ID = cst.FirmId.YANDEX_OOO


def get_person(session):
    client = ob.ClientBuilder(is_agency=False)
    person = ob.PersonBuilder(client=client, type='ph').build(session).obj
    session.flush()
    return person


def get_agency_person(session):
    client = ob.ClientBuilder(is_agency=True)
    person = ob.PersonBuilder(client=client).build(session).obj
    session.flush()
    return person


def set_overdraft_limit(session, person, **kwargs):
    service = session.query(mapper.Service).getone(SERVICE_ID)
    iso_currency = kwargs.get('currency', ISO_CURRENCY)
    person.client.set_overdraft_limit(service, FIRM_ID, OVERDRAFT_LIMIT, iso_currency)
    session.flush()


class TestSetOverdraftParams(object):
    @pytest.mark.parametrize('autooverdraft_limit', [0, CLIENT_OVERDRAFT_LIMIT])
    def test_set(self, session, xmlrpcserver, autooverdraft_limit):
        person = get_person(session)
        set_overdraft_limit(session, person)

        params = {
            'PersonID': person.id,
            'ServiceID': SERVICE_ID,
            'PaymentMethodCC': PAYMENT_METHOD_CC,
            'Currency': ISO_CURRENCY,
            'ClientLimit': autooverdraft_limit
        }
        code, res = xmlrpcserver.SetOverdraftParams(params)
        assert code == 0
        assert res == 'SUCCESS'

        overdraft_params = session.query(mapper.OverdraftParams).getone(client_id=person.client_id,
                                                                        service_id=SERVICE_ID)
        assert overdraft_params.person == person
        assert overdraft_params.service.id == SERVICE_ID
        assert overdraft_params.payment_method.cc == PAYMENT_METHOD_CC
        assert overdraft_params.iso_currency == ISO_CURRENCY
        assert overdraft_params.client == person.client
        assert overdraft_params.client_limit == autooverdraft_limit

    def test_change(self, session, xmlrpcserver):
        person = get_person(session)
        set_overdraft_limit(session, person)

        first_limit = CLIENT_OVERDRAFT_LIMIT
        second_limit = 123

        first_payment_method_cc = PAYMENT_METHOD_CC
        second_payment_method_cc = 'bank'

        # создаем новую запись
        params = {
            'PersonID': person.id,
            'ServiceID': SERVICE_ID,
            'PaymentMethodCC': first_payment_method_cc,
            'Currency': ISO_CURRENCY,
            'ClientLimit': first_limit
        }
        code, res = xmlrpcserver.SetOverdraftParams(params)
        assert code == 0
        assert res == 'SUCCESS'

        overdraft_params = session.query(mapper.OverdraftParams).getone(client_id=person.client_id,
                                                                        service_id=SERVICE_ID)
        overdraft_params_history = session.query(mapper.OverdraftParamsHistory) \
            .filter_by(overdraft_params_id=overdraft_params.id)

        assert len(list(overdraft_params_history)) == 0

        # меняем лимит на новый
        params = {
            'PersonID': person.id,
            'ServiceID': SERVICE_ID,
            'PaymentMethodCC': second_payment_method_cc,
            'Currency': ISO_CURRENCY,
            'ClientLimit': second_limit
        }
        code, res = xmlrpcserver.SetOverdraftParams(params)
        assert code == 0
        assert res == 'SUCCESS'

        overdraft_params = session.query(mapper.OverdraftParams).getone(client_id=person.client_id,
                                                                        service_id=SERVICE_ID)
        overdraft_params_history = session.query(mapper.OverdraftParamsHistory) \
            .filter_by(overdraft_params_id=overdraft_params.id)

        assert overdraft_params.person == person
        assert overdraft_params.service.id == SERVICE_ID
        assert overdraft_params.payment_method.cc == second_payment_method_cc
        assert overdraft_params.iso_currency == ISO_CURRENCY
        assert overdraft_params.client == person.client
        assert overdraft_params.client_limit == second_limit

        assert len(list(overdraft_params_history)) == 1

        overdraft_params_history = overdraft_params_history.one()

        assert overdraft_params_history.person_id == person.id
        assert overdraft_params_history.service_id == SERVICE_ID
        assert overdraft_params_history.payment_method_cc == first_payment_method_cc
        assert overdraft_params_history.iso_currency == ISO_CURRENCY
        assert overdraft_params_history.client_id == person.client.id
        assert overdraft_params_history.client_limit == first_limit

    def test_set_with_agency(self, xmlrpcserver, session):
        agency_person = get_agency_person(session)

        params = {
            'PersonID': agency_person.id,
            'ServiceID': SERVICE_ID,
            'PaymentMethodCC': PAYMENT_METHOD_CC,
            'Currency': ISO_CURRENCY,
            'ClientLimit': CLIENT_OVERDRAFT_LIMIT,
        }

        with pytest.raises(xmlrpclib.Fault) as exc_info:
            xmlrpcserver.SetOverdraftParams(params)
        expected_msg = "Invalid parameter for function: Agencies can't have an overdraft"
        assert expected_msg == tut.get_exception_code(exc_info.value, 'msg')

    def test_set_without_limit(self, xmlrpcserver, session):
        person = get_person(session)

        params = {
            'PersonID': person.id,
            'ServiceID': SERVICE_ID,
            'PaymentMethodCC': PAYMENT_METHOD_CC,
            'Currency': ISO_CURRENCY,
            'ClientLimit': CLIENT_OVERDRAFT_LIMIT,
        }

        with pytest.raises(xmlrpclib.Fault) as exc_info:
            xmlrpcserver.SetOverdraftParams(params)
        expected_msg = "Invalid parameter for function: Client {} doesn't have overdraft".format(person.client.id)
        assert expected_msg == tut.get_exception_code(exc_info.value, 'msg')

    @pytest.mark.linked_clients
    def test_set_w_equi(self, session, xmlrpcserver):
        person = get_person(session)
        set_overdraft_limit(session, person)

        eq_client = ob.ClientBuilder.construct(session)
        eq_client.make_equivalent(person.client)
        session.flush()
        session.expire_all()

        params = {
            'PersonID': person.id,
            'ServiceID': SERVICE_ID,
            'PaymentMethodCC': PAYMENT_METHOD_CC,
            'Currency': ISO_CURRENCY,
            'ClientLimit': CLIENT_OVERDRAFT_LIMIT,
        }

        with pytest.raises(xmlrpclib.Fault) as exc_info:
            xmlrpcserver.SetOverdraftParams(params)
        expected_msg = "Invalid parameter for function: Client has equivalent clients or brand"
        assert expected_msg == tut.get_exception_code(exc_info.value, 'msg')

    @pytest.mark.linked_clients
    @pytest.mark.parametrize('brand_dt_delta', [-10, 10], ids=['past', 'future'])
    def test_set_w_brand(self, session, xmlrpcserver, brand_dt_delta):
        person_1 = get_person(session)
        set_overdraft_limit(session, person_1)

        person_2 = get_person(session)
        set_overdraft_limit(session, person_2)

        ob.create_brand(
            session,
            [(
                datetime.datetime.now() + datetime.timedelta(brand_dt_delta),
                [person_1.client, person_2.client]
            )],
            datetime.datetime.now() + datetime.timedelta(20)
        )
        session.expire_all()

        params = {
            'PersonID': person_1.id,
            'ServiceID': SERVICE_ID,
            'PaymentMethodCC': PAYMENT_METHOD_CC,
            'Currency': ISO_CURRENCY,
            'ClientLimit': CLIENT_OVERDRAFT_LIMIT,
        }

        with pytest.raises(xmlrpclib.Fault) as exc_info:
            xmlrpcserver.SetOverdraftParams(params)
        expected_msg = "Invalid parameter for function: Client has equivalent clients or brand"
        assert expected_msg == tut.get_exception_code(exc_info.value, 'msg')

        params = {
            'PersonID': person_2.id,
            'ServiceID': SERVICE_ID,
            'PaymentMethodCC': PAYMENT_METHOD_CC,
            'Currency': ISO_CURRENCY,
            'ClientLimit': CLIENT_OVERDRAFT_LIMIT,
        }

        with pytest.raises(xmlrpclib.Fault) as exc_info:
            xmlrpcserver.SetOverdraftParams(params)
        expected_msg = "Invalid parameter for function: Client has equivalent clients or brand"
        assert expected_msg == tut.get_exception_code(exc_info.value, 'msg')

    @pytest.mark.linked_clients
    def test_set_after_brand(self, session, xmlrpcserver):
        person_1 = get_person(session)
        set_overdraft_limit(session, person_1)

        person_2 = get_person(session)
        set_overdraft_limit(session, person_2)

        ob.create_brand(
            session,
            [(
                datetime.datetime.now() - datetime.timedelta(10),
                [person_1.client, person_2.client]
            )],
            datetime.datetime.now() - datetime.timedelta(1)
        )
        session.expire_all()

        params = {
            'PersonID': person_1.id,
            'ServiceID': SERVICE_ID,
            'PaymentMethodCC': PAYMENT_METHOD_CC,
            'Currency': ISO_CURRENCY,
            'ClientLimit': CLIENT_OVERDRAFT_LIMIT,
        }

        code, res = xmlrpcserver.SetOverdraftParams(params)
        assert code == 0
        assert res == 'SUCCESS'

        params = {
            'PersonID': person_2.id,
            'ServiceID': SERVICE_ID,
            'PaymentMethodCC': PAYMENT_METHOD_CC,
            'Currency': ISO_CURRENCY,
            'ClientLimit': CLIENT_OVERDRAFT_LIMIT,
        }

        code, res = xmlrpcserver.SetOverdraftParams(params)
        assert code == 0
        assert res == 'SUCCESS'

        overdraft_params_1 = session.query(mapper.OverdraftParams).getone(client_id=person_1.client_id,
                                                                          service_id=SERVICE_ID)
        overdraft_params_2 = session.query(mapper.OverdraftParams).getone(client_id=person_2.client_id,
                                                                          service_id=SERVICE_ID)

        assert overdraft_params_1.person == person_1
        assert overdraft_params_1.service.id == SERVICE_ID
        assert overdraft_params_1.payment_method.cc == PAYMENT_METHOD_CC
        assert overdraft_params_1.iso_currency == ISO_CURRENCY
        assert overdraft_params_1.client == person_1.client
        assert overdraft_params_1.client_limit == CLIENT_OVERDRAFT_LIMIT

        assert overdraft_params_2.person == person_2
        assert overdraft_params_2.service.id == SERVICE_ID
        assert overdraft_params_2.payment_method.cc == PAYMENT_METHOD_CC
        assert overdraft_params_2.iso_currency == ISO_CURRENCY
        assert overdraft_params_2.client == person_2.client
        assert overdraft_params_2.client_limit == CLIENT_OVERDRAFT_LIMIT

    def test_unexist_currency(self, session, xmlrpcserver):
        person = get_person(session)
        set_overdraft_limit(session, person)

        unexist_currency = 'RUX'

        params = {
            'PersonID': person.id,
            'ServiceID': SERVICE_ID,
            'PaymentMethodCC': PAYMENT_METHOD_CC,
            'Currency': unexist_currency,
            'ClientLimit': CLIENT_OVERDRAFT_LIMIT,
        }

        with pytest.raises(xmlrpclib.Fault) as exc_info:
            xmlrpcserver.SetOverdraftParams(params)
        expected_msg = "Invalid parameter for function: Invalid iso_currency={}".format(unexist_currency)
        assert expected_msg == tut.get_exception_code(exc_info.value, 'msg')

    def test_fish_client(self, session, xmlrpcserver):
        person = get_person(session)
        set_overdraft_limit(session, person, currency=None)

        params = {
            'PersonID': person.id,
            'ServiceID': SERVICE_ID,
            'PaymentMethodCC': PAYMENT_METHOD_CC,
            'Currency': ISO_CURRENCY,
            'ClientLimit': CLIENT_OVERDRAFT_LIMIT,
        }

        with pytest.raises(xmlrpclib.Fault) as exc_info:
            xmlrpcserver.SetOverdraftParams(params)
        expected_msg = "Invalid parameter for function: Autooverdraft not allowed for fish clients"
        assert expected_msg == tut.get_exception_code(exc_info.value, 'msg')

        params = {
            'PersonID': person.id,
            'ServiceID': SERVICE_ID,
            'PaymentMethodCC': PAYMENT_METHOD_CC,
            'Currency': None,
            'ClientLimit': CLIENT_OVERDRAFT_LIMIT,
        }

        with pytest.raises(xmlrpclib.Fault) as exc_info:
            xmlrpcserver.SetOverdraftParams(params)
        expected_msg = "Invalid parameter for function: Autooverdraft not allowed for fish clients"
        assert expected_msg == tut.get_exception_code(exc_info.value, 'msg')

    @pytest.mark.parametrize('with_overdraft', [True, False])
    def test_zero_limit(self, session, xmlrpcserver, with_overdraft):
        person = get_person(session)
        if with_overdraft:
            set_overdraft_limit(session, person, currency=None)

        params = {
            'PersonID': person.id,
            'ServiceID': SERVICE_ID,
            'PaymentMethodCC': PAYMENT_METHOD_CC,
            'Currency': ISO_CURRENCY,
            'ClientLimit': 0,
        }

        xmlrpcserver.SetOverdraftParams(params)

        overdraft_params = session.query(mapper.OverdraftParams).getone(client_id=person.client_id,
                                                                        service_id=SERVICE_ID)
        assert overdraft_params.person == person
        assert overdraft_params.service.id == SERVICE_ID
        assert overdraft_params.payment_method.cc == PAYMENT_METHOD_CC
        assert overdraft_params.iso_currency == ISO_CURRENCY
        assert overdraft_params.client == person.client
        assert overdraft_params.client_limit == 0


class TestGetOverdraftParams(object):
    def test_ok(self, xmlrpcserver, session):
        person = get_person(session)
        set_overdraft_limit(session, person)

        exp_ov_params = mapper.OverdraftParams(
            client=person.client,
            person=person,
            payment_method_cc=PAYMENT_METHOD_CC,
            client_limit=CLIENT_OVERDRAFT_LIMIT,
            iso_currency=ISO_CURRENCY,
            service_id=SERVICE_ID,
        )

        res = xmlrpcserver.GetOverdraftParams(SERVICE_ID, person.client.id)
        assert_that(res, has_entries({
            'PersonID': exp_ov_params.person_id,
            'PaymentMethodCC': exp_ov_params.payment_method_cc,
            'Currency': exp_ov_params.iso_currency,
            'ClientLimit': str(D(exp_ov_params.client_limit)),
        }))
