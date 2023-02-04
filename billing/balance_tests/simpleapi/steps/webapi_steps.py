# coding: utf-8

from hamcrest import has_entry, contains_string

import btestlib.reporter as reporter
from btestlib import environments
from btestlib.utils import CheckMode
from simpleapi.common.utils import remove_empty, call_http

__author__ = 'slppls'


@CheckMode.result_matches(has_entry('status', 'success'))
def check_payment(purchase_token, domain='ru'):
    params = remove_empty({
        'purchase_token': purchase_token,
    })
    url = environments.simpleapi_env().trust_web_url.format(domain)
    with reporter.step(u'Проверяем платёж {} через WEBAPI'.format(purchase_token)):
        return call_http(url + 'check_payment', params)


@CheckMode.result_matches(has_entry('status', 'success'))
def cancel_payment(purchase_token, domain='ru'):
    params = remove_empty({
        'purchase_token': purchase_token,
    })
    url = environments.simpleapi_env().trust_web_url.format(domain)
    with reporter.step(u'Отменяем платёж {} через WEBAPI'.format(purchase_token)):
        return call_http(url + 'cancel_payment', params)


@CheckMode.result_matches(has_entry('status', 'success'))
def update_payment(purchase_token, email=None, bind_card=None, promocode=None, promocode_id=None, domain='ru'):
    params = remove_empty({
        'purchase_token': purchase_token,
        'email': email,
        'bind_card': bind_card,
        'promocode': promocode,
        'promocode_id': promocode_id,
    })
    url = environments.simpleapi_env().trust_web_url.format(domain)
    with reporter.step(u'Модифицируем параметры платёжа {} через WEBAPI'.format(purchase_token)):
        return call_http(url + 'update_payment', params)


@CheckMode.result_matches(has_entry('status', 'success'))
def start_payment(purchase_token, payment_method, domain='ru'):
    params = remove_empty({
        'purchase_token': purchase_token,
        'payment_method': payment_method,
    })
    url = environments.simpleapi_env().trust_web_url.format(domain)
    with reporter.step(u'Запускаем оплату платёжа {} через WEBAPI'.format(purchase_token)):
        return call_http(url + 'start_payment', params)


@CheckMode.result_matches(has_entry('status', 'success'))
def preview_payment(purchase_token, card_type=None, promocode=None, promocode_id=None, domain='ru'):
    params = remove_empty({
        'purchase_token': purchase_token,
        'card_type': card_type,
        'promocode': promocode,
        'promocode_id': promocode_id,
    })
    url = environments.simpleapi_env().trust_web_url.format(domain)
    with reporter.step(u'Запускаем предпросмотр платёжа {} через WEBAPI'.format(purchase_token)):
        return call_http(url + 'preview_payment', params)


@CheckMode.result_matches(has_entry('status', 'success'))
def unbind_card(purchase_token, card_id, domain='ru'):
    params = remove_empty({
        'purchase_token': purchase_token,
        'card_id': card_id
    })
    url = environments.simpleapi_env().trust_web_url.format(domain)
    with reporter.step(u'Отвязываем карту {} у {} через WEBAPI'.format(card_id, purchase_token)):
        return call_http(url + 'unbind_card', params)


@CheckMode.result_matches(contains_string('Payment started'))
def pcidss_start_payment(purchase_token, payment_method=None, card_number=None, expiration_month=None,
                         expiration_year=None, cardholder=None, cvn=None, xrf_token=None, cookies=None):
    params = remove_empty({
        'purchase_token': purchase_token,
        'payment_method': payment_method,
        'card_number': card_number,
        'expiration_month': expiration_month,
        'expiration_year': expiration_year,
        'cardholder': cardholder,
        'cvn': cvn,
        'xrf_token': xrf_token,
    })
    url = environments.simpleapi_env().pcidss_web_url
    with reporter.step(u'Запускаем оплату платёжа {} через WEBAPI'.format(purchase_token)):
        return call_http(url + 'start_payment', params, cookies=cookies)
