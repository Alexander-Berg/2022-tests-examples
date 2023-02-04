from copy import deepcopy

import pytest

from maps.b2bgeo.libs.py_flask_utils import i18n
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import MOCK_APIKEYS_CONTEXT
from ya_courier_backend.config.common import KNOWN_LOCALES, IGNORED_LOCALES
from ya_courier_backend.models import UserRole
from ya_courier_backend.interservice.apikeys.yandex import account_info_from_link_info, APIKEYS_SERVICE_TOKEN

EXPECTED = {
    'balance': None,
    'ban_info': {
        'id': 122,
        'reason': 'оплаченный период закончился',
        'ban_date': '2021-03-17T03:00:00.589000+00:00',
    },
    'banned': True,
    'tariff': {
        'id': 'ordersdistribution_9000_locations_vnm_3months_2021',
        'limits': [
            {
                'counter': 0,
                'id': 'ordersdistribution_tasks_small_low_solved_daily',
                'last_counter_reset': '2021-03-22T21:00:00+00:00',
                'limit': 900,
                'name': 'Задачи размера small, качество решения low',
                'next_counter_reset': '2021-03-23T21:00:00+00:00'
            }
        ],
        'name': 'Планирование и Мониторинг до 9 000 заказов на 3 месяца, '
            'неограниченное количество ТС',
        'description': 'Планирование и Мониторинг маршрутов для 9 000 '
            'заказов на срок 3 месяца без ограничения на '
            'количество транспортных средств (ТС). При '
            'исчерпании пакета 5.9 рублей за 1 заказ. '
            'Подключается по оферте.',
        'activation_date': None,
        'expiration_date': None,
        'next_debit': None,
    }
}


@pytest.fixture(scope='session')
def i18n_keysets():
    i18n.Keysets.init(KNOWN_LOCALES, IGNORED_LOCALES)


def test_link_info_no_state(i18n_keysets):
    # no tarifficator_state
    link_info = MOCK_APIKEYS_CONTEXT[APIKEYS_SERVICE_TOKEN]['project_link_info_response']['data']
    assert account_info_from_link_info(link_info, 'ru', UserRole.admin) == EXPECTED

    link_info = deepcopy(MOCK_APIKEYS_CONTEXT[APIKEYS_SERVICE_TOKEN]['project_link_info_response']['data'])
    link_info['tarifficator_state'] = None
    assert account_info_from_link_info(link_info, 'ru', UserRole.admin) == EXPECTED

    link_info['banned'] = False
    link_info['ban_reason_id'] = link_info['ban_reason'] = None
    expected = deepcopy(EXPECTED)
    expected['ban_info'] = None
    expected['banned'] = False
    assert account_info_from_link_info(link_info, 'ru', UserRole.admin) == expected


def test_link_info_with_state(i18n_keysets):
    link_info = deepcopy(MOCK_APIKEYS_CONTEXT[APIKEYS_SERVICE_TOKEN]['project_link_info_response']['data'])
    link_info['tarifficator_state'] = MOCK_APIKEYS_CONTEXT[APIKEYS_SERVICE_TOKEN]['INCLUDES']['tarifficator_state']

    expected = deepcopy(EXPECTED)
    expected['balance'] = {'currency': 'RUB', 'value': -44612.5}
    expected['tariff']['next_debit'] = {'amount': {'currency': 'RUB', 'value': 93000.0},
                                        'date': '2020-02-26T20:01:08.388000+00:00'}
    expected['tariff']['activation_date'] = '2021-02-17T04:41:41.784000+00:00'
    assert account_info_from_link_info(link_info, 'ru', UserRole.admin) == expected


def test_link_info_with_stats(i18n_keysets):
    link_info = deepcopy(MOCK_APIKEYS_CONTEXT[APIKEYS_SERVICE_TOKEN]['project_link_info_response']['data'])
    link_info['tarifficator_state'] = deepcopy(MOCK_APIKEYS_CONTEXT[APIKEYS_SERVICE_TOKEN]['INCLUDES']['tarifficator_state'])
    link_info['limit_stats'] = MOCK_APIKEYS_CONTEXT[APIKEYS_SERVICE_TOKEN]['INCLUDES']['limit_stats']

    expected = deepcopy(EXPECTED)
    expected['balance'] = {'currency': 'RUB', 'value': -44612.5}
    expected['tariff']['next_debit'] = {'amount': {'currency': 'RUB', 'value': 93000.0},
                                        'date': '2020-02-26T20:01:08.388000+00:00'}
    expected['tariff']['limits'][0]['counter'] = 3
    expected['tariff']['activation_date'] = '2021-02-17T04:41:41.784000+00:00'
    assert account_info_from_link_info(link_info, 'ru', UserRole.admin) == expected

    # another currency
    link_info['tarifficator_state']['state']['personal_account']['currency'] = 'UE'
    expected['balance'] = {'currency': 'UE', 'value': -44612.5}
    expected['tariff']['next_debit'] = {'amount': {'currency': 'UE', 'value': 93000.0},
                                        'date': '2020-02-26T20:01:08.388000+00:00'}
    assert account_info_from_link_info(link_info, 'ru', UserRole.admin) == expected

    # bad next debit
    link_info['tarifficator_state']['state']['products']['510973']['next_consume_value'] = 'qqq'
    expected['tariff']['next_debit'] = None
    assert account_info_from_link_info(link_info, 'ru', UserRole.admin) == expected


