# coding: utf-8

import json
import time

from hamcrest import has_entry

import btestlib.reporter as reporter
from btestlib import environments
from btestlib.utils import CheckMode
from simpleapi.common.utils import SimpleRandom
from simpleapi.common.utils import call_http, remove_empty, get_uber_signature

__author__ = 'fellow'

random = SimpleRandom()

cur_url = environments.simpleapi_env().pcidss_api_url + 'card_forwarding/v1/'


def gen_uber_payment_method_id():
    return 'uber_' + str(random.randint(1, 10 ** 5))


def format_payment_method_id_to_card_id(payment_method_id):
    return u'card-x:uber:{}'.format(payment_method_id)


def format_card_id_to_payment_method_id(card_id):
    return card_id.replace(u'card-x:uber:', '')


def get_base_headers(merchant_id, timestamp):
    return remove_empty({
        'X-Merchant-Id': merchant_id,
        'X-Timestamp': timestamp,
    })


class Binding(object):
    @staticmethod
    @CheckMode.result_matches(has_entry('state', 'success'))
    def create(card, user, payment_method_id=gen_uber_payment_method_id(),
               pass_cvn=False, region_name='RU'):
        method_url = cur_url + 'payment-methods'

        params = remove_empty({
            'card_number': card.get('card_number'),
            'cvn': card.get('cvn') if pass_cvn else None,
            'expiration_month': card.get('expiration_month'),
            'expiration_year': card.get('expiration_year'),
            'payment_method_id': payment_method_id,
            'region_name': region_name,
            'user_id': user.uber_user_id
        })
        headers = get_base_headers(merchant_id='uber', timestamp=int(time.time()))

        signature = get_uber_signature(params=params, headers=headers)
        headers.update({'X-Signature': signature})

        with reporter.step(u'Создаем внешнюю привязку (для убера)'):
            return call_http(method_url, json.dumps(params), headers=headers)

    @staticmethod
    @CheckMode.result_matches(has_entry('state', 'success'))
    def update(user, payment_method_id, expiration_month, expiration_year, region_name='RU'):
        method_url = cur_url + 'payment-methods/{}'.format(payment_method_id)

        params = remove_empty({
            'expiration_month': expiration_month,
            'expiration_year': expiration_year,
            'region_name': region_name,
            'user_id': user.uber_user_id
        })
        headers = get_base_headers(merchant_id='uber', timestamp=int(time.time()))

        signature = get_uber_signature(params=params, headers=headers)
        headers.update({'X-Signature': signature})

        with reporter.step(u'Изменяем параметры привязанной карты (для убера)'):
            return call_http(method_url, json.dumps(params), headers=headers, method='PUT')

    @staticmethod
    @CheckMode.result_matches(has_entry('state', 'success'))
    def unbind(payment_method_id, user):
        method_url = cur_url + 'payment-methods/{}/unbind'.format(payment_method_id)

        params = remove_empty({
            'user_id': user.uber_user_id
        })
        headers = get_base_headers(merchant_id='uber', timestamp=int(time.time()))

        signature = get_uber_signature(params=params, headers=headers)
        headers.update({'X-Signature': signature})

        with reporter.step(u'Отвязываем привязанную карту (для убера)'):
            return call_http(method_url, json.dumps(params), headers=headers)

    @staticmethod
    def unbind_all_cards_of(user, service, bounded_card_num=3):
        with reporter.step(
                u'Если у пользователя {} более {} привязанных карт, отвяжем их'.format(user, bounded_card_num)):
            from simpleapi.steps import simple_steps as simple

            bounded_cards_ids = [key for key in simple.list_payment_methods(service, user)[1].keys() if
                                 'card-x' in key]

            if len(bounded_cards_ids) > bounded_card_num:
                for card_id in bounded_cards_ids:
                    Binding.unbind(user=user, payment_method_id=format_card_id_to_payment_method_id(card_id))
