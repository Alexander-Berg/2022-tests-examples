# coding: utf-8
from btestlib.constants import Services

__author__ = 'fellow'


class Scope(object):
    def __init__(self, client_id, client_secret):
        self.client_id = client_id
        self.client_secret = client_secret


class Scopes(object):
    """
    Для Стора на продовском oauth у нас есть скоуп
    Для остальных сервисов до поры до времени используем Сторовски
    Со временем заменяем скоупы для сервисов, по мере их узнавания где-либо
    """
    PROD = {
        # для продовского паспорта используем везде скоуп Стора
        Services.STORE: Scope('07cdfe530d2649e8a1ef8484fd50633d', '48e127d9cbf04954ade887b8b88c8248'),
    }

    TEST = {
        Services.DISK: Scope('e0d11cdb1b78478981c7d60ae5603aa0', 'b76cb274d89b4f41bb01956fe8560409'),
        Services.PARKOVKI: Scope('8f651cd0eb7745bea3c467a296d91beb', '6b3168e846cf45c9a224ddbf508774db'),
        Services.TAXI: Scope('e8b9f6c2d0a143eb8109bfe98eb42b05', 'c41bba40d01d4508a45923a02fcde795'),
        Services.UBER: Scope('e8b9f6c2d0a143eb8109bfe98eb42b05', 'c41bba40d01d4508a45923a02fcde795'),
        Services.MARKETPLACE: Scope('5d7ef3cf9a84420eb99f02b423893685', '22196800d32d44dbbe34d810c15387ce'),
        Services.NEW_MARKET: Scope('5d7ef3cf9a84420eb99f02b423893685', '22196800d32d44dbbe34d810c15387ce'),
        Services.MUSIC: Scope('466b9e1e54ac43ae8011f9217491a0fa', '48428eea863f4c0698ee88acf1de67f1'),
        Services.DRIVE: Scope('f7d87c529a4343558927ece2c1effd59', '25079cef361a4c32aa3b9f6f8ae4bf3d'),
        # для всех сервисов, которые здесь не указаны, используем скоуп Стора
        Services.STORE: Scope('339e9a9d5bf346629394d7c8b4625c06', 'b9f4f6b80b674be8bdc0538ca0eb7b76'),
        Services.PHONE: Scope('466b9e1e54ac43ae8011f9217491a0fa', '48428eea863f4c0698ee88acf1de67f1')
    }


class Auth(object):
    def __init__(self, service=Services.STORE):
        self.service = service

    def get_prod(self):
        return {
            'passport_url': 'https://passport.yandex.ru/auth',
            'token_url': 'https://oauth.yandex.ru/token',
            'client_id': Scopes.PROD.get(self.service, Scopes.PROD[Services.STORE]).client_id,
            'client_secret': Scopes.PROD.get(self.service, Scopes.PROD[Services.STORE]).client_secret,
        }

    def get_test(self):
        return {
            'passport_url': 'https://passport-test.yandex.ru/auth',
            'token_url': 'http://oauth-test.yandex.ru/token',
            'client_id': Scopes.TEST.get(self.service, Scopes.TEST[Services.STORE]).client_id,
            'client_secret': Scopes.TEST.get(self.service, Scopes.TEST[Services.STORE]).client_secret
        }

    @staticmethod
    def get_auth(user, service=Services.STORE):
        if user.is_fake:
            return 'fake_oauth_scope'

        return Auth(service).get_test() if user.is_test() else Auth(service).get_prod()


class AuthUber(object):
    @staticmethod
    def get_auth(user):
        return Scope(client_id='sWIoGgsCw_bPJM-KU-GaRxR8pZY9sr-z',
                     client_secret='UvIrsyIM-wlDNdQJob0LWL9w70FI-e9E_ftAI2pn')
