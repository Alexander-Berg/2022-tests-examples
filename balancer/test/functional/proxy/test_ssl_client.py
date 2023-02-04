# -*- coding: utf-8 -*-
import datetime
import time

import pytest

import balancer.test.plugin.context as mod_ctx
from balancer.test.util.stdlib.multirun import Multirun

from common import watch_client_close, connection_manager_required
from configs import SSLClientConfig, SSLServerConfig, SSLClientTcpConfig

from balancer.test.util import asserts
from balancer.test.util.process import BalancerStartError
from balancer.test.util.stream.ssl.stream import SSLServerStream
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.predef import http

from balancer.test.util.stream.io import byte
from balancer.test.util.proto.http.stream import HTTPServerStream


SERVERNAME = 'detroit'


class OpenSSLClientContext(object):
    def __init__(self):
        super(OpenSSLClientContext, self).__init__()
        self.__backend_port = None
        self.__server_socket = None

    def create_ssl_server_socket(self):
        self.__backend_port = self.manager.port.get_port()
        self.__server_socket = SSLServerStream(
            self.manager.process, port=self.__backend_port,
            key=self.certs.abs_path('default.key'), cert=self.certs.abs_path('default.crt'),
            openssl_path=self.manager.stream.openssl_path, quiet=False,
            opts=['-servername', SERVERNAME, '-tlsextdebug',
                  '-key2', self.certs.abs_path('detroit.key'), '-cert2', self.certs.abs_path('detroit.crt')])
        self.manager.resource.register(self.__server_socket)
        return self.__server_socket

    def write_backend_response(self, response):
        self.__server_socket.set_timeout(1)  # just magic
        stream = HTTPServerStream(byte.ByteReader(self.__server_socket), byte.ByteWriter(self.__server_socket))
        stream.write_response(response)

    def start_ssl_backend(self, config, key='default.key', crt='default.crt'):
        backend = self.manager.backend.start_ssl(
            config, self.certs.abs_path(key), self.certs.abs_path(crt))
        self.__backend_port = backend.server_config.port
        return backend

    def start_balancer_as_backend(self, config):
        balancer = self.start_balancer(config)
        self.__backend_port = balancer.config.port
        return balancer

    def start_ssl_client_balancer(self, config, **balancer_kwargs):
        return self.start_balancer(config(self.__backend_port, **balancer_kwargs))


ssl_client_ctx = mod_ctx.create_fixture(OpenSSLClientContext)


@watch_client_close
def test_tcp_request(ssl_client_ctx, watch_client_close):
    """
    Балансер должен по шифрованному соединению задать запрос бэкенду по tcp и получить ответ
    """
    sock = ssl_client_ctx.create_ssl_server_socket()
    request = http.request.post(path='/somewhere', data=['led', 'zeppelin'])
    response = http.response.ok(data="OK")
    backend = ssl_client_ctx.start_ssl_backend(SimpleConfig(response=response))
    ssl_client_ctx.start_ssl_client_balancer(
        SSLClientTcpConfig, ca_file=ssl_client_ctx.certs.root_ca, sni_on=1,
        watch_client_close=watch_client_close,
    )

    conn = ssl_client_ctx.create_http_connection()
    stream = conn.create_stream()
    stream.write_request(request.to_raw_request())
    time.sleep(1)
    ssl_client_ctx.write_backend_response(response.to_raw_response())
    resp = stream.read_response()
    ssl_data = sock.recv_quiet()

    asserts.status(resp, 200)
    asserts.content(resp, "OK")
    assert 'TLS client extension "server name"' not in ssl_data

    req = backend.state.get_request()
    assert req == request


@watch_client_close
def test_tcp_request_sni_host(ssl_client_ctx, watch_client_close):
    """
    Балансер должен по шифрованному соединению задать запрос бэкенду по tcp и получить ответ
    """
    sock = ssl_client_ctx.create_ssl_server_socket()
    request = http.request.post(path='/somewhere', data=['led', 'zeppelin'])
    response = http.response.ok(data="OK")
    ssl_client_ctx.start_ssl_client_balancer(
        SSLClientTcpConfig, ca_file=ssl_client_ctx.certs.root_ca, sni_on=1, sni_host=SERVERNAME,
        watch_client_close=watch_client_close,
    )

    conn = ssl_client_ctx.create_http_connection()
    stream = conn.create_stream()
    stream.write_request(request.to_raw_request())
    time.sleep(1)
    ssl_client_ctx.write_backend_response(response.to_raw_response())
    resp = stream.read_response()
    ssl_data = sock.recv_quiet()

    asserts.status(resp, 200)
    asserts.content(resp, "OK")
    assert 'TLS client extension "server name"' in ssl_data
    assert SERVERNAME in ssl_data


