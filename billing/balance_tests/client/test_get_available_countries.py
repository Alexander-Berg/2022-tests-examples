# -*- coding: utf-8 -*-

import pytest
from tests.balance_tests.client.client_common import (create_client,
                           create_country,
                           create_service,
                           create_firm,
                           create_person_category,
                           create_person,
                           create_pay_policy,
                           create_paid_invoice)

PAYSYS_GROUP_ID = 0


@pytest.fixture
def client(session, **kwargs):
    return create_client(session, is_agency=0, **kwargs)


def test_client_w_region(session, client):
    """Клиенты с регионом - только из этого региона, если передан флаг with_self"""
    client.region_id = create_country(session)
    regions = client.get_available_countries(with_self=True)
    assert regions == {client.region_id}


def test_client_w_person_resident(session):
    """Клиенты с плательщиком резидентом - из региона резиденства плательщика"""
    person_category = create_person_category(session, resident=1, country=create_country(session))
    person = create_person(session, type=person_category.category)
    regions = person.client.get_available_countries()

    assert regions == {person_category.country.region_id}


def test_client_w_person_nonresident(session):
    """Клиент с плательщиком-нерезидентом - не из региона, относительно которого нерезидент"""
    country_1 = create_country(session)
    country_2 = create_country(session)
    service = create_service(session)
    non_resident_person_category = create_person_category(session, is_default=1, resident=0, country=country_2, ur=0)

    create_pay_policy(session, firm_id=create_firm(session, country=country_2).id, region_id=country_1.region_id,
                   service_id=service.id)
    person = create_person(session, type=non_resident_person_category.category)
    regions = person.client.get_available_countries()
    assert regions == {country_1.region_id}


def test_w_multiple_regions(session):
    """Клиент с плательщиком-нерезидентом - из любого региона, из которго можно платить в регион,
     относительно которого плательщик нерезидент"""
    country_1 = create_country(session)
    country_2 = create_country(session)
    service_1 = create_service(session)
    firm = create_firm(session, country=country_1)
    non_default_person_category = create_person_category(session, is_default=0, resident=0, country=country_1, ur=0)
    person = create_person(session, type=non_default_person_category.category)
    pay_policy_service_id = create_pay_policy(
        session, firm_id=firm.id, region_id=country_1.region_id, service_id=service_1.id, category=non_default_person_category.category
    )
    create_pay_policy(session, pay_policy_service_id=pay_policy_service_id, region_id=country_2.region_id)

    regions = person.client.get_available_countries()
    assert regions == {country_1.region_id, country_2.region_id}


@pytest.mark.parametrize('with_used', [True, False])
def test_w_force_service(session, client, with_used):
    """c флагом with_used=True учитываются сервисы оплаченных счетов плательщика"""
    service_1 = create_service(session)
    create_pay_policy(
        session, firm_id=1, region_id=225, service_id=service_1.id,
        paymethods_params=[('USD', 1001)]
    )
    service_2 = create_service(session)
    create_pay_policy(
        session, firm_id=1, region_id=225, service_id=service_2.id,
        paymethods_params=[('USD', 1001)]
    )

    country_1 = create_country(session)
    country_2 = create_country(session)
    country_3 = create_country(session)
    firm = create_firm(session, country=country_1)

    invoice = create_paid_invoice(session, order_client=client, service_id=service_1.id)
    non_resident_person_category = create_person_category(session, is_default=1, resident=0,
                                                          country=country_1, ur=0)
    invoice.person.type = non_resident_person_category.category

    create_pay_policy(session, firm_id=firm.id, region_id=country_2.region_id, service_id=service_1.id)
    create_pay_policy(session, firm_id=firm.id, region_id=country_3.region_id, service_id=service_2.id)

    regions = invoice.client.get_available_countries(with_used=with_used)
    if with_used:
        assert regions == {country_2.region_id}
    else:
        assert regions == {country_3.region_id, country_2.region_id}


