# -*- coding: utf-8 -*-

import pytest
import datetime

from balance.mapper import Currency
from billing.contract_iface import ContractTypeId
from billing.contract_iface.contract_meta import collateral_types

from tests import object_builder as ob
from tests.balance_tests.pay_policy.pay_policy_common import (FIELDS,
                                                              CATEGORY_FIELDS,
                                                              CLIENT_CATEGORY,
                                                              AGENCY_CATEGORY,
                                                              WITH_CONTRACT,
                                                              WITHOUT_CONTRACT,
                                                              PAYSYS_GROUP_ID,
                                                              BANK,
                                                              CARD,
                                                              get_pay_policy_manager,
                                                              create_service,
                                                              create_country,
                                                              create_firm,
                                                              create_person_category,
                                                              create_pay_policy,
                                                              _extract_ans,
                                                              create_client,
                                                              create_person,
                                                              create_contract
                                                              )

NOW = datetime.datetime.now()


@pytest.mark.parametrize('legal_entity_value', [0, 1])
def test_legal_entity_non_defined(session, pay_policy_manager, service, country, legal_entity_value):
    """Если в строке pay_policy_part не указан признак юридического лица, платежные политики подберутся
    и для физика, и для юрика из договора"""
    firm = create_firm(session, country=country)
    person_category = create_person_category(session, country=country, ur=legal_entity_value)
    client = create_client(session)
    person = create_person(session, type=person_category.category)
    currency = ob.Getter(Currency, 'USD').build(session).obj
    create_pay_policy(session, firm_id=firm.id, legal_entity=None, paymethods_params=[(currency.char_code, BANK)],
                      region_id=country.region_id, service_id=service.id, is_agency=0)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person,
                               services=service.id, client=client, ctype='GENERAL',
                               commission=ContractTypeId.NON_AGENCY, firm=firm.id, currency=currency.num_code)
    rows = pay_policy_manager.get_rows_by_contract(fields=FIELDS, contract=contract, service_id=None)
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITH_CONTRACT, firm,
                                   person_category, BANK, currency.char_code, PAYSYS_GROUP_ID)}


def test_several_paymethods(session, pay_policy_manager, service, country):
    """Если в платежных настройках указано несколько валют, вернутся только платежные настройки с валютой из договора
    c каждым из доступных для валюты методов оплат"""
    firm = create_firm(session, country=country)
    person_category = create_person_category(session, country=country, ur=1)
    client = create_client(session)
    person = create_person(session, type=person_category.category)
    currency = ob.Getter(Currency, 'USD').build(session).obj
    create_pay_policy(session, firm_id=firm.id, legal_entity=None, paymethods_params=[('USD', BANK),
                                                                                      ('EUR', BANK),
                                                                                      ('USD', CARD)],
                      region_id=country.region_id, service_id=service.id, is_agency=0)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person,
                               services=service.id, client=client, ctype='GENERAL',
                               commission=ContractTypeId.NON_AGENCY, firm=firm.id, currency=currency.num_code)
    rows = pay_policy_manager.get_rows_by_contract(fields=FIELDS, contract=contract, service_id=None)
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITH_CONTRACT, firm,
                                   person_category, BANK, currency.char_code, PAYSYS_GROUP_ID),
                                  (service.id, country.region_id, CLIENT_CATEGORY, WITH_CONTRACT, firm,
                                   person_category, CARD, currency.char_code, PAYSYS_GROUP_ID)}


def test_w_resident_person(session, pay_policy_manager, service, country):
    """Если в строке pay_policy_part не указан признак агенства, платежные политики подберутся
    и для клиента, и для агенства из договора"""
    firm = create_firm(session, country=country)
    person_category = create_person_category(session, country=country, ur=1, resident=1)
    client = create_client(session)
    person = create_person(session, type=person_category.category)
    currency = ob.Getter(Currency, 'USD').build(session).obj
    create_pay_policy(session, firm_id=firm.id, legal_entity=None, paymethods_params=[(currency.char_code, BANK)],
                      region_id=country.region_id, service_id=service.id, is_agency=None)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person,
                               services=service.id, client=client, ctype='GENERAL',
                               commission=ContractTypeId.NON_AGENCY, firm=firm.id, currency=currency.num_code)
    rows = pay_policy_manager.get_rows_by_contract(fields=FIELDS, contract=contract, service_id=None)
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITH_CONTRACT, firm,
                                   person_category, BANK, currency.char_code, PAYSYS_GROUP_ID)}


