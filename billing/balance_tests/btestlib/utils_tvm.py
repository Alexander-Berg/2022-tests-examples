# coding: utf-8
import time
import xmlrpclib
import collections

import requests

from btestlib import reporter
from btestlib.secrets import get_secret, TvmSecrets


_TICKETS_CACHE = {}

# Требуется обновлять тикеты каждый час
CACHE_TIMEOUT = 1 * 60 * 60

CacheKey = collections.namedtuple('CacheKey', 'tvm_client_id dst_client_id')
CacheValue = collections.namedtuple('CacheValue', 'update_dt value')


def _get_tvm_ticket(dst_client_id, tvm_client_id, secret):
    # import здесь, т.к. у людей есть проблемы с установкой либы ticket_parser2
    import ticket_parser2 as tp2
    from ticket_parser2.api.v1 import ServiceContext

    # todo-igogor доделать нормальный логгинг
    with reporter.step(u'Получаем тикет tvm'):
        tvm_api_url = 'tvm-api.yandex.net'
        tvm_keys = requests.get(
            'https://{tvm_api_url}/2/keys?lib_version={version}'.format(
                tvm_api_url=tvm_api_url, version=tp2.__version__)).content
        ts = int(time.time())

        service_context = ServiceContext(tvm_client_id, secret, tvm_keys)

        request_data = {
            'grant_type': 'client_credentials',
            'src': tvm_client_id,
            'dst': dst_client_id,
            'ts': ts,
            'sign': service_context.sign(ts, dst_client_id)
        }
        reporter.attach(u'Тело запроса', request_data)
        ticket_response = requests.post(
            'https://%s/2/ticket/' % tvm_api_url,
            data=request_data
        ).json()
        reporter.attach(u'Ответ', ticket_response)

        return ticket_response[str(dst_client_id)]['ticket']


def get_tvm_ticket(dst_client_id, tvm_client_id, secret, refresh=False):
    """
    Кешируем каждый полученный тикет, чтобы не ходить за ними каждое обращение.
    """
    ticket_cache_key = CacheKey(tvm_client_id, dst_client_id)
    if not refresh and ticket_cache_key in _TICKETS_CACHE:
        cache_value = _TICKETS_CACHE[ticket_cache_key]
        if time.time() - cache_value.update_dt < CACHE_TIMEOUT:
            return cache_value.value

    ticket = _get_tvm_ticket(dst_client_id, tvm_client_id, secret)
    _TICKETS_CACHE[ticket_cache_key] = CacheValue(update_dt=time.time(), value=ticket)
    return ticket


def get_tvm_ticket_cloud():
    # Циклический импорт
    from btestlib.constants import TvmClientIds

    return get_tvm_ticket(dst_client_id=TvmClientIds.YB_MEDIUM, tvm_client_id=TvmClientIds.BALANCE_TESTS,
                          secret=get_secret(*TvmSecrets.BALANCE_TESTS_SECRET))


def supply_tvm_ticket(headers):
    headers['X-Ya-Service-Ticket'] = get_tvm_ticket_cloud()
    return headers


class TvmTransport(xmlrpclib.SafeTransport, object):
    """ Добавляет заголовок с TVM-ключом в запрос """
    def __init__(self, use_datetime=0, tvm_ticket=None):
        super(TvmTransport, self).__init__(use_datetime)
        self.tvm_ticket = tvm_ticket

    def send_user_agent(self, connection):
        xmlrpclib.Transport.send_user_agent(self, connection)
        if self.tvm_ticket:
            connection.putheader("X-Ya-Service-Ticket", self.tvm_ticket)
