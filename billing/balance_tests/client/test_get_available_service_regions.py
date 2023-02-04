# -*- coding: utf-8 -*-

import pytest

from tests.balance_tests.client.client_common import (create_client,
                           create_country,
                           create_service,
                           create_firm,
                           create_person_category,
                           create_person,
                           create_pay_policy,
                           BANK)


def to_tuples(ans):
    return {(service_id, region_id) for service_id, regions in ans.iteritems() for region_id in regions}


def test_client_w_region_with_self(session, firm, country):
    client = create_client(session, region_id=country.region_id)

    services = [create_service(session) for _ in range(2)]
    for service in services:
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                          region_id=client.region_id, service_id=service.id, is_agency=0, is_atypical=0,
                          is_contract=0)
    assert client.get_available_service_regions(with_self=True) == {services[0].id: [country.region_id],
                                                                    services[1].id: [country.region_id]}


def test_client_w_region_wo_self(session, firm, country):
    client = create_client(session, region_id=country.region_id)

    services = [create_service(session) for _ in range(2)]
    for service in services:
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                          region_id=client.region_id, service_id=service.id, is_agency=0, is_atypical=0,
                          is_contract=0)
    assert {(services[0].id, country.region_id),
            (services[1].id, country.region_id)}.issubset(to_tuples(client.get_available_service_regions()))


def test_client_w_person_resident(session, country):
    client = create_client(session)
    firm = create_firm(session, country=country)
    person = create_person(session, type=create_person_category(session, country=country, ur=0, resident=1,
                                                                is_default=1).category, client=client)

    services = [create_service(session) for _ in range(2)]
    for service in services:
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                          region_id=country.region_id, service_id=service.id, is_agency=0, is_atypical=0,
                          is_contract=0)
    assert client.get_available_service_regions() == {services[0].id: [country.region_id],
                                                      services[1].id: [country.region_id]}


def test_client_w_person_non_resident(session):
    countries = [create_country(session) for _ in range(2)]
    services = [create_service(session) for _ in range(2)]
    client = create_client(session)
    firm = create_firm(session, country=countries[0])
    person = create_person(session, type=create_person_category(session, country=countries[0], ur=0, resident=0,
                                                                is_default=1).category, client=client)

    for service, country in zip(services, countries):
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                          region_id=country.region_id, service_id=service.id, is_agency=0, is_atypical=0,
                          is_contract=0)
    assert client.get_available_service_regions() == {services[1].id: [countries[1].region_id]}


def test_client_w_person_non_resident_w_service_filter(session):
    countries = [create_country(session) for _ in range(2)]
    client = create_client(session)
    firm = create_firm(session, country=countries[0])
    person = create_person(session, client=client,
                           type=create_person_category(session, country=countries[0], ur=0, resident=0,
                                                       is_default=1).category)

    services = [create_service(session) for _ in range(2)]
    for service in services:
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                          region_id=countries[1].region_id, service_id=service.id, is_agency=0, is_atypical=0,
                          is_contract=0)
    assert client.get_available_service_regions(services=services[1].id) == {services[1].id: [countries[1].region_id]}
    assert client.get_available_service_regions() == {services[0].id: [countries[1].region_id],
                                                      services[1].id: [countries[1].region_id]}
