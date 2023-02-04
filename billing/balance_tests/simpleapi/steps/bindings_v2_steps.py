# coding: utf-8
import json

from hamcrest import has_entry, has_entries, any_of

import btestlib.reporter as reporter
from btestlib import environments
from btestlib.utils import wait_until
from simpleapi.common.oauth import Auth
from simpleapi.common.utils import remove_empty, call_http, encrypt_data_by_service
from simpleapi.data import defaults
from simpleapi.steps import mongo_steps as mongo
from simpleapi.steps import trust_steps as trust
from simpleapi.steps import web_payment_steps as web

__author__ = 'slppls'

'''
https://wiki.yandex-team.ru/TRUST/newbindingapi/
'''


def mobile_url():
    return environments.simpleapi_env().pcidss_api_url


def server_url():
    return environments.simpleapi_env().trust_paysys_url


def notification_url():
    return environments.simpleapi_env().emulator_outer_url


def get_base_headers_mobile_bindings(oauth_token):
    return remove_empty({
        'X-Oauth-Token': oauth_token,
        "Content-Type": "application/json"
    })


def get_base_headers_server_bindings(user, service):
    return remove_empty({
        'X-Uid': str(user.uid),
        'X-Service-Token': service.token,
        "Content-Type": "application/json"
    })


class MobileApi(object):
    @staticmethod
    def get_base_url():
        return mobile_url() + 'bindings/v2.0/bindings/'

    @staticmethod
    def bind_card(token, service, card_params, name='test', region_id=None):
        card_data = {
            'expiration_month': card_params.get('expiration_month'),
            'expiration_year': card_params.get('expiration_year'),
            'card_number': card_params.get('card_number'),
            'cvn': card_params.get('cvn'),
        }
        card_holder = card_params.get('cardholder')

        method_url = MobileApi.get_base_url()
        card_data_encrypted = encrypt_data_by_service(json.dumps(card_data), service)
        params = remove_empty({
            'card_data_encrypted': card_data_encrypted,
            'card_holder': card_holder,
            'name': name,
            'region_id': region_id
        })
        with reporter.step(u'Создаем привязку для токена {}'.format(token)):
            return call_http(method_url, json.dumps(params), headers=get_base_headers_mobile_bindings(token),
                             check_code=True)

    @staticmethod
    def get(user, service, show_unverified=False):
        method_url = MobileApi.get_base_url()
        if show_unverified:
            method_url += '?show_unverified=True'
        token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']
        with reporter.step(u'Получаем список привязок для пользователя {}'.format(user)):
            return call_http(method_url, method='GET', headers=get_base_headers_mobile_bindings(token), check_code=True)

    @staticmethod
    def verify(token, user, service, binding_id, method, currency='RUB', country_code='RUS'):
        method_url = MobileApi.get_base_url() + '{}/verify/'.format(binding_id)
        params = remove_empty({
            'method': method,
            'currency': currency,
            'country_code': country_code,
        })
        with reporter.step(u'Верифицируем карту для токена {}'.format(token)):
            return call_http(method_url, json.dumps(params), headers=get_base_headers_mobile_bindings(token),
                             check_code=[200, 400])

    @staticmethod
    def unbind(token, binding_id):
        method_url = MobileApi.get_base_url() + '{}/unbind/'.format(binding_id)
        with reporter.step(u'Отвязываем карту для токена {}'.format(token)):
            return call_http(method_url, headers=get_base_headers_mobile_bindings(token), check_code=True)

    @staticmethod
    def get_verification(token, binding_id, verification_id):
        method_url = MobileApi.get_base_url() + '{}/verifications/{}/'.format(binding_id, verification_id)
        with reporter.step(u'Получаем статус верификации привязки, verification_id={}'.format(verification_id)):
            return call_http(method_url, method='GET', headers=get_base_headers_mobile_bindings(token), check_code=True)

    @staticmethod
    def guess_amount(token, binding_id, verification_id, authorize_amount):
        method_url = MobileApi.get_base_url() + '{}/verifications/{}/guess_amount/'.format(binding_id,
                                                                                           verification_id)
        params = remove_empty({
            'authorize_amount': str(authorize_amount),
        })
        with reporter.step(u'Отсылаем сумму для авторизации {}'.format(authorize_amount)):
            return call_http(method_url, json.dumps(params), headers=get_base_headers_mobile_bindings(token),
                             check_code=True)

    @staticmethod
    def process_verify(token, user, service, binding_id, method=defaults.BindingMethods.auto,
                       currency=None, country_code=None, wait_3ds=True, second_random_amt=False):
        """
            Флаг second_random_amt сообщает о том, что мы дергаем verify с методом random_amt
            не первый раз в течении времени ханения верификации в кеше. В этом случае вместо
            status: amount_expected возвращается success. Нам необходимо учитывать это при проверке.
        """
        resp = MobileApi.verify(token, user, service, binding_id, method, currency, country_code)
        verification_id = resp['verification']['id']
        verify_method = resp['verification']['method']
        if verify_method == defaults.BindingMethods.random_amt and not second_random_amt:
            Wait.until_verification_mobile_done(token, user, service, binding_id, verification_id,
                                                status=defaults.BindingStatus.amount_expected)
            amount = float(mongo.Payment.find_by(purchase_token=verification_id).sum)
            MobileApi.guess_amount(token, binding_id, verification_id, amount)
        elif verify_method == defaults.BindingMethods.standard2_3ds and wait_3ds:
            resp = Wait.until_verification_mobile_done(token, user, service, binding_id, verification_id,
                                                       status=defaults.BindingStatus.required_3ds)
            web.fill_emulator_3ds_page(resp['3ds_url'], defaults.Binding3dsCode.success)
        Wait.until_verification_mobile_done(token, user, service, binding_id, verification_id)
        return verification_id

    @staticmethod
    def process_binding(user, service, card_params, method=None, currency='RUB', country_code='RUS',
                        wait_3ds=True, region_id=None, second_random_amt=True):
        token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']

        binding_id = MobileApi.bind_card(token, service, card_params, region_id=region_id)['binding']['id']
        last_verify_id = MobileApi.process_verify(token, user, service, binding_id, currency=currency,
                                                  country_code=country_code, wait_3ds=wait_3ds)
        if method:
            last_verify_id = MobileApi.process_verify(token, user, service, binding_id, method=method, currency=currency,
                                                      country_code=country_code, wait_3ds=wait_3ds,
                                                      second_random_amt=second_random_amt)
        return binding_id, last_verify_id

    @staticmethod
    def process_unbinding(user, service, binding_id=None):
        token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']
        if binding_id:
            MobileApi.unbind(token, binding_id)
        else:
            paymethods = MobileApi.get(user, service)['bindings']
            for paymethod in paymethods:
                MobileApi.unbind(token, paymethod['id'])


