# -*- coding: utf-8 -*-

import pytest
import mock

from tests.balance_tests.pay_policy.pay_policy_common import (
    FIELDS,
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
    _extract_category
)
from tests.object_builder import create_pay_policy_payment_method


def test_legal_entity_non_defined(session, pay_policy_manager, service, country):
    """Если в строке pay_policy_part не указан признак юридического лица,
    в строках платежных настроек будут категории плательщиков как с признаком юр.лица, так и без"""
    firm = create_firm(session, country=country)
    ur_person_category = create_person_category(session, country=country, ur=1)
    ph_person_category = create_person_category(session, country=country, ur=0)

    create_pay_policy(session, firm_id=firm.id, legal_entity=None, paymethods_params=[('USD', BANK)],
                      region_id=country.region_id, service_id=service.id, is_agency=0)

    rows = pay_policy_manager.get_rows(fields=FIELDS, service_id=None, region_id=country.region_id)
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   ur_person_category, BANK, 'USD', PAYSYS_GROUP_ID),
                                  (service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   ph_person_category, BANK, 'USD', PAYSYS_GROUP_ID)}


def test_several_paymethods(session, pay_policy_manager, service, country):
    """Если pay_policy_part соответствуют несколько методов оплаты,в строках платежных настроек будет указан каждый"""
    person_category = create_person_category(session, country=country, ur=0)
    firm = create_firm(session, country=country)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('RUB', BANK), ('USD', CARD)],
                      region_id=country.region_id, service_id=service.id, is_agency=0)

    rows = pay_policy_manager.get_rows(fields=FIELDS, service_id=None, region_id=country.region_id)
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   person_category, BANK, 'RUB', PAYSYS_GROUP_ID),
                                  (service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   person_category, CARD, 'USD', PAYSYS_GROUP_ID)}


def test_is_agency_non_defined(session, pay_policy_manager, service, country):
    """Если в pay_policy_routing не указан признак агенства, в ответе будут платежные настройки и для клиентов,
    и для агенств"""
    person_category = create_person_category(session, country=country, ur=0)
    firm = create_firm(session, country=country)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('RUB', BANK)],
                      region_id=country.region_id, service_id=service.id, is_agency=None)

    rows = pay_policy_manager.get_rows(fields=FIELDS, service_id=None, region_id=country.region_id)
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   person_category, BANK, 'RUB', PAYSYS_GROUP_ID),
                                  (service.id, country.region_id, AGENCY_CATEGORY, WITHOUT_CONTRACT, firm,
                                   person_category, BANK, 'RUB', PAYSYS_GROUP_ID)}


def test_resident_and_not(session, pay_policy_manager, service, country):
    """Если в pay_policy_routing регион совпадает с регионом фирмы,
    в строке ответа будет указана резидентская категория плательщика и наоборот"""
    firm = create_firm(session, country=country)
    country_2 = create_country(session)
    firm_2 = create_firm(session, country=country_2)
    resident_person_category = create_person_category(session, country=country, ur=0, resident=1)
    non_resident_person_category = create_person_category(session, country=country_2, ur=0, resident=0)

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('RUB', BANK)],
                      region_id=country.region_id, service_id=service.id, is_agency=0)
    create_pay_policy(session, firm_id=firm_2.id, legal_entity=0, paymethods_params=[('RUB', BANK)],
                      region_id=country.region_id, service_id=service.id, is_agency=0)

    rows = pay_policy_manager.get_rows(fields=FIELDS, service_id=None, region_id=country.region_id)
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   resident_person_category, BANK, 'RUB', PAYSYS_GROUP_ID),
                                  (service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm_2,
                                   non_resident_person_category, BANK, 'RUB', PAYSYS_GROUP_ID)}


