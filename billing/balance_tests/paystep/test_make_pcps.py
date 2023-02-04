# -*- coding: utf-8 -*-
import pytest
import datetime

from balance.paystep import PaystepNS, make_pcps
from billing.contract_iface import ContractTypeId
from paystep_common import (create_paysys,
                            create_request,
                            create_client,
                            create_firm,
                            create_person_category,
                            create_currency,
                            create_person,
                            create_contract,
                            BANK,
                            pcps_to_set)

pytestmark = [
    pytest.mark.paystep,
    pytest.mark.usefixtures('switch_new_paystep_flag'),
]

NOW = datetime.datetime.now()


def test_make_pcps_empty_result(session, client):
    request_ = create_request(session, client=client)
    ns = (PaystepNS(request=request_))
    assert make_pcps([], [], None, {}, ns) == []


def test_make_pcps_existing_person_is_creatable(session, client, firm, currency):
    """если категория плательщика из способа оплаты, доступна для создания и плательщик такого типа уже существует,
     возвращаем такой способ оплаты и с сушествующим плательщиком и с временным"""
    request_ = create_request(session, client=client)
    person_category = create_person_category(session, country=firm.country)
    person = create_person(session, client=request_.client, type=person_category.category)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category)
    ns = (PaystepNS(request=request_))
    pcps = make_pcps([paysys], [person_category], None, {person}, ns)
    assert pcps_to_set(session, pcps) == {(None, paysys, person_category, None),
                                          (None, paysys, person_category, person)}


def test_make_pcps_several_paysyses(session, client, firm, currency):
    request_ = create_request(session, client=client)
    person_category = create_person_category(session, country=firm.country)
    person = create_person(session, client=request_.client, type=person_category.category)
    paysys_list = []
    for x in range(0, 2):
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category))
    ns = (PaystepNS(request=request_))
    pcps = make_pcps(paysys_list, [], None, {person}, ns)
    assert pcps_to_set(session, pcps) == {(None, frozenset(paysys_list), person_category, person)}


def test_make_pcps_existing_person_is_non_creatable(session, client, firm, currency):
    """если категория плательщика из способа оплаты  недоступна для создания, но плательщик такого типа уже существует,
     возвращаем такой способ оплаты только с сушествующим плательщиком"""
    request_ = create_request(session, client=client)
    person_category = create_person_category(session, country=firm.country)
    person = create_person(session, client=request_.client, type=person_category.category)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category)
    ns = (PaystepNS(request=request_))
    pcps = make_pcps([paysys], [], None, {person}, ns)
    assert pcps_to_set(session, pcps) == {(None, paysys, person_category, person)}


def test_make_pcps_w_contract(session, client, firm, currency):
    """если категория плательщика из способа оплаты  недоступна для создания, но плательщик такого типа уже существует,
     возвращаем такой способ оплаты только с сушествующим плательщиком"""
    request = create_request(session, client=client)
    person_category = create_person_category(session, country=firm.country)
    person = create_person(session, client=request.client, type=person_category.category)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person, currency=currency.num_code,
                               services=request.request_orders[0].order.service.id,
                               client=request.client, ctype='GENERAL', commission=ContractTypeId.NON_AGENCY,
                               personal_account=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category)
    ns = (PaystepNS(request=request))
    pcps = make_pcps([paysys], [], contract, {person}, ns)
    assert pcps_to_set(session, pcps) == {(contract, paysys, person_category, person)}


@pytest.mark.parametrize('ur', [1, 0])
@pytest.mark.parametrize('auto_createable_persons_only', [True, False, None])
def test_make_pcps_auto_createable_persons_only(session, client, firm, currency, auto_createable_persons_only, ur):
    """если категория плательщика из способа оплаты, доступна для создания, создаем временного плательщика
    с указанной категорией и возвращаетм в сочетании со способом оплаты и договором

    если передан флаг auto_createable_persons_only, и среди доступных категорий плательщика есть физик,
    и существующих физиков нет, создаем такого плательщика и возвращаем в сочетании со способом оплаты

    если передан флаг auto_createable_persons_only, и среди доступных категорий плательщика нет физиков,
    не возвращаем ничего """
    request_ = create_request(session, client=client)
    person_category = create_person_category(session, ur=ur, country=firm.country)

    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category)
    ns = (PaystepNS(request=request_, auto_createable_persons_only=auto_createable_persons_only))
    pcps = make_pcps([paysys], [person_category], None, {}, ns)
    if auto_createable_persons_only:
        if not ur:
            assert pcps_to_set(session, pcps) == {(None, paysys, person_category, pcps[0].person)}
        else:
            assert pcps == []
    else:
        assert pcps_to_set(session, pcps) == {(None, paysys, person_category, None)}


@pytest.mark.parametrize('ur', [1, 0])
def test_make_pcps_auto_createable_persons_only_ph_exists(session, client, firm, currency, ur):
    request_ = create_request(session, client=client)
    person_category = create_person_category(session, ur=ur, country=firm.country)
    person = create_person(session, client=request_.client, type=person_category.category)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category)
    ns = (PaystepNS(request=request_, auto_createable_persons_only=True))
    pcps = make_pcps([paysys], [person_category], None, {person}, ns)
    assert pcps_to_set(session, pcps) == {(None, paysys, person_category, person)}
