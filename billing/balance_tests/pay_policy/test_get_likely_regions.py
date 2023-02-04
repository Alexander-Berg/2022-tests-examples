# -*- coding: utf-8 -*-

import pytest

from balance.constants import *
from tests.balance_tests.pay_policy.pay_policy_common import (create_country,
                               get_pay_policy_manager,
                               create_service,
                               create_firm,
                               create_person_category,
                               create_pay_policy,
                               )
from balance.mapper import PayPolicyRegion, PayPolicyService

PAYSYS_GROUP_ID = 0
BANK = PaymentMethodIDs.bank
CARD = PaymentMethodIDs.credit_card


def test_get_likely_regions(session, pay_policy_manager):
    """ Вызов get_likely_regions без параметров вернет все регионы из v_pay_policy_routing
    без признака is_atypical=0"""
    countries = [create_country(session) for _ in range(2)]
    for country, is_atypical in zip(countries, [0, 1]):
        create_pay_policy(session, region_id=country.region_id, firm_id=create_firm(session, country=country).id,
                          is_atypical=is_atypical, service_id=create_service(session).id)

    regions = pay_policy_manager.get_likely_regions()
    expected_regions = session.query(PayPolicyRegion.region_id).join(PayPolicyService).filter(
        PayPolicyService.is_atypical == 0).distinct().all()
    assert regions == set(r.region_id for r in expected_regions)
    assert countries[0].region_id in regions
    assert countries[1].region_id not in regions


def test_get_likely_regions_no_compatible_rows(session, pay_policy_manager, country):
    """если после фильтрации ни одной строки не нашлось, вернутся все регионы без признака is_atypical=0"""
    person_category = create_person_category(session, is_default=1, resident=1, country=country, ur=0)

    regions = pay_policy_manager.get_likely_regions(categories=[person_category])
    expected_regions = session.query(PayPolicyRegion.region_id).join(PayPolicyService).filter(
        PayPolicyService.is_atypical == 0).distinct().all()
    assert regions == set(r.region_id for r in expected_regions)


def test_get_likely_regions_is_agency_non_defined(session, pay_policy_manager):
    """get_likely_regions при вызове без фильтра is_agency, возвращает регионы из строк v_pay_policy_routing
    и для агенств, и для клиентов"""
    countries = [create_country(session) for _ in range(2)]
    for country, is_agency in zip(countries, [0, 1]):
        create_pay_policy(session, region_id=country.region_id, firm_id=create_firm(session, country=country).id,
                       is_atypical=0, service_id=create_service(session).id, is_agency=is_agency)
    regions = pay_policy_manager.get_likely_regions(is_agency=None)
    assert set(country.region_id for country in countries).issubset(regions)


def test_get_likely_regions_is_agency_defined(session, pay_policy_manager):
    """при вызове get_likely_regions можно передавать значение is_agency"""
    countries = [create_country(session) for _ in range(2)]
    for country, is_agency in zip(countries, [0, 1]):
        create_pay_policy(session, region_id=country.region_id, firm_id=create_firm(session, country=country).id,
                       is_atypical=0, service_id=create_service(session).id, is_agency=is_agency)
    regions = pay_policy_manager.get_likely_regions(is_agency=1)
    assert countries[0].region_id not in regions
    assert countries[1].region_id in regions


def test_get_likely_regions_with_non_default_category(session, pay_policy_manager):
    """если при вызове get_likely_regions передана категория плательщика с признаком is_default=0,
    вернутся регионы из строк v_pay_policy_routing с признаком is_atypical=0 и категорией, равной переданной"""
    non_default_person_category = create_person_category(session, is_default=0)
    countries = [create_country(session) for _ in range(3)]
    create_pay_policy(session, region_id=countries[0].region_id, category=non_default_person_category.category,
                   is_atypical=0, firm_id=create_firm(session, country=countries[0]).id,
                   service_id=create_service(session).id)
    create_pay_policy(session, region_id=countries[1].region_id, category=None, is_atypical=0,
                   firm_id=create_firm(session, country=countries[1]).id, service_id=create_service(session).id)
    create_pay_policy(session, region_id=countries[2].region_id, category=non_default_person_category.category,
                   is_atypical=1, firm_id=create_firm(session, country=countries[2]).id,
                   service_id=create_service(session).id)
    regions = pay_policy_manager.get_likely_regions(categories=[non_default_person_category])
    assert regions == {countries[0].region_id}