def test_is_contract(session, pay_policy_manager, service, country):
    """Если в pay_policy_part не указан признак is_contract, платежные настройки размножатся на строки с договором
    и без. Если в get_rows is_contract не указан явно, в ответе будут только платежные настройки без договора"""
    person_category = create_person_category(session, country=country, ur=0)
    firm = create_firm(session, country=country)
    pay_policy_service_id = create_pay_policy(session, firm_id=firm.id, legal_entity=0,
                                              paymethods_params=[('USD', BANK)],
                                              region_id=country.region_id, service_id=service.id, is_contract=None,
                                              is_agency=0)

    create_pay_policy(session, pay_policy_service_id=pay_policy_service_id, firm_id=firm.id, legal_entity=0,
                      paymethods_params=[('USD', BANK)], region_id=country.region_id, service_id=service.id,
                      is_contract=1, is_agency=1)

    rows = pay_policy_manager.get_rows(fields=FIELDS, service_id=None, region_id=country.region_id)
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   person_category, BANK, 'USD', PAYSYS_GROUP_ID)}


def test_is_contract_force(session, pay_policy_manager, service, country):
    """Признак is_contract в get_rows можно передавать явно"""
    person_category = create_person_category(session, country=country, ur=0)
    firm = create_firm(session, country=country)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                      region_id=country.region_id, service_id=service.id, is_contract=None, is_agency=0)

    rows = pay_policy_manager.get_rows(fields=FIELDS, service_id=None, region_id=country.region_id, is_contract=1)
    assert _extract_ans(rows) == {
        (service.id, country.region_id, CLIENT_CATEGORY, WITH_CONTRACT, firm, person_category,
         BANK, 'USD', PAYSYS_GROUP_ID)}


def test_is_atypical(session, pay_policy_manager, service, country):
    """Строки из pay_policy_part с признаком is_atypical, не попадут в ответ метода"""
    firm = create_firm(session, country=country)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, is_atypical=1, paymethods_params=[('USD', BANK)],
                      region_id=country.region_id, service_id=service.id, is_agency=0)

    rows = pay_policy_manager.get_rows(fields=FIELDS, service_id=None, region_id=country.region_id)
    assert not _extract_ans(rows)


def test_is_atypical_none(session, pay_policy_manager, service, country):
    """С фильтром is_atypical=None, возвращаются строки без фильтрации по признаку"""
    person_category = create_person_category(session, country=country, ur=0)
    firm = create_firm(session, country=country)
    pay_policy_part = create_pay_policy(session, firm_id=firm.id, legal_entity=0, is_atypical=1,
                                        paymethods_params=[('USD', BANK)], region_id=country.region_id,
                                        service_id=service.id, is_agency=0)
    create_pay_policy(session, pay_policy_service_id=pay_policy_part, firm_id=firm.id, legal_entity=0, is_atypical=0,
                      paymethods_params=[('USD', BANK)], region_id=country.region_id, service_id=service.id, is_agency=1)

    rows = pay_policy_manager.get_rows(fields=FIELDS, service_id=None, region_id=country.region_id, is_atypical=None)
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   person_category, BANK, 'USD', PAYSYS_GROUP_ID),
                                  (service.id, country.region_id, AGENCY_CATEGORY, WITHOUT_CONTRACT, firm,
                                   person_category, BANK, 'USD', PAYSYS_GROUP_ID)}


def test_is_atypical_force(session, pay_policy_manager, service, country):
    """Признак is_atypical в get_rows можно передавать явно"""
    person_category = create_person_category(session, country=country, ur=0)
    firm = create_firm(session, country=country)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, is_atypical=1, paymethods_params=[('USD', BANK)],
                      region_id=country.region_id, service_id=service.id, is_agency=0)

    rows = pay_policy_manager.get_rows(fields=FIELDS, service_id=None, region_id=country.region_id, is_atypical=1)
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   person_category, BANK, 'USD', PAYSYS_GROUP_ID)}


