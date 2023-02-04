# -*- coding: utf-8 -*-
import datetime
import requests
import json
from threading import Thread

import btestlib.environments as env
from btestlib import utils, secrets, utils_tvm
from btestlib.constants import TvmClientIds
from btestlib.secrets import get_secret, TvmSecrets

from core import print_res

_TVM_TICKET = None
_idx = 0


def get_tvm_ticket():
    """ Кэшируем тикет, чтобы не ходить на каждый тест за ним. Они сейчас живут по 12 часов. """
    global _TVM_TICKET
    if _TVM_TICKET is None:
        _TVM_TICKET = utils_tvm.get_tvm_ticket(
            dst_client_id=TvmClientIds.YB_MEDIUM,
            tvm_client_id=TvmClientIds.BALANCE_TESTS,
            secret=get_secret(*TvmSecrets.BALANCE_TESTS_SECRET)
        )

    return _TVM_TICKET


def make_request(method, path, params=None):
    url = '{}{}'.format(env.balance_env().idm_url, path)
    # url = env.balance_env().idm_url

    global _idx
    _idx += 1
    print '{}. {:%H:%M:%S.%f} URL: {}'.format(_idx, datetime.datetime.now(), url)

    headers = {
        'Content-Type': 'application/x-www-form-urlencoded',
        'X-Ya-Service-Ticket': get_tvm_ticket(),
    }
    response = requests.request(
        method, url, params=params,
        headers=headers, verify=False
    )

    response.raise_for_status()
    return response.json()


def get_roles():
    required = []
    next_url = '/get-roles'

    while next_url is not None:
        data = make_request('get', next_url)
        next_url = data.get('next-url')
        if next_url.startswith('/idm'):
            next_url = next_url[4:]

        if data.get('code') != 0:
            print data

        print_res(data)

        for row in data.get('roles', []):
            if (row.get('login') == 'natabers' or row.get('group') == 45699):
                required.append(row)

    print_res(required)


def add_role(login='natabers', client_id=None):
    res = make_request(
        'post',
        '/add-role',
        params={
            'role': json.dumps({"role": "10156"}),
            # 'group': 45699,
            'login': login,
            'fields': json.dumps({
                "client_id": client_id,
                # 'abc_clients': False,
                # 'firm_id': 125,
            }),
            'path': '/role/10156/',
        }
    )
    print_res(res)


def remove_role(login='antony-zudov', client_id=None):
    res = make_request(
        'post',
        '/remove-role',
        params={
            'role': json.dumps({"role": "55"}),
            # 'group': 45699,
            'login': login,
            'fields': json.dumps({
                # "client_id": client_id,
                # 'abc_clients': False,
                'firm_id': 123,
            }),
            'path': '/role/55/',
        }
    )
    print_res(res)


def add_batch_memberships():
    res = make_request(
        'post',
        '/add-batch-memberships',
        params={
            'data': json.dumps([
                {
                    'login': 'natabers',
                    # 'passport_login': 'yndx-natabers',
                    'group': 45699,
                },
            ])
        },
    )
    print_res(res)


class MyTread(Thread):

    def __init__(self, method_name, kwargs):
        super(MyTread, self).__init__()
        self.method_name = method_name
        self.kwargs = kwargs

    def run(self):
        self.method_name(**self.kwargs)


def run():
    for client_id in ['15']:
        for login in ['natabers', 'apopkov']:
            thread = MyTread(remove_role, {'login': login, 'client_id': client_id})
            thread.start()


if __name__ == '__main__':
    get_roles()
    # add_role()
    # remove_role()
    # add_batch_memberships()

    # run()


# https://balance-xmlrpc-tvm-tc.paysys.yandex.net:8004/idm/