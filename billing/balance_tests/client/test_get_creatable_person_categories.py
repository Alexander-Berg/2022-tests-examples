# -*- coding: utf-8 -*-

import pytest

from balance.constants import (
    PersonCategoryCodes,
    ServiceId,
    RegionId,
    PermissionCode,
)
from balance.mapper import Permission, PersonCategory
from tests.balance_tests.client.client_common import (
    create_client,
    create_person_category,
    create_person,
    create_firm,
    create_service,
    create_pay_policy,
    create_country,
    create_role,
    create_passport,
    USE_ADMIN_PERSONS,
    PermType,
    get_client_permission,
)


def test_creatable_person_category_auto_only_check(session, client):  # Посмотреть эти тесты после миграции на dev
    """get_creatable_person_categories возвращает только категории плательщиков с признаком auto_only=0"""
    creatable_categories = client.get_creatable_person_categories()
    assert {category.auto_only for category in creatable_categories} == {0}


@pytest.mark.permissions
@pytest.mark.parametrize(
    'perm_type, expected_admin_only_values',
    [
        pytest.param(PermType.w_perm, {0, 1}, id='w perm'),
        pytest.param(PermType.wo_perm, {0}, id='wo perm'),
        pytest.param(PermType.w_right_client, {0, 1}, id='w right client'),
        pytest.param(PermType.w_wrong_client, {0}, id='w wrong client'),
    ],
)
def test_creatable_person_category_admin_only_check(session, client, perm_type, expected_admin_only_values):
    """c правом UseAdminPersons отдаем категории без учета признака admin_only,
    без этого права только с admin_only=0"""
    roles = []
    perm = get_client_permission(session, perm_type, USE_ADMIN_PERSONS, client)
    if perm:
        roles.append(create_role(session, perm))
    create_passport(session, roles, patch_session=True)
    creatable_categories = client.get_creatable_person_categories()
    assert {category.admin_only for category in creatable_categories} == expected_admin_only_values


def test_check_perm(session, client):
    """c флагом  check_perm=False не проверемся права и отдам категории платлеьщиков без учета признака admin_only"""
    client = create_client(session)
    role = create_role(session)
    create_passport(session, [role], patch_session=True)
    creatable_categories = client.get_creatable_person_categories()
    assert {category.admin_only for category in creatable_categories} == {0}
    creatable_categories = client.get_creatable_person_categories(check_perm=False)
    assert {category.admin_only for category in creatable_categories} == {0, 1}


@pytest.mark.parametrize('client_only', [0, 1])
def test_not_client_only_service(session, firm, country, client_only):
    """get_creatable_person_categories без фильтра по сервисам подбирает категорию плательщика только
     по платежным настройкам с сервисами с признаком client_only = 0"""
    client = create_client(session, region_id=country.region_id)
    service = create_service(session, client_only=client_only)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=0, is_default=1)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', 1001)],
                      region_id=country.region_id, service_id=service.id, is_agency=0)

    categories = set(client.get_creatable_person_categories())
    if client_only:
        assert categories == set()
    else:
        assert categories == {person_category}


def test_filter_by_not_client_only_service(session, firm, country):
    """при фильтрации по сервису игнорируем признак client_only"""
    client = create_client(session, region_id=country.region_id)
    service = create_service(session, client_only=1)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=0, is_default=1)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', 1001)],
                      region_id=country.region_id, service_id=service.id, is_agency=0)

    categories = set(client.get_creatable_person_categories(service_id=service.id))
    assert categories == {person_category}


def test_filter_by_service(session, firm, country):
    """get_creatable_person_categories можно фильтровать по сервису платежных настроек"""
    client = create_client(session, region_id=country.region_id)
    firms = [create_firm(session) for _ in range(2)]
    services = [create_service(session, client_only=0) for _ in range(2)]
    person_categories = []
    for firm, service in zip(firms, services):
        person_categories.append(create_person_category(session, country=firm.country, ur=0, resident=0, is_default=1))
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', 1001)],
                          region_id=country.region_id, service_id=service.id, is_agency=0)

    categories = set(client.get_creatable_person_categories(service_id=services[1].id))
    assert categories == {person_categories[1]}
    categories = set(client.get_creatable_person_categories())
    assert set(person_categories) == categories


