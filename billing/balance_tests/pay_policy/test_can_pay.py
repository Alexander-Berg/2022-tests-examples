from tests.balance_tests.pay_policy.pay_policy_common import (get_pay_policy_manager,
                                                              create_service,
                                                              create_country,
                                                              create_pay_policy,
                                                              create_firm)


def test_can_pay(session, pay_policy_manager, service, firm):
    create_pay_policy(session, region_id=firm.country.region_id, category=None, is_atypical=0,
                      firm_id=firm.id, service_id=service.id, legal_entity=0, is_contract=0)
    assert pay_policy_manager.can_pay(service.id, firm.country.region_id) is True


def test_can_not_pay(session, pay_policy_manager, service, country):
    assert pay_policy_manager.can_pay(service.id, country.region_id) is False