def test_client_w_non_default_person(session):
    """если у клиента есть плательщик с недефолтной категорией, ему доступны все регионы из платежных настроек,
     в которых явно указана эта категория плательщика, вне зависимости от региона фирмы и признака юр. лица"""
    country_1 = create_country(session)
    service_1 = create_service(session)
    country_2 = create_country(session)
    firm = create_firm(session, country=country_2)
    non_default_person_category = create_person_category(session, is_default=0, resident=0, country=country_1, ur=0)
    person = create_person(session, type=non_default_person_category.category)
    create_pay_policy(
        session, firm_id=firm.id, region_id=country_2.region_id, service_id=service_1.id,
        category=non_default_person_category.category, paymethods_params=[('USD', 1001)], legal_entity=1
    )
    regions = person.client.get_available_countries()
    assert regions == {country_2.region_id}


def test_client_w_person_non_resident_w_category_in_routing(session):
    """если категория плательщика явно указана в платежных настройках, считаем,
    что он может платить из соответствующего региона, даже если это противоречит его статусу резиденства """
    country_1 = create_country(session)
    service_1 = create_service(session)
    firm = create_firm(session, country=country_1)
    person_category = create_person_category(session, is_default=1, resident=0, country=country_1, ur=0)
    person = create_person(session, type=person_category.category)
    create_pay_policy(
        session, firm_id=firm.id, region_id=country_1.region_id, service_id=service_1.id,
        category=person_category.category, paymethods_params=[('USD', 1001)]
    )

    regions = person.client.get_available_countries()
    assert regions == {country_1.region_id}


@pytest.mark.parametrize('with_used', [True, False])
def test_client_w_non_default_person_with_service_filter(session, client, with_used):
    """c флагом with_used=True учитываются сервисы оплаченных счетов плательщика, в т.ч.
     и для недефолтных категорий плательщиков"""
    country_1 = create_country(session)
    country_2 = create_country(session)

    service_1 = create_service(session)
    create_pay_policy(
        session, firm_id=1, region_id=225, service_id=service_1.id,
        paymethods_params=[('USD', 1001)]
    )
    service_2 = create_service(session)
    create_pay_policy(
        session, firm_id=1, region_id=225, service_id=service_2.id,
        paymethods_params=[('USD', 1001)]
    )
    firm = create_firm(session, country=country_2)

    non_default_person_category = create_person_category(session, is_default=0, resident=0, country=country_1, ur=0)
    person = create_person(session, type=non_default_person_category.category)
    invoice = create_paid_invoice(session, order_client=client, service_id=service_1.id)
    invoice.person = person

    create_pay_policy(
        session, firm_id=firm.id, region_id=country_2.region_id, service_id=service_1.id,
        category=non_default_person_category.category, paymethods_params=[('USD', 1001)], legal_entity=1
    )
    create_pay_policy(
        session, firm_id=firm.id, region_id=country_1.region_id, service_id=service_2.id,
        category=non_default_person_category.category, paymethods_params=[('USD', 1001)], legal_entity=1
    )

    regions = person.client.get_available_countries(with_used=with_used)
    if with_used:
        assert regions == {country_2.region_id}
    else:
        assert regions == {country_1.region_id, country_2.region_id}


def test_w_force_client_category(session):
    """платежные политики фильтруются по категории клиента"""
    country_1 = create_country(session)
    country_2 = create_country(session)
    service_1 = create_service(session)
    firm = create_firm(session, country=country_1)
    non_default_person_category = create_person_category(session, is_default=0, resident=0, country=country_1, ur=0)
    person = create_person(session, type=non_default_person_category.category)
    pay_policy_service_id = create_pay_policy(
        session, firm_id=firm.id, region_id=country_1.region_id, service_id=service_1.id,
        category=non_default_person_category.category, is_agency=1
    )
    create_pay_policy(session, pay_policy_service_id=pay_policy_service_id, region_id=country_2.region_id, is_agency=0)

    regions = person.client.get_available_countries()
    assert regions == {country_2.region_id}
