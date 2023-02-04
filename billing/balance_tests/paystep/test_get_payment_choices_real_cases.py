# -*- coding: utf-8 -*-
import pytest

from balance.paystep import get_payment_choices
from balance import mapper
from tests.balance_tests.paystep.paystep_common import (
    create_client,
    create_order,
    create_request,
    create_product,
    create_person,
    group_paysyses_by_params,
    extract_pcps
)
from tests.object_builder import Getter
from balance.constants import ServiceId

pytestmark = [
    pytest.mark.paystep,
    pytest.mark.usefixtures('switch_new_paystep_flag'),
]


@pytest.mark.parametrize('context, expected_paysyses, expected_pcps',
                         [
                             ({'region_id': 225,
                               'service_id': ServiceId.DIRECT},  # Россия
                              {0: {1: {'ph': {'RUR': ['bank', 'card', 'paypal_wallet',
                                                      'webmoney_wallet', 'yamoney_wallet']},
                                       'ur': {'RUR': ['bank', 'card']}}}},

                              {(None, 'ph', None, (('yamoney_wallet', 'RUR', 1, 'ph', 0),
                                                   ('webmoney_wallet', 'RUR', 1, 'ph', 0),
                                                   ('card', 'RUR', 1, 'ph', 0),
                                                   ('bank', 'RUR', 1, 'ph', 0),
                                                   ('paypal_wallet', 'RUR', 1, 'ph', 0))),
                               (None, 'ur', None, (('bank', 'RUR', 1, 'ur', 0),
                                                   ('card', 'RUR', 1, 'ur', 0)))}),

                             ({'region_id': 20739,
                               'service_id': ServiceId.DIRECT},  # Папуа - Новая Гвинея
                              {0: {7: {'sw_ytph': {'USD': ['bank', 'card', 'paypal_wallet'],
                                                   'CHF': ['bank', 'card', 'paypal_wallet'],
                                                   'EUR': ['bank', 'card', 'paypal_wallet']},
                                       'sw_yt': {'USD': ['bank', 'card', 'paypal_wallet'],
                                                 'CHF': ['bank', 'card', 'paypal_wallet'],
                                                 'EUR': ['bank', 'card', 'paypal_wallet']}}}},

                              {(None, 'sw_yt', None, (('paypal_wallet', 'USD', 7, 'sw_yt', 0),
                                                      ('paypal_wallet', 'EUR', 7, 'sw_yt', 0),
                                                      ('paypal_wallet', 'CHF', 7, 'sw_yt', 0),
                                                      ('bank', 'USD', 7, 'sw_yt', 0),
                                                      ('bank', 'EUR', 7, 'sw_yt', 0),
                                                      ('bank', 'CHF', 7, 'sw_yt', 0),
                                                      ('card', 'USD', 7, 'sw_yt', 0),
                                                      ('card', 'EUR', 7, 'sw_yt', 0),
                                                      ('card', 'CHF', 7, 'sw_yt', 0))),
                               (None, 'sw_ytph', None, (('paypal_wallet', 'USD', 7, 'sw_ytph', 0),
                                                        ('bank', 'USD', 7, 'sw_ytph', 0),
                                                        ('card', 'USD', 7, 'sw_ytph', 0),
                                                        ('paypal_wallet', 'EUR', 7, 'sw_ytph', 0),
                                                        ('bank', 'EUR', 7, 'sw_ytph', 0),
                                                        ('card', 'EUR', 7, 'sw_ytph', 0),
                                                        ('paypal_wallet', 'CHF', 7, 'sw_ytph', 0),
                                                        ('bank', 'CHF', 7, 'sw_ytph', 0),
                                                        ('card', 'CHF', 7, 'sw_ytph', 0)))})

                         ])
def test_get_real_payment_choices_wo_contract(session, context, expected_paysyses, expected_pcps):
    client = create_client(session, region_id=context.get('region_id', None))
    if context.get('person_type'):
        create_person(session, client=client, type=context.get('person_type'))
    order = create_order(session, client=client,
                         service=Getter(mapper.Service, context.get('service_id', ServiceId.DIRECT)),
                         product=create_product(session, create_taxes=True, create_price=True,
                                                reference_price_currency='RUR'))
    request_ = create_request(session, client=client, orders=[order])

    pcp_info = get_payment_choices(request_, skip_trust=True)
    assert group_paysyses_by_params(pcp_info.paysys_list) == expected_paysyses
    assert extract_pcps(pcp_info.pcp_list) == expected_pcps
