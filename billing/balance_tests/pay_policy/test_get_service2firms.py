from tests.balance_tests.pay_policy.pay_policy_common import (get_pay_policy_manager,
                                                              create_service,
                                                              create_country,
                                                              create_pay_policy,
                                                              create_firm)


def test_get_service2firms(session, pay_policy_manager, service, firm):
    create_pay_policy(session, region_id=firm.country.region_id, category=None, is_atypical=0,
                      firm_id=firm.id, service_id=service.id, legal_entity=0, is_contract=0)
    assert pay_policy_manager.get_service2firms([firm.id]) == {service.id: firm.id}


def test_get_service2firms_dict_params(session, pay_policy_manager, firm):
    services = [create_service(session) for _ in range(2)]
    for service in services:
        create_pay_policy(session, region_id=firm.country.region_id, category=None, is_atypical=0,
                          firm_id=firm.id, service_id=service.id, legal_entity=0, is_contract=0)
    assert pay_policy_manager.get_service2firms([firm.id]) == {services[0].id: firm.id, services[1].id: firm.id}