@watch_client_close
def test_http_request(ssl_client_ctx, watch_client_close):
    """
    Балансер должен по шифрованному соединению задать запрос бэкенду и получить ответ
    """
    request = http.request.post(path='/somewhere', data=['led', 'zeppelin'])
    backend = ssl_client_ctx.start_ssl_backend(SimpleConfig())
    ssl_client_ctx.start_ssl_client_balancer(
        SSLClientConfig, ca_file=ssl_client_ctx.certs.root_ca,
        watch_client_close=watch_client_close,
    )

    resp = ssl_client_ctx.perform_request(request)
    req = backend.state.get_request()

    asserts.status(resp, 200)
    assert req == request


@watch_client_close
def test_multiple_certs(ssl_client_ctx, watch_client_close):
    """
    Если в ca_file несколько сертификатов, то сертификат сервера должен быть подписан любым из них
    """
    detroit_cert = ssl_client_ctx.manager.fs.read_file(ssl_client_ctx.certs.abs_path('detroit.crt'))
    root_cert = ssl_client_ctx.manager.fs.read_file(ssl_client_ctx.certs.root_ca)
    ca_file = ssl_client_ctx.manager.fs.create_file('ca_file')
    ssl_client_ctx.manager.fs.rewrite(ca_file, detroit_cert + root_cert)
    ssl_client_ctx.start_ssl_backend(SimpleConfig())
    ssl_client_ctx.start_ssl_client_balancer(
        SSLClientConfig, ca_file=ca_file,
        watch_client_close=watch_client_close,
    )

    resp = ssl_client_ctx.perform_request(http.request.get())

    asserts.status(resp, 200)


@watch_client_close
def test_cert_not_verified(ssl_client_ctx, watch_client_close):
    """
    Если сертификат не является доверенным, то балансер должен разорвать соединение с бэкендом
    """
    backend = ssl_client_ctx.start_ssl_backend(SimpleConfig())
    # giving balancer wrong root certificate, so the check fails
    ssl_client_ctx.start_ssl_client_balancer(
        SSLClientConfig, ca_file=ssl_client_ctx.certs.abs_path('detroit.crt'),
        watch_client_close=watch_client_close,
    )

    ssl_client_ctx.perform_request_xfail(http.request.get())

    assert backend.state.requests.empty()


@watch_client_close
def test_verify_depth_not_enough(ssl_client_ctx, watch_client_close):
    """
    Если длина цепочки сертификатов превышает verify_depth + 1, то балансер должен
    разорвать соединение с бэкэндом.
    """
    backend = ssl_client_ctx.start_ssl_backend(SimpleConfig(), key='subclient.key', crt='subclient.crt')
    ssl_client_ctx.start_ssl_client_balancer(
        SSLClientConfig, ca_file=ssl_client_ctx.certs.root_ca, verify_depth=0,
        watch_client_close=watch_client_close,
    )

    ssl_client_ctx.perform_request_xfail(http.request.get())

    assert backend.state.requests.empty()


def test_ca_required_to_start(ssl_client_ctx):
    """
    ca_file обязателен, чтобы балансер запустился
    """
    with pytest.raises(BalancerStartError):
        ssl_client_ctx.start_ssl_client_balancer(SSLClientConfig)


def test_ca_file_exists_to_start(ssl_client_ctx):
    """
    ca_file обязателен, должен существовать и быть валидным файлом с сертификатами
    """
    broken_ca = ssl_client_ctx.manager.fs.create_file('broken_ca')
    with pytest.raises(BalancerStartError):
        ssl_client_ctx.start_ssl_client_balancer(SSLClientConfig, ca_file=broken_ca)


def test_verify_depth_nonegative_to_start(ssl_client_ctx):
    """
    verify_depth >= 0, чтобы балансер запустился
    """
    with pytest.raises(BalancerStartError):
        ssl_client_ctx.start_ssl_client_balancer(SSLClientConfig, ca_file=ssl_client_ctx.certs.root_ca, verify_depth=-1)


