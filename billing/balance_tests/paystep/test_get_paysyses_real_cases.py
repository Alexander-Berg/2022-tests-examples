# -*- coding: utf-8 -*-
import pytest
import collections
import datetime

from balance.paystep import (
    get_paysyses,
    PaystepNS,
)
from balance import mapper
from tests.balance_tests.paystep.paystep_common import (
    create_client,
    create_order,
    create_request,
    create_product,
    create_person,
    ddict2dict
)
from tests.object_builder import (
    Getter,
    ContractBuilder
)
from balance.constants import ServiceId, FirmId

pytestmark = [
    pytest.mark.paystep,
    pytest.mark.usefixtures('switch_new_paystep_flag'),
]


def rec_dd():
    return collections.defaultdict(rec_dd)


def extract_ans(paysyses):
    result = rec_dd()
    for ps in paysyses:
        if result[ps.group_id][ps.firm_id][ps.category][ps.currency]:
            result[ps.group_id][ps.firm_id][ps.category][ps.currency].append(ps.payment_method.cc)
        else:
            result[ps.group_id][ps.firm_id][ps.category][ps.currency] = [ps.payment_method.cc]
    return ddict2dict(result)


def create_contract(session, firm, person, currency='RUB', services=None):
    currency_map = {
        'RUB': 810,
        'USD': 840,
        'EUR': 978,
        'BYN': 933
    }
    return ContractBuilder(
        client=person.client,
        person=person,
        commission=0,
        firm=firm.id,
        services=set(services or {7, 11}),
        is_signed=datetime.datetime.now(),
        currency=currency_map.get(currency)
    ).build(session).obj


@pytest.mark.parametrize('context, expected_paysyses',
                         [({'region_id': None,
                            'service_id': ServiceId.DIRECT},
                           {0: {1: {'ph': {'RUR': ['bank', 'card', 'paypal_wallet',
                                                   'webmoney_wallet', 'yamoney_wallet']},
                                    'yt': {'RUR': ['bank'], 'USD': ['bank'], 'EUR': ['bank']},
                                    'ur': {'RUR': ['bank', 'card']}},
                                4: {'usu': {'USD': ['bank', 'paypal_wallet']},
                                    'usp': {'USD': ['bank', 'paypal_wallet']}},
                                7: {'sw_ph': {'USD': ['bank', 'card', 'paypal_wallet'],
                                              'CHF': ['bank', 'card', 'paypal_wallet'],
                                              'EUR': ['bank', 'card', 'paypal_wallet']},
                                    'sw_ytph': {'USD': ['bank', 'card', 'paypal_wallet'],
                                                'CHF': ['bank', 'card', 'paypal_wallet'],
                                                'EUR': ['bank', 'card', 'paypal_wallet']},
                                    'sw_yt': {'USD': ['bank', 'card', 'paypal_wallet'],
                                              'CHF': ['bank', 'card', 'paypal_wallet'],
                                              'EUR': ['bank', 'card', 'paypal_wallet']},
                                    'by_ytph': {'RUR': ['card'], 'UZS': ['card']},
                                    'sw_ur': {'USD': ['bank', 'card', 'paypal_wallet'],
                                              'CHF': ['bank', 'card', 'paypal_wallet'],
                                              'EUR': ['bank', 'card', 'paypal_wallet']}},
                                8: {'trp': {'TRY': ['bank', 'card']},
                                    'tru': {'TRY': ['bank', 'card']}},
                                25: {'kzp': {'KZT': ['bank', 'card', 'yamoney_wallet']},
                                     'kzu': {'KZT': ['bank', 'card']}},
                                27: {'byu': {'BYN': ['bank', 'card']},
                                     'byp': {'BYN': ['bank', 'card', 'yamoney_wallet']}}}}),

                          ({'region_id': 225,
                            'service_id': ServiceId.DIRECT},  # Россия
                           {0: {1: {'ph': {'RUR': ['bank', 'card', 'paypal_wallet',
                                                   'webmoney_wallet', 'yamoney_wallet']},
                                    'ur': {'RUR': ['bank', 'card']}}}}),

                          ({'region_id': 225,
                            'service_id': ServiceId.MEDIA_SELLING},
                           {0: {1: {'ph': {'RUR': ['bank', 'card', 'yamoney_wallet']},
                                    'ur': {'RUR': ['bank', 'card']}}}}),

                          ({'region_id': 169,
                            'service_id': ServiceId.DIRECT},  # Грузия
                           {0: {1: {'yt': {'RUR': ['bank'],
                                           'USD': ['bank'],
                                           'EUR': ['bank']}},
                                7: {'by_ytph': {'RUR': ['card']}}}}),

                          ({'region_id': 20739,
                            'service_id': ServiceId.DIRECT},  # Папуа - Новая Гвинея
                           {0: {7: {'sw_ytph': {'USD': ['bank', 'card', 'paypal_wallet'],
                                                'CHF': ['bank', 'card', 'paypal_wallet'],
                                                'EUR': ['bank', 'card', 'paypal_wallet']},
                                    'sw_yt': {'USD': ['bank', 'card', 'paypal_wallet'],
                                              'CHF': ['bank', 'card', 'paypal_wallet'],
                                              'EUR': ['bank', 'card', 'paypal_wallet']}}}}),

                          ({'person_type': 'ur',
                            'service_id': ServiceId.DIRECT},
                           {0: {1: {'ph': {'RUR': ['bank', 'card', 'paypal_wallet',
                                                   'webmoney_wallet', 'yamoney_wallet']},
                                    'ur': {'RUR': ['bank', 'card']}}}}),

                          ])
