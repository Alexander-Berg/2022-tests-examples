# -*- coding: utf-8 -*-
import requests

from btestlib import utils, secrets, utils_tvm
from btestlib.constants import TvmClientIds
from btestlib.secrets import get_secret, TvmSecrets

from core import print_res

CERT = utils.project_file(secrets.get_secret(*secrets.Certificates.XMLRPC_CLIENT_CERT))  # Client certificate
KEY = utils.project_file(secrets.get_secret(*secrets.Certificates.XMLRPC_CLIENT_KEY))  # Client private key

_TVM_TICKET = None


def get_user_ticket():
    res = requests.get(
        'https://blackbox.yandex-team.ru/blackbox',
        headers={
            'Authorization': 'OAuth %s' % '',
        },
        params={
            'method': 'oauth',
            'get_user_ticket': True,
        },
        cert=(CERT, KEY),
        verify=False,
    )
    res.raise_for_status()
    return res.json()


def get_tvm_ticket():
    """ Кэшируем тикет, чтобы не ходить на каждый тест за ним. Они сейчас живут по 12 часов. """
    global _TVM_TICKET
    if _TVM_TICKET is None:
        _TVM_TICKET = utils_tvm.get_tvm_ticket(
            dst_client_id=2021031,  # test ya.doc
            # dst_client_id=TvmClientIds.YB_MEDIUM,
            # dst_client_id=TvmClientIds.MDS_PROXY,
            # tvm_client_id=2001864,
            # tvm_client_id=TvmClientIds.BALANCE_TESTS,
            tvm_client_id=TvmClientIds.YB_MEDIUM,
            # secret='',
            secret=get_secret(*TvmSecrets.YB_MEDIUM_SECRET),
        )

    return _TVM_TICKET


def make_request(url, params=None):
    headers = {
        # 'Content-Type': 'application/x-www-form-urlencoded',
        'X-Ya-Service-Ticket': get_tvm_ticket(),
        # 'X-Ya-User-Ticket': '',
        # 'X-Is-Admin': 'true',
    }
    response = requests.get(
        url,
        # params={'unique_key': 999},
        headers=headers,
        cert=(CERT, KEY),
        verify=False,
        # verify='/etc/ssl/certs/ca-certificates.crt',
    )

    response.raise_for_status()
    print_res(response.json())


if __name__ == '__main__':
    print get_tvm_ticket()

    # print make_request(
    #     'https://snout-api-balance-33498.greed-branch.paysys.yandex-team.ru/v1/debug/dump',
    #     # 'http://127.0.0.1:8081/v1/debug/dump',
    # )
    # print get_tvm_ticket()

    # print make_request(
    #     # 'http://greed-tm.paysys.yandex.ru:8002/mdsproxy/get_invoice_from_mds?invoice_id=114261212',
    #     # 'https://greed-tm.paysys.yandex.net:8007/documents/invoices/114260297',
    #     'https://user-balance.greed-tm.paysys.yandex-team.ru/documents/invoices/114260297',
    # )
