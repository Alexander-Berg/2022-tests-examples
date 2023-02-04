# -*- coding: utf-8 -*-
import pytest
import datetime

from balance.paystep import get_base_paysyses_routing, PaystepNS
from billing.contract_iface import ContractTypeId
from balance.constants import PaysysGroupIDs, PREPAY_PAYMENT_TYPE
from paystep_common import (
    create_request,
    create_country,
    create_firm,
    create_person_category,
    create_paysys,
    create_pay_policy,
    create_order,
    create_client,
    create_service,
    create_person,
    create_contract,
    CARD,
    BANK,
    YAMONEY
)

pytestmark = [
    pytest.mark.paystep,
    pytest.mark.usefixtures('switch_new_paystep_flag'),
]

NOW = datetime.datetime.now()


def test_no_compatible_paysys(session, client):
    """пустой ответ, если ни один способ оплаты подобрать не удалось"""
    request = create_request(session, client=client)

    result = get_base_paysyses_routing(ns=PaystepNS(request=request), contract=None)
    assert result == []


def test_filter_by_internal(session, firm, client):
    """не возвращаем внутренние способы оплаты при подборе"""
    request = create_request(session, client=client)
    person_category = create_person_category(session, country=firm.country, ur=0)

    paysys_1, paysys_2 = [create_paysys(session, firm=firm, iso_currency='USD', category=person_category.category,
                                        group_id=0, payment_method_id=BANK, extern=1) for _ in range(2)]
    paysys_2.extern = 0

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                      region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                      is_agency=0)

    result = get_base_paysyses_routing(ns=PaystepNS(request=request), contract=None)
    assert set(result) == {paysys_1}


def test_wo_contract_w_atypical_routing(session, firm, client):
    """используем платежные настройки с признаком is_atypical=1"""
    request = create_request(session, client=client)
    request.client.region_id = firm.country.region_id
    person_category = create_person_category(session, country=firm.country, ur=0)

    paysys_1 = create_paysys(session, firm=firm, iso_currency='USD', category=person_category.category,
                             group_id=0, payment_method_id=BANK, extern=1)

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                      region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                      is_agency=0, is_atypical=1)

    result = get_base_paysyses_routing(ns=PaystepNS(request=request), contract=None)
    assert set(result) == {paysys_1}


def test_filter_by_currency(session, firm, client):
    """действительно фильтруем пейсисы по валюте"""
    request = create_request(session, client=client)
    person_category = create_person_category(session, country=firm.country, ur=0)

    paysys_1, paysys_2 = [create_paysys(session, firm=firm, iso_currency='USD', category=person_category.category,
                                        group_id=0, payment_method_id=BANK, extern=1) for _ in range(2)]
    paysys_2.iso_currency = 'EUR'

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                      region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                      is_agency=0)

    result = get_base_paysyses_routing(ns=PaystepNS(request=request), contract=None)
    assert set(result) == {paysys_1}


def test_filter_by_paysys_group(session, firm, client):
    """действительно фильтруем пейсисы по группе способов оплаты"""
    request = create_request(session, client=client)
    person_category = create_person_category(session, country=firm.country, ur=0)

    paysys_1, paysys_2 = [create_paysys(session, firm=firm, iso_currency='USD', category=person_category.category,
                                        group_id=0, payment_method_id=BANK, extern=1) for _ in range(2)]
    paysys_2.group_id = 1

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                      region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                      is_agency=0)

    result = get_base_paysyses_routing(ns=PaystepNS(request=request), contract=None)
    assert set(result) == {paysys_1}


def test_filter_by_payment_method(session, firm, client):
    """действительно фильтруем пейсисы по методу способа оплаты"""
    request = create_request(session, client=client)
    person_category = create_person_category(session, country=firm.country, ur=0)

    paysys_1, paysys_2 = [create_paysys(session, firm=firm, iso_currency='USD', category=person_category.category,
                                        group_id=0, payment_method_id=BANK, extern=1) for _ in range(2)]
    paysys_2.payment_method_id = CARD

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                      region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                      is_agency=0)

    result = get_base_paysyses_routing(ns=PaystepNS(request=request), contract=None)
    assert set(result) == {paysys_1}


