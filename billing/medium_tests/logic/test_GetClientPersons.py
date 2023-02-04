# -*- coding: utf-8 -*-

import pytest

from tests import object_builder as ob
from tests.balance_tests.person.person_defaults import get_full_person_defaults

persons_keys_expected = ['ACCOUNT', 'ADDRESS', 'AUTHORITY_DOC_DETAILS', 'AUTHORITY_DOC_TYPE', 'BIK', 'CITY',
                         'CLIENT_ID', 'DELIVERY_CITY', 'DELIVERY_TYPE', 'DT', 'EMAIL', 'FAX', 'FIAS_GUID', 'ID', 'INN',
                         'KPP', 'LEGALADDRESS', 'LIVE_SIGNATURE', 'LONGNAME', 'NAME', 'OGRN', 'PHONE',
                         'POSTCODE', 'POSTSUFFIX', 'REPRESENTATIVE', 'SIGNER_PERSON_GENDER', 'SIGNER_PERSON_NAME',
                         'SIGNER_POSITION_NAME', 'TYPE', 'VIP', 'COUNTRY_ID', 'LEGAL_ADDRESS_POSTCODE', 'API_VERSION']


@pytest.fixture
def client(session):
    res = ob.ClientBuilder().build(session).obj
    return res


@pytest.mark.tickets('BALANCE-26694')
@pytest.mark.parametrize('endbuyer_type', ['endbuyer_ur',
                                           'endbuyer_yt',
                                           'endbuyer_ph'])
def test_get_client_persons(session, xmlrpcserver, client, endbuyer_type):
    person = ob.PersonBuilder(client=client, type='ur').build(session).obj
    endbuyer = ob.PersonBuilder(client=client, type=endbuyer_type).build(session).obj
    client_persons = xmlrpcserver.GetClientPersons(client.id)
    # проверяем, что метод возвращает только плательщика != endbuyer
    assert len(client_persons) == 1
    assert str(endbuyer.id) != client_persons[0]['ID']
    assert str(person.id) == client_persons[0]['ID']


@pytest.mark.tickets('BALANCE-27990')
def test_check_fields_get_client_persons_xmlprc(session, client, xmlrpcserver, person_type='ur'):
    base_params = {'client_id': client.id, 'type': person_type}
    base_params.update(get_full_person_defaults(person_type))
    xmlrpcserver.CreatePerson(session.oper_id, base_params)
    session.expire_all()
    client_persons = xmlrpcserver.GetClientPersons(client.id)
    persons_keys = sorted(client_persons[0].keys())

    assert sorted(persons_keys) == sorted(persons_keys_expected)
