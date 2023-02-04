# -*- coding: utf-8 -*-

import pytest
import random

from balance.paystep import get_paysys_sort_keyfunc, PaystepNS
from tests import object_builder as ob
from balance.constants import *
from balance.mapper import Service

from tests.balance_tests.paystep.paystep_common import (BANK, CARD, YAMONEY, WEBMONEY, QIWI, PAYPAL,
                            create_paysys, create_person_category,
                            create_firm, create_request)

pytestmark = [
    pytest.mark.paystep,
    pytest.mark.usefixtures('switch_new_paystep_flag'),
]


def test_paysys_list_sort_by_default(session, firm):
    """для всех, кроме  юр лиц из Взгляда и Коннекта и физ.лиц РФ сортируем методы оплаты в таком порядке
    YM, WEBMONEY, PAYPAL, QIWI, BANK, CARD
    """
    request = create_request(session)
    person_category = create_person_category(session, country=firm.country, ur=1)
    paysys_list = []
    for payment_method in [BANK, CARD, YAMONEY, WEBMONEY, QIWI, PAYPAL]:
        paysys_list.append(create_paysys(session, firm=firm, currency='USD', category=person_category.category,
                                         payment_method_id=payment_method))
    ns = PaystepNS(request=request)
    random.shuffle(paysys_list)
    paysys_list.sort(key=get_paysys_sort_keyfunc(ns))
    assert [paysys.payment_method_id for paysys in paysys_list] == [YAMONEY, WEBMONEY, PAYPAL, QIWI, BANK, CARD]


def test_paysys_list_sort_ph_by_default(session, firm):
    """для всех физ.лиц РФ (кроме Взгляда) сортируем методы оплаты в таком порядке
    YM, WEBMONEY, QIWI, CARD, BANK, PAYPAL
    """
    request = create_request(session)
    paysys_list = []
    for payment_method in [BANK, CARD, YAMONEY, WEBMONEY, QIWI, PAYPAL]:
        paysys_list.append(create_paysys(session, firm=firm, currency='USD', category='ph',
                                         payment_method_id=payment_method))
    ns = PaystepNS(request=request)
    random.shuffle(paysys_list)
    paysys_list.sort(key=get_paysys_sort_keyfunc(ns))
    assert [paysys.payment_method_id for paysys in paysys_list] == [YAMONEY, WEBMONEY, QIWI, CARD, BANK, PAYPAL]


def test_paysys_list_sort_ph_surveys(session, firm):
    """для всех физ.лиц РФ Взгляда сортируем методы оплаты в таком порядке
    YM, WEBMONEY, QIWI, BANK, CARD, PAYPAL
    """
    request = create_request(session)
    paysys_list = []
    for payment_method in [BANK, CARD, YAMONEY, WEBMONEY, QIWI, PAYPAL]:
        paysys_list.append(create_paysys(session, firm=firm, currency='USD', category='ph',
                                         payment_method_id=payment_method))
    request.request_orders[0].order.service = ob.Getter(Service, ServiceId.SURVEYS).build(session).obj
    ns = PaystepNS(request=request)
    random.shuffle(paysys_list)
    paysys_list.sort(key=get_paysys_sort_keyfunc(ns))
    assert [paysys.payment_method_id for paysys in paysys_list] == [YAMONEY, WEBMONEY, QIWI, BANK, CARD, PAYPAL]


@pytest.mark.parametrize('service_id', [ServiceId.SURVEYS])
def test_paysys_list_sort_ur_surveys_connect(session, firm, service_id):
    """для всех юр.лиц РФ Взгляда сортируем методы оплаты в таком порядке
    YM, WEBMONEY, PAYPAL, QIWI, CARD, BANK
    """
    request = create_request(session)
    paysys_list = []
    for payment_method in [BANK, CARD, YAMONEY, WEBMONEY, QIWI, PAYPAL]:
        paysys_list.append(create_paysys(session, firm=firm, currency='USD', category='ur',
                                         payment_method_id=payment_method))
    request.request_orders[0].order.service = ob.Getter(Service, service_id).build(session).obj
    ns = PaystepNS(request=request)
    random.shuffle(paysys_list)
    paysys_list.sort(key=get_paysys_sort_keyfunc(ns))
    assert [paysys.payment_method_id for paysys in paysys_list] == [YAMONEY, WEBMONEY, PAYPAL, QIWI, CARD, BANK]


def test_paysys_list_sort_by_legal_entity(session, firm):
    """физики первее, чем юрики"""
    request = create_request(session)
    paysys_list = []
    for legal_entity in [1, 0]:
        person_category = create_person_category(session, country=firm.country, ur=legal_entity)
        paysys_list.append(create_paysys(session, firm=firm, currency='USD', category=person_category.category,
                                         payment_method_id=BANK))
    ns = PaystepNS(request=request)
    random.shuffle(paysys_list)
    paysys_list.sort(key=get_paysys_sort_keyfunc(ns))
    assert [paysys.person_category.ur for paysys in paysys_list] == [0, 1]


def test_paysys_list_sort_by_currency(session, firm):
    """валюты тоже имеют вес"""
    request = create_request(session)
    paysys_list = []
    for currency in ['UZS', 'RUR', 'UAH', 'USD', 'EUR', 'CHF', 'CAD', 'TRY', 'GEL', 'BYN', 'KZT', 'AZN', 'AMD', 'KGS',
                     'MDL']:
        paysys_list.append(create_paysys(session, firm=firm, currency=currency, category='ph', payment_method_id=BANK))
    ns = PaystepNS(request=request)
    random.shuffle(paysys_list)
    paysys_list.sort(key=get_paysys_sort_keyfunc(ns))
    print [paysys.currency for paysys in paysys_list]
    assert [paysys.currency for paysys in paysys_list] == ['RUR', 'UAH', 'USD', 'EUR', 'CHF',
                                                           'CAD', 'TRY', 'GEL', 'BYN', 'KZT',
                                                           'AZN', 'AMD', 'KGS', 'MDL', 'UZS']


@pytest.mark.parametrize('category, expected_result', [('ph', [('RUR', CARD),
                                                               ('RUR', BANK),
                                                               ('EUR', CARD)]),

                                                       ('ur', [('RUR', BANK),
                                                               ('RUR', CARD),
                                                               ('EUR', CARD)])])
def test_paysys_list_sort_by_currency_and_weight(session, firm, category, expected_result):
    """для физлиц сортируем способы оплаты по ключу вес валюты, вес метода оплаты
    для юр лиц - вес метода оплаты, вес валюты
    """
    request = create_request(session)
    paysys_list = []
    for currency, payment_method in [('RUR', BANK),
                                     ('RUR', CARD),
                                     ('EUR', CARD)]:
        paysys_list.append(create_paysys(session, firm=firm, currency=currency, category=category,
                                         payment_method_id=payment_method))
    ns = PaystepNS(request=request)
    random.shuffle(paysys_list)
    paysys_list.sort(key=get_paysys_sort_keyfunc(ns))
    print [(paysys.currency, paysys.payment_method_id) for paysys in paysys_list]
    assert [(paysys.currency, paysys.payment_method_id) for paysys in paysys_list] == expected_result
