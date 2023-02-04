# coding: utf-8
import json

import btestlib.reporter as reporter
from btestlib import environments
from btestlib import matchers
from btestlib import passport_steps
from btestlib.constants import Services
from simpleapi.common.utils import remove_empty, call_http
from hamcrest import has_entry
from btestlib.utils import CheckMode
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card
from simpleapi.steps import mongo_steps
from simpleapi.steps import trust_steps as trust

__author__ = 'sunshineguy'

'''Шаги описанны в тикете: https://st.yandex-team.ru/TESTTRUST-20'''


def is_valid_resp_status(resp):
    return resp['success'] or resp['error']['message'] == 'Profile not found'


class KinopoiskPlus(object):
    @property
    def service(self):
        return Services.KINOPOISK_PLUS

    @property
    def base_url(self):
        return environments.simpleapi_env_ora().kinopoisk_plus_api_url

    @staticmethod
    def get_base_headers(accept=False, content_type=False, session_key=None, session_id=None):
        return remove_empty({
            'Accept': 'application/json' if accept else None,
            'Content-Type': 'application/json' if content_type else None,
            'Session-Key': session_key,
            'X-Session-Id': session_id,
        })

    def __init__(self, passport_user, need_full_init=True, **kwargs):
        self.full_init = need_full_init
        self.passport_user = passport_user
        self.kp_user = passport_user.kinopoisk_linked_user
        self.session_key = kwargs.get("session_key") or self.login()['data']['sessionKey']
        self.task_id = kwargs.get('task_id') or passport_steps.process_getting_broker_task_id(passport_user)
        self.linked_users_status = self.get_link_status()['success']
        if self.full_init:
            self.full_preparation()

    def __del__(self):
        if self.full_init:
            self.full_preparation()

    def full_preparation(self):
        self.process_unlinking_users()
        trust.process_unbinding(self.passport_user, service=self.service)
        mongo_steps.Bindings.delete_all_cards(self.kp_user)

    @CheckMode.result_matches(has_entry('success', True))
    def login(self):
        method_url = self.base_url + 'v2/authentication/passport'
        params = {
            'login': self.kp_user.login,
            'password': self.kp_user.password,
        }
        with reporter.step(u'Входим пользователем {} в Кинопоиск'.format(self.kp_user)):
            return call_http(method_url, json.dumps(params), headers=self.get_base_headers(True, True))

    @CheckMode.result_matches(matchers.matcher_for(is_valid_resp_status))
    def link_account(self):
        method_url = self.base_url + 'v1/users/me/social'
        params = {
            'taskId': self.task_id
        }
        with reporter.step(u'Связываем аккаунты Кинопоиска {} и Паспорта {}'.format(self.kp_user, self.passport_user)):
            return call_http(method_url, json.dumps(params),
                             headers=self.get_base_headers(True, True, session_key=self.session_key))

    @CheckMode.result_matches(matchers.matcher_for(is_valid_resp_status))
    def get_link_status(self):
        method_url = self.base_url + 'v1/users/me/social/yandex'
        with reporter.step(u'Смотрим связан ли аккаунт Кинопоиска {}'.format(self.kp_user)):
            return call_http(method_url, method='GET',
                             headers=self.get_base_headers(session_key=self.session_key))

    @CheckMode.result_matches(has_entry('success', True))
    def delete_link(self):
        method_url = self.base_url + 'v1/users/me/social/yandex'
        with reporter.step(u'Отвязываем аккаунт Кинопоиска {} от Паспорта {}'.format(self.kp_user,
                                                                                     self.passport_user)):
            return call_http(method_url, method='DELETE',
                             headers=self.get_base_headers(session_key=self.session_key))

    @CheckMode.result_matches(has_entry('success', True))
    def admin_link(self):
        method_url = 'https://kp1admin-api.tst.yandex-team.ru/v1/users/{}' \
                     '/social-profiles/YANDEX?external_id={}'.format(self.kp_user.kinopoisk_user_id,
                                                                     self.passport_user.id_)
        with reporter.step(u'Используем тестовую ручку Кинопоиска для восстановления связи аккаунта Кинопоиска {} '
                           u'с Паспортным {}'.format(self.kp_user, self.passport_user)):
            return call_http(method_url, method='PUT')

    def process_unlinking_users(self):
        if self.linked_users_status:
                self.delete_link()
                self.linked_users_status = False

    def process_linking_users(self):
        if not self.linked_users_status:
                self.link_account()
                self.linked_users_status = True

    def binding_card_for_kp(self, card=None, need_tpi=False):
        if not card:
            card = get_card()
        user = uids.get_random_of(uids.temp_users)
        linked_cards, trust_payment_id = trust.process_binding(user=user, cards=card)
        mongo_steps.Bindings.change_card_holder(user, self.kp_user, linked_cards[0])
        mongo_steps.PaymentMethodsCache.clean_lpm_cache_for_user(user)
        mongo_steps.PaymentMethodsCache.clean_lpm_cache_for_user(self.kp_user)
        if need_tpi:
            return linked_cards[0], trust_payment_id[0]
        return linked_cards[0]

    def delete_card_for_kp(self, card_id):
        mongo_steps.Bindings.delete_card(card_id=card_id, user=self.kp_user)
        mongo_steps.PaymentMethodsCache.clean_lpm_cache_for_user(self.kp_user)

    def process_discollision_linked_users(self):
        """
            Коллизия возникает в случае, когда один из аккаунтов (Кинопоиска или Паспорта) считает, что у него есть
            связь со вторым аккаунтом, а второй считает, что её нет.
            Пример: Аккаунты были связаны -> При переналивке базы Кинопоиска, на стороне Кинопоиска аккаунт отвязали ->
            Паспорт все ещё считает, что аккаунты связаны.

            В таком случае при попутке связать, посмотреть статус привязки или отвязать
            аккаунты будет возращаться ошибка.

            Способ рещения кодлизии:
                Если на запрос о статусе привязки возвращается успех -> Коллизии точно нет
                Если после этого при попытке связать аккаунты возвращается успех -> Коллизии нет, в остальных случаях -
                коллизия есть -> Вызываем тестовыю ручку для принудительнолй связи аккаунтов только на стороне
                Кинопоиска -> Отвязываем аккаунты.
        """
        # Метод не опробован на коллизии, возможно, нужно поправить отлов ошибки в self.link_account
        with reporter.step(u'Начинаем проверку на коллизии в '
                           u'аккаунтах Кинопоиска {} и Паспорта {}'.format(self.kp_user, self.passport_user.login)):
            if self.linked_users_status:
                reporter.logger().debug(u'Коллизий нет')
            else:
                if self.link_account()['success']:
                    reporter.logger().debug(u'Коллизий нет.')
                else:
                    with reporter.logger().debug(u'Коллизия есть, решаем её.'):
                        self.admin_link()