def test_filter_by_firm(session, firm, client):
    """действительно фильтруем пейсисы по фирме"""
    request = create_request(session, client=client)
    person_category = create_person_category(session, country=firm.country, ur=0)

    paysys_1, paysys_2 = [create_paysys(session, firm=firm, iso_currency='USD', category=person_category.category,
                                        group_id=0, payment_method_id=BANK, extern=1) for _ in range(2)]
    paysys_2.firm = create_firm(session)

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                      region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                      is_agency=0)

    result = get_base_paysyses_routing(ns=PaystepNS(request=request), contract=None)
    assert set(result) == {paysys_1}


def test_filter_by_legal_entity(session, firm, client):
    """действительно фильтруем пейсисы по плательщикам, учитывая признак юр. лица"""
    request = create_request(session, client=client)
    person_category = create_person_category(session, country=firm.country, ur=0)

    paysys_1, paysys_2 = [create_paysys(session, firm=firm, iso_currency='USD', category=person_category.category,
                                        group_id=PaysysGroupIDs.default, payment_method_id=BANK, extern=1) for _ in
                          range(2)]
    paysys_2.category = create_person_category(session, country=firm.country, ur=1).category

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                      region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                      is_agency=0)

    result = get_base_paysyses_routing(ns=PaystepNS(request=request), contract=None)
    assert set(result) == {paysys_1}


def test_filter_by_resident(session, firm, client):
    """действительно фильтруем пейсисы по плательщикам, учитывая признак резиденства"""
    request = create_request(session, client=client)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)

    paysys_1, paysys_2 = [create_paysys(session, firm=firm, iso_currency='USD', category=person_category.category,
                                        group_id=PaysysGroupIDs.default, payment_method_id=BANK, extern=1) for _ in
                          range(2)]
    paysys_2.category = create_person_category(session, country=firm.country, ur=0, resident=0).category

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK)],
                      region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                      is_agency=0)

    result = get_base_paysyses_routing(ns=PaystepNS(request=request), contract=None)
    assert set(result) == {paysys_1}


def test_filter_by_paysys(session, firm, client, service):
    """можно явно указать способ оплаты"""
    request = create_request(
        session,
        orders=[
            create_order(session, client, service=service),
            create_order(session, client, service=service)
        ])

    person_category = create_person_category(session, country=firm.country, ur=0)
    paysyses = []
    for payment_method_id in [CARD, BANK]:
        paysyses.append(create_paysys(session, firm=firm, iso_currency='USD', category=person_category.category,
                                      group_id=PaysysGroupIDs.default, payment_method_id=payment_method_id, extern=1,
                                      cc='paysys_{}_cc'.format(payment_method_id)))
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[('USD', BANK),
                                                                                ('USD', CARD)],
                      region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                      is_agency=0)

    result = get_base_paysyses_routing(ns=PaystepNS(request=request, paysys=paysyses[0]), contract=None)
    assert set(result) == {paysyses[0]}


