# -*- coding: utf-8 -*-

from tests.balance_tests.pay_policy.pay_policy_common import  (get_pay_policy_manager,
                               create_country,
                               create_service,
                               create_person_category,
                               create_firm,
                               create_pay_policy)


def test_wo_routings(session, pay_policy_manager, service):
    """Если пара регион-сервис не встречаются в платежных настройках, возвращаем пустое множество"""
    country = create_country(session)
    categories = pay_policy_manager.get_categories(service.id, [country.region_id])
    assert categories == set()


def test_base(session, pay_policy_manager, service):
    """подбирает категории плательщиков по региону, признаку юр. лица и признаку резиденства
    из платежных политик, заведенных для переданных сервисов/регионов"""
    countries = [create_country(session) for _ in range(2)]
    firm = create_firm(session, country=countries[0])

    person_categories = []
    for country, person_country, resident in [(countries[0], countries[0], 1),
                                              (countries[1], countries[0], 0)]:
        person_categories.append(
            create_person_category(session, is_default=1, resident=resident, country=person_country, ur=0))
        create_pay_policy(session, region_id=country.region_id, category=None, is_atypical=0,
                       firm_id=firm.id, service_id=service.id, legal_entity=0, is_contract=0)

    categories = pay_policy_manager.get_categories(service.id, [firm.country.region_id])
    assert categories == {person_categories[0]}
    categories = pay_policy_manager.get_categories(service.id, [countries[1].region_id])
    assert categories == {person_categories[1]}
    categories = pay_policy_manager.get_categories(service.id, [country.region_id for country in countries])
    assert categories == set(person_categories)