def test_w_non_resident_person(session, pay_policy_manager, service, country):
    """Если в строке pay_policy_part не указан признак агенства, платежные политики подберутся
    и для клиента, и для агенства из договора"""
    firm = create_firm(session, country=country)
    person_category = create_person_category(session, country=country, ur=1, resident=0)
    client = create_client(session)
    person = create_person(session, type=person_category.category)
    currency = ob.Getter(Currency, 'USD').build(session).obj
    country_2 = create_country(session)
    create_pay_policy(session, firm_id=firm.id, legal_entity=None, paymethods_params=[(currency.char_code, BANK)],
                      region_id=country_2.region_id, service_id=service.id, is_agency=None)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person,
                               services=service.id, client=client, ctype='GENERAL',
                               commission=ContractTypeId.NON_AGENCY, firm=firm.id, currency=currency.num_code)
    rows = pay_policy_manager.get_rows_by_contract(fields=FIELDS, contract=contract, service_id=None)
    assert _extract_ans(rows) == {(service.id, country_2.region_id, CLIENT_CATEGORY, WITH_CONTRACT, firm,
                                   person_category, BANK, currency.char_code, PAYSYS_GROUP_ID)}


def test_w_client_region_w_person_non_resident(session, pay_policy_manager, service, country):
    """Если в строке pay_policy_part не указан признак агенства, платежные политики подберутся
    и для клиента, и для агенства из договора"""
    firm = create_firm(session, country=country)
    person_category = create_person_category(session, country=country, ur=1, resident=0)
    client = create_client(session)
    person = create_person(session, type=person_category.category)
    currency = ob.Getter(Currency, 'USD').build(session).obj
    countries = [create_country(session) for _ in range(2)]
    pay_policy_service_id = create_pay_policy(
        session, firm_id=firm.id, legal_entity=None, paymethods_params=[(currency.char_code, BANK)],
        region_id=countries[0].region_id, service_id=service.id, is_agency=None
    )
    create_pay_policy(
        session, pay_policy_service_id=pay_policy_service_id, region_id=countries[1].region_id, is_agency=None
    )
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person,
                               services=service.id, client=client, ctype='GENERAL',
                               commission=ContractTypeId.NON_AGENCY, firm=firm.id, currency=currency.num_code)
    rows = pay_policy_manager.get_rows_by_contract(fields=FIELDS, contract=contract, service_id=None)
    assert _extract_ans(rows) == {(service.id, countries[0].region_id, CLIENT_CATEGORY, WITH_CONTRACT, firm,
                                   person_category, BANK, currency.char_code, PAYSYS_GROUP_ID),

                                  (service.id, countries[1].region_id, CLIENT_CATEGORY, WITH_CONTRACT, firm,
                                   person_category, BANK, currency.char_code, PAYSYS_GROUP_ID)}
    client.region_id = countries[0].region_id
    rows = pay_policy_manager.get_rows_by_contract(fields=FIELDS, contract=contract, service_id=None)
    assert _extract_ans(rows) == {(service.id, countries[0].region_id, CLIENT_CATEGORY, WITH_CONTRACT, firm,
                                   person_category, BANK, currency.char_code, PAYSYS_GROUP_ID)}


@pytest.mark.parametrize('is_agency', [0, 1])
def test_is_agency_non_defined(session, pay_policy_manager, service, country, is_agency):
    """Если в строке pay_policy_part не указан признак агенства, платежные политики подберутся
    и для клиента, и для агенства из договора"""
    firm = create_firm(session, country=country)
    person_category = create_person_category(session, country=firm.country, ur=1)
    client = create_client(session, is_agency=is_agency)
    person = create_person(session, type=person_category.category)
    currency = ob.Getter(Currency, 'USD').build(session).obj
    create_pay_policy(session, firm_id=firm.id, legal_entity=None, paymethods_params=[(currency.char_code, BANK)],
                      region_id=country.region_id, service_id=service.id, is_agency=None)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person,
                               services=service.id, client=client, ctype='GENERAL',
                               commission=ContractTypeId.NON_AGENCY, firm=firm.id, currency=currency.num_code)
    rows = pay_policy_manager.get_rows_by_contract(fields=FIELDS, contract=contract, service_id=None)
    assert _extract_ans(rows) == {(service.id, country.region_id, is_agency, WITH_CONTRACT, firm,
                                   person_category, BANK, currency.char_code, PAYSYS_GROUP_ID)}


