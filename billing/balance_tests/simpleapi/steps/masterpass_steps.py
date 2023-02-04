# coding: utf-8

import json
import os

from hamcrest import has_entry, is_

import btestlib.reporter as reporter
import simpleapi.data.certificates as certs
from btestlib import environments
from btestlib.utils import CheckMode
from simpleapi.common.utils import remove_empty, call_http
from simpleapi.data import defaults
from simpleapi.steps import check_steps as check

__author__ = 'slppls'

CER_PATH = os.path.dirname(certs.__file__) + '/masterpass.cer'
KEY_PATH = os.path.dirname(certs.__file__) + '/masterpass.key'


@CheckMode.result_matches(has_entry('Success', True))
def get_request_token(phone):
    params = remove_empty({
        'Phone': phone,
        'MerchantName': defaults.MasterPass.MerchantName,
    })
    url = environments.simpleapi_env().masterpass_wallet + 'mpapi/GetRequestToken'
    with reporter.step(u'Запрашиваем request-token для телефона {} в MasterPass'.format(phone)):
        return call_http(url, params, cert_path=CER_PATH, key_path=KEY_PATH)


@CheckMode.result_matches(has_entry('Success', True))
def login(request_token):
    params = remove_empty({
        'Fingerprint': 'RGV2aWNlSWQ9Njk0YWRlODctMjQ0Mi1hYzQyLWVmNTctYTVkNDllZGQzNDZjfHx8dXNlcl9hZ2VudD1Nb3ppbGxhLzUuMCAoV2luZG93cyBOVCAxMC4wOyBXaW42NDsgeDY0KSBBcHBsZVdlYktpdC81MzcuMzYgKEtIVE1MLCBsaWtlIEdlY2tvKSBDaHJvbWUvNjEuMC4zMTYzLjEwMCBTYWZhcmkvNTM3LjM2fHx8Y29sb3JfZGVwdGg9MjR8fHxsYW5ndWFnZT1ydXx8fHBpeGVsX3JhdGlvPTF8fHxyZXNvbHV0aW9uPTE5MjAsMTA4MHx8fGF2YWlsYWJsZV9yZXNvbHV0aW9uPTE5MjAsMTA1MHx8fHRpbWV6b25lX29mZnNldD0tMTgwfHx8bG9jYWxfc3RvcmFnZT0xfHx8bmF2aWdhdG9yX3BsYXRmb3JtPVdpbjMyfHx8Y2FudmFzPTEwMTg3MDA4Mjl8fHxoYXNfbGllZF9sYW5ndWFnZXM9ZmFsc2V8fHxoYXNfbGllZF9yZXNvbHV0aW9uPWZhbHNlfHx8aGFzX2xpZWRfb3M9ZmFsc2V8fHxoYXNfbGllZF9icm93c2VyPWZhbHNlfHx8anNfZm9udHM9MTM1MjQ0Mzc3',
        'RequestToken': request_token,
    })
    url = environments.simpleapi_env().masterpass_wallet + 'masterpassapi/Login'
    with reporter.step(u'Логинимся с токеном {} в MasterPass'.format(request_token)):
        return call_http(url, params, cert_path=CER_PATH, key_path=KEY_PATH)


@CheckMode.result_matches(has_entry('Success', True))
def logout(session):
    params = remove_empty({
        'Session': session,
    })
    url = environments.simpleapi_env().masterpass_wallet + 'masterpassapi/Logout'
    with reporter.step(u'Совершаем логаут для сессии {} в MasterPass'.format(session)):
        return call_http(url, params, cert_path=CER_PATH, key_path=KEY_PATH)


@CheckMode.result_matches(has_entry('Success', True))
def save_card(card, session):
    params = remove_empty({
        'PAN': card['card_number'],
        'CardHolder': card['cardholder'],
        'ExpMonth': card['expiration_month'],
        'ExpYear': card['expiration_year'],
        'Session': session,
    })
    url = environments.simpleapi_env().masterpass_wallet + 'masterpassapi/SaveCard'
    with reporter.step(u'Сохраняем карту {} для сессии {} в MasterPass'.format(card['card_number'], session)):
        return call_http(url, params, cert_path=CER_PATH, key_path=KEY_PATH)