@watch_client_close
def test_sni_host(ssl_client_ctx, watch_client_close):
    """
    Балансер должен соединиться с бэкендом, используя sni servername, указанный в конфиге
    """
    sock = ssl_client_ctx.create_ssl_server_socket()
    ssl_client_ctx.start_ssl_client_balancer(
        SSLClientConfig, ca_file=ssl_client_ctx.certs.root_ca, sni_host=SERVERNAME,
        watch_client_close=watch_client_close,
    )

    conn = ssl_client_ctx.create_http_connection()
    stream = conn.create_stream()
    stream.write_request(http.request.get().to_raw_request())
    time.sleep(1)
    ssl_client_ctx.write_backend_response(http.response.ok().to_raw_response())
    response = stream.read_response()
    ssl_data = sock.recv_quiet()

    asserts.status(response, 200)
    assert 'TLS client extension "server name"' in ssl_data
    assert SERVERNAME in ssl_data


@watch_client_close
def test_sni_on(ssl_client_ctx, watch_client_close):
    """
    Балансер должен соединиться с бэкендом, используя sni servername из заголовка Host
    """
    sock = ssl_client_ctx.create_ssl_server_socket()
    ssl_client_ctx.start_ssl_client_balancer(
        SSLClientConfig, ca_file=ssl_client_ctx.certs.root_ca, sni_on=1,
        watch_client_close=watch_client_close,
    )

    conn = ssl_client_ctx.create_http_connection()
    stream = conn.create_stream()
    stream.write_request(http.request.get(headers={'Host': SERVERNAME}).to_raw_request())
    time.sleep(1)
    ssl_client_ctx.write_backend_response(http.response.ok().to_raw_response())
    response = stream.read_response()
    ssl_data = sock.recv_quiet()

    asserts.status(response, 200)
    assert 'TLS client extension "server name"' in ssl_data
    assert SERVERNAME in ssl_data


@watch_client_close
def test_sni_host_beats_sni_on(ssl_client_ctx, watch_client_close):
    """
    Если указаны и sni_host и sni_on, то предпочтение отдается sni_host, даже если запрос
    клиента содержит заголовок Host
    """
    sock = ssl_client_ctx.create_ssl_server_socket()
    ssl_client_ctx.start_ssl_client_balancer(
        SSLClientConfig, ca_file=ssl_client_ctx.certs.root_ca, sni_host=SERVERNAME, sni_on=1,
        watch_client_close=watch_client_close,
    )

    conn = ssl_client_ctx.create_http_connection()
    stream = conn.create_stream()
    stream.write_request(http.request.get(headers={'Host': 'vegas'}).to_raw_request())
    time.sleep(1)
    ssl_client_ctx.write_backend_response(http.response.ok().to_raw_response())
    response = stream.read_response()
    ssl_data = sock.recv_quiet()

    asserts.status(response, 200)
    assert 'TLS client extension "server name"' in ssl_data
    assert SERVERNAME in ssl_data


@watch_client_close
def test_sni_on_no_host(ssl_client_ctx, watch_client_close):
    """
    Если включена опция sni_on, но запрос клиента не содержит заголовок Host,
    то балансер не должен использовать sni servername
    """
    sock = ssl_client_ctx.create_ssl_server_socket()
    ssl_client_ctx.start_ssl_client_balancer(
        SSLClientConfig, ca_file=ssl_client_ctx.certs.root_ca, sni_on=1,
        watch_client_close=watch_client_close,
    )

    conn = ssl_client_ctx.create_http_connection()
    stream = conn.create_stream()
    stream.write_request(http.request.get().to_raw_request())
    time.sleep(1)
    ssl_client_ctx.write_backend_response(http.response.ok().to_raw_response())
    response = stream.read_response()
    ssl_data = sock.recv_quiet()

    asserts.status(response, 200)
    assert 'TLS client extension "server name"' not in ssl_data


