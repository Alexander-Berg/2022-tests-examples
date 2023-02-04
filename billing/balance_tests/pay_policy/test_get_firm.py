import pytest
from balance.exc import INVALID_PARAM
from tests.balance_tests.pay_policy.pay_policy_common import (get_pay_policy_manager,
                               create_service,
                               create_country,
                               create_firm,
                               create_pay_policy,
                               create_person_category)


def test_wo_routing(session, pay_policy_manager, service, country):
    firm = pay_policy_manager.get_firm(service.id, country.region_id)
    assert firm is None


def test_single_firm(session, pay_policy_manager, service, country):
    firm = create_firm(session, country=country)
    create_pay_policy(session, region_id=country.region_id, category=None, is_atypical=0,
                      firm_id=firm.id, service_id=service.id, legal_entity=0, is_contract=0)
    assert pay_policy_manager.get_firm(service.id, country.region_id) == firm


def test_atypical_routing(session, pay_policy_manager, service, country):
    firm = create_firm(session, country=country)
    create_pay_policy(session, region_id=country.region_id, category=None, is_atypical=1,
                      firm_id=firm.id, service_id=service.id, legal_entity=0, is_contract=0)
    assert pay_policy_manager.get_firm(service.id, country.region_id) is None


def test_multiple_firms(session, pay_policy_manager, service, country):
    firms = [create_firm(session, country=country) for _ in range(2)]
    for firm in firms:
        create_pay_policy(session, region_id=country.region_id, category=None, is_atypical=0,
                          firm_id=firm.id, service_id=service.id, legal_entity=0, is_contract=0)
    with pytest.raises(INVALID_PARAM) as exc_info:
        pay_policy_manager.get_firm(service.id, country.region_id)
    assert exc_info.value.msg.startswith('Invalid parameter for function: Several firms were found:')


def test_multiple_firms_single_with_category(session, pay_policy_manager, service, country):
    firms = [create_firm(session, country=country) for _ in range(2)]
    categories = []
    for firm, ur in zip(firms, [0, 1]):
        categories.append(create_person_category(session, is_default=1, resident=1, country=firm.country, ur=ur))
        create_pay_policy(session, region_id=country.region_id, category=None, is_atypical=0,
                          firm_id=firm.id, service_id=service.id, legal_entity=ur, is_contract=0)
    assert pay_policy_manager.get_firm(service.id, country.region_id, category=categories[0]) == firms[0]


def test_w_resident_category(session, pay_policy_manager, service, country):
    firm = create_firm(session, country=country)
    category = create_person_category(session, is_default=1, resident=1, country=firm.country, ur=0)
    create_pay_policy(session, region_id=country.region_id, category=None, is_atypical=0,
                      firm_id=firm.id, service_id=service.id, legal_entity=0, is_contract=0)
    assert pay_policy_manager.get_firm(service.id, category=category) == firm


def test_w_non_resident_category(session, pay_policy_manager, service, country):
    firm = create_firm(session, country=country)
    category = create_person_category(session, is_default=1, resident=0, country=firm.country, ur=0)
    create_pay_policy(session, region_id=create_country(session).region_id, category=None, is_atypical=0,
                      firm_id=firm.id, service_id=service.id, legal_entity=0, is_contract=0)
    assert pay_policy_manager.get_firm(service.id, category=category) == firm


def test_w_non_resident_non_default_category(session, pay_policy_manager, service, country):
    firm = create_firm(session, country=country)
    category = create_person_category(session, is_default=0, resident=0, country=firm.country, ur=0)
    create_pay_policy(session, region_id=create_country(session).region_id, category=category.category, is_atypical=0,
                      firm_id=firm.id, service_id=service.id, legal_entity=0, is_contract=0)
    assert pay_policy_manager.get_firm(service.id, category=category) == firm
