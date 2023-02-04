# coding=utf-8

"""
Тесты класса mapper.Person
"""
import pytest
import mock
from balance.mapper import Person, Permission, Contract
from balance.constants import PersonCategoryCodes, FirmId
from balance.exc import INVALID_PERSON_TYPE, INVALID_PERSON_TYPE_AND_CURRENCY
from tests.balance_tests.person.person_common import (
    create_client, create_person, create_country, create_currency, create_contract,
    create_service, create_person_category, create_fias_city, create_fias_street,
    create_pay_policy, create_firm)
from tests import object_builder as ob


def test_client_persons(person):
    assert person.client.persons == [person]


@pytest.mark.single_account
@mock.patch('balance.actions.single_account.availability.check_person_params')
def test_with_single_account(check_person_params_mock, session):
    category = PersonCategoryCodes.russia_resident_legal_entity
    inn = 1
    is_partner = 0
    client = create_client(session, with_single_account=True)
    Person(client, type=category, is_partner=is_partner, inn=inn)
    assert check_person_params_mock.call_count == 1
    call_args, call_kwargs = check_person_params_mock.call_args
    person_params = call_kwargs['params']
    assert person_params['category'] == category
    assert person_params['is_partner'] == is_partner
    assert person_params['inn'] == inn


def test_invalid_type(client):
    with pytest.raises(INVALID_PERSON_TYPE) as exc_info:
        Person(client, type='er')
    assert exc_info.value.msg == 'Invalid person type: er'


@pytest.mark.parametrize('is_partner', [0, 1])
@pytest.mark.parametrize('is_with_AlterAllInvoices', [True, False])
def test_invalid_type_non_resident(session, firm, is_with_AlterAllInvoices, is_partner):
    """Непартнерских плательщиков нерезидентов без права AlterAllInvoices можно создавать только с типом
    доступным по платежным настройкам"""
    perms = ['UseAdminPersons'] if is_with_AlterAllInvoices else []
    role = ob.create_role(session, *perms)
    ob.set_roles(session, session.passport, role)

    client = create_client(session, region_id=firm.country.region_id)
    service = create_service(session, client_only=0)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1, is_default=1)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', 1001)],
                      region_id=firm.country.region_id, service_id=service.id, is_agency=0)

    categories = set(client.get_creatable_person_categories())
    assert categories == {person_category}
    person_category = create_person_category(session, country=firm.country, ur=0, resident=0, is_default=1)
    if is_with_AlterAllInvoices or is_partner:
        Person(client, type=person_category.category, is_partner=is_partner)
    else:
        with pytest.raises(INVALID_PERSON_TYPE) as exc_info:
            Person(client, type=person_category.category, is_partner=is_partner)
        assert exc_info.value.msg == 'Invalid person type: {}'.format(person_category.category)


@pytest.mark.parametrize('is_partner', [0, 1])
@pytest.mark.parametrize('is_with_AlterAllInvoices', [True, False])
def test_invalid_type_resident(session, country, is_with_AlterAllInvoices, is_partner):
    """Непартнерских плательщиков резидентов без права AlterAllInvoices можно создавать только с типом
    доступным по платежным настройкам"""
    perms = ['UseAdminPersons'] if is_with_AlterAllInvoices else []
    role = ob.create_role(session, *perms)
    ob.set_roles(session, session.passport, role)

    firm = create_firm(session, country=create_country(session, default_currency=create_currency(session).char_code))
    client = create_client(session, region_id=country.region_id)
    service = create_service(session, client_only=0)
    available_person_category = create_person_category(session, country=firm.country, ur=0, resident=0, is_default=1)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', 1001)],
                      region_id=country.region_id, service_id=service.id, is_agency=0)

    categories = set(client.get_creatable_person_categories())
    assert categories == {available_person_category}
    person_category = create_person_category(session, country=create_country(session, default_currency=create_currency(
        session).char_code), ur=0, resident=1, is_default=1)
    if is_with_AlterAllInvoices or is_partner:
        Person(client, type=person_category.category, is_partner=is_partner)
    else:
        with pytest.raises(INVALID_PERSON_TYPE_AND_CURRENCY) as exc_info:
            Person(client, type=person_category.category, is_partner=is_partner)
        assert exc_info.value.msg == 'Invalid person type: {0}, ' \
                                     'new default currency {1}, used default currency: {2}'.format(
            person_category.category, person_category.country.default_currency,
            available_person_category.country.default_currency)


class TestPersonAddress(object):
    def test_legal_sample_from_legaladdress(self, session):
        person = create_person(session, legaladdress='person_legal_address')
        assert person.legal_sample == person.legaladdress

    def test_legal_sample_from_fias(self, session):
        city, _ = create_fias_city(session, formal_name=u'Москва')
        street, _ = create_fias_street(session, city=city, formal_name=u'Центральная')
        person = create_person(session, legal_fias_guid=street.guid, legal_address_home=u'д.2',
                               legal_address_postcode='123456',
                               legal_address_street=street.formal_name + ' ' + street.short_name)
        assert person.legal_sample == u'Центральная ул., д.2\nг. Москва\n123456'

    def test_legal_address_joint(self, session):
        city, _ = create_fias_city(session, formal_name=u'Москва')
        street, _ = create_fias_street(session, city=city, formal_name=u'Центральная')
        person = create_person(session, legal_fias_guid=street.guid, legal_address_home=u'д.2',
                               legal_address_postcode='123456',
                               legal_address_street=street.formal_name + ' ' + street.short_name)
        assert person.legal_address_joint == u'123456, г. Москва, Центральная ул., д.2'


def test_person_firms(session, person):
    assert person.firms == []
    assert person.person_firms == []
    firm = create_firm(session)
    person.firms.append(firm)
    assert person.firms == [firm]
    assert person.person_firms[0].firm == firm
    assert person.person_firms[0].person == person
    assert person.person_firms[0].oebs_export_dt is None


def test_person_firms_after_contract_create(session, xmlrpcserver, firm):
    person = create_person(session, type='ur')
    contract = create_contract(session,
                               person=person,
                               firm_id=1)
    session.flush()
    assert person.firms == [contract.firm]
    assert person.client
    contract = create_contract(session,
                               person=person,
                               firm_id=1)
    session.flush()
    assert person.firms == [contract.firm]
    contract2 = create_contract(session,
                                person=person,
                                firm_id=2)
    session.flush()

    assert sorted(person.firms) == sorted([contract.firm, contract2.firm])
