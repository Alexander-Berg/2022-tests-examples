# -*- coding: utf-8 -*-

import pytest

from tests.balance_tests.pay_policy.pay_policy_common import (BANK,
                               get_pay_policy_manager,
                               create_service,
                               create_country,
                               create_firm,
                               create_pay_policy,
                               _extract_ans)


@pytest.fixture
def pay_policy_manager(session):
    return get_pay_policy_manager(session)


@pytest.fixture
def service(session):
    return create_service(session)


@pytest.fixture
def country(session):
    return create_country(session)


@pytest.fixture
def firm(session):
    return create_firm(session)


@pytest.mark.parametrize('is_atypical, is_contract', [(0, 0),
                                                      (0, 1),
                                                      (1, 0)])
def test_default_filters(session, pay_policy_manager, service, country, firm, is_atypical, is_contract):
    """если не передан ни сервис, ни регион, будут указаны пары сервис регион из всех платежных политик
     не по договору, без признака is_atypical"""
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                      region_id=country.region_id, service_id=service.id, is_agency=0, is_atypical=is_atypical,
                      is_contract=is_contract)
    result = _extract_ans(pay_policy_manager.get_services_regions())
    if is_atypical or is_contract:
        assert (service.id, country.region_id) not in result
    else:
        assert (service.id, country.region_id) in result


def test_w_service_filter(session, pay_policy_manager, country, firm):
    """Если сервис передан явно, фильтруем пары сервис-регион по сервису"""
    services = [create_service(session) for _ in range(2)]
    for service in services:
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                          region_id=country.region_id, service_id=service.id, is_agency=0, is_atypical=0,
                          is_contract=0)
    result = _extract_ans(pay_policy_manager.get_services_regions(service_id=services[0].id))
    assert (services[0].id, country.region_id) in result
    assert (services[1].id, country.region_id) not in result
    result = _extract_ans(pay_policy_manager.get_services_regions())
    assert {(services[0].id, country.region_id), (services[1].id, country.region_id)}.issubset(result)


def test_w_region_filter(session, pay_policy_manager, service, firm):
    """Если регион передан явно, фильтруем пары сервис-регион по региону"""
    countries = [create_country(session) for _ in range(2)]
    for country in countries:
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                          region_id=country.region_id, service_id=service.id, is_agency=0, is_atypical=0,
                          is_contract=0)
    result = _extract_ans(pay_policy_manager.get_services_regions(region_id=countries[0].region_id))
    assert (service.id, countries[0].region_id) in result
    assert (service.id, countries[1].region_id) not in result
    result = _extract_ans(pay_policy_manager.get_services_regions())
    assert {(service.id, countries[0].region_id), (service.id, countries[1].region_id)}.issubset(result)


def test_w_is_agency_filter(session, pay_policy_manager, service, country, firm):
    """фильтруем платежные политику по признаку is_agency"""
    countries = [create_country(session) for _ in range(2)]
    for country, is_agency in zip(countries, [0, 1]):
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                          region_id=country.region_id, service_id=service.id, is_agency=is_agency, is_atypical=0,
                          is_contract=0)
    result = _extract_ans(pay_policy_manager.get_services_regions(is_agency=0))
    assert (service.id, countries[0].region_id) in result
    assert (service.id, countries[1].region_id) not in result