def test_service_undefined(session, pay_policy_manager, service, country):
    """Если не указывать сервис в параметрах метода, в ответе метода вернутся настройки для всех доступных сервисов"""
    person_category = create_person_category(session, country=country, ur=0)
    firm = create_firm(session, country=country)
    service_2 = create_service(session)

    for s in (service, service_2):
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, is_atypical=0, paymethods_params=[('USD', BANK)],
                          region_id=country.region_id, service_id=s.id, is_agency=0)

    rows = pay_policy_manager.get_rows(fields=FIELDS, service_id=None, region_id=country.region_id)
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   person_category, BANK, 'USD', PAYSYS_GROUP_ID),
                                  (service_2.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   person_category, BANK, 'USD', PAYSYS_GROUP_ID)}


def test_with_service_defined(session, pay_policy_manager, service, country):
    """service_id в get_rows можно передавать явно"""
    person_category = create_person_category(session, country=country, ur=0)
    firm = create_firm(session, country=country)
    service_2 = create_service(session)

    for s in (service, service_2):
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                          region_id=country.region_id, service_id=s.id, is_agency=0)

    rows = pay_policy_manager.get_rows(fields=FIELDS, service_id=service.id, region_id=country.region_id)
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   person_category, BANK, 'USD', PAYSYS_GROUP_ID)}


def test_several_services(session, pay_policy_manager, service, country):
    """в get_rows можно передавать список сервисов"""
    person_category = create_person_category(session, country=country, ur=0)
    firm = create_firm(session, country=country)
    service_2 = create_service(session)
    service_3 = create_service(session)

    for s in (service, service_2, service_3):
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                          region_id=country.region_id, service_id=s.id, is_agency=0)

    rows = pay_policy_manager.get_rows(fields=FIELDS, service_id=[service.id, service_2.id],
                                       region_id=country.region_id)
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   person_category, BANK, 'USD', PAYSYS_GROUP_ID),
                                  (service_2.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   person_category, BANK, 'USD', PAYSYS_GROUP_ID)}


def test_with_several_regions(session, pay_policy_manager, service, country):
    """в get_rows можно передавать список регионов"""
    firm = create_firm(session, country=country)
    country_2 = create_country(session)
    resident_person_category = create_person_category(session, country=country, ur=0, resident=1)
    non_resident_person_category = create_person_category(session, country=country, ur=0, resident=0)

    pay_policy_service_id = create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                                        region_id=country.region_id, service_id=service.id, is_agency=0)
    create_pay_policy(session, pay_policy_service_id=pay_policy_service_id, region_id=country_2.region_id, is_agency=0)

    rows = pay_policy_manager.get_rows(fields=FIELDS, service_id=None, region_id=[country.region_id,
                                                                                  country_2.region_id])
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   resident_person_category, BANK, 'USD', PAYSYS_GROUP_ID),
                                  (service.id, country_2.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   non_resident_person_category, BANK, 'USD', PAYSYS_GROUP_ID)}


def test_get_category(session, pay_policy_manager, country, service):
    """Категории плательщика в ответе метода совпадают с  legal_entity, resident, region_id платежных настроек
    и имеют is_default=1"""
    firm = create_firm(session, country=country)
    default_person_category = create_person_category(session, country=country, ur=0, resident=1, is_default=1)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                      region_id=country.region_id, service_id=service.id, is_agency=0)

    country_2 = create_country(session)
    non_default_person_category = create_person_category(session, country=country_2, ur=0, resident=1, is_default=0)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                      region_id=country_2.region_id, service_id=service.id, is_agency=0)

    rows = pay_policy_manager.get_rows(fields=CATEGORY_FIELDS)
    assert default_person_category in _extract_category(rows)
    assert non_default_person_category not in _extract_category(rows)


def test_get_category_resident_and_not(session, pay_policy_manager, country, service):
    firm = create_firm(session, country=country)
    country_2 = create_country(session)
    firm_2 = create_firm(session, country=country_2)
    resident_person_category = create_person_category(session, country=country, ur=0, resident=1, is_default=1)
    non_resident_person_category = create_person_category(session, country=country_2, ur=0, resident=0, is_default=1)

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                      region_id=country.region_id, service_id=service.id, is_agency=0)
    create_pay_policy(session, firm_id=firm_2.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                      region_id=country.region_id, service_id=service.id, is_agency=0)

    rows = pay_policy_manager.get_rows(fields=CATEGORY_FIELDS, region_id=country.region_id)
    assert _extract_category(rows) == {resident_person_category, non_resident_person_category}