@pytest.mark.parametrize('is_collateral_signed', [NOW, None])
def test_services_from_contract(session, pay_policy_manager, country, is_collateral_signed):
    """фильтруем платежные политики по сервисам из подписанных атрибутов договора,
     если сервис не передан явно"""
    services = [create_service(session) for _ in range(2)]
    firm = create_firm(session, country=country)
    person_category = create_person_category(session, country=country, ur=1)
    client = create_client(session)
    person = create_person(session, type=person_category.category)
    currency = ob.Getter(Currency, 'USD').build(session).obj

    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person,
                               services=services[0].id, client=client, ctype='GENERAL',
                               commission=ContractTypeId.NON_AGENCY, firm=firm.id, currency=currency.num_code)
    contract.append_collateral(dt=NOW, services=services[1].id, collateral_type=collateral_types['GENERAL'][1001],
                               is_signed=is_collateral_signed)
    for service in services:
        create_pay_policy(session, firm_id=firm.id, legal_entity=None, paymethods_params=[(currency.char_code, BANK)],
                          region_id=country.region_id, service_id=service.id, is_agency=None)

    rows = pay_policy_manager.get_rows_by_contract(fields=FIELDS, contract=contract, service_id=None)
    if is_collateral_signed:
        assert _extract_ans(rows) == {(services[1].id, country.region_id, CLIENT_CATEGORY, WITH_CONTRACT, firm,
                                       person_category, BANK, currency.char_code, PAYSYS_GROUP_ID)}
    else:
        assert _extract_ans(rows) == {(services[0].id, country.region_id, CLIENT_CATEGORY, WITH_CONTRACT, firm,
                                       person_category, BANK, currency.char_code, PAYSYS_GROUP_ID)}


def test_filter_by_contract(session, pay_policy_manager, country):
    """фильтруем платежные политики по сервисам из подписанных атрибутов договора или по сервису,
     если он передан явно"""
    services = [create_service(session) for _ in range(2)]
    firm = create_firm(session, country=country)
    person_category = create_person_category(session, country=country, ur=1)
    client = create_client(session)
    person = create_person(session, type=person_category.category)
    currency = ob.Getter(Currency, 'USD').build(session).obj

    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person,
                               services=[service.id for service in services], client=client, ctype='GENERAL',
                               commission=ContractTypeId.NON_AGENCY, firm=firm.id, currency=currency.num_code)

    for service in services:
        create_pay_policy(session, firm_id=firm.id, legal_entity=None, paymethods_params=[(currency.char_code, BANK)],
                          region_id=country.region_id, service_id=service.id, is_agency=None)

    rows = pay_policy_manager.get_rows_by_contract(fields=FIELDS, contract=contract)
    assert _extract_ans(rows) == {(services[0].id, country.region_id, CLIENT_CATEGORY, WITH_CONTRACT, firm,
                                   person_category, BANK, currency.char_code, PAYSYS_GROUP_ID),
                                  (services[1].id, country.region_id, CLIENT_CATEGORY, WITH_CONTRACT, firm,
                                   person_category, BANK, currency.char_code, PAYSYS_GROUP_ID)}

    rows = pay_policy_manager.get_rows_by_contract(fields=FIELDS, contract=contract, service_id=services[0].id)
    assert _extract_ans(rows) == {(services[0].id, country.region_id, CLIENT_CATEGORY, WITH_CONTRACT, firm,
                                   person_category, BANK, currency.char_code, PAYSYS_GROUP_ID)}


