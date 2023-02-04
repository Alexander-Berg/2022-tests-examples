# coding: utf-8
import time

import requests
import ticket_parser2 as tp2
from ticket_parser2.api.v1 import ServiceContext

from btestlib import reporter
from btestlib.secrets import get_secret, TvmSecrets
from btestlib.constants import TvmClientIds


def get_tvm_ticket_cloud():
    get_tvm_ticket(dst_client_id=TvmClientIds.YB_MEDIUM,
                   tvm_client_id=TvmClientIds.BALANCE_TESTS,
                   secret=get_secret(*TvmSecrets.BALANCE_TESTS_SECRET))


def get_tvm_ticket(dst_client_id, tvm_client_id, secret):
    with reporter.step(u'Получаем тикет tvm'):
        tvm_api_url = 'tvm-api.yandex.net'
        tvm_keys = requests.get(
            'https://{tvm_api_url}/2/keys?lib_version={version}'.format(
                tvm_api_url=tvm_api_url, version=tp2.__version__)).content
        ts = int(time.time())
        service_context = ServiceContext(tvm_client_id, secret, tvm_keys)
        ticket_response = requests.post(
            'https://%s/2/ticket/' % tvm_api_url,
            data={
                'grant_type': 'client_credentials',
                'src': tvm_client_id,
                'dst': dst_client_id,
                'ts': ts,
                'sign': service_context.sign(ts, dst_client_id)
            }
        ).json()

        return ticket_response[str(dst_client_id)]['ticket']