def test_multiple_services_intersect(session, firm, client):
    """платежные настройки группируются по  'firm_id', 'category_code', 'payment_method_id', 'iso_currency',
    'paysys_group_id, сервисы в этих группах сравниваются с сервисами из реквеста. Только по тем группам платежных
    настроек, у которых все сервисы из реквеста находятся в группе сервисов из платежных настроек,
     подбираются способы оплаты'"""
    person_category = create_person_category(session, country=firm.country, ur=0)

    service1 = create_service(session)
    service2 = create_service(session)
    request = create_request(
        session,
        orders=[
            create_order(session, client, service=service1),
            create_order(session, client, service=service2)
        ])
    paysyses = []
    for payment_method_id in [BANK, CARD, YAMONEY]:
        paysyses.append(create_paysys(session, firm=firm, iso_currency='USD', category=person_category.category,
                                      group_id=PaysysGroupIDs.default, payment_method_id=payment_method_id, extern=1,
                                      cc='paysys_b'))

    for service, paymethods_params in [(service1, [('USD', BANK), ('USD', CARD)]),
                                       (service2, [('USD', CARD), ('USD', YAMONEY)])]:
        create_pay_policy(
            session,
            region_id=firm.country.region_id, service_id=service.id,
            firm_id=firm.id, legal_entity=0,
            paymethods_params=paymethods_params)

    result = get_base_paysyses_routing(ns=PaystepNS(request=request), contract=None)
    assert set(result) == {paysyses[1]}


def test_w_contract(session, firm, client):
    request = create_request(session, client=client)
    person = create_person(session, client=request.client,
                           type=create_person_category(session, country=firm.country, ur=1).category)
    service = request.request_orders[0].order.service
    paysys = create_paysys(session, firm=firm, iso_currency='USD', category=person.type,
                           group_id=PaysysGroupIDs.default, payment_method_id=BANK, extern=1)
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                               client=request.client, payment_type=PREPAY_PAYMENT_TYPE,
                               services={service.id}, is_signed=NOW, person=person, currency=840)
    create_pay_policy(
        session,
        region_id=firm.country.region_id, service_id=service.id,
        firm_id=firm.id, legal_entity=1,
        paymethods_params=[('USD', BANK)])
    result = get_base_paysyses_routing(ns=PaystepNS(request=request), contract=contract)
    assert set(result) == {paysys}


@pytest.mark.parametrize('group_id', [PaysysGroupIDs.default, PaysysGroupIDs.nr_via_agency])
def test_w_contract_w_subclient_non_resident_paysys_group(session, group_id, firm, client):
    request = create_request(session, client=client)
    request.client.fullname = 'client_fullname'
    request.client.non_resident_currency_payment = 'USD'
    person = create_person(session, client=request.client,
                           type=create_person_category(session, country=firm.country, ur=1).category)
    service = request.request_orders[0].order.service
    paysys = create_paysys(session, firm=firm, currency='USD', category=person.type,
                           group_id=group_id, payment_method_id=BANK, extern=1)
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                               client=request.client, payment_type=PREPAY_PAYMENT_TYPE,
                               services={service.id}, is_signed=NOW, person=person, currency=840)
    create_pay_policy(
        session,
        region_id=firm.country.region_id, service_id=service.id,
        firm_id=firm.id, legal_entity=1,
        paymethods_params=[('USD', BANK)])
    result = get_base_paysyses_routing(ns=PaystepNS(request=request), contract=contract)
    if paysys.group_id == PaysysGroupIDs.default:
        assert set(result) == set()
    else:
        assert set(result) == {paysys}


def test_w_contract_w_subclient_non_resident_any_paymethod(session, firm, client):
    request = create_request(session, client=client)
    request.client.fullname = 'client_fullname'
    request.client.non_resident_currency_payment = 'USD'
    person = create_person(session, client=client,
                           type=create_person_category(session, country=firm.country, ur=1).category)
    service = request.request_orders[0].order.service
    paysys = create_paysys(session, firm=firm, currency='USD', category=person.type,
                           group_id=PaysysGroupIDs.nr_via_agency, payment_method_id=BANK, extern=1)
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                               client=client, payment_type=PREPAY_PAYMENT_TYPE,
                               services={service.id}, is_signed=NOW, person=person, currency=840)
    create_pay_policy(
        session,
        region_id=firm.country.region_id, service_id=service.id,
        firm_id=firm.id, legal_entity=1,
        paymethods_params=[('USD', CARD)])
    result = get_base_paysyses_routing(ns=PaystepNS(request=request), contract=contract)
    assert set(result) == {paysys}
