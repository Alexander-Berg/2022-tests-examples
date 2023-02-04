# coding: utf-8
from btestlib.secrets import get_secret, TvmSecrets

__author__ = 'a-vasin'

from time import time

import requests
from decorator import decorator

from btestlib import utils
from btestlib.utils import cached
from simpleapi.common.utils import call_http_raw


# a-vasin: я надеялся до файла с utils не дойдет :D

# a-vasin: использовать исключительно после parametrize, да иначе то и не получится =)
@decorator
def need_clean_up(func, *args, **kwargs):
    try:
        func(*args, **kwargs)
    finally:
        import cashmachines.whitespirit_steps as steps
        steps.CMSteps.restore_cashmachines()


# a-vasin: выпилить, когда пофиксят либу для Windows и Python 2.7
@cached
def get_tvm_token_crutch():
    import os
    if os.name == 'nt':
        return run_python36_get_tvm_token()
    return get_tvm_token()


@cached
def get_tvm_token():
    import ticket_parser2 as tp2
    from ticket_parser2.api.v1 import ServiceContext

    secret = get_secret(*TvmSecrets.CASHMASHINES_SECRET)
    dst = '2010004'
    src = '2000359'
    ts = int(time())

    tvm_keys = requests.get('https://tvm-api.yandex.net/2/keys?lib_version={version}'
                            .format(version=tp2.__version__)).content.decode('ascii')

    service_context = ServiceContext(int(src), secret, tvm_keys)

    ticket_response = requests.post('https://tvm-api.yandex.net/2/ticket/',
                                    data={'grant_type': 'client_credentials',
                                          'src': src,
                                          'dst': dst,
                                          'ts': ts,
                                          'sign': service_context.sign(ts, dst)}).json()

    return ticket_response[dst]['ticket']


# a-vasin: выпилить, когда пофиксят либу для Windows и Python 2.7
@cached
def run_python36_get_tvm_token():
    from subprocess import Popen, PIPE
    process = Popen(["%PYTHON36%", utils.project_file("cashmachines/get_tvm_token.py")], shell=True, stdout=PIPE)
    token, _ = process.communicate()
    return token.strip()


def call_http_tvm(uri, params=None, method='POST', headers=None, json_data=None, cookies=None, auth_user=None,
                  cert_path=None, key_path=None, tvm_token=None, timeout=None):
    _, result_content = call_http_raw_tvm(uri, params, method, headers, json_data, cookies,
                                          auth_user, cert_path, key_path, tvm_token, timeout=timeout)
    return result_content


def call_http_raw_tvm(uri, params=None, method='POST', headers=None, json_data=None, cookies=None, auth_user=None,
                      cert_path=None, key_path=None, tvm_token=None, timeout=None):
    if tvm_token is None:
        tvm_token = get_tvm_token_crutch()

    headers = utils.copy_and_update_dict({'X-Ya-Service-Ticket': tvm_token}, headers if headers else {})
    return call_http_raw(uri, params, method, headers, json_data, cookies, auth_user,
                         cert_path, key_path, timeout=timeout)
