# -*- coding: utf-8 -*-

import pytest

from tests.balance_tests.client.client_common import (create_client,
                           create_country,
                           create_service,
                           create_firm,
                           create_person_category,
                           create_person,
                           create_pay_policy,
                           _extract_categories)


def test_client_w_unknown_region(session, client, country):
    """Региона клиента нет в настройках - нет доступных категорий плательщика"""
    client.region_id = country.region_id
    categories = client.get_available_person_categories(with_self=True)
    assert _extract_categories(categories) == set()


def test_client_wo_region(session, client, service):
    """Для клиента без региона бернутся все категории плательщиков, соответ-сую строкам с is_atypical=0,
    в тч резиденты разных регионов"""
    client.region_id = None
    firms = [create_firm(session)]
    person_categories = []
    for firm in firms:
        person_categories.append(create_person_category(session, country=firm.country, ur=0, resident=1,
                                                        is_default=1))

        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', 1001)],
                       region_id=firm.country.region_id, service_id=service.id, is_agency=0)
    categories = _extract_categories(client.get_available_person_categories(with_self=True))
    assert {category.category for category in person_categories}.issubset(categories)


@pytest.mark.parametrize('with_self', [True, False])
def test_client_w_region_with_self(session, client, service, with_self):
    """клиенту с регионом предлагаем создавать резидентов только этого региона, если передан флаг with_self """
    firms = [create_firm(session) for _ in range(2)]
    client.region_id = firms[0].country.region_id
    person_categories = []
    for firm in firms:
        person_categories.append(create_person_category(session, country=firm.country, ur=0,
                                                        resident=1, is_default=1))

        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', 1001)],
                       region_id=firm.country.region_id, service_id=service.id, is_agency=0)

    categories = _extract_categories(client.get_available_person_categories(with_self=with_self))
    if with_self:
        assert categories == {person_categories[0].category}
    else:
        assert {category.category for category in person_categories}.issubset(categories)


def test_client_w_region(session, client, service):
    """with_self флаг по умолчанию равен False"""
    firms = [create_firm(session) for _ in range(2)]
    client.region_id = firms[0].country.region_id
    person_categories = []
    for firm in firms:
        person_categories.append(create_person_category(session, country=firm.country, ur=0,
                                                        resident=1, is_default=1))

        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', 1001)],
                       region_id=firm.country.region_id, service_id=service.id, is_agency=0)

    categories = _extract_categories(client.get_available_person_categories())
    assert {category.category for category in person_categories}.issubset(categories)


def test_client_w_region_w_person_non_resident(session, client, firm, service):
    """отдаем нерезидентов, для которых есть платежные настройки из региона клиента"""
    client.region_id = firm.country.region_id

    firm_2 = create_firm(session)
    non_resident_person_category = create_person_category(session, country=firm_2.country, ur=0, resident=0,
                                                          is_default=1)
    create_pay_policy(session, firm_id=firm_2.id, legal_entity=0, paymethods_params=[('USD', 1001)],
                   region_id=firm.country.region_id, service_id=service.id, is_agency=0)

    categories = client.get_available_person_categories(with_self=True)
    assert _extract_categories(categories) == {non_resident_person_category.category}


def test_client_w_region_w_person_resident_and_not_resident(session, client, service):
    """в ответе могут быть и резиденты, и нерезиденты"""
    firms = [create_firm(session) for _ in range(2)]
    client.region_id = firms[0].country.region_id
    person_categories = []
    for firm, country_from, resident in [(firms[0], firms[0].country, 1),
                                         (firms[1], firms[0].country, 0)]:
        person_categories.append(create_person_category(session, country=firm.country, ur=0, resident=resident,
                                                        is_default=1))
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', 1001)],
                       region_id=country_from.region_id, service_id=service.id, is_agency=0)

    categories = client.get_available_person_categories(with_self=True)
    assert _extract_categories(categories) == {category.category for category in person_categories}


