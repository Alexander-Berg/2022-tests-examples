import pytest

from tests.balance_tests.request.request_common import (
    create_person_category, create_person, create_country, create_request, create_order,
    create_service,
    create_pay_policy, create_firm, create_currency, BANK)


def test_fail(session, service, country):
    person = create_person(session, type=create_person_category(session, country=country, ur=0,
                                                                resident=1).category)
    request = create_request(session, client=person.client,
                             orders=[create_order(session, client=person.client, service=service)])
    session.config.__dict__['DIRECT_PAYMENT_SERVICE_REGIONS'] = [[service.id, person.client.region_id]]
    assert request.direct_payment is False


def test_ok(session, service, firm, currency):
    person = create_person(session, type=create_person_category(session, country=firm.country, ur=0,
                                                                resident=1).category)
    request = create_request(session, client=person.client,
                             orders=[create_order(session, client=person.client, service=service)])
    session.config.__dict__['DIRECT_PAYMENT_SERVICE_REGIONS'] = [[service.id, firm.country.region_id]]

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                      region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                      is_agency=0, paysys_group_id=0)
    assert request.direct_payment is True


def test_another_country(session, service, firm, currency):
    person = create_person(session, type=create_person_category(session, country=firm.country, ur=0,
                                                                resident=1).category)
    request = create_request(session, client=person.client,
                             orders=[create_order(session, client=person.client, service=service)])
    session.config.__dict__['DIRECT_PAYMENT_SERVICE_REGIONS'] = [[service.id, create_country(session).region_id]]

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                      region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                      is_agency=0, paysys_group_id=0)
    assert request.direct_payment is False


def test_another_service(session, service, firm, currency):
    person = create_person(session, type=create_person_category(session, country=firm.country, ur=0,
                                                                resident=1).category)
    request = create_request(session, client=person.client,
                             orders=[create_order(session, client=person.client, service=service)])
    session.config.__dict__['DIRECT_PAYMENT_SERVICE_REGIONS'] = [[create_service(session).id, firm.country.region_id]]

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                      region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                      is_agency=0, paysys_group_id=0)
    assert request.direct_payment is False