class ServerApi(object):
    @staticmethod
    def get_base_url():
        return server_url() + 'bindings-external/v2.0/'

    @staticmethod
    def get(user, service, show_unverified=False):
        method_url = ServerApi.get_base_url() + 'bindings/'
        if show_unverified:
            method_url += '?show_unverified=True'
        with reporter.step(u'Получаем список привязок для uid {}'.format(user.uid)):
            return call_http(method_url, method='GET', headers=get_base_headers_server_bindings(user, service),
                             check_code=True)

    @staticmethod
    def verify(token, user, service, binding_id, method, currency='RUB', country_code='RUS'):
        method_url = ServerApi.get_base_url() + 'bindings/{}/verify/'.format(binding_id)
        params = remove_empty({
            'method': method,
            'currency': currency,
            'country_code': country_code,
        })
        with reporter.step(u'Верифицируем карту для uid {}'.format(user.uid)):
            return call_http(method_url, json.dumps(params), headers=get_base_headers_server_bindings(user, service),
                             check_code=[200, 400])

    @staticmethod
    def unbind(user, service, binding_id, session_id=None, user_ip=None, host=None):  # add all to this shit
        method_url = ServerApi.get_base_url() + 'bindings/{}/unbind/'.format(binding_id)
        params = remove_empty({
            'session_id': session_id,
            'user_ip': user_ip,
            'host': host,
        })
        with reporter.step(u'Отвязываем карту для uid {}'.format(user.uid)):
            return call_http(method_url, json.dumps(params), headers=get_base_headers_server_bindings(user, service),
                             check_code=True)

    @staticmethod
    def get_verification(user, service, binding_id, verification_id):
        method_url = ServerApi.get_base_url() + 'bindings/{}/verifications/{}/'.format(binding_id,
                                                                                       verification_id)
        with reporter.step(u'Получаем статус верификации привязки, verification_id={}'.format(verification_id)):
            return call_http(method_url, method='GET', headers=get_base_headers_server_bindings(user, service),
                             check_code=True)

    @staticmethod
    def settings(user, service, binding_notify_url=environments.simpleapi_env().emulator_outer_url + 'callbacks'):
        method_url = ServerApi.get_base_url() + 'settings/'
        params = remove_empty({
            'binding_notify_url': binding_notify_url,
        })
        with reporter.step(u'ПРивязываем для uid {} урл для нотификаций {}'.format(user.uid, binding_notify_url)):
            return call_http(method_url, json.dumps(params), headers=get_base_headers_server_bindings(user, service),
                             check_code=True)

    @staticmethod
    def process_verify(token, user, service, binding_id, method=defaults.BindingMethods.auto, currency=None,
                       country_code=None, wait_3ds=True, second_random_amt=False):
        resp = ServerApi.verify(token, user, service, binding_id, method, currency, country_code)
        verification_id = resp['verification']['id']
        verify_method = resp['verification']['method']
        if verify_method == defaults.BindingMethods.random_amt and not second_random_amt:
            Wait.until_verification_server_done(token, user, service, binding_id, verification_id,
                                                status=defaults.BindingStatus.amount_expected)
            amount = float(mongo.Payment.find_by(purchase_token=verification_id).sum)
            MobileApi.guess_amount(token, binding_id, verification_id, amount)
        elif verify_method == defaults.BindingMethods.standard2_3ds and wait_3ds:
            resp = Wait.until_verification_server_done(token, user, service, binding_id, verification_id,
                                                       status=defaults.BindingStatus.required_3ds)
            url = resp['3ds_url']
            web.fill_emulator_3ds_page(url, 200)
        Wait.until_verification_server_done(token, user, service, binding_id, verification_id)
        return verification_id

    @staticmethod
    def process_binding(user, service, card_params, method=None, currency='RUB', country_code='RUS',
                        wait_3ds=True, region_id=None, second_random_amt=True):
        token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']

        binding_id = MobileApi.bind_card(token, service, card_params, region_id=region_id)['binding']['id']
        last_verify_id = ServerApi.process_verify(token, user, service, binding_id, currency=currency,
                                                  country_code=country_code, wait_3ds=wait_3ds)
        # флаг second_random_amt используется для обозначения повторного вызова random_amt, во время этого вызова
        # возвращается другой статус: success, надо уметь отличать их!
        if method:
            last_verify_id = ServerApi.process_verify(token, user, service, binding_id, method=method,
                                                      currency=currency, country_code=country_code, wait_3ds=wait_3ds,
                                                      second_random_amt=second_random_amt)
        return binding_id, last_verify_id


