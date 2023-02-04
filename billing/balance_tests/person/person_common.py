# -*- coding: utf-8 -*-
import pytest
import datetime

from balance.mapper import Bank, fias
import tests.object_builder as ob

NOT_CENTER = 0

CITY = 4

POSTCODE = ob.generate_numeric_string(5)


@pytest.fixture(params=[{'Person': 1}, {}], ids=['oebs_api', 'oebs'])
def export_w_oebs_api(request, session):
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = request.param
    return request.param


@pytest.fixture(name='client')
def create_client(session, **kwargs):
    return ob.ClientBuilder(**kwargs).build(session).obj


@pytest.fixture(name='person')
def create_person(session, type='ph', **kwargs):
    return ob.PersonBuilder(type=type, **kwargs).build(session).obj


@pytest.fixture(name='firm')
def create_firm(session, country=None, w_firm_export=False, **kwargs):
    firm = ob.FirmBuilder(country=country or create_country(session), **kwargs).build(session).obj
    if w_firm_export:
        create_firm_export(session, firm)
    return firm


def create_firm_export(session, firm):
    oebs_org_id = ob.get_big_number()
    session.execute(
        '''INSERT INTO BO.T_FIRM_EXPORT (FIRM_ID, EXPORT_TYPE, OEBS_ORG_ID) VALUES (:firm_id, 'OEBS', :oebs_org_id)''',
        {'firm_id': firm.id, 'oebs_org_id': oebs_org_id})


@pytest.fixture(name='country')
def create_country(session, **kwargs):
    return ob.CountryBuilder(**kwargs).build(session).obj


def create_person_category(session, **kwargs):
    return ob.PersonCategoryBuilder(**kwargs).build(session).obj


def create_currency(session, **kwargs):
    currency = ob.CurrencyBuilder(**kwargs).build(session).obj
    currency.iso_code = currency.char_code
    return currency


def create_pay_policy(session, pay_policy_service_id=None, legal_entity=0, category=None, is_atypical=0,
                      paymethods_params=None, region_id=None, **kwargs):
    if not pay_policy_service_id:
        pay_policy_service_id = ob.create_pay_policy_service(
            session, legal_entity=legal_entity, category=category, is_atypical=is_atypical,
            paymethods_params=paymethods_params, **kwargs
        )
    ob.create_pay_policy_region(session, pay_policy_service_id=pay_policy_service_id, region_id=region_id, **kwargs)
    return pay_policy_service_id


@pytest.fixture(name='service')
def create_service(session, **kwargs):
    return ob.ServiceBuilder(**kwargs).build(session).obj


def non_existing_bik(session):
    return str(ob.get_big_number())


def existing_bik(session):
    return session.query(Bank).filter(Bank.bik.isnot(None)).first().bik


def create_contract(session, person, firm_id=None):
    return ob.ContractBuilder(
        client=person.client,
        person=person,
        commission=0,
        payment_type=3,
        services={7},
        is_signed=datetime.datetime.now(),
        firm=firm_id or create_firm(session).id
    ).build(session).obj


def create_fias_row(session, short_name, formal_name, parent_fias=None, center_status=NOT_CENTER, **attrs):
    return ob.FiasBuilder(parent_fias=parent_fias, short_name=short_name, formal_name=formal_name,
                          center_status=center_status, **attrs).build(session).obj


def create_fias_city(session, formal_name, short_name=u'г.', obj_level=CITY, **attrs):
    fias_row = create_fias_row(session, short_name=short_name, formal_name=formal_name, obj_level=obj_level,
                               **attrs)
    city = fias.FiasCity(guid=fias_row.guid, short_name=fias_row.short_name, formal_name=fias_row.formal_name)
    session.add(city)
    session.flush()
    return fias_row, city


def create_fias_street(session, city, formal_name, parent_name=u'', **kwargs):
    fias_row = create_fias_row(session, short_name=u'ул.', formal_name=formal_name, parent_guid=city and city.guid,
                               **kwargs)
    street = fias.FiasStreet(guid=fias_row.guid, short_name=fias_row.short_name, formal_name=fias_row.formal_name,
                             parent_guid=city and city.guid, parent_name=parent_name)
    session.add(street)
    session.flush()
    return fias_row, street


def create_restricted_person_param(session, person_type, attrname, value_str):
    return ob.RestrictedPersonParamBuilder(
        person_type=person_type,
        attrname=attrname,
        value_str=value_str,
        comment='test'
    ).build(session).obj