@CheckMode.result_matches(has_entry('Success', True))
def postback(token, session):
    params = {
        'DATA': json.dumps(
            remove_empty({
                'OriginalOrderId': defaults.MasterPass.OriginalOrderId,
                'Amount': '100',
                'CurrencyCode': 'RUB',
                'DealDate': defaults.MasterPass.DealDate,
                'MerchantName': defaults.MasterPass.MerchantName,
                'ThreeDS': False,
                'CVC2': True,
                'Success': True,
                'TransactionType': defaults.MasterPass.TransactionType,
                'Token': token,
                'Session': session,
            })
        )
    }

    url = environments.simpleapi_env().masterpass_pspapi + 'mpapi/Postback'
    with reporter.step(u'Активируем карту под токеном {} в MasterPass'.format(token)):
        return call_http(url, params, cert_path=CER_PATH, key_path=KEY_PATH)


@CheckMode.result_matches(has_entry('Success', True))
def delete_card(token, session):
    params = remove_empty({
        'Token': token,
        'Session': session,
    })
    url = environments.simpleapi_env().masterpass_wallet + 'masterpassapi/DeleteCard'
    with reporter.step(u'Удаляем карту {} для сессии {} в MasterPass'.format(token, session)):
        return call_http(url, params, cert_path=CER_PATH, key_path=KEY_PATH)


@CheckMode.result_matches(has_entry('Success', True))
def get_cards(session):
    params = remove_empty({
        'Session': session,
    })
    url = environments.simpleapi_env().masterpass_wallet + 'masterpassapi/GetCards'
    with reporter.step(u'Получаем список карт для сессии {} в MasterPass'.format(session)):
        return call_http(url, params, cert_path=CER_PATH, key_path=KEY_PATH)


def get_session(phone):
    req_token = get_request_token(phone=phone)['RequestToken']
    return login(req_token)['Session']


def process_card_deleting(phone):
    with reporter.step(u'Удаляем все карты из MasterPass'):
        req_token = get_request_token(phone=phone)['RequestToken']
        session = login(req_token)['Session']
        mp_cards = get_cards(session)['CardList']

        for binded_card in mp_cards:
            delete_card(binded_card['Token'], session)


def process_binding_to_masterpass(phone, card, unbind_before=True):
    if unbind_before:
        process_card_deleting(phone)

    # slppls: masterpass требует формат из 10 символов без плюса и семерки, у нас же все телефоны вида +7 1234567890
    phone = phone[2:]

    with reporter.step(u'Проводим привязку карты в MasterPass'):
        binding_success = False
        req_token = get_request_token(phone=phone)['RequestToken']
        session = login(req_token)['Session']
        token = save_card(card, session)['Token']
        postback(token, session)
        mp_cards = get_cards(session)['CardList']

        for binded_card in mp_cards:
            if binded_card['Token'] == token:
                binding_success = True

        check.check_that(binding_success, is_(True),
                         step=u'Проверяем, что карта действительно привязана в Masterpass',
                         error=u'Карта в Masterpass не привязана!')
        return token


def check_card_in_masterpass_cards(phone, card):
    with reporter.step(u'Проверяем привязанные карты в MasterPass'):
        card_in_masterpass = False
        req_token = get_request_token(phone=phone)['RequestToken']
        session = login(req_token)['Session']
        mp_cards = get_cards(session)['CardList']

        for binded_card in mp_cards:
            if binded_card['PANMask'][-4:] == card['card_number'][-4:]:
                card_in_masterpass = True

        check.check_that(card_in_masterpass, is_(True),
                         step=u'Проверяем, что карта  привязана в Masterpass',
                         error=u'Карта в Masterpass не привязана!')


def check_card_not_in_masterpass_cards(phone, card):
    with reporter.step(u'Проверяем привязанные карты в MasterPass'):
        card_in_masterpass = False
        req_token = get_request_token(phone=phone)['RequestToken']
        session = login(req_token)['Session']
        mp_cards = get_cards(session)['CardList']

        for binded_card in mp_cards:
            if binded_card['PANMask'][-4:] == card['card_number'][-4:]:
                card_in_masterpass = True

        check.check_that(card_in_masterpass, is_(False),
                         step=u'Проверяем, что карта не привязана в Masterpass',
                         error=u'Карта привязана в Masterpass!')