class Notifications(object):
    @staticmethod
    def get_base_url():
        return notification_url() + 'notifications'

    @staticmethod
    def get_binding_notification(user, card_id):
        method_url = Notifications.get_base_url()
        params = remove_empty({
            'uid': user.uid,
            'card_id': card_id,
        })
        with reporter.step(u'Получаем нотификацию по привязке {}'.format(card_id)):
            return call_http(method_url, json.dumps(params), check_code=True)

    @staticmethod
    def get_verification_notification(user, status, event, verification_id):
        method_url = Notifications.get_base_url()
        params = remove_empty({
            'uid': user.uid,
            'status': status,
            'event': event,
            'verification_id': verification_id,
        })
        with reporter.step(u'Получаем нотификацию по верификации {}'.format(verification_id)):
            return call_http(method_url, json.dumps(params), check_code=True)


class Wait(object):
    @staticmethod
    def until_verification_done(system, *args, **kwargs):
        if system == MobileApi:
            return Wait.until_verification_mobile_done(*args, **kwargs)
        elif system == ServerApi:
            return Wait.until_verification_server_done(*args, **kwargs)

    @staticmethod
    def until_verification_mobile_done(token, user, service, binding_id, verification_id, status='success'):
        with reporter.step(u'Ждём, пока осуществится верификация...'):
            return wait_until(
                lambda: MobileApi.get_verification(token, binding_id, verification_id).get('verification'),
                success_condition=has_entries({'status': status}),
                failure_condition=has_entry('status', any_of('cancelled', 'error', 'failure')),
                timeout=1 * 60)

    @staticmethod
    def until_verification_server_done(token, user, service, binding_id, verification_id, status='success'):
        with reporter.step(u'Ждём, пока осуществится верификация...'):
            return wait_until(
                lambda: ServerApi.get_verification(user, service, binding_id,
                                                   verification_id).get('verification'),
                success_condition=has_entries({'status': status}),
                failure_condition=has_entry('status', any_of('cancelled', 'error', 'failure')),
                timeout=1 * 60)

    @staticmethod
    def until_binding_notify_done(user, card_id):
        with reporter.step(u'Ждём, пока придет нотификация...'):
            return wait_until(
                lambda: Notifications.get_binding_notification(user, card_id),
                success_condition=has_entry('card_id', card_id),
                timeout=0.5 * 60)

    @staticmethod
    def until_verification_notify_done(user, status, event, verification_id):
        with reporter.step(u'Ждём, пока придет нотификация...'):
            return wait_until(
                lambda: Notifications.get_verification_notification(user, status, event, verification_id),
                success_condition=has_entry('event', event),
                timeout=0.5 * 60)