@pytest.mark.parametrize(
    ['keepalive_count', 'backend_timeout', 'sleep_timeout', 'keepalive_timeout', 'sess_count'],
    [
        (0, 5, 0, None, 2),
        (1, 5, 0, None, 1),
        (1, 1, 2, None, 1),
        (1, 1, 2, '1s', 2),
    ],
    ids=[
        'no_keepalive',
        'keepalive',
        'timeout_keepalive',
        'keepalive_timeout',
    ],
)
@watch_client_close
@connection_manager_required
def test_keepalive_count(
    ssl_client_ctx, keepalive_count, backend_timeout, sleep_timeout,
    keepalive_timeout, sess_count, watch_client_close, connection_manager_required
):
    """
    BALANCER-893
    Tests on keepalive to backend
    """
    balancer_backend = ssl_client_ctx.start_balancer_as_backend(SSLServerConfig(
        ssl_client_ctx.certs.abs_path('default.key'), ssl_client_ctx.certs.abs_path('default.crt'),
        ssl_client_ctx.certs.root_ca,
    ))
    ssl_client_ctx.start_ssl_client_balancer(
        SSLClientConfig,
        ca_file=ssl_client_ctx.certs.root_ca,
        keepalive_count=keepalive_count,
        keepalive_timeout=keepalive_timeout,
        backend_timeout=backend_timeout,
        watch_client_close=watch_client_close,
        connection_manager_required=connection_manager_required,
    )

    tcpdump = ssl_client_ctx.manager.tcpdump.start(balancer_backend.config.port)
    resp1 = ssl_client_ctx.perform_request(http.request.get())
    time.sleep(sleep_timeout)
    resp2 = ssl_client_ctx.perform_request(http.request.get())

    asserts.status(resp1, 200)
    asserts.status(resp2, 200)
    for run in Multirun(sum_delay=3):
        with run:
            tcpdump.read_all()
            sessions = tcpdump.get_sessions()
            assert len(sessions) == sess_count


@connection_manager_required
def test_keepalive_check_unexpected_data(ssl_client_ctx, connection_manager_required):
    response = http.response.ok(data="OK")
    sock = ssl_client_ctx.create_ssl_server_socket()
    ssl_client_ctx.start_ssl_client_balancer(
        SSLClientConfig,
        ca_file=ssl_client_ctx.certs.root_ca,
        keepalive_count=1,
        keepalive_check_for_unexpected_data=True,
        connection_manager_required=connection_manager_required,
    )

    conn = ssl_client_ctx.create_http_connection()
    stream = conn.create_stream()
    stream.write_request(http.request.get().to_raw_request())
    time.sleep(1)
    ssl_client_ctx.write_backend_response(response.to_raw_response())
    resp = stream.read_response()
    sock.recv_quiet()

    asserts.status(resp, 200)
    unistat = ssl_client_ctx.get_unistat()
    assert unistat['proxy-unused_keepalives_ammv'] == 1
    byte.ByteWriter(sock).write('unexpected data')  # typically, it could be close_notify
    time.sleep(1)
    if connection_manager_required:
        # assure, connection had been closed by balancer before attempt to reuse
        unistat = ssl_client_ctx.get_unistat()
        assert unistat['proxy-unused_keepalives_ammv'] == 0
    else:
        # close connection by openssl s_server itself to allow new session
        byte.ByteWriter(sock).write('q')

    stream.write_request(http.request.get().to_raw_request())
    time.sleep(1)
    ssl_client_ctx.write_backend_response(response.to_raw_response())
    resp = stream.read_response()
    sock.recv_quiet()

    asserts.status(resp, 200)


@watch_client_close
@connection_manager_required
def test_keepalive_shutdown(ssl_client_ctx, watch_client_close, connection_manager_required):
    """
    BALANCER-1696
    Test shutdown with keepalive to ssl backend
    """
    ssl_client_ctx.start_balancer_as_backend(SSLServerConfig(
        ssl_client_ctx.certs.abs_path('default.key'), ssl_client_ctx.certs.abs_path('default.crt'),
        ssl_client_ctx.certs.root_ca))
    ssl_client_ctx.start_ssl_client_balancer(
        SSLClientConfig,
        ca_file=ssl_client_ctx.certs.root_ca,
        keepalive_count=1,
        watch_client_close=watch_client_close,
        connection_manager_required=connection_manager_required,
    )

    resp = ssl_client_ctx.perform_request(http.request.get())
    asserts.status(resp, 200)

    ssl_client_ctx.perform_request(http.request.get('/admin?action=shutdown'), port=ssl_client_ctx.balancer.config.admin_port)

    shutdown_start = datetime.datetime.now()
    delta = datetime.timedelta(seconds=60)

    while datetime.datetime.now() - shutdown_start < delta:
        if ssl_client_ctx.balancer.is_alive():
            time.sleep(0.1)
        else:
            break

    assert not ssl_client_ctx.balancer.is_alive()
    assert ssl_client_ctx.balancer.return_code == 0
    ssl_client_ctx.balancer.set_finished()