def test_routing_with_contract_only(session, pay_policy_manager, country):
    services = [create_service(session) for _ in range(2)]
    firm = create_firm(session, country=country)
    person_category = create_person_category(session, country=country, ur=1)
    client = create_client(session)
    person = create_person(session, type=person_category.category)
    currency = ob.Getter(Currency, 'USD').build(session).obj
    for service, is_contract in zip(services, [0, 1]):
        create_pay_policy(session, firm_id=firm.id, legal_entity=None, paymethods_params=[(currency.char_code, BANK)],
                          region_id=country.region_id, service_id=service.id, is_agency=None, is_contract=is_contract)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person,
                               services=service.id, client=client, ctype='GENERAL',
                               commission=ContractTypeId.NON_AGENCY, firm=firm.id, currency=currency.num_code)
    rows = pay_policy_manager.get_rows_by_contract(fields=FIELDS, contract=contract,
                                                   service_id=[service.id for service in services])
    assert _extract_ans(rows) == {(services[1].id, country.region_id, CLIENT_CATEGORY, WITH_CONTRACT, firm,
                                   person_category, BANK, currency.char_code, PAYSYS_GROUP_ID)}


def test_wo_atypical_routings_filter(session, pay_policy_manager, country):
    services = [create_service(session) for _ in range(2)]
    firm = create_firm(session, country=country)
    person_category = create_person_category(session, country=country, ur=1)
    client = create_client(session)
    person = create_person(session, type=person_category.category)
    currency = ob.Getter(Currency, 'USD').build(session).obj
    for service, is_atypical in zip(services, [0, 1]):
        create_pay_policy(session, firm_id=firm.id, legal_entity=None, paymethods_params=[(currency.char_code, BANK)],
                          region_id=country.region_id, service_id=service.id, is_agency=None, is_contract=1,
                          is_atypical=is_atypical)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person,
                               services=service.id, client=client, ctype='GENERAL',
                               commission=ContractTypeId.NON_AGENCY, firm=firm.id, currency=currency.num_code)
    rows = pay_policy_manager.get_rows_by_contract(fields=FIELDS, contract=contract,
                                                   service_id=[service.id for service in services])
    assert _extract_ans(rows) == {(services[1].id, country.region_id, CLIENT_CATEGORY, WITH_CONTRACT, firm,
                                   person_category, BANK, currency.char_code, PAYSYS_GROUP_ID),

                                  (services[0].id, country.region_id, CLIENT_CATEGORY, WITH_CONTRACT, firm,
                                   person_category, BANK, currency.char_code, PAYSYS_GROUP_ID)}


def test_filter_by_firm_contract(session, pay_policy_manager, country):
    service = create_service(session)
    firms = [create_firm(session, country=create_country(session)) for _ in range(2)]
    person_category = create_person_category(session, country=country, ur=1, resident=1, is_default=0)
    client = create_client(session)
    person = create_person(session, type=person_category.category)
    currency = ob.Getter(Currency, 'USD').build(session).obj
    for firm, person_country in zip(firms, [0, 1]):
        create_pay_policy(session, firm_id=firm.id, legal_entity=None, paymethods_params=[(currency.char_code, BANK)],
                          region_id=person_category.country.region_id, service_id=service.id, is_agency=None,
                          is_contract=1, is_atypical=0, category=person_category.category)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person,
                               services=service.id, client=client, ctype='GENERAL',
                               commission=ContractTypeId.NON_AGENCY, firm=firms[0].id, currency=currency.num_code)
    rows = pay_policy_manager.get_rows_by_contract(fields=FIELDS, contract=contract, service_id=service.id)
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITH_CONTRACT, firms[0],
                                   person_category, BANK, currency.char_code, PAYSYS_GROUP_ID)}


def test_filter_by_contract_currency(session, pay_policy_manager, country):
    service = create_service(session)
    firm = create_firm(session, country=create_country(session))
    person_category = create_person_category(session, country=country, ur=1, resident=1, is_default=0)
    client = create_client(session)
    person = create_person(session, type=person_category.category)
    for char_code in ['TRY', 'CHF']:
        create_pay_policy(session, firm_id=firm.id, legal_entity=None, paymethods_params=[(char_code, BANK)],
                          region_id=person_category.country.region_id, service_id=service.id, is_agency=None,
                          is_contract=1, is_atypical=0, category=person_category.category)

    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person,
                               services=service.id, client=client, ctype='GENERAL',
                               commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                               currency=ob.Getter(Currency, 'TRY').build(session).obj.num_code)
    rows = pay_policy_manager.get_rows_by_contract(fields=FIELDS, contract=contract, service_id=service.id)
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITH_CONTRACT, firm,
                                   person_category, BANK, 'TRY', PAYSYS_GROUP_ID)}