def test_link_info_no_balance(i18n_keysets):
    link_info = MOCK_APIKEYS_CONTEXT[APIKEYS_SERVICE_TOKEN]['project_link_info_response']['data']
    link_info['tarifficator_state'] = deepcopy(MOCK_APIKEYS_CONTEXT[APIKEYS_SERVICE_TOKEN]['INCLUDES']['tarifficator_state'])
    link_info['tarifficator_state']['state']['personal_account']['receipt_sum'] = 'qqq'

    expected = deepcopy(EXPECTED)
    expected['tariff']['next_debit'] = {'amount': {'currency': 'RUB', 'value': 93000.0},
                                        'date': '2020-02-26T20:01:08.388000+00:00'}
    expected['tariff']['activation_date'] = '2021-02-17T04:41:41.784000+00:00'
    assert account_info_from_link_info(link_info, 'ru', UserRole.admin) == expected

    del link_info['tarifficator_state']['state']['personal_account']
    assert account_info_from_link_info(link_info, 'ru', UserRole.admin) == expected


def test_link_info_trial(i18n_keysets):
    link_info = deepcopy(MOCK_APIKEYS_CONTEXT[APIKEYS_SERVICE_TOKEN]['project_link_info_response']['data'])
    link_info['tarifficator_state'] = deepcopy(MOCK_APIKEYS_CONTEXT[APIKEYS_SERVICE_TOKEN]['INCLUDES']['tarifficator_state'])
    link_info['limit_stats'] = MOCK_APIKEYS_CONTEXT[APIKEYS_SERVICE_TOKEN]['INCLUDES']['limit_stats']
    link_info['tariff'] = 'ordersdistribution_trial'

    expected = deepcopy(EXPECTED)
    expected['balance'] = {'currency': 'RUB', 'value': -44612.5}
    expected['tariff']['activation_date'] = '2021-02-17T04:41:41.784000+00:00'
    expected['tariff']['expiration_date'] = '2021-03-03T04:41:41.784000+00:00'
    expected['tariff']['next_debit'] = {'amount': {'currency': 'RUB', 'value': 93000.0},
                                        'date': '2020-02-26T20:01:08.388000+00:00'}
    expected['tariff']['limits'][0]['counter'] = 3
    expected['tariff']['name'] = 'Пробный'
    expected['tariff']['description'] = 'бесплатно, 14 дней'
    expected['tariff']['id'] = 'ordersdistribution_trial'
    assert account_info_from_link_info(link_info, 'ru', UserRole.admin) == expected

    # bad date
    link_info['tarifficator_state']['state']['activated_date'] = 'qqq'
    expected['tariff']['activation_date'] = None
    expected['tariff']['expiration_date'] = None
    assert account_info_from_link_info(link_info, 'ru', UserRole.admin) == expected


def test_link_info_no_debit(i18n_keysets):
    link_info = deepcopy(MOCK_APIKEYS_CONTEXT[APIKEYS_SERVICE_TOKEN]['project_link_info_response']['data'])
    link_info['tarifficator_state'] = deepcopy(MOCK_APIKEYS_CONTEXT[APIKEYS_SERVICE_TOKEN]['INCLUDES']['tarifficator_state'])
    del link_info['tarifficator_state']['state']['products']['510973']['next_consume_date']
    del link_info['tarifficator_state']['state']['products']['510973']['next_consume_value']

    expected = deepcopy(EXPECTED)
    expected['balance'] = {'currency': 'RUB', 'value': -44612.5}
    expected['tariff']['activation_date'] = '2021-02-17T04:41:41.784000+00:00'
    assert account_info_from_link_info(link_info, 'ru', UserRole.admin) == expected