def test_get_real_paysyses_wo_contract(session, context, expected_paysyses):
    client = create_client(session, region_id=context.get('region_id', None))
    if context.get('person_type'):
        create_person(session, client=client, type=context.get('person_type'))
    order = create_order(session, client=client,
                         service=Getter(mapper.Service, context.get('service_id', ServiceId.DIRECT)),
                         product=create_product(session, create_taxes=True, create_price=True,
                                                reference_price_currency='RUR'))
    request_ = create_request(session, client=client, orders=[order])

    paysyses = get_paysyses(PaystepNS(request_), None)
    print extract_ans(paysyses)
    assert extract_ans(paysyses) == expected_paysyses


@pytest.mark.parametrize('context, expected_paysyses',
                         [({'person_type': 'ur',
                            'service_id': ServiceId.DIRECT,
                            'firm_id': FirmId.YANDEX_OOO,
                            'currency': 'RUR'},
                           {0: {1: {'ur': {'RUR': ['bank', 'card']}}}}),

                          ({'person_type': 'sw_yt',
                            'service_id': ServiceId.DIRECT,
                            'firm_id': FirmId.YANDEX_EU_AG,
                            'currency': 'EUR'},
                           {0: {7: {'sw_yt': {'EUR': ['bank', 'card', 'paypal_wallet']}}}}),

                          ({'person_type': 'ur',
                            'service_id': ServiceId.MEDIA_SELLING,
                            'firm_id': FirmId.VERTIKALI,
                            'currency': 'RUR'},
                           {0: {12: {'ur': {'RUR': ['bank', 'card']}}}}),

                          ])
def test_get_real_paysyses_w_contract(session, context, expected_paysyses):
    client = create_client(session, region_id=context.get('region_id', None))
    order = create_order(session, client=client,
                         service=Getter(mapper.Service, context['service_id']),
                         product=create_product(session, create_taxes=True, create_price=True,
                                                reference_price_currency='RUR'))
    request_ = create_request(session, client=client, orders=[order])
    person = create_person(session, client=client, type=context['person_type'])
    contract = create_contract(session, Getter(mapper.Firm, context['firm_id']).build(session).obj, person,
                               currency=context['currency'], services=[context['service_id']])

    paysyses = get_paysyses(PaystepNS(request_), contract)
    assert extract_ans(paysyses) == expected_paysyses


def test_direct_ru_ofert_trust(session):
    client = create_client(session, region_id=225)
    order = create_order(session, client=client,
                         service=Getter(mapper.Service, ServiceId.DIRECT),
                         product=create_product(session, create_taxes=True, create_price=True,
                                                reference_price_currency='RUR'))
    request = create_request(session, client=client, orders=[order])
    ns = PaystepNS(request, skip_trust=False)
    paysyses = get_paysyses(ns, None)
    assert extract_ans(paysyses) == {0: {1: {'ph': {'RUR': ['bank', 'card', 'paypal_wallet',
                                                            'webmoney_wallet', 'yamoney_wallet']},
                                             'ur': {'RUR': ['bank', 'card']}}},
                                     1: {1: {'ph': {'RUR': ['card', 'yamoney_wallet']}}}}


def test_direct_subclient_nonresident(session):
    client = create_client(session, is_agency=1)
    subclient = create_client(session, agency=client, is_non_resident=1, currency_payment='EUR',
                              iso_currency_payment='EUR', fullname='Romashka LTD')
    person = create_person(session, client=client, type='ur')
    firm = Getter(mapper.Firm, 1).build(session).obj
    contract = create_contract(session, firm, person, 'EUR')
    order = create_order(session, client=subclient,
                         service=Getter(mapper.Service, ServiceId.DIRECT),
                         product=create_product(session, create_taxes=True, create_price=True,
                                                reference_price_currency='RUR'))
    request = create_request(session, client=client, orders=[order])
    paysyses = get_paysyses(PaystepNS(request), contract)
    assert extract_ans(paysyses) == {3: {1: {'ur': {'EUR': ['bank']}}}}
