# coding=utf-8

import requests
import btestlib.config as balance_config
import os

TUS_ENDPOINT = 'https://tus.yandex-team.ru'
TUS_CONSUMER = 'balance'
TUS_ENV = 'prod'
TUS_LOCK_DURATION_SECONDS = str(5 * 60)

# Не лучшее решение, но к сожалению установка YandexInternalCA не сработала
# Для CI, который выполняется в контуре ДЦ выглядит адекватным tradeoff
TUS_VERIFY_SSL = False if os.getenv('PYTHONHTTPSVERIFY', None) == '0' else True

if balance_config.TUS_OAUTH_TOKEN is None:
    raise Exception(u'Не задан TUS_OAUTH_TOKEN, см. local_config_example.py')


def tus_get_account(env):
    return requests.get(
        TUS_ENDPOINT + '/1/get_account/',
        data={'tus_consumer': TUS_CONSUMER, 'env': env, 'lock_duration': TUS_LOCK_DURATION_SECONDS},
        headers={'Authorization': 'OAuth ' + balance_config.TUS_OAUTH_TOKEN},
        verify=TUS_VERIFY_SSL
    ).json()


def tus_create_account(env):
    return requests.post(
        TUS_ENDPOINT + '/1/create_account/portal/',
        data={'tus_consumer': TUS_CONSUMER, 'env': env},
        headers={'Authorization': 'OAuth ' + balance_config.TUS_OAUTH_TOKEN},
        verify=TUS_VERIFY_SSL
    )


def tus_unlock_account(uid):
    return requests.post(
        TUS_ENDPOINT + '/1/unlock_account/',
        data={'tus_consumer': TUS_CONSUMER, 'env': TUS_ENV, 'uid': uid},
        headers={'Authorization': 'OAuth ' + balance_config.TUS_OAUTH_TOKEN},
        verify=TUS_VERIFY_SSL
    )
