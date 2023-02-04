#!/usr/bin/env python
# -*- coding: utf-8 -*-
import requests
import os
import json


def get_sandbox_token():
    token = os.environ['YAV_OAUTH']
    if os.path.exists(token):
        with open(token) as f:
            return f.read().strip()
    return token


def _get_yav_data(yav_id):
    try:
        from vault_client import instances
        ARCADIA_RUN = False
    except ImportError:
        from library.python.vault_client import instances
        ARCADIA_RUN = True

    yav_id = yav_id

    init_kwargs = {}

    if ARCADIA_RUN:
        oauth_login, oauth_token = 'testuser-balance1', get_sandbox_token()
        init_kwargs.update(dict(
            rsa_login=oauth_login,
            authorization=oauth_token,
        ))

    yav = instances.Production(**init_kwargs)
    secret = yav.get_version(yav_id)

    return secret['value']


MOLLY_SECRETS = _get_yav_data(yav_id='sec-01d47r1jd996wxa56qgqcfd0gg')


def launch_scan():
    base_url = 'https://molly.yandex-team.ru/'
    api_url = base_url + 'api/v1.1/scan/'
    target_uri = 'https://balalayka-test.paysys.yandex-team.ru'
    result_users = 'chihiro'
    resp = requests.post(
        api_url,
        headers={'Authorization': 'OAuth %s' % MOLLY_SECRETS['molly_oauth']},
        data={
            'profile': 'Yandex', 'target_uri': target_uri,
            'users': result_users, 'auth_profile': MOLLY_SECRETS['auth_profile'],
            'abc_id': 571,  # id Балалайки в ABC
            'target': 'bcl_test',
            'st_queue': 'BCL', 'target_name': 'BCL'
        },
        verify=False
    )
    print('Ответ %s' % resp.text)

    assert resp.status_code == 200
    result = json.loads(resp.text)
    if result.get('scan_id'):
        for scan_id in result['scan_id']:
            print('URL с резульататом сканирования %sreport/%s' % (base_url, scan_id))
    else:
        print('Ошибка при запуске %s' % json.dumps(result))


if __name__ == "__main__":
    launch_scan()
