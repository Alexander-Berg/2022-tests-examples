# coding: utf-8

import json
import re

from hamcrest import has_entry
from hamcrest import is_, equal_to, is_in, has_key, any_of, has_items
from tenacity import retry, retry_if_result, stop_after_attempt, wait_random

import btestlib.reporter as reporter
from btestlib import environments
from btestlib import utils as butils
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common import logger
from simpleapi.common.oauth import Auth
from simpleapi.common.utils import remove_empty, restart_if, call_http, parse_xml
from simpleapi.data import uids_pool
from simpleapi.steps import check_steps as check
from simpleapi.steps import passport_steps as passport
__author__ = 'fellow'

log = logger.get_logger()


@CheckMode.result_matches(has_entry('status', 'success'))
@butils.retry_if(any_of(has_entry('status_desc', 'INVALID: expired_token'),
                        has_entry('status', 'no_auth')))
def bind_card(token, card_params, multiple=None, user_ip=None, region_id=None, kinopoisk_session_id=None):
    params = remove_empty({
        'params': remove_empty({
            'token': token,
            'expiration_month': card_params.get('expiration_month'),
            'expiration_year': card_params.get('expiration_year'),
            'card_number': card_params.get('card_number'),
            'cvn': card_params.get('cvn'),
            'cardholder': card_params.get('cardholder'),
            'region_id': region_id,
            'user_ip': user_ip,
            'multiple': multiple,
            'kinopoisk_session_id': kinopoisk_session_id,
        })
    })
    url = environments.simpleapi_env().pcidss_api_url
    with reporter.step(u'Привязываем карту с параметрами {}'.format(json.dumps(params))):
        return call_http(url + 'bind_credit_card', json.dumps(params))


@CheckMode.result_matches(has_entry('status', 'success'))
@restart_if(key='status_desc', error=u'unbound_card_didnt_disappear')
def unbind_card(token, card_id=None):
    params = remove_empty({
        'params': remove_empty({
            'token': token,
            'card': card_id,
        }),
    })
    url = environments.simpleapi_env().pcidss_api_url
    with reporter.step(u'Отвязываем карту с параметрами {}'.format(json.dumps(params))):
        return call_http(url + 'unbind_card', json.dumps(params))


def oauth_need_retry(value):
    return value == 'Service temporarily unavailable'


def oauth_retry_error(last_attempt):
    return butils.UnsuccessfulResponse(last_attempt.result())


@CheckMode.result_matches(has_key('access_token'))
@retry(retry=retry_if_result(oauth_need_retry), wait=wait_random(min=1, max=2),
       stop=stop_after_attempt(5), reraise=True, retry_error_callback=oauth_retry_error)
def get_auth_token(auth, user):
    # у фонишей токен уже есть
    if user.token:
        return {'access_token': user.token}
    if user.is_fake:
        with reporter.step(u'Используем фэйкового пользователя {} и фэйковый токен для него'.format(user)):
            return {'access_token': str(user.uid) + '-token'}

    params = {
        'client_id': auth['client_id'],
        'client_secret': auth['client_secret'],
        # 'grant_type': 'client_credentials',
        'grant_type': 'password',
        'username': user.login,
        'password': user.password
    }
    with reporter.step(u'Получаем oauth-токен для пользователя {}'.format(user)):
        return call_http(auth['token_url'], params)


@CheckMode.result_matches(has_entry('status', any_of('success', 'wait_for_notification')))
def check_payment(purchase_token):
    params = remove_empty({
        'params': {
            'purchase_token': purchase_token,
        },
    })
    url = environments.simpleapi_env().pcidss_api_url
    with reporter.step(u'Проверяем статус платежа purchase_token={}'.format(purchase_token)):
        return call_http(url + 'check_payment', json.dumps(params))


@CheckMode.result_matches(has_entry('status', 'success'))
def supply_payment_data(token, purchase_token=None, trust_payment_id=None, cvn=None,
                        card_number=None, cardholder=None, expiration_year=None,
                        expiration_month=None, payment_method=None,
                        email=None, phone=None, bind_card=None, apple_token=None):
    params = remove_empty({
        'params': remove_empty({
            'token': token,
            'purchase_token': purchase_token,
            'trust_payment_id': trust_payment_id,
            'cvn': cvn,
            'card_number': card_number,
            'cardholder': cardholder,
            'expiration_year': expiration_year,
            'expiration_month': expiration_month,
            'payment_method': payment_method,
            'email': email,
            'phone': phone,
            'bind_card': bind_card,
            'apple_token': apple_token,
        }),
    })
    url = environments.simpleapi_env().pcidss_api_url
    with reporter.step(u'Передаем параметры карты или cvn: {}'.format(json.dumps(params))):
        return call_http(url + 'supply_payment_data', json.dumps(params))


@CheckMode.result_matches(has_entry('status', 'success'))
def check_card(token, card_id, cvn=None, currency=None, region_id=None):
    params = remove_empty({
        'params': remove_empty({
            'token': token,
            'card_id': card_id,
            'cvn': cvn,
            'currency': currency,
            'region_id': region_id,
        }),
    })
    url = environments.simpleapi_env().pcidss_api_url
    with reporter.step(u'Проверяем платежеспособость карты {}'.format(card_id)):
        return call_http(url + 'check_card', json.dumps(params))


@CheckMode.result_matches(has_entry('status', 'success'))
def list_payment_methods(token, user_ip='127.0.0.1', ym_schema=None, region_id=None, show_all=False):
    params = remove_empty({
        'params': remove_empty({
            'token': token,
            'user_ip': user_ip,
            'ym_schema': ym_schema,
            'region_id': region_id,
            'show_all': 'true' if show_all else 'false'
        }),
    })
    url = environments.simpleapi_env().pcidss_api_url
    with reporter.step(u'Получаем список доступных методов оплаты для токена {}'.format(token)):
        return call_http(url + 'list_payment_methods', json.dumps(params))


