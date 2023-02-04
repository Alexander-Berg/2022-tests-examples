# -*- coding: utf-8 -*-
import time

import balancer.test.plugin.context as mod_ctx

from configs import ThdbVersionConfig

from balancer.test.util import asserts
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.predef import http


VERSION_FILE_NAME = 'thdbversion.txt'
HEADER_NAME = 'x-thdb-version'


class ThdbContext(object):
    def __init__(self):
        super(ThdbContext, self).__init__()
        self.version_file_path = self.manager.fs.create_file(VERSION_FILE_NAME)
        self.version_file = None

    def write_version(self, version, filename=None):
        if version is not None:
            self.manager.fs.rewrite(self.version_file_path, version)

    def start_all(self, version_file_name=None, **balancer_kwargs):
        self.start_backend(SimpleConfig())
        balancer = self.start_balancer(ThdbVersionConfig(
            self.backend.server_config.port,
            version_file_name or self.version_file_path,
            **balancer_kwargs))
        time.sleep(5)  # wait till balancer will read version file at startup
        return balancer


thdb_ctx = mod_ctx.create_fixture(ThdbContext)


def test_header(thdb_ctx):
    """
    Проверяем, что отправляется корректный заголовок
    """

    version = 'ver-123'

    thdb_ctx.write_version(version=version)
    thdb_ctx.start_all()

    thdb_ctx.perform_request(http.request.get(path='/i?id=123'))

    client_req = thdb_ctx.backend.state.get_request()
    asserts.single_header(client_req, HEADER_NAME)
    asserts.header_value(client_req, HEADER_NAME, version)


def test_client_header(thdb_ctx):
    """
    Проверяем, что балансер правильно обрабатывает ситуацию, когда клиент посылает такой же заговоловок
    """

    version = 'ver-123'

    thdb_ctx.write_version(version=version)
    thdb_ctx.start_all()

    client_header_value = 'client_header_value'

    thdb_ctx.perform_request(http.request.get(path='/i?id=123', headers={HEADER_NAME: client_header_value}))

    client_req = thdb_ctx.backend.state.get_request()
    asserts.single_header(client_req, HEADER_NAME)
    asserts.header_value(client_req, HEADER_NAME, version)


def test_retry_empty_version(thdb_ctx):
    """
    Проверяем, что балансер не отправляет заголовок, если файла нет
    """
    thdb_ctx.start_all(version_file_name='dummy.txt')

    thdb_ctx.perform_request(http.request.get(path='/i?id=123'))
    asserts.no_header(thdb_ctx.backend.state.get_request(), HEADER_NAME)


def test_file_read_timeout(thdb_ctx):
    """
    Проверяем, что балансер читает файл с версией по таймауту
    """
    version1 = '1'
    timeout = 5
    thdb_ctx.write_version(version=version1)
    balancer = thdb_ctx.start_all(file_read_timeout='{}s'.format(timeout))

    version2 = '2'

    with thdb_ctx.create_http_connection(balancer.config.port) as conn:
        request = http.request.get(path='/i?id=123')
        conn.perform_request(request)
        asserts.header_value(thdb_ctx.backend.state.get_request(), HEADER_NAME, version1)

        thdb_ctx.write_version(version=version2)
        time.sleep(10)

        conn.perform_request(request)
        asserts.header_value(thdb_ctx.backend.state.get_request(), HEADER_NAME, version2)


def test_version_correctness(thdb_ctx):
    """
    Проверяем, что балансер игнорирует некорректную версию в файле
    """

    thdb_ctx.write_version(version='Hello,\x00\x01\x82World!')
    thdb_ctx.start_all()

    request = http.request.get(path='/i?id=123')
    thdb_ctx.perform_request(request)
    asserts.no_header(thdb_ctx.backend.state.get_request(), HEADER_NAME)


def test_user_header_incorrect_version(thdb_ctx):
    """
    Пользовательские заколовки X-Thdb-Version должны быть удалены
    """
    thdb_ctx.write_version(version='Hello,\x00\x01\x82World!')
    thdb_ctx.start_all()

    headers = {'X-Thdb-Version-Y': 'big', 'Y-X-Thdb-Version': 'city',
               'Y-X-Thdb-Version-Y': 'nights'}

    request = http.request.get(path='/i?id=123', headers=headers)
    thdb_ctx.perform_request(request)
    backend_request = thdb_ctx.backend.state.get_request()
    asserts.no_header(backend_request, HEADER_NAME)
    asserts.headers_values(backend_request, headers)


def test_version_max_len(thdb_ctx):
    """
    Проверяем, что балансер отправляет заголовок максимальной длины - 256 байт
    """

    version = 'w' * 256

    thdb_ctx.write_version(version=version)
    thdb_ctx.start_all()

    request = http.request.get(path='/i?id=123')
    thdb_ctx.perform_request(request)
    asserts.header_value(thdb_ctx.backend.state.get_request(), HEADER_NAME, version)


def test_version_greater_then_max_len(thdb_ctx):
    """
    Проверяем, что балансер игнорирует слишком длинную (> 256) версию в файле
    """

    version = 'w' * 257

    thdb_ctx.write_version(version=version)
    thdb_ctx.start_all()

    request = http.request.get(path='/i?id=123')
    thdb_ctx.perform_request(request)
    asserts.no_header(thdb_ctx.backend.state.get_request(), HEADER_NAME)