def test_partner(session, firm, service, client):
    """get_creatable_person_categories c признаком is_partner возвращает всех партнерские категории плательщиков"""
    person = create_person(session, client=client,
                           type=create_person_category(session, country=firm.country, ur=0, resident=1,
                                                       is_default=1).category)
    create_pay_policy(session, firm_id=firm.id, region_id=firm.country.region_id,
                      service_id=service.id, category=person.person_category.category, is_agency=0)
    assert set(client.get_creatable_person_categories(service_id=service.id)) == {person.person_category}
    categories = client.get_creatable_person_categories(is_partner=True, service_id=service.id)
    assert set(categories) == set(session.query(PersonCategory).filter_by(partnerable=1))


@pytest.mark.single_account
def test_client_can_create_allowed_categories(session):
    """ Проверяем, что клиенту без плательщиков можно создавать физика """
    client = create_client(session, region_id=RegionId.RUSSIA, with_single_account=True)
    creatable_categories = {
        category.category for category
        in client.get_creatable_person_categories(service_id=ServiceId.DIRECT)
    }
    assert PersonCategoryCodes.russia_resident_individual in creatable_categories


@pytest.mark.single_account
def test_denied_categories_filtered(session):
    """
    Проверяем, что клиенту с существующим физиком нельзя создавать еще одного,
    но можно создавать юрика.
    """
    client = create_client(session, region_id=RegionId.RUSSIA, with_single_account=True)
    create_person(session, type='ph', client=client)
    creatable_categories = {
        category.category for category
        in client.get_creatable_person_categories(service_id=ServiceId.DIRECT)
    }
    assert PersonCategoryCodes.russia_resident_individual not in creatable_categories
    assert PersonCategoryCodes.russia_resident_legal_entity in creatable_categories


def test_client_w_region_multiple_non_resident(session, client, country):
    """из одного региона можно платить в фирмы разных регионов как нерезидент"""
    service = create_service(session, client_only=0)
    client.region_id = country.region_id
    firms = [create_firm(session) for _ in range(2)]

    person_categories = []
    for firm in firms:
        person_categories.append(create_person_category(session, country=firm.country, ur=0, resident=0,
                                                        is_default=1))
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', 1001)],
                          region_id=country.region_id, service_id=service.id, is_agency=0)

    categories = set(client.get_creatable_person_categories())
    assert categories == {category for category in person_categories}


def test_client_w_region_w_person_resident_and_not_resident(session, client):
    """в ответе могут быть и резиденты, и нерезиденты"""
    service = create_service(session, client_only=0)
    firms = [create_firm(session) for _ in range(2)]
    client.region_id = firms[0].country.region_id
    person_categories = []
    for firm, country_from, resident in [(firms[0], firms[0].country, 1),
                                         (firms[1], firms[0].country, 0)]:
        person_categories.append(create_person_category(session, country=firm.country, ur=0, resident=resident,
                                                        is_default=1))
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', 1001)],
                          region_id=country_from.region_id, service_id=service.id, is_agency=0)

    categories = set(client.get_creatable_person_categories())
    assert categories == {category for category in person_categories}


def test_client_w_region_w_person_non_resident(session, client, firm, service):
    """отдаем нерезидентов, для которых есть платежные настройки из региона клиента"""
    client.region_id = firm.country.region_id

    firm_2 = create_firm(session)
    non_resident_person_category = create_person_category(session, country=firm_2.country, ur=0, resident=0,
                                                          is_default=1)
    create_pay_policy(session, firm_id=firm_2.id, legal_entity=0, paymethods_params=[('USD', 1001)],
                      region_id=firm.country.region_id, service_id=service.id, is_agency=0)

    categories = set(client.get_creatable_person_categories())
    assert categories == {non_resident_person_category}


def test_client_wo_region_w_resident_person(session, client, service):
    """регион резидентского плательщика работает, как и регион клиента"""
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
    categories = set(client.get_creatable_person_categories())
    assert categories == set(person_categories)


def test_client_w_region(session, client, service):
    """клиенту с регионом предлагаем создавать резидентов только этого региона, если передан флаг with_self """
    firms = [create_firm(session) for _ in range(2)]
    client.region_id = firms[0].country.region_id
    person_categories = []
    for firm in firms:
        person_categories.append(create_person_category(session, country=firm.country, ur=0,
                                                        resident=1, is_default=1))

        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', 1001)],
                          region_id=firm.country.region_id, service_id=service.id, is_agency=0)

    categories = set(client.get_creatable_person_categories())
    assert categories == {person_categories[0]}
