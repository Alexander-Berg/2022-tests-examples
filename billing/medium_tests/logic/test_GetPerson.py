# -*- coding: utf-8 -*-

import pytest
from xmlrpclib import Fault

from tests import object_builder as ob
from tests.balance_tests.person.person_defaults import get_full_person_defaults


@pytest.fixture
def client(session):
    res = ob.ClientBuilder().build(session).obj
    return res

@pytest.mark.tickets('BALANCE-30807')
def test_get_person(session, xmlrpcserver):
    person = ob.PersonBuilder(type='ur').build(session).obj
    api_persons = xmlrpcserver.GetPerson({'ID': person.id})
    assert len(api_persons) == 1
    assert str(person.id) == api_persons[0]['ID']

@pytest.mark.tickets('BALANCE-30807')
def test_check_fields_get_person_xmlprc(session, client, xmlrpcserver, person_type='ur'):
    persons_keys_expected = ['ACCOUNT', 'ADDRESS', 'AUTHORITY_DOC_DETAILS', 'AUTHORITY_DOC_TYPE', 'BIK', 'CITY',
                             'CLIENT_ID', 'DELIVERY_CITY', 'DELIVERY_TYPE', 'DT', 'EMAIL', 'FAX', 'FIAS_GUID', 'ID',
                             'INN',
                             # 'IS_POSTBOX',
                             'KPP', 'LEGALADDRESS', 'LIVE_SIGNATURE', 'LONGNAME', 'NAME', 'OGRN', 'PHONE',
                             'POSTCODE', 'POSTSUFFIX', 'REPRESENTATIVE', 'SIGNER_PERSON_GENDER', 'SIGNER_PERSON_NAME',
                             'SIGNER_POSITION_NAME', 'TYPE', 'VIP', 'COUNTRY_ID', 'LEGAL_ADDRESS_POSTCODE',
                             'API_VERSION']

    base_params = {'client_id': client.id, 'type': person_type}
    base_params.update(get_full_person_defaults(person_type))
    person_id = xmlrpcserver.CreatePerson(session.oper_id, base_params)
    session.expire_all()
    api_persons = xmlrpcserver.GetPerson({'ID': person_id})
    persons_keys = sorted(api_persons[0].keys())
    assert set(persons_keys_expected).symmetric_difference(set(api_persons[0].keys())) == set()
    assert sorted(persons_keys) == sorted(persons_keys_expected)

@pytest.mark.tickets('BALANCE-30807')
def test_get_person_filter_not_implemented(session, xmlrpcserver):
    person = ob.PersonBuilder(type='ur').build(session).obj

    with pytest.raises(Fault) as exc_info:
        xmlrpcserver.GetPerson({'INN': 7729773587})

    assert 'NotImplementedError' in exc_info.value.faultString

@pytest.mark.tickets('BALANCE-30807')
def test_get_person_not_found(session, xmlrpcserver):
    person_id = -1
    with pytest.raises(Fault) as exc_info:
        response = xmlrpcserver.GetPerson({'ID': person_id})
    assert 'Person with ID {person_id} not found in DB'.format(person_id=person_id)

@pytest.mark.tickets('BALANCE-30807')
def test_get_person_partner(session, xmlrpcserver):
    person = ob.PersonBuilder(is_partner=1).build(session).obj
    person_id = person.id

    api_persons = xmlrpcserver.GetPerson({'ID': person_id}, 1)
    assert len(api_persons) == 1
    assert str(person.id) == api_persons[0]['ID']

    with pytest.raises(Fault) as exc_info:
        response = xmlrpcserver.GetPerson({'ID': person_id}, 0)

    assert 'Person with ID {person_id} not found in DB'.format(person_id=person_id)

@pytest.mark.tickets('BALANCE-30807')
def test_get_person_endbuyer(session, xmlrpcserver):
    person = ob.PersonBuilder(type='endbuyer_ph').build(session).obj
    person_id = person.id


    with pytest.raises(Fault) as exc_info:
        xmlrpcserver.GetPerson({'ID': person_id}, 0, 1)
    assert 'Person with ID {person_id} not found in DB'.format(person_id=person_id)

    api_persons = xmlrpcserver.GetPerson({'ID': person_id}, 0, 0)
    assert len(api_persons) == 1
    assert str(person_id) == api_persons[0]['ID']


def test_check(medium_xmlrpc, client):
    medium_xmlrpc.CreatePerson(-1,
                         {'account': '40702810812413974784',
                          'address': u'Улица 4',
                          'authority-doc-details': 'g',
                          'authority-doc-type': u'Свидетельство о регистрации',
                          'bik': '044525440',
                          'client_id': client.id,
                          'country_id': '225',
                          'delivery-city': 'NSK',
                          'delivery-type': '4',
                          'email': 'm-SC@qCWF.rKU',
                          'fax': '+7 812 5696286',
                          'inn': '7883306231',
                          'invalid-address': '1',
                          'invalid-bankprops': '0',
                          'kpp': '767726208',
                          'legal_address_postcode': '666666',
                          'legaladdress': 'Avenue 5',
                          'live-signature': '1',
                          'longname': u'000 WBXG',
                          'name': u'Юр. лицо или ПБОЮЛqdZd НПО «Лапина»',
                          'ogrn': '379956466494603',
                          'person_id': 0,
                          'phone': '+7 812 3990776',
                          'postaddress': 'ewrwer',
                          'postcode': '6666666',
                          'representative': 'tPLLK',
                          'revise-act-period-type': '',
                          's_signer-position-name': 'President',
                          'signer_person_gender': 'W',
                          'signer_person_name': 'Signer RR',
                          'type': 'ur',
                          'vip': '1'})
