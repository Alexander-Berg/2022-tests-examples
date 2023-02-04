# -*- coding: utf-8 -*-
import datetime
import requests
import json

from btestlib import utils, secrets, utils_tvm
from btestlib.constants import TvmClientIds
from btestlib.secrets import get_secret, TvmSecrets

from core import print_res

CERT = utils.project_file(secrets.get_secret(*secrets.Certificates.IDM_CERT))  # Client certificate
KEY = utils.project_file(secrets.get_secret(*secrets.Certificates.IDM_KEY))  # Client private key

BASE_URL = 'https://staff-api.test.yandex-team.ru/v3'

_TVM_TICKET = None
_idx = 0


def get_tvm_ticket():
    """ Кэшируем тикет, чтобы не ходить на каждый тест за ним. Они сейчас живут по 12 часов. """
    global _TVM_TICKET
    if _TVM_TICKET is None:
        _TVM_TICKET = utils_tvm.get_tvm_ticket(
            dst_client_id=2001976,
            tvm_client_id=TvmClientIds.YB_MEDIUM,
            secret=get_secret(*TvmSecrets.)
        )

    return _TVM_TICKET


def make_request(method, path, params=None):
    url = '{}{}'.format(BASE_URL, path)

    global _idx
    _idx += 1
    print '{}. {:%H:%M:%S.%f} URL: {}'.format(_idx, datetime.datetime.now(), url)

    headers = {
        'Content-Type': 'application/x-www-form-urlencoded',
        'X-Ya-Service-Ticket': get_tvm_ticket(),
    }
    response = requests.request(
        method, url, params=params,
        headers=headers, cert=(CERT, KEY), verify=False
    )

    response.raise_for_status()
    return response.json()


def get_group():
    required = []
    next_url = '/idm/get-roles'

    while next_url is not None:
        data = make_request('get', next_url)
        next_url = data.get('next-url')

        if data.get('code') != 0:
            print data

        for row in data.get('roles', []):
            if (row.get('login') == 'natabers' or row.get('group') == 45699):
                required.append(row)

    print_res(required)


if __name__ == '__main__':
    get_group()
