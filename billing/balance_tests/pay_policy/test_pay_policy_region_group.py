# -*- coding: utf-8 -*-

from tests.balance_tests.pay_policy.pay_policy_common import (
    FIELDS,
    CLIENT_CATEGORY,
    WITHOUT_CONTRACT,
    PAYSYS_GROUP_ID,
    BANK,
    get_pay_policy_manager,
    create_service,
    create_country,
    create_firm,
    create_person_category,
    create_pay_policy,
    create_pay_policy_region_group,
    _extract_ans
)


def test_region_group_and_region(session, pay_policy_manager, service, country):
    """Если в платежных политиках пересекаются группа регионов и регион"""
    resident_person_category = create_person_category(session, country=country, ur=0, resident=1)
    non_resident_person_category = create_person_category(session, country=country, ur=0, resident=0)
    firm = create_firm(session, country=country)
    country_2 = create_country(session)
    pay_policy_group_id = create_pay_policy_region_group(session, regions=[country.region_id, country_2.region_id])

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                      region_group_id=pay_policy_group_id, service_id=service.id, is_agency=0)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('RUB', BANK)],
                      region_id=country.region_id, service_id=service.id, is_agency=0)

    rows = pay_policy_manager.get_rows(fields=FIELDS, service_id=service.id)
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   resident_person_category, BANK, 'USD', PAYSYS_GROUP_ID),
                                  (service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   resident_person_category, BANK, 'RUB', PAYSYS_GROUP_ID),
                                  (service.id, country_2.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   non_resident_person_category, BANK, 'USD', PAYSYS_GROUP_ID)}


def test_region_groups(session, pay_policy_manager, service, country):
    """Если в платежных политиках пересекаются группы регионов"""
    resident_person_category = create_person_category(session, country=country, ur=0, resident=1)
    non_resident_person_category = create_person_category(session, country=country, ur=0, resident=0)
    firm = create_firm(session, country=country)
    country_2 = create_country(session)
    country_3 = create_country(session)
    pay_policy_group_id_1 = create_pay_policy_region_group(session, regions=[country.region_id, country_2.region_id])
    pay_policy_group_id_2 = create_pay_policy_region_group(session, regions=[country.region_id, country_3.region_id])

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                      region_group_id=pay_policy_group_id_1, service_id=service.id, is_agency=0)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('RUB', BANK)],
                      region_group_id=pay_policy_group_id_2, service_id=service.id, is_agency=0)

    rows = pay_policy_manager.get_rows(fields=FIELDS, service_id=service.id)
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   resident_person_category, BANK, 'USD', PAYSYS_GROUP_ID),
                                  (service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   resident_person_category, BANK, 'RUB', PAYSYS_GROUP_ID),
                                  (service.id, country_2.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   non_resident_person_category, BANK, 'USD', PAYSYS_GROUP_ID),
                                  (service.id, country_3.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   non_resident_person_category, BANK, 'RUB', PAYSYS_GROUP_ID)}


def test_region_group_and_region_in_one(session, pay_policy_manager, service, country):
    """Если в одной платежной политике пересекаются группа регионов и регион"""
    resident_person_category = create_person_category(session, country=country, ur=0, resident=1)
    non_resident_person_category = create_person_category(session, country=country, ur=0, resident=0)
    firm = create_firm(session, country=country)
    country_2 = create_country(session)
    pay_policy_group_id = create_pay_policy_region_group(session, regions=[country.region_id, country_2.region_id])

    pay_policy_service_id = create_pay_policy(session, firm_id=firm.id, legal_entity=0,
                                              paymethods_params=[('USD', BANK), ('RUB', BANK)],
                                              region_group_id=pay_policy_group_id, service_id=service.id, is_agency=0)
    create_pay_policy(session, pay_policy_service_id=pay_policy_service_id, region_id=country.region_id, is_agency=0)
    rows = pay_policy_manager.get_rows(fields=FIELDS, service_id=service.id)
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   resident_person_category, BANK, 'USD', PAYSYS_GROUP_ID),
                                  (service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   resident_person_category, BANK, 'RUB', PAYSYS_GROUP_ID),
                                  (service.id, country_2.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   non_resident_person_category, BANK, 'USD', PAYSYS_GROUP_ID),
                                  (service.id, country_2.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   non_resident_person_category, BANK, 'RUB', PAYSYS_GROUP_ID)}


def test_region_groups_in_one(session, pay_policy_manager, service, country):
    """Если в одной платежной политике пересекаются группы регионов"""
    resident_person_category = create_person_category(session, country=country, ur=0, resident=1)
    non_resident_person_category = create_person_category(session, country=country, ur=0, resident=0)
    firm = create_firm(session, country=country)
    country_2 = create_country(session)
    country_3 = create_country(session)
    pay_policy_group_id_1 = create_pay_policy_region_group(session, regions=[country.region_id, country_2.region_id])
    pay_policy_group_id_2 = create_pay_policy_region_group(session, regions=[country.region_id, country_3.region_id])

    pay_policy_service_id = create_pay_policy(session, firm_id=firm.id, legal_entity=0,
                                              paymethods_params=[('USD', BANK), ('RUB', BANK)],
                                              region_group_id=pay_policy_group_id_1, service_id=service.id, is_agency=0)
    create_pay_policy(session, pay_policy_service_id=pay_policy_service_id, region_group_id=pay_policy_group_id_2,
                      is_agency=0)

    rows = pay_policy_manager.get_rows(fields=FIELDS, service_id=service.id)
    assert _extract_ans(rows) == {(service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   resident_person_category, BANK, 'USD', PAYSYS_GROUP_ID),
                                  (service.id, country.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   resident_person_category, BANK, 'RUB', PAYSYS_GROUP_ID),
                                  (service.id, country_2.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   non_resident_person_category, BANK, 'USD', PAYSYS_GROUP_ID),
                                  (service.id, country_2.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   non_resident_person_category, BANK, 'RUB', PAYSYS_GROUP_ID),
                                  (service.id, country_3.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   non_resident_person_category, BANK, 'USD', PAYSYS_GROUP_ID),
                                  (service.id, country_3.region_id, CLIENT_CATEGORY, WITHOUT_CONTRACT, firm,
                                   non_resident_person_category, BANK, 'RUB', PAYSYS_GROUP_ID)}