@CheckMode.result_matches(has_entry('status', 'success'))
def bind_apple_pay_token(token, apple_token, order_tag, region_id='225'):
    params = remove_empty({
        'params': remove_empty({
            'token': token,
            'apple_token': apple_token,
            'order_tag': order_tag,
            'region_id': region_id
        }),
    })
    url = environments.simpleapi_env().pcidss_api_url
    with reporter.step(u'Привязываем apple_pay_token к учётной записи Яндекс.Паспорта {}'.format(json.dumps(params))):
        return call_http(url + 'bind_apple_token', json.dumps(params))


@CheckMode.result_matches(has_entry('status', 'success'))
def bind_google_pay_token(token, google_pay_token, order_tag, region_id='225'):
    params = remove_empty({
        'params': remove_empty({
            'token': token,
            'google_pay_token': google_pay_token,
            'order_tag': order_tag,
            'region_id': region_id
        }),
    })
    url = environments.simpleapi_env().pcidss_api_url
    with reporter.step(u'Привязываем google_pay_token к учётной записи Яндекс.Паспорта {}'.format(json.dumps(params))):
        return call_http(url + 'bind_google_pay_token', json.dumps(params))


def process_binding(user, cards, service=Services.STORE, multiple=None, region_id=225,
                    user_ip=None, kinopoisk_session_id=False):
    # if uids_pool.users_holder_mode_enabled() and user != uids_pool.anonymous:
    #     uids_pool.users_holder.append(user)
    #     uids_pool.mark_user(user)

    linked_cards = []
    trust_payment_ids = []
    if not isinstance(cards, (list, tuple)):
        cards = (cards,)

    session_id, token = None, None

    if kinopoisk_session_id:
        passport.auth_via_page(user=user)
        session_id = passport.get_current_session_id()
    else:
        token = get_auth_token(Auth.get_auth(user, service), user)['access_token']

    for card in cards:
        with check_mode(CheckMode.FAILED):
            resp = bind_card(token, card, multiple=multiple, region_id=region_id,
                             user_ip=user_ip, kinopoisk_session_id=session_id)
        check.check_that('payment_method', is_in(resp),
                         step=u'Проверяем что ответ метода содержит ключ \'payment_method\'',
                         error=u'Ответ метода не содержит ключ \'payment_method\'')
        linked_cards.append(resp['payment_method'])
        trust_payment_ids.append(resp['trust_payment_id'])

    return linked_cards, trust_payment_ids


def process_unbinding(user, card=None, service=Services.STORE, token=None):
    if not token:
        token = get_auth_token(Auth.get_auth(user, service), user)['access_token']
        
    def check_status(resp):
        # Ошибка unbound_card_didnt_disappear появляется периодически, проверять ее не будет
        # потом просто проверим, что карта по факту отвязалась
        if resp['status'] == 'error' and resp['status_desc'] == 'unbound_card_didnt_disappear':
            pass
        else:
            check.check_that(resp['status'], is_(equal_to('success')),
                             step=u'Проверяем корректность ответа метода unbind_card',
                             error=u'Ошибка при вызове метода unbind_card. Ответ {}'.format(resp))

    if card is None:
        from simpleapi.steps import simple_steps as simple
        # Берем lpm из simple потому, что trust.lpm использует Oauth токен, а они есть не для все сервисов
        # по дефолту будет браться Services.STORE, а его lpm выдает только одну карту.
        _, paymethods = simple.list_payment_methods(Services.TAXI if service == Services.STORE else service, user)
        for binding_card in paymethods:
            check_status(unbind_card(token, card_id=binding_card))
    else:
        check_status(unbind_card(token, card_id=card))


def unbind_all_cards_of(user, service=Services.STORE, bounded_card_num=2):
    if user == uids_pool.anonymous:
        return

    with reporter.step(u'Если у пользователя {} более {} привязанных карт, отвяжем их'.format(user, bounded_card_num)):
        from simpleapi.steps import simple_steps as simple

        bounded_cards_ids = [key for key in simple.list_payment_methods(service, user)[1].keys() if
                             'card-' in key]

        if len(bounded_cards_ids) > bounded_card_num:
            for card_id in bounded_cards_ids:
                process_unbinding(user, card_id, service)


def pay_by(payment_method, service, *args, **kwargs):
    with reporter.step(u'Совершаем оплату корзины (или заказа). Способ оплаты {}'.format(payment_method)):
        payment_method.pay(service=service, *args, **kwargs)


@parse_xml
def _get_sms_content(phone):
    url = environments.simpleapi_env().yam_test_phone_url
    params = {'phone': phone}

    with reporter.step(u'Получаем текст смс от ЯД. Телефон '.format(phone)):
        return call_http(url, params, method='GET')


def get_code_from_sms(phone):
    if phone.startswith('+'):
        phone = phone[1:]
    sms_list = _get_sms_content(phone)['FakeSmsGate']['SmsList']['sms']
    if not isinstance(sms_list, (list, tuple)):
        sms_list = (sms_list,)

    return re.search(r'\d{4}', sms_list[0]['#text']).group(0)


def wait_until_3ds_url_added(purchase_token):
    with reporter.step(u'Ждём, пока в ответ метода добавится redirect_3ds_url...'):
        return butils.wait_until(lambda: check_payment(purchase_token),
                                 success_condition=has_items('redirect_3ds_url'),
                                 failure_condition=has_entry('status', any_of('cancelled', 'error')),
                                 timeout=4 * 60)