def test_get_likely_regions_with_non_default_category_and_service(session, pay_policy_manager):
    """если при вызове get_likely_regions переданы пара сервис и категория плательщика (с признаком is_default=0),
    вернутся регионы из строк платежных настроек с признаком is_atypical=0, и соответствующими категорией и сервисом"""
    countries = [create_country(session) for _ in range(2)]
    services = [create_service(session) for _ in range(2)]
    non_default_person_category = create_person_category(session, is_default=0)
    for country, service in zip(countries, services):
        create_pay_policy(session, region_id=country.region_id, category=non_default_person_category.category,
                       is_atypical=0, firm_id=create_firm(session, country=country).id,
                       service_id=service.id)

    regions = pay_policy_manager.get_likely_regions(service_categories=[(services[0].id, non_default_person_category)])
    assert regions == {countries[0].region_id}


def test_get_likely_regions_resident_and_not(session, pay_policy_manager, service):
    """если при вызове get_likely_regions передана дефолтная категория плательщика резидента,
    вернется регион резиденства, если есть платежные настройки c фирмой из этого региона и соот-им legal_entity"""
    countries = [create_country(session) for _ in range(2)]
    firm = create_firm(session, country=countries[0])
    person_categories = []
    for country, person_country, resident in [(countries[0], countries[0], 1),
                                              (countries[1], countries[0], 0)]:
        person_categories.append(
            create_person_category(session, is_default=1, resident=resident, country=countries[0], ur=0))
        create_pay_policy(session, region_id=country.region_id, category=None, is_atypical=0, firm_id=firm.id,
                          service_id=service.id)

    regions = pay_policy_manager.get_likely_regions(categories=[person_categories[0]])
    assert regions == {countries[0].region_id}
    regions = pay_policy_manager.get_likely_regions(categories=[person_categories[1]])
    assert regions == {countries[1].region_id}


def test_get_likely_regions_defined_default_category(session, pay_policy_manager, service):
    """если в строках платежных настроек явно прописана дефолтная категория плательщика,
    берем ее без учета резиденства"""
    countries = [create_country(session) for _ in range(2)]
    firm = create_firm(session, country=countries[0])
    resident_person_category = create_person_category(session, is_default=1, resident=1, country=countries[0], ur=0)
    for country in countries:
        create_pay_policy(session, region_id=country.region_id, category=resident_person_category.category,
                          is_atypical=0, firm_id=firm.id, service_id=service.id)

    regions = pay_policy_manager.get_likely_regions(categories=[resident_person_category])
    assert regions == {country.region_id for country in countries}


def test_get_likely_regions_legal_entity(session, pay_policy_manager, service):
    """если при вызове get_likely_regions передана категория плательщика, вернувшиеся строки будут профильтрованы
    по признаку ur категории плательщика"""
    firm = create_firm(session, country=create_country(session))
    countries = [create_country(session) for _ in range(2)]
    person_categories = []
    for country, ur in zip(countries, [1, 0]):
        category = create_person_category(session, is_default=1, resident=0, country=firm.country, ur=ur)
        person_categories.append(category)
        create_pay_policy(session, region_id=country.region_id, category=category.category, is_atypical=0,
                          firm_id=firm.id, service_id=service.id, legal_enity=ur)

    regions = pay_policy_manager.get_likely_regions(categories=[person_categories[0]])
    assert regions == {countries[0].region_id}
    regions = pay_policy_manager.get_likely_regions(categories=[person_categories[1]])
    assert regions == {countries[1].region_id}