def test_get_category_fixed(session, pay_policy_manager, country, service):
    """категория явно указанная в платежных политиках, возвращается без  проверок на is_default, legal_entity,
    и resident"""
    firm = create_firm(session, country=country)
    country_2 = create_country(session)
    person_category = create_person_category(session, country=country, ur=1, resident=1, is_default=0)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, category=person_category.category,
                      paymethods_params=[('USD', BANK)], region_id=country_2.region_id, service_id=service.id,
                      is_agency=0)
    rows = pay_policy_manager.get_rows(fields=CATEGORY_FIELDS)
    assert person_category in _extract_category(rows)


def test_several_regions_and_services(session, pay_policy_manager, country):
    """сложный фильтр как в подборе платежных настроек для бездоговорных оплат"""
    firm = create_firm(session, country=country)
    country_2 = create_country(session)
    resident_person_category = create_person_category(session, country=country, ur=0, resident=1)
    non_resident_person_category = create_person_category(session, country=country, ur=0, resident=0)
    service_1 = create_service(session)
    service_2 = create_service(session)
    service_3 = create_service(session)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                      region_id=country.region_id, service_id=service_1.id, is_agency=0)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                      region_id=country_2.region_id, service_id=service_2.id, is_agency=0)
    pay_policy_service_id = create_pay_policy(
        session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)], region_id=country_2.region_id,
        service_id=service_3.id, is_agency=0
    )
    create_pay_policy(session, pay_policy_service_id=pay_policy_service_id, region_id=country_2.region_id, is_agency=1)

    rows = pay_policy_manager.get_rows(fields=FIELDS,
                                       service_id=[service_2.id,
                                                   service_1.id,
                                                   service_3.id],
                                       region_id=[country.region_id,
                                                  country_2.region_id],
                                       is_agency=0,
                                       is_atypical=None)
    assert _extract_ans(rows) == {(service_1.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   resident_person_category, BANK, 'USD', PAYSYS_GROUP_ID),
                                  (service_2.id, country_2.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   non_resident_person_category, BANK, 'USD', PAYSYS_GROUP_ID),
                                  (service_3.id, country_2.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   non_resident_person_category, BANK, 'USD', PAYSYS_GROUP_ID)}


def test_hidden_payment_method(session, pay_policy_manager, country, service):
    firm = create_firm(session, country=country)
    resident_person_category = create_person_category(session, country=country, ur=0, resident=1)
    pay_policy_service_id = create_pay_policy(
        session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)], region_id=country.region_id,
        service_id=service.id, is_agency=0
    )
    create_pay_policy_payment_method(session, pay_policy_service_id=pay_policy_service_id, iso_currency='RUB',
                                     payment_method_id=BANK, hidden=1)

    rows = pay_policy_manager.get_rows(fields=FIELDS,
                                       service_id=[service.id],
                                       region_id=[country.region_id],
                                       is_atypical=None)
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   resident_person_category, BANK, 'USD', PAYSYS_GROUP_ID)}


def test_hidden_region(session, pay_policy_manager, country, service):
    firm = create_firm(session, country=country)
    country_2 = create_country(session)
    resident_person_category = create_person_category(session, country=country, ur=0, resident=1)
    pay_policy_service_id = create_pay_policy(
        session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)], region_id=country.region_id,
        service_id=service.id, is_agency=0
    )
    create_pay_policy(session, pay_policy_service_id=pay_policy_service_id, region_id=country_2.region_id, is_agency=0,
                      hidden=1)

    rows = pay_policy_manager.get_rows(fields=FIELDS,
                                       service_id=[service.id],
                                       region_id=[country.region_id,
                                                  country_2.region_id],
                                       is_atypical=None)
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   resident_person_category, BANK, 'USD', PAYSYS_GROUP_ID)}