def test_client_w_region_multiple_non_resident(session, client, country, service):
    """из одного региона можно платить в фирмы разных регионов как нерезидент"""
    client.region_id = country.region_id
    firms = [create_firm(session) for _ in range(2)]

    person_categories = []
    for firm in firms:
        person_categories.append(create_person_category(session, country=firm.country, ur=0, resident=0,
                                                        is_default=1))
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', 1001)],
                       region_id=country.region_id, service_id=service.id, is_agency=0)

    categories = client.get_available_person_categories(with_self=True)
    assert _extract_categories(categories) == {category.category for category in person_categories}


@pytest.mark.parametrize('with_self', [True, False])
def test_client_wo_region_w_resident_person(session, client, service, with_self):
    """регион резидентского плательщика работает, как и регион клиента, но не зависит от значения флага
    with_self"""
    client.region_id = None
    person_categories = []
    firms = [create_firm(session) for _ in range(2)]
    for firm, country_from, resident in [(firms[0], firms[0].country, 1),
                                         (firms[1], firms[0].country, 0)]:
        person_categories.append(
            create_person_category(session, country=firm.country, ur=0, resident=resident, is_default=1))
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', 1001)],
                       region_id=country_from.region_id, service_id=service.id, is_agency=0)

    create_person(session, client=client, type=person_categories[0].category)
    categories = client.get_available_person_categories()
    assert _extract_categories(categories) == {category.category for category in person_categories}


def test_client_w_non_resident_person(session, client, service):
    """если у клиента нет региона и есть только плательщикинерезидент, по платежным настройкам подбираем вероятный
     регион (тот, который есть в платежных настройках и который не равен региону, относительно которго задан статус
     резиденства плательщика). С полученным регионом подбираем доступные категории"""
    client.region_id = None
    firms = [create_firm(session) for _ in range(3)]
    person_categories = []
    for firm, resident in zip(firms, [1, 0, 0]):
        person_categories.append(create_person_category(session, country=firm.country, ur=0, resident=resident,
                                                        is_default=1))
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', 1001)],
                       region_id=firms[0].country.region_id, service_id=service.id, is_agency=0)

    person = create_person(session, client=client, type=person_categories[1].category)

    categories = client.get_available_person_categories()
    assert _extract_categories(categories) == {category.category for category in person_categories}


def test_w_non_resident_person_w_force_service(session, client, country):
    """в get_available_person_categories можно задать фильтр по сервису"""
    client.region_id = country.region_id
    firms = [create_firm(session) for _ in range(2)]
    services = [create_service(session) for _ in range(2)]
    person_categories = []
    for firm, service in zip(firms, services):
        person_categories.append(create_person_category(session, country=firm.country, ur=0, resident=0, is_default=1))
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', 1001)],
                       region_id=country.region_id, service_id=service.id, is_agency=0)

    categories = client.get_available_person_categories(with_self=True, services=services[1].id)
    assert _extract_categories(categories) == {person_categories[1].category}


def test_w_resident_person_w_force_service(session, client):
    """в get_available_person_categories можно задать фильтр по сервису"""
    client.region_id = None
    person_firm = create_firm(session)
    resident_person_category = create_person_category(session, country=person_firm.country, ur=0, resident=1,
                                                      is_default=1)
    person = create_person(session, client=client, type=resident_person_category.category)
    firms = [create_firm(session) for _ in range(2)]
    services = [create_service(session) for _ in range(2)]
    person_categories = []
    for firm, service in zip(firms, services):
        person_categories.append(
            create_person_category(session, country=firm.country, ur=0, resident=0, is_default=1))
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', 1001)],
                       region_id=person_firm.country.region_id, service_id=service.id, is_agency=0)

    categories = client.get_available_person_categories(services=services[1].id)
    assert _extract_categories(categories) == {person_categories[1].category}


def test_w_force_client_category(session, client, country, service):
    client.region_id = country.region_id
    firms = [create_firm(session) for _ in range(2)]
    person_categories = []
    for firm, is_agency in zip(firms, [0, 1]):
        person_categories.append(create_person_category(session, country=firm.country, ur=0, resident=0,
                                                        is_default=1))
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', 1001)],
                          region_id=country.region_id, service_id=service.id, is_agency=is_agency)

    categories = client.get_available_person_categories(with_self=True)
    assert _extract_categories(categories) == {person_categories[0].category}
