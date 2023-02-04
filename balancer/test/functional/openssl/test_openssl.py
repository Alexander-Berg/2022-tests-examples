# -*- coding: utf-8 -*-
# pylint: disable=redefined-outer-name
import pytest

import re
import time
import datetime
import socket
from collections import Counter, namedtuple

import balancer.test.plugin.context as mod_ctx
from balancer.test.util.proto.http2.framing import frames, flags
from balancer.test.util.stdlib.multirun import Multirun

from configs import OpenSSLSimpleConfig, OpenSSLComplexConfig, OpenSSLH2Config,\
    OpenSSLDualCertsConfig, OpenSSLComplexDualCertsConfig, OpenSSLSimpleClientConfig, OpenSSLDualCertsClientConfig,\
    OpenSSLMultictxClientConfig, OpenSSLMaxSendFragmentConfig, OpenSSLExpContextsConfig
from balancer.test.util.stream.ssl.stream import SSLClientOptions
from balancer.test.util.stream.ssl import parse
from balancer.test.util.predef import http
from balancer.test.util.predef import http2
from balancer.test.util.process import BalancerStartError
from balancer.test.util.proto.http.stream import HTTPReaderException

from balancer.test.util.balancer import asserts

from . import earlydata

RELOAD_FORMAT = 'Reloading %s... %s'
RELOAD_MATCHER = 'Reloading '
VALIDATION_FORMAT = 'Validating %s... %s'
VALIDATION_MATCHER = 'Validating '


def assert_closed(conn):
    for run in Multirun(sum_delay=5):
        with run:
            asserts.is_closed(conn.sock)


class SSLContext(object):
    def __init__(self):
        super(SSLContext, self).__init__()
        self.__simple_config_class = self.request.param[0]
        self.__complex_config_class = self.request.param[1]
        self.__simple_ocsp_count = self.request.param[2]

    @property
    def simple_ocsp_count(self):
        return self.__simple_ocsp_count

    def start_simple_balancer(self, **kwargs):
        self.start_balancer(self.__simple_config_class(self.certs.root_dir, **kwargs))
        self.wait_ssllog_nonempty(self.balancer.config.log)
        return self.balancer

    def start_complex_balancer(self, **kwargs):
        self.start_balancer(self.__complex_config_class(self.certs.root_dir, **kwargs))
        self.wait_ssllog_nonempty(self.balancer.config.default_log)
        return self.balancer

    def start_h2_balancer(self, **kwargs):
        self.start_balancer(OpenSSLH2Config(self.certs.root_dir, **kwargs))
        self.wait_ssllog_nonempty(self.balancer.config.log)
        return self.balancer

    def create_https_connection(self, port=None, check_closed=True, conn_timeout=None, **ssl_options):
        if port is None:
            port = self.balancer.config.port
        if 'quiet' not in ssl_options:
            ssl_options['quiet'] = False
        if 'ca_file' not in ssl_options:
            ssl_options['ca_file'] = self.certs.root_ca
        return self.manager.connection.http.create_ssl(
            port,
            SSLClientOptions(**ssl_options),
            check_closed=check_closed,
            conn_timeout=conn_timeout,
        )

    def create_secondary_https_connection(self, **ssl_options):
        return self.create_https_connection(ca_file=self.certs.abs_path('old_ca.crt'), cipher='ECDHE-RSA-AES256-SHA', **ssl_options)

    def create_h2_ssl_connection(self, port=None, h2=True, **ssl_options):
        if port is None:
            port = self.balancer.config.port
        if 'quiet' not in ssl_options:
            ssl_options['quiet'] = False
        if h2:
            return self.manager.connection.http2.create_ssl(
                port,
                SSLClientOptions(ca_file=self.certs.root_ca, **ssl_options),
                host='[::1]'
            )
        else:
            return self.manager.connection.http.create_ssl(
                port,
                SSLClientOptions(**ssl_options),
                check_closed=True,
                host='[::1]'
            )

    def assert_cert(self, conn, cert_name):
        cert_path = self.certs.abs_path(cert_name)
        cert = parse.cert(cert_path)
        assert cert == conn.sock.handshake_info.server_cert

    def assert_no_ocsp(self, conn):
        assert not conn.sock.handshake_info.has_ocsp

    def assert_ocsp(self, conn, resp_name):
        ocsp_data = parse.ocsp_response(self.certs.abs_path(resp_name))
        assert ocsp_data == conn.sock.handshake_info.ocsp_data

    def assert_ticket(self, conn, ticket_name):
        ticket_id = parse.ticket_key(self.certs.abs_path(ticket_name))
        assert ticket_id == conn.sock.handshake_info.ticket_id

    def __split_log(self, lines, has_pid):
        if (len(lines) == 0) or (not has_pid):
            return [], lines
        lines = [line.split(' ', 1) for line in lines]
        return zip(*lines)

    def assert_reload_status_lines(self, lines, exp_statuses, count=None, has_pid=False):
        self.__assert_resp_status_lines(lines, exp_statuses, count, has_pid, RELOAD_FORMAT, RELOAD_MATCHER)

    def assert_validation_status_lines(self, lines, exp_statuses, count=None, has_pid=False):
        self.__assert_resp_status_lines(lines, exp_statuses, count, has_pid, VALIDATION_FORMAT, VALIDATION_MATCHER)

    def __assert_resp_status_lines(self, lines, exp_statuses, count=None, has_pid=False, fmt=None, match=None):
        if count is None:
            count = 1

        pids, lines = self.__split_log(lines, has_pid)

        expected = Counter()
        for file_name, status in exp_statuses:
            key = fmt % (self.certs.abs_path(file_name), status)
            expected[key] += count

        parsed_lines = []
        for l in lines:
            if match in l:
                parsed_lines.append(l)
        result = Counter(parsed_lines)
        assert result == expected

    def assert_reload_status(self, response, exp_statuses, count=None, has_pid=False):
        self.assert_reload_status_lines(response.data.content.splitlines(), exp_statuses, count, has_pid=has_pid)

    def assert_validation_status(self, response, exp_statuses, count=None, has_pid=False):
        self.assert_validation_status_lines(response.data.content.splitlines(), exp_statuses, count, has_pid=has_pid)

    def assert_ssllog(self, log, msg, count, has_pid=False):
        for run in Multirun():
            with run:
                lines = [line.split(' ', 1)[1][:-1] for line in self.manager.fs.read_lines(log)]
                pids, lines = self.__split_log(lines, has_pid)
                cnt = len(filter(lambda x: msg in x, lines))
                assert cnt == count

    def assert_ssllog_ocsp_ok(self, log, count):
        self.assert_ssllog(log, 'ocsp response check ok, storing ocsp response', count, has_pid=True)

    def assert_ssllog_no_ocsp(self, log, file_name, count):
        file_path = self.certs.abs_path(file_name)
        msg = 'ocsp response reload failed, error reading file "%s": ' % file_path
        self.assert_ssllog(log, msg, count, has_pid=True)

    def assert_ssllog_broken_ocsp(self, log, count, empty=True):
        msg = 'ocsp response check failed, using the previous response'
        if empty:
            msg += ' (empty)'
        self.assert_ssllog(log, msg, count, has_pid=True)

    def assert_ssllog_ticket_ok(self, log, count):
        self.assert_ssllog(
            log,
            'tls session ticket key was reloaded successfully, enabling tls session ticket extension',
            count)

    def assert_ssllog_ticket_restored(self, log, count):
        self.assert_ssllog(
            log,
            'tls session ticket key was restored successfully, enabling tls session ticket extension',
            count)

    def assert_ssllog_ticket_fail(self, log, file_name, count):
        file_path = self.certs.abs_path(file_name)
        msg = 'failed to read tls key %s' % file_path
        self.assert_ssllog(log, msg, count)

    def assert_ssllog_ticket_disable(self, log, count):
        self.assert_ssllog(
            log,
            'fail in reloading tls session ticket key: disabling tls session ticket extension',
            count)

    def assert_ssllog_ticket_validate(self, log, count):
        self.assert_ssllog(
            log,
            'validatation of tls session keys was successfull',
            count)

    def wait_ssllog_nonempty(self, log):
        for run in Multirun():
            with run:
                assert len(self.manager.fs.read_lines(log)) > 0

    # the same as assert_*, but used to make tests more stable
    wait_ssllog_ocsp_ok = assert_ssllog_ocsp_ok
    wait_ssllog_ticket_ok = assert_ssllog_ticket_ok
    wait_ssllog_ticket_restored = assert_ssllog_ticket_restored
    wait_ssllog_ticket_disable = assert_ssllog_ticket_disable
    wait_ssllog_ticket_validate = assert_ssllog_ticket_validate

    def reload_file(self, event):
        with self.manager.connection.http.create(self.balancer.config.admin_port) as conn:
            return conn.perform_request(http.request.get(path='/admin/events/call/%s' % event))

    def reload_ocsp(self, name='default'):
        return self.reload_file('%s_reload_ocsp' % name)

    def reload_tickets(self, name='default', force=False):
        if force:
            return self.reload_file('%s_force_reload_tickets' % name)
        else:
            return self.reload_file('%s_reload_tickets' % name)

    def do_requests(self, count, cipher=None, server_name=None):
        ssl_options = dict()
        ssl_options['quiet'] = True
        if cipher is not None:
            ssl_options['cipher'] = cipher
        if server_name is not None:
            ssl_options['server_name'] = server_name
        for _ in range(count):
            with self.create_https_connection(**ssl_options) as conn:
                conn.perform_request(http.request.get())


ssl_ctx = mod_ctx.create_fixture(
    SSLContext,
    params=[
        (OpenSSLSimpleConfig, OpenSSLComplexConfig, 1),
        (OpenSSLDualCertsConfig, OpenSSLComplexDualCertsConfig, 2),
    ],
    ids=[
        'single',
        'dualcerts',
    ],
)


ssl_single_ctx = mod_ctx.create_fixture(
    SSLContext,
    params=[(OpenSSLSimpleConfig, OpenSSLComplexConfig, 1)],
)


ssl_dualcerts_ctx = mod_ctx.create_fixture(
    SSLContext,
    params=[(OpenSSLDualCertsConfig, OpenSSLComplexDualCertsConfig, 2)],
)


ssl_client_ctx = mod_ctx.create_fixture(
    SSLContext,
    params=[
        (OpenSSLSimpleClientConfig, None, None),
        (OpenSSLDualCertsClientConfig, None, None),
        (OpenSSLMultictxClientConfig, None, None),
    ],
    ids=[
        'single_client',
        'dualcerts_client',
        'multictx_client',
    ],
)


ssl_dualcerts_client_ctx = mod_ctx.create_fixture(
    SSLContext,
    params=[(OpenSSLDualCertsClientConfig, None, None)],
)


ssl_multictx_client_ctx = mod_ctx.create_fixture(
    SSLContext,
    params=[(OpenSSLMultictxClientConfig, None, None)],
)


ssl_max_send_fragment_ctx = mod_ctx.create_fixture(
    SSLContext,
    params=[(OpenSSLMaxSendFragmentConfig, None, None)],
)


ssl_exp_ctx = mod_ctx.create_fixture(
    SSLContext,
    params=[(None, OpenSSLExpContextsConfig, None)],
)


def copy_tickets(certs, name, count):
    for i in range(count):
        path_format = certs.abs_path('%s_ticket.%d.' % (name, i))
        src_name = path_format + 'raw'
        dst_name = path_format + 'key'
        certs.copy(src_name, dst_name)


@pytest.fixture(autouse=True)
def setup_certs(certs):
    copy_tickets(certs, 'default', 3)
    copy_tickets(certs, 'detroit', 1)
    copy_tickets(certs, 'vegas', 1)


@pytest.mark.parametrize('no_ca', [None, True], ids=['ca', 'no_ca'])
def test_cert_verified(ssl_ctx, no_ca):
    """
    Проверка правильности сертификата
    """
    ssl_ctx.start_simple_balancer(no_ca=no_ca)
    info = ssl_ctx.create_https_connection().sock.handshake_info
    assert info.verified


@pytest.mark.parametrize('force_ssl', [None, False, True], ids=['no_force_ssl', 'force_ssl_false', 'force_ssl_true'])
def test_http_request(ssl_ctx, force_ssl):
    """
    Задать HTTP запрос и получить ответ по шифрованному соединению
    """
    ssl_ctx.start_simple_balancer(force_ssl=force_ssl)
    with ssl_ctx.create_https_connection() as conn:
        response = conn.perform_request(http.request.get())

    asserts.content(response, 'Hello')


def test_insecure_http_request(ssl_ctx):
    """
    Задать HTTP запрос без tls-обвязки и получить HTTP-ответ
    по нешифрованному соединению, если force_ssl = false
    """
    ssl_ctx.start_simple_balancer(force_ssl=False)
    response = ssl_ctx.perform_request(http.request.get())

    asserts.content(response, 'Hello')


@pytest.mark.parametrize('force_ssl', [None, True], ids=['no_force_ssl', 'force_ssl_true'])
def test_insecure_http_request_force_ssl(ssl_ctx, force_ssl):
    """
    Задать HTTP запрос без tls-обвязки и получить
    обрыв соединения, если force_ssl = true
    """
    ssl_ctx.start_simple_balancer(force_ssl=force_ssl)
    with pytest.raises((socket.error, HTTPReaderException)):
        ssl_ctx.perform_request(http.request.get())


@pytest.mark.parametrize('no_ca', [None, True], ids=['ca', 'no_ca'])
def test_secondary_cert(ssl_dualcerts_ctx, no_ca):
    """
    Если приходит клиент с ciphersuite = SHA,
    то надо отдать сертификат из secondary.
    """
    ssl_dualcerts_ctx.start_simple_balancer(no_ca=no_ca)
    info = ssl_dualcerts_ctx.create_secondary_https_connection().sock.handshake_info
    assert info.verified


@pytest.mark.parametrize('no_ca', [None, True], ids=['ca', 'no_ca'])
def test_ocsp_stapling(ssl_ctx, no_ca):
    """
    Проверка OCSP stapling
    """
    ssl_ctx.start_simple_balancer(no_ca=no_ca)
    conn = ssl_ctx.create_https_connection()
    assert conn.sock.handshake_info.ocsp_verified
    ssl_ctx.assert_ocsp(conn, 'default_ocsp.0.der')


@pytest.mark.parametrize('no_ca', [None, True], ids=['ca', 'no_ca'])
def test_ocsp_stapling_secondary(ssl_dualcerts_ctx, no_ca):
    """
    Проверка OCSP stapling, отдаваемого из secondary
    """
    ssl_dualcerts_ctx.start_simple_balancer(no_ca=no_ca)
    conn = ssl_dualcerts_ctx.create_secondary_https_connection()
    assert conn.sock.handshake_info.ocsp_verified
    ssl_dualcerts_ctx.assert_ocsp(conn, 'old_ocsp.0.der')


def test_ocsp_stapling_file_switch(ssl_ctx):
    """
    BALANCER-931
    Ocsp stapling выключается файловой ручкой ocsp_file_switch
    """
    file_switch = ssl_ctx.manager.fs.create_file('file_switch')
    ssl_ctx.start_simple_balancer(ocsp_file_switch=file_switch)
    ssl_ctx.manager.fs.rewrite(file_switch, '')

    conn = ssl_ctx.create_https_connection()
    ssl_ctx.assert_no_ocsp(conn)

    ssl_ctx.manager.fs.remove(file_switch)
    time.sleep(2)

    conn = ssl_ctx.create_https_connection()
    assert conn.sock.handshake_info.ocsp_verified
    ssl_ctx.assert_ocsp(conn, 'default_ocsp.0.der')


def test_ocsp_stapling_file_switch_secondary(ssl_dualcerts_ctx):
    """
    BALANCER-931
    Ocsp stapling выключается файловой ручкой ocsp_file_switch
    """
    file_switch = ssl_dualcerts_ctx.manager.fs.create_file('file_switch')
    old_file_switch = ssl_dualcerts_ctx.manager.fs.create_file('old_file_switch')
    ssl_dualcerts_ctx.start_simple_balancer(ocsp_file_switch=file_switch, old_ocsp_file_switch=old_file_switch)
    ssl_dualcerts_ctx.manager.fs.rewrite(file_switch, '')
    ssl_dualcerts_ctx.manager.fs.rewrite(old_file_switch, '')
    time.sleep(2)

    # all on, no ocsp at all
    conn = ssl_dualcerts_ctx.create_https_connection()
    ssl_dualcerts_ctx.assert_no_ocsp(conn)

    conn = ssl_dualcerts_ctx.create_secondary_https_connection()
    ssl_dualcerts_ctx.assert_no_ocsp(conn)

    # erase primary switch, primary has ocsp, secondary doesn't
    ssl_dualcerts_ctx.manager.fs.remove(file_switch)
    time.sleep(1.5)

    conn = ssl_dualcerts_ctx.create_https_connection()
    assert conn.sock.handshake_info.ocsp_verified
    ssl_dualcerts_ctx.assert_ocsp(conn, 'default_ocsp.0.der')

    conn = ssl_dualcerts_ctx.create_secondary_https_connection()
    ssl_dualcerts_ctx.assert_no_ocsp(conn)

    # erase both, secondary has ocsp
    ssl_dualcerts_ctx.manager.fs.remove(old_file_switch)
    time.sleep(1.5)

    conn = ssl_dualcerts_ctx.create_secondary_https_connection()
    assert conn.sock.handshake_info.ocsp_verified
    ssl_dualcerts_ctx.assert_ocsp(conn, 'old_ocsp.0.der')


def test_swap_certs(ssl_dualcerts_ctx):
    """
    Если основоной и secondary сертификаты перепутаны местами,
    то балансер все равно должен новым клиентам отдавать SHA256 сертификат, а старым SHA1
    """
    ssl_dualcerts_ctx.start_simple_balancer(
        ca=ssl_dualcerts_ctx.certs.abs_path('old_ca.crt'),
        cert=ssl_dualcerts_ctx.certs.abs_path('old.crt'),
        priv=ssl_dualcerts_ctx.certs.abs_path('old.key'),
        ocsp=ssl_dualcerts_ctx.certs.abs_path('old_ocsp.0.der'),
        old_ca=ssl_dualcerts_ctx.certs.abs_path('root_ca.crt'),
        old_cert=ssl_dualcerts_ctx.certs.abs_path('default.crt'),
        old_priv=ssl_dualcerts_ctx.certs.abs_path('default.key'),
        old_ocsp=ssl_dualcerts_ctx.certs.abs_path('default_ocsp.0.der')
    )
    conn = ssl_dualcerts_ctx.create_https_connection()
    conn_scnd = ssl_dualcerts_ctx.create_secondary_https_connection()

    assert conn.sock.handshake_info.verified
    assert conn.sock.handshake_info.ocsp_verified
    ssl_dualcerts_ctx.assert_ocsp(conn, 'default_ocsp.0.der')
    assert conn_scnd.sock.handshake_info.verified
    assert conn_scnd.sock.handshake_info.ocsp_verified
    ssl_dualcerts_ctx.assert_ocsp(conn_scnd, 'old_ocsp.0.der')


def test_secure_renegotiation(ssl_ctx):
    """
    Проверка Secure Renegotiation
    """
    ssl_ctx.start_simple_balancer()
    info = ssl_ctx.create_https_connection().sock.handshake_info
    assert info.secure_renegotiation


def test_sessions(ssl_ctx):
    """
    Проверка кеширования сессий
    """
    ssl_ctx.start_simple_balancer()

    conn1 = ssl_ctx.create_https_connection(no_ticket=True)
    conn1.close()
    conn2 = ssl_ctx.create_https_connection(no_ticket=True, sess_in=conn1.sock.ssl_options.sess_out)
    conn2.close()

    info1 = conn1.sock.handshake_info
    info2 = conn2.sock.handshake_info

    assert info1.session_id == info2.session_id
    assert not info1.has_ticket
    assert not info2.has_ticket


def test_tickets(ssl_ctx):
    """
    Проверка TLS tickets
    """
    ssl_ctx.start_simple_balancer()
    conn = ssl_ctx.create_https_connection()
    ssl_ctx.assert_ticket(conn, 'default_ticket.0.raw')


def test_secondary_tickets(ssl_dualcerts_ctx):
    """
    Проверка TLS tickets при использовании secondary сертификата
    """
    ssl_dualcerts_ctx.start_simple_balancer()
    conn = ssl_dualcerts_ctx.create_secondary_https_connection()
    ssl_dualcerts_ctx.assert_ticket(conn, 'default_ticket.0.raw')


def test_tickets_reuse(ssl_ctx):
    """
    Проверка, что соединение переиспользуется с использованием TLS tickets
    """
    balancer_simple = ssl_ctx.start_simple_balancer()
    balancer_complex = ssl_ctx.start_complex_balancer()

    conn1 = ssl_ctx.create_https_connection(port=balancer_simple.config.port)
    conn2 = ssl_ctx.create_https_connection(
        port=balancer_complex.config.port,
        sess_in=conn1.sock.ssl_options.sess_out)
    info1 = conn1.sock.handshake_info
    info2 = conn2.sock.handshake_info

    assert info2.reused
    assert info1.ticket_data == info2.ticket_data


def test_tickets_timeout(ssl_ctx):
    """
    Проверка таймаутов TLS tickets
    """
    timeout = 1
    ssl_ctx.start_simple_balancer(timeout=timeout)

    conn1 = ssl_ctx.create_https_connection()
    conn1.close()
    time.sleep(timeout + 1)
    conn2 = ssl_ctx.create_https_connection(sess_in=conn1.sock.ssl_options.sess_out)
    info1 = conn1.sock.handshake_info
    info2 = conn2.sock.handshake_info

    assert info1.ticket_ttl == str(timeout)
    assert not info2.reused


def base_tickets_event_test(ssl_ctx, workers=None):
    ssl_ctx.start_simple_balancer(workers=workers)

    conn1 = ssl_ctx.create_https_connection()
    conn1.close()
    ssl_ctx.certs.copy('default_ticket.0.pem', 'default_ticket.0.key')
    response = ssl_ctx.reload_tickets()
    conn2 = ssl_ctx.create_https_connection()

    ssl_ctx.assert_ticket(conn2, 'default_ticket.0.pem')
    ssl_ctx.assert_reload_status(response, [('default_ticket.0.key', 'OK')], count=workers)


def test_tickets_event(ssl_ctx):
    """
    Проверка, что тикет перечитается после получения event-а
    """
    base_tickets_event_test(ssl_ctx)


def test_tickets_event_multiprocess(ssl_ctx):
    """
    Проверка перечитывания тикетов в многопроцессном режиме
    """
    base_tickets_event_test(ssl_ctx, 2)


@pytest.mark.parametrize('workers', [None, 2], ids=['singleprocess', 'multiprocess'])
def test_ocsp_event(ssl_single_ctx, workers):
    """
    Проверка, что OCSP response перечитывается после получения event-а
    """
    ssl_single_ctx.start_simple_balancer(workers=workers)
    if workers is None:
        workers = 1
    ssl_single_ctx.wait_ssllog_ocsp_ok(ssl_single_ctx.balancer.config.log, workers * ssl_single_ctx.simple_ocsp_count)

    conn1 = ssl_single_ctx.create_https_connection()
    conn1.close()
    ssl_single_ctx.certs.copy('default_ocsp.1.der', 'default_ocsp.0.der')
    response = ssl_single_ctx.reload_ocsp()
    expected_ocsp_count = workers * ssl_single_ctx.simple_ocsp_count + workers
    ssl_single_ctx.assert_ssllog_ocsp_ok(ssl_single_ctx.balancer.config.log, expected_ocsp_count)
    conn2 = ssl_single_ctx.create_https_connection()

    ssl_single_ctx.assert_ocsp(conn2, 'default_ocsp.1.der')
    ssl_single_ctx.assert_reload_status(response, [('default_ocsp.0.der', 'OK')], count=workers, has_pid=True)


@pytest.mark.parametrize('workers', [None, 2], ids=['singleprocess', 'multiprocess'])
def test_dualcerts_ocsp_event(ssl_dualcerts_ctx, workers):
    """
    Проверка, что OCSP response перечитывается после получения event-а в конфигурации с двумя сертификатами
    """
    backup = 'default_ocsp_backup'
    ssl_dualcerts_ctx.start_simple_balancer(workers=workers)

    conn1 = ssl_dualcerts_ctx.create_https_connection()
    conn1.close()

    ssl_dualcerts_ctx.certs.copy('old_ocsp.0.der', backup)
    ssl_dualcerts_ctx.certs.copy('old_ocsp.1.der', 'old_ocsp.0.der')
    ssl_dualcerts_ctx.certs.copy('default_ocsp.1.der', 'default_ocsp.0.der')

    ssl_dualcerts_ctx.reload_ocsp()
    conn2 = ssl_dualcerts_ctx.create_https_connection()

    ssl_dualcerts_ctx.assert_ocsp(conn2, 'default_ocsp.1.der')

    # secondary OCSP response не должен перечитаться
    conn_scnd = ssl_dualcerts_ctx.create_secondary_https_connection()
    ssl_dualcerts_ctx.assert_ocsp(conn_scnd, backup)


@pytest.mark.parametrize('workers', [None, 2], ids=['singleprocess', 'multiprocess'])
def test_secondary_ocsp_event(ssl_dualcerts_ctx, workers):
    """
    Проверка, что secondary OCSP response перечитывается после получения event-а
    """
    backup = 'default_ocsp_backup'
    ssl_dualcerts_ctx.start_simple_balancer(workers=workers)

    conn1_scnd = ssl_dualcerts_ctx.create_secondary_https_connection()
    conn1_scnd.close()

    ssl_dualcerts_ctx.certs.copy('default_ocsp.0.der', backup)
    ssl_dualcerts_ctx.certs.copy('default_ocsp.1.der', 'default_ocsp.0.der')
    ssl_dualcerts_ctx.certs.copy('old_ocsp.1.der', 'old_ocsp.0.der')

    response = ssl_dualcerts_ctx.reload_ocsp(name='secondary_default')
    conn2_scnd = ssl_dualcerts_ctx.create_secondary_https_connection()

    ssl_dualcerts_ctx.assert_ocsp(conn2_scnd, 'old_ocsp.1.der')
    ssl_dualcerts_ctx.assert_reload_status(response, [('old_ocsp.0.der', 'OK')], count=workers, has_pid=True)

    # основной OCSP response не должен перечитаться
    conn = ssl_dualcerts_ctx.create_https_connection()
    ssl_dualcerts_ctx.assert_ocsp(conn, backup)


def test_all_ocsp_event(ssl_dualcerts_ctx):
    """
    """
    ssl_dualcerts_ctx.start_complex_balancer()

    ssl_dualcerts_ctx.certs.copy('default_ocsp.1.der', 'default_ocsp.0.der')
    ssl_dualcerts_ctx.certs.copy('detroit_ocsp.1.der', 'detroit_ocsp.0.der')
    ssl_dualcerts_ctx.certs.copy('old_ocsp.1.der', 'old_ocsp.0.der')

    response = ssl_dualcerts_ctx.reload_ocsp(name='all')

    conn_default = ssl_dualcerts_ctx.create_https_connection()
    conn_secondary = ssl_dualcerts_ctx.create_secondary_https_connection()
    conn_detroit = ssl_dualcerts_ctx.create_https_connection(server_name='detroit.yandex.ru')
    conn_vegas = ssl_dualcerts_ctx.create_https_connection(server_name='vegas.yandex.ru')

    lines = response.data.content.splitlines()[1:]
    ssl_dualcerts_ctx.assert_reload_status_lines(lines, has_pid=True, exp_statuses=[
        ('default_ocsp.0.der', 'OK'),
        ('old_ocsp.0.der', 'OK'),
        ('detroit_ocsp.0.der', 'OK'),
        ('vegas_ocsp.0.der', 'OK'),
    ])
    ssl_dualcerts_ctx.assert_ocsp(conn_default, 'default_ocsp.1.der')
    ssl_dualcerts_ctx.assert_ocsp(conn_secondary, 'old_ocsp.1.der')
    ssl_dualcerts_ctx.assert_ocsp(conn_detroit, 'detroit_ocsp.1.der')
    ssl_dualcerts_ctx.assert_ocsp(conn_vegas, 'vegas_ocsp.0.der')


def test_tickets_rotation(ssl_ctx):
    """
    Если указано два ключа для тикетов и клиент устанавливает соединение,
    используя второй, то надо принять тикет и выдать новый
    """
    new_key = ssl_ctx.certs.read_file('default_ticket.1.pem') + \
        ssl_ctx.certs.read_file('default_ticket.0.pem')
    ssl_ctx.certs.copy('default_ticket.0.pem', 'default_ticket.0.key')
    ssl_ctx.start_simple_balancer()
    time.sleep(1)

    conn1 = ssl_ctx.create_https_connection()
    conn1.close()
    ssl_ctx.certs.rewrite('default_ticket.0.key', new_key)
    ssl_ctx.reload_tickets()
    time.sleep(1)
    conn2 = ssl_ctx.create_https_connection(sess_in=conn1.sock.ssl_options.sess_out)

    assert conn2.sock.handshake_info.reused
    ssl_ctx.assert_ticket(conn2, 'default_ticket.1.pem')


def base_start_ok_ocsp_test(ssl_ctx, count):
    balancer = ssl_ctx.start_simple_balancer()
    time.sleep(1)

    ssl_ctx.assert_ssllog_ocsp_ok(balancer.config.log, count)


def test_start_ok_ocsp(ssl_single_ctx):
    """
    Если при старте балансера OCSP response корректен,
    то в логе должна появиться запись о его успешном считывании
    """
    base_start_ok_ocsp_test(ssl_single_ctx, 1)


def test_start_ok_dualcerts_ocsp(ssl_dualcerts_ctx):
    """
    Если при старте балансера OCSP response корректен,
    то в логе должна появиться запись о его успешном считывании
    Проверка конфига с двумя сертификатами
    """
    base_start_ok_ocsp_test(ssl_dualcerts_ctx, 2)


def test_start_ok_tickets(ssl_ctx):
    """
    Если при старте балансера ticket key корректен,
    то в логе должна появиться запись о его успешном считывании
    """
    balancer = ssl_ctx.start_simple_balancer()
    time.sleep(1)

    ssl_ctx.assert_ssllog_ticket_ok(balancer.config.log, 1)


def base_disabled_ocsp_test(ssl_ctx):
    balancer = ssl_ctx.start_simple_balancer()

    conn = ssl_ctx.create_https_connection()
    response = conn.perform_request(http.request.get())
    time.sleep(0.5)

    assert balancer.is_alive(), 'balancer is not alive'
    asserts.content(response, 'Hello')
    assert not conn.sock.handshake_info.has_ocsp
    return balancer


def test_start_no_ocsp_file(ssl_ctx):
    """
    SEPE-5871
    SEPE-6760
    Если при старте балансера отсутствует файл с OCSP response,
    то балансер не должен падать при установлении соединения и должен отключить OCSP stapling
    """
    ssl_ctx.certs.remove('default_ocsp.0.der')

    balancer = base_disabled_ocsp_test(ssl_ctx)

    ssl_ctx.assert_ssllog_no_ocsp(balancer.config.log, 'default_ocsp.0.der', 1)


def test_start_broken_ocsp_file(ssl_ctx):
    """
    SEPE-6760
    Если при старте балансера файл с OCSP response битый,
    то балансер должен отключить OCSP stapling
    """
    ssl_ctx.certs.copy('default_ticket.0.pem', 'default_ocsp.0.der')

    balancer = base_disabled_ocsp_test(ssl_ctx)

    ssl_ctx.assert_ssllog_broken_ocsp(balancer.config.log, 1)


def base_disabled_secondary_ocsp_test(ssl_dualcerts_ctx):
    balancer = ssl_dualcerts_ctx.start_simple_balancer()

    conn = ssl_dualcerts_ctx.create_secondary_https_connection()
    response = conn.perform_request(http.request.get())
    time.sleep(0.5)

    assert balancer.is_alive(), 'balancer is not alive'
    asserts.content(response, 'Hello')
    assert not conn.sock.handshake_info.has_ocsp
    return balancer


def test_start_no_secondary_ocsp_file(ssl_dualcerts_ctx):
    """
    SEPE-5871
    SEPE-6760
    Если при старте балансера отсутствует файл с secondary OCSP response,
    то балансер не должен падать при установлении соединения и должен отключить OCSP stapling
    """
    ssl_dualcerts_ctx.certs.remove('old_ocsp.0.der')

    balancer = base_disabled_secondary_ocsp_test(ssl_dualcerts_ctx)

    time.sleep(1)
    ssl_dualcerts_ctx.assert_ssllog_no_ocsp(balancer.config.log, 'old_ocsp.0.der', 1)


def test_start_broken_secondary_ocsp_file(ssl_dualcerts_ctx):
    """
    SEPE-6760
    Если при старте балансера файл с secondary OCSP response битый,
    то балансер должен отключить OCSP stapling
    """
    ssl_dualcerts_ctx.certs.copy('default_ticket.0.pem', 'old_ocsp.0.der')

    balancer = base_disabled_secondary_ocsp_test(ssl_dualcerts_ctx)

    ssl_dualcerts_ctx.assert_ssllog_broken_ocsp(balancer.config.log, 1)


def test_reload_no_ocsp_file(ssl_ctx):
    """
    SEPE-6760
    Если отутствует файл с OCSP response,
    то после попытки перечитывания балансер должен отдавать старый OCSP response
    и написать в лог об ошибке перечитывания
    """
    balancer = ssl_ctx.start_simple_balancer()

    conn1 = ssl_ctx.create_https_connection()
    conn1.close()
    ssl_ctx.certs.remove('default_ocsp.0.der')
    response = ssl_ctx.reload_ocsp()
    conn2 = ssl_ctx.create_https_connection()

    ssl_ctx.assert_ssllog_no_ocsp(balancer.config.log, 'default_ocsp.0.der', 1)
    ssl_ctx.assert_reload_status(response, [('default_ocsp.0.der', 'failed')], has_pid=True)
    assert conn1.sock.handshake_info.ocsp_data == conn2.sock.handshake_info.ocsp_data


def test_reload_broken_ocsp_file(ssl_ctx):
    """
    SEPE-6760
    Если файл с OCSP response битый,
    то после попытки перечитывания балансер должен отдавать старый OCSP response
    и написать в лог об ошибке перечитывания
    """
    balancer = ssl_ctx.start_simple_balancer()

    conn1 = ssl_ctx.create_https_connection()
    conn1.close()
    ssl_ctx.certs.copy('default_ticket.0.pem', 'default_ocsp.0.der')
    response = ssl_ctx.reload_ocsp()
    conn2 = ssl_ctx.create_https_connection()

    ssl_ctx.assert_ssllog_broken_ocsp(balancer.config.log, 1, empty=False)
    ssl_ctx.assert_reload_status(response, [('default_ocsp.0.der', 'failed')], has_pid=True)
    assert conn1.sock.handshake_info.ocsp_data == conn2.sock.handshake_info.ocsp_data


def test_reload_no_secondary_ocsp_file(ssl_dualcerts_ctx):
    """
    SEPE-6760
    Если отутствует файл с secondary OCSP response,
    то после попытки перечитывания балансер должен отдавать старый OCSP response
    и написать в лог об ошибке перечитывания
    """
    balancer = ssl_dualcerts_ctx.start_simple_balancer()

    conn1 = ssl_dualcerts_ctx.create_secondary_https_connection()
    conn1.close()
    ssl_dualcerts_ctx.certs.remove('old_ocsp.0.der')
    response = ssl_dualcerts_ctx.reload_ocsp(name='secondary_default')
    conn2 = ssl_dualcerts_ctx.create_secondary_https_connection()

    ssl_dualcerts_ctx.assert_ssllog_no_ocsp(balancer.config.log, 'old_ocsp.0.der', 1)
    ssl_dualcerts_ctx.assert_reload_status(response, [('old_ocsp.0.der', 'failed')], has_pid=True)
    assert conn1.sock.handshake_info.ocsp_data == conn2.sock.handshake_info.ocsp_data


def test_reload_broken_secondary_ocsp_file(ssl_dualcerts_ctx):
    """
    SEPE-6760
    Если файл с secondary OCSP response битый,
    то после попытки перечитывания балансер должен отдавать старый OCSP response
    и написать в лог об ошибке перечитывания
    """
    balancer = ssl_dualcerts_ctx.start_simple_balancer()

    conn1 = ssl_dualcerts_ctx.create_secondary_https_connection()
    conn1.close()
    ssl_dualcerts_ctx.certs.copy('default_ticket.0.pem', 'old_ocsp.0.der')
    response = ssl_dualcerts_ctx.reload_ocsp(name='secondary_default')
    conn2 = ssl_dualcerts_ctx.create_secondary_https_connection()

    ssl_dualcerts_ctx.assert_ssllog_broken_ocsp(balancer.config.log, 1, empty=False)
    ssl_dualcerts_ctx.assert_reload_status(response, [('old_ocsp.0.der', 'failed')], has_pid=True)
    assert conn1.sock.handshake_info.ocsp_data == conn2.sock.handshake_info.ocsp_data


def base_disabled_tickets_test(ssl_ctx):
    balancer = ssl_ctx.start_simple_balancer()

    conn = ssl_ctx.create_https_connection()
    response = conn.perform_request(http.request.get())
    time.sleep(0.5)

    assert balancer.is_alive(), 'balancer is not alive'
    asserts.content(response, 'Hello')
    assert not conn.sock.handshake_info.has_ticket
    ssl_ctx.assert_ssllog_ticket_fail(balancer.config.log, 'default_ticket.0.key', 1)
    ssl_ctx.assert_ssllog_ticket_disable(balancer.config.log, 1)


def test_start_no_tickets_file(ssl_ctx):
    """
    SEPE-6760
    Если при старте балансера отсутствует файл с ticket keys,
    то балансер не должен падать при установлении соединения и должен отключить TLS tickets
    """
    ssl_ctx.certs.remove('default_ticket.0.key')

    base_disabled_tickets_test(ssl_ctx)


def test_start_broken_tickets_file(ssl_ctx):
    """
    SEPE-6760
    Если при старте балансера файл с ticket keys битый,
    то балансер должен отключить TLS tickets
    """
    ssl_ctx.certs.copy('default_ocsp.0.der', 'default_ticket.0.key')

    base_disabled_tickets_test(ssl_ctx)


def test_reload_no_tickets_file(ssl_ctx):
    """
    SEPE-6760
    Если отутствует файл с ticket keys,
    то после попытки перечитывания балансер должен отключить TLS tickets
    """
    balancer = ssl_ctx.start_simple_balancer()

    conn1 = ssl_ctx.create_https_connection()
    conn1.close()
    ssl_ctx.certs.remove('default_ticket.0.key')
    response = ssl_ctx.reload_tickets()
    conn2 = ssl_ctx.create_https_connection()

    ssl_ctx.assert_ssllog_ticket_fail(balancer.config.log, 'default_ticket.0.key', 1)
    ssl_ctx.assert_ssllog_ticket_disable(balancer.config.log, 1)
    ssl_ctx.assert_reload_status(response, [('default_ticket.0.key', 'failed')])
    assert not conn2.sock.handshake_info.has_ticket


def test_reload_broken_tickets_file(ssl_ctx):
    """
    SEPE-6760
    Если файл с ticket keys битый,
    то после попытки перечитывания балансер должен отключить TLS tickets
    """
    balancer = ssl_ctx.start_simple_balancer()

    conn1 = ssl_ctx.create_https_connection()
    conn1.close()
    ssl_ctx.certs.copy('default_ocsp.0.der', 'default_ticket.0.key')
    response = ssl_ctx.reload_tickets()
    conn2 = ssl_ctx.create_https_connection()

    ssl_ctx.assert_ssllog_ticket_fail(balancer.config.log, 'default_ticket.0.key', 1)
    ssl_ctx.assert_ssllog_ticket_disable(balancer.config.log, 1)
    ssl_ctx.assert_reload_status(response, [('default_ticket.0.key', 'failed')])
    assert not conn2.sock.handshake_info.has_ticket


def base_restore_ocsp_test(ssl_ctx, count):
    ocsp_resp = ssl_ctx.certs.read_file('default_ocsp.0.der')
    balancer = ssl_ctx.start_simple_balancer()

    ssl_ctx.certs.copy('default_ticket.0.pem', 'default_ocsp.0.der')
    ssl_ctx.reload_ocsp()
    ssl_ctx.certs.rewrite('default_ocsp.0.der', ocsp_resp)
    response2 = ssl_ctx.reload_ocsp()
    conn = ssl_ctx.create_https_connection()

    ssl_ctx.assert_ssllog_ocsp_ok(balancer.config.log, count)
    ssl_ctx.assert_reload_status(response2, [('default_ocsp.0.der', 'OK')], has_pid=True)
    assert conn.sock.handshake_info.has_ocsp


def test_restore_ocsp(ssl_single_ctx):
    """
    После восстановления файла с OCSP response и его перечитывания,
    балансер должен снова включить OCSP stapling
    """
    base_restore_ocsp_test(ssl_single_ctx, 2)


def test_restore_ocsp_dualcerts(ssl_dualcerts_ctx):
    """
    После восстановления файла с OCSP response и его перечитывания,
    балансер должен снова включить OCSP stapling
    Случай, когда указаны два сертификата
    """
    base_restore_ocsp_test(ssl_dualcerts_ctx, 3)


def test_restore_secondary_ocsp(ssl_dualcerts_ctx):
    """
    После восстановления файла с secondary OCSP response и его перечитывания,
    балансер должен снова включить OCSP stapling
    """
    ocsp_resp = ssl_dualcerts_ctx.certs.read_file('old_ocsp.0.der')
    balancer = ssl_dualcerts_ctx.start_simple_balancer()

    ssl_dualcerts_ctx.certs.copy('default_ticket.0.pem', 'old_ocsp.0.der')
    ssl_dualcerts_ctx.reload_ocsp(name='secondary_default')
    ssl_dualcerts_ctx.certs.rewrite('old_ocsp.0.der', ocsp_resp)
    response2 = ssl_dualcerts_ctx.reload_ocsp(name='secondary_default')
    conn = ssl_dualcerts_ctx.create_secondary_https_connection()

    ssl_dualcerts_ctx.assert_ssllog_ocsp_ok(balancer.config.log, 3)
    ssl_dualcerts_ctx.assert_reload_status(response2, [('old_ocsp.0.der', 'OK')], has_pid=True)
    assert conn.sock.handshake_info.has_ocsp


def test_restore_tickets(ssl_ctx):
    """
    После восстановления файла с ticket keys и его перечитывания,
    балансер должен снова включить TLS tickets
    """
    ticket_key = ssl_ctx.certs.read_file('default_ticket.0.key')
    balancer = ssl_ctx.start_simple_balancer()

    ssl_ctx.certs.copy('default_ocsp.0.der', 'default_ticket.0.key')
    ssl_ctx.reload_tickets()
    ssl_ctx.certs.rewrite('default_ticket.0.key', ticket_key)
    response2 = ssl_ctx.reload_tickets()
    conn = ssl_ctx.create_https_connection()

    ssl_ctx.assert_ssllog_ticket_ok(balancer.config.log, 2)
    ssl_ctx.assert_reload_status(response2, [('default_ticket.0.key', 'OK')])
    assert conn.sock.handshake_info.has_ticket


def test_empty_ocsp_file(ssl_ctx):
    """
    Если файл с OCSP response пуст, то балансер не должен падать при попытке соединения
    """
    ssl_ctx.certs.rewrite('default_ocsp.0.der', '')
    balancer = ssl_ctx.start_simple_balancer()

    ssl_ctx.create_https_connection()
    time.sleep(0.5)

    assert balancer.is_alive(), 'balancer is not alive'
    ssl_ctx.assert_ssllog_broken_ocsp(balancer.config.log, 1)


def test_empty_secondary_ocsp_file(ssl_dualcerts_ctx):
    """
    Если файл с secondary OCSP response пуст, то балансер не должен падать при попытке соединения
    """
    ssl_dualcerts_ctx.certs.rewrite('old_ocsp.0.der', '', chmod=True)  # FIXME: problems in ci
    balancer = ssl_dualcerts_ctx.start_simple_balancer()

    ssl_dualcerts_ctx.create_secondary_https_connection()
    time.sleep(0.5)

    assert balancer.is_alive(), 'balancer is not alive'
    ssl_dualcerts_ctx.assert_ssllog_broken_ocsp(balancer.config.log, 1)


def test_tickets_list(ssl_ctx):
    """
    Проверка TLS tickets c использованием ticket_keys_list
    """
    ssl_ctx.start_complex_balancer()
    ssl_ctx.wait_ssllog_ticket_ok(ssl_ctx.balancer.config.default_log, 1)

    conn = ssl_ctx.create_https_connection()

    ssl_ctx.assert_ticket(conn, 'default_ticket.0.key')


def test_tickets_list_timeout(ssl_ctx):
    """
    Проверка таймаутов TLS tickets c использованием ticket_keys_list
    """
    timeout = 1
    ssl_ctx.start_complex_balancer(timeout=timeout)
    ssl_ctx.wait_ssllog_ticket_ok(ssl_ctx.balancer.config.default_log, 1)

    conn1 = ssl_ctx.create_https_connection()
    conn1.close()
    time.sleep(timeout + 1)
    conn2 = ssl_ctx.create_https_connection(sess_in=conn1.sock.ssl_options.sess_out)

    assert conn1.sock.handshake_info.ticket_ttl == str(timeout)
    assert not conn2.sock.handshake_info.reused


def test_tickets_list_event(ssl_ctx):
    """
    Проверка, что тикет перечитается после получения event-а c использованием ticket_keys_list
    """
    ssl_ctx.start_complex_balancer()
    ssl_ctx.wait_ssllog_ticket_ok(ssl_ctx.balancer.config.default_log, 1)

    conn1 = ssl_ctx.create_https_connection()
    conn1.close()
    ssl_ctx.certs.copy('default_ticket.0.pem', 'default_ticket.0.key')
    response = ssl_ctx.reload_tickets()
    ssl_ctx.wait_ssllog_ticket_ok(ssl_ctx.balancer.config.default_log, 2)
    conn2 = ssl_ctx.create_https_connection()

    ssl_ctx.assert_ticket(conn2, 'default_ticket.0.pem')
    ssl_ctx.assert_reload_status(
        response,
        [('default_ticket.0.key', 'OK'), ('default_ticket.1.key', 'OK'), ('default_ticket.2.key', 'OK')])


def test_tickets_list_rotation(ssl_ctx):
    """
    Проверка tickets rotation c использованием ticket_keys_list
    """
    ssl_ctx.start_complex_balancer()
    ssl_ctx.wait_ssllog_ticket_ok(ssl_ctx.balancer.config.default_log, 1)

    conn1 = ssl_ctx.create_https_connection()
    conn1.close()
    ssl_ctx.certs.copy('default_ticket.0.key', 'default_ticket.1.key')
    ssl_ctx.certs.copy('default_ticket.0.pem', 'default_ticket.0.key')
    ssl_ctx.reload_tickets()
    ssl_ctx.wait_ssllog_ticket_ok(ssl_ctx.balancer.config.default_log, 2)
    conn2 = ssl_ctx.create_https_connection(sess_in=conn1.sock.ssl_options.sess_out)

    assert conn2.sock.handshake_info.reused
    assert conn1.sock.handshake_info.ticket_id != conn2.sock.handshake_info.ticket_id


def test_tickets_list_prio_non_unique(ssl_ctx):
    """
    Если в конфиге для двух тикетов выставлен одинаковый приоритет,
    то балансер не должен запуститься
    """
    with pytest.raises(BalancerStartError):
        ssl_ctx.start_complex_balancer(default_ticket_prio_1=1, default_ticket_prio_2=1)


def test_tickets_list_not_all_broken(ssl_ctx):
    """
    Если некоторые из файлов с ticket keys битые или отсутствуют,
    то балансер должен отдать клиенту тикет с максимальным приоритетом из оставшихся
    """
    ssl_ctx.start_complex_balancer()

    conn1 = ssl_ctx.create_https_connection()
    conn1.close()
    ssl_ctx.certs.remove('default_ticket.0.key')
    response1 = ssl_ctx.reload_tickets()
    conn2 = ssl_ctx.create_https_connection(sess_in=conn1.sock.ssl_options.sess_out)
    conn2.close()
    ssl_ctx.certs.copy('default_ocsp.0.der', 'default_ticket.1.key')
    response2 = ssl_ctx.reload_tickets()
    conn3 = ssl_ctx.create_https_connection()

    ssl_ctx.assert_reload_status(
        response1,
        [('default_ticket.0.key', 'failed'), ('default_ticket.1.key', 'OK'), ('default_ticket.2.key', 'OK')])
    ssl_ctx.assert_reload_status(
        response2,
        [('default_ticket.0.key', 'failed'), ('default_ticket.1.key', 'failed'), ('default_ticket.2.key', 'OK')])
    ssl_ctx.assert_ticket(conn1, 'default_ticket.0.raw')
    ssl_ctx.assert_ticket(conn2, 'default_ticket.1.raw')
    ssl_ctx.assert_ticket(conn3, 'default_ticket.2.raw')


def test_tickets_list_reload_all_broken(ssl_ctx):
    """
    Если все файлы с ticket keys битые или отсутствуют в момент перечитывания,
    то балансер должен отключить TLS tickets
    """
    balancer = ssl_ctx.start_complex_balancer()
    ssl_ctx.wait_ssllog_ticket_ok(balancer.config.default_log, 1)

    ssl_ctx.certs.remove('default_ticket.0.key')
    ssl_ctx.certs.remove('default_ticket.1.key')
    ssl_ctx.certs.copy('default_ocsp.0.der', 'default_ticket.2.key')
    reload_resp = ssl_ctx.reload_tickets()
    conn = ssl_ctx.create_https_connection()
    response = conn.perform_request(http.request.get())
    time.sleep(0.5)

    assert balancer.is_alive(), 'balancer is not alive'
    ssl_ctx.assert_reload_status(
        reload_resp,
        [('default_ticket.0.key', 'failed'), ('default_ticket.1.key', 'failed'), ('default_ticket.2.key', 'failed')])
    asserts.content(response, 'Hello')
    assert not conn.sock.handshake_info.has_ticket
    ssl_ctx.assert_ssllog_ticket_fail(balancer.config.default_log, 'default_ticket.0.key', 1)
    ssl_ctx.assert_ssllog_ticket_fail(balancer.config.default_log, 'default_ticket.1.key', 1)
    ssl_ctx.assert_ssllog_ticket_fail(balancer.config.default_log, 'default_ticket.2.key', 1)
    ssl_ctx.assert_ssllog_ticket_disable(balancer.config.default_log, 1)


def test_tickets_list_restore(ssl_ctx):
    """
    Проверка восстановления TLS tickets при использовании ticket_keys_list
    """
    ssl_ctx.start_complex_balancer()

    ssl_ctx.certs.remove('default_ticket.0.key')
    ssl_ctx.certs.remove('default_ticket.1.key')
    ssl_ctx.certs.copy('default_ocsp.0.der', 'default_ticket.2.key')
    ssl_ctx.reload_tickets()
    conn1 = ssl_ctx.create_https_connection()
    conn1.close()
    ssl_ctx.certs.copy('default_ticket.0.raw', 'default_ticket.0.key')
    reload_resp = ssl_ctx.reload_tickets()
    conn2 = ssl_ctx.create_https_connection()

    ssl_ctx.assert_reload_status(
        reload_resp,
        [('default_ticket.0.key', 'OK'), ('default_ticket.1.key', 'failed'), ('default_ticket.2.key', 'failed')])
    ssl_ctx.assert_ticket(conn2, 'default_ticket.0.raw')


def test_tickets_list_multiple_keys_in_file(ssl_ctx):
    """
    Если в файл содержит несколько тикетов, то наибольший приоритет имеет первый
    """
    new_key = ssl_ctx.certs.read_file('default_ticket.1.pem') + \
        ssl_ctx.certs.read_file('default_ticket.0.pem')
    ssl_ctx.certs.rewrite('default_ticket.0.key', new_key)
    ssl_ctx.start_complex_balancer()

    conn = ssl_ctx.create_https_connection()

    ssl_ctx.assert_ticket(conn, 'default_ticket.1.pem')


def test_sni(ssl_ctx):
    """
    Проверка SNI
    """
    ssl_ctx.start_complex_balancer()

    conn = ssl_ctx.create_https_connection(server_name='detroit.yandex.ru')

    ssl_ctx.assert_cert(conn, 'detroit.crt')
    ssl_ctx.assert_ocsp(conn, 'detroit_ocsp.0.der')
    ssl_ctx.assert_ticket(conn, 'detroit_ticket.0.key')


def test_sni_default(ssl_ctx):
    """
    Если servername не подходит ни под одну из регулярок,
    то надо вернуть сертификаты из default
    """
    ssl_ctx.start_complex_balancer()

    conn = ssl_ctx.create_https_connection(server_name='unknown.yandex.ru')

    ssl_ctx.assert_cert(conn, 'default.crt')
    ssl_ctx.assert_ocsp(conn, 'default_ocsp.0.der')
    ssl_ctx.assert_ticket(conn, 'default_ticket.0.key')


def test_sni_priority(ssl_ctx):
    """
    Если servername матчится несколькими регулярками,
    то надо отдать сертификаты из контекста с маскимальным приоритетом.
    Проверяем, что обе возможные регулярки работают
    """
    ssl_ctx.start_complex_balancer(detroit_server_name='[^.]+[.]yandex[.]ru',
                                   vegas_server_name='.+\\.yandex\\.ru')

    conn = ssl_ctx.create_https_connection(server_name='unknown.yandex.ru')

    ssl_ctx.assert_cert(conn, 'detroit.crt')
    ssl_ctx.assert_ocsp(conn, 'detroit_ocsp.0.der')
    ssl_ctx.assert_ticket(conn, 'detroit_ticket.0.key')


def test_sni_priority_broken_ocsp_tickets(ssl_ctx):
    """
    Если servername матчится несколькими регулярками,
    то надо отдать сертификаты из контекста с маскимальным приоритетом,
    даже если в нем не подгрузились ocsp/ticket keys
    """
    ssl_ctx.certs.remove('detroit_ocsp.0.der')
    ssl_ctx.certs.remove('detroit_ticket.0.key')
    ssl_ctx.start_complex_balancer(detroit_server_name='.*\\.yandex\\.ru',
                                   vegas_server_name='.*\\.yandex\\.ru')

    conn = ssl_ctx.create_https_connection(server_name='unknown.yandex.ru')

    ssl_ctx.assert_cert(conn, 'detroit.crt')
    assert not conn.sock.handshake_info.has_ocsp
    # assert not conn.sock.handshake_info.has_ticket # ticket is generated anyway, lol


def test_sni_equal_ocsp_events(ssl_ctx):
    """
    Если в двух контекстах одинаковые имена event-а для перечитывания ocsp,
    то при его вызове он должен выполниться в обоих контекстах
    """
    ssl_ctx.start_complex_balancer(default_reload_ocsp='unknown_reload_ocsp',
                                   detroit_reload_ocsp='unknown_reload_ocsp')

    ssl_ctx.certs.copy('default_ocsp.1.der', 'default_ocsp.0.der')
    ssl_ctx.certs.copy('detroit_ocsp.1.der', 'detroit_ocsp.0.der')
    response = ssl_ctx.reload_ocsp(name='unknown')
    conn_default = ssl_ctx.create_https_connection()
    conn_detroit = ssl_ctx.create_https_connection(server_name='detroit.yandex.ru')

    ssl_ctx.assert_reload_status(response, exp_statuses=[('default_ocsp.0.der', 'OK'), ('detroit_ocsp.0.der', 'OK')], has_pid=True)
    ssl_ctx.assert_ocsp(conn_default, 'default_ocsp.1.der')
    ssl_ctx.assert_ocsp(conn_detroit, 'detroit_ocsp.1.der')


def test_sni_equal_tickets_events(ssl_ctx):
    """
    Если в двух контекстах одинаковые имена event-а для перечитывания ocsp,
    то при его вызове он должен выполниться в обоих контекстах
    """
    ssl_ctx.start_complex_balancer(default_reload_tickets='unknown_reload_tickets',
                                   detroit_reload_tickets='unknown_reload_tickets')

    ssl_ctx.certs.copy('default_ticket.0.pem', 'default_ticket.0.key')
    ssl_ctx.certs.copy('detroit_ticket.0.pem', 'detroit_ticket.0.key')
    response = ssl_ctx.reload_tickets(name='unknown')
    conn_default = ssl_ctx.create_https_connection()
    conn_detroit = ssl_ctx.create_https_connection(server_name='detroit.yandex.ru')

    ssl_ctx.assert_reload_status(
        response,
        exp_statuses=[('default_ticket.0.key', 'OK'), ('default_ticket.1.key', 'OK'),
                      ('default_ticket.2.key', 'OK'), ('detroit_ticket.0.key', 'OK')])
    ssl_ctx.assert_ticket(conn_default, 'default_ticket.0.pem')
    ssl_ctx.assert_ticket(conn_detroit, 'detroit_ticket.0.pem')


def test_dh(ssl_single_ctx):
    """
    Проверка поддержки DH
    """
    cipher = 'DHE-RSA-AES256-SHA'
    cipher_suite = 'kEECDH:kEDH:kRSA+AES128:kRSA:+3DES:' \
                   'RC4:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2'
    ssl_single_ctx.start_simple_balancer(
        ciphers=cipher_suite,
        ca=ssl_single_ctx.certs.abs_path('old_ca.crt'),
        cert=ssl_single_ctx.certs.abs_path('old.crt'),
        priv=ssl_single_ctx.certs.abs_path('old.key'),
        ocsp=ssl_single_ctx.certs.abs_path('old_ocsp.0.der'),
    )

    conn = ssl_single_ctx.create_https_connection(cipher=cipher)

    assert conn.sock.handshake_info.cipher == cipher


def test_dualcerts_dh(ssl_dualcerts_ctx):
    """
    Проверка поддержки DH
    """
    cipher = 'DHE-RSA-AES256-SHA'
    cipher_suite = 'kEECDH:kEDH:kRSA+AES128:kRSA:+3DES:' \
                   'RC4:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2'
    ssl_dualcerts_ctx.start_complex_balancer(ciphers=cipher_suite)

    conn = ssl_dualcerts_ctx.create_https_connection(cipher=cipher)

    assert conn.sock.handshake_info.cipher == cipher


def test_ecdh(ssl_ctx):
    """
    Проверка поддержки ECDH
    """
    ssl_ctx.start_complex_balancer()

    conn = ssl_ctx.create_https_connection()

    assert 'ECDH' in conn.sock.handshake_info.cipher


def test_alpn_selection(ssl_ctx):
    """
    BALANCER-942
    Проверка поддержки alpn negotiation
    """
    ssl_ctx.start_complex_balancer()

    conn = ssl_ctx.create_https_connection(alpn='http/1.1')

    assert 'http/1.1' in conn.sock.handshake_info.alpn_protocols


def test_tls12(ssl_ctx):
    """
    Проверка поддержки TLSv1.2
    """
    ssl_ctx.start_complex_balancer()

    conn = ssl_ctx.create_https_connection(tls1_2=True)

    assert conn.sock.handshake_info.protocol == 'TLSv1.2'
    unistat = ssl_ctx.get_unistat()
    assert unistat['ssl_sni-tls1_2_requests_summ'] == 1


@pytest.mark.xfail(reason="can't catch post-handshake info")
def test_tls13(ssl_ctx):
    """
    Проверка поддержки TLSv1.3
    """
    ssl_ctx.start_complex_balancer(ssl_protocols="tlsv1.3")

    conn = ssl_ctx.create_https_connection(tls1_3=True)

    assert conn.sock.handshake_info.protocol == 'TLSv1.3'
    unistat = ssl_ctx.get_unistat()
    assert unistat['ssl_sni-tls1_3_requests_summ'] == 1


def test_disable_sslv3(ssl_ctx):
    """
    BALANCER-2392
    Если sslv3 нет в списке ssl_protocols, то балансер не должен принимать соединения по SSLv3
    """
    ssl_ctx.start_complex_balancer(ssl_protocols="tlsv1 tlsv1.1 tlsv1.2 tlsv1.3")

    conn = ssl_ctx.create_https_connection(ssl3=True, check_closed=False)
    assert_closed(conn)
    unistat = ssl_ctx.get_unistat()
    assert unistat['ssl_sni-ssl3_requests_summ'] == 0


def test_not_disable_sslv3(ssl_ctx):
    """
    BALANCER-2392
    Если sslv3 в списке ssl_protocols, то балансер должен принимать соединения по SSLv3
    """
    ssl_ctx.start_complex_balancer(ssl_protocols="sslv3 tlsv1.3")

    conn = ssl_ctx.create_https_connection(ssl3=True)

    asserts.is_not_closed(conn.sock)


def test_disable_tlsv1_3(ssl_ctx):
    """
    BALANCER-2392
    Если tlsv1.3 нет в списке ssl_protocols, то балансер не доолжен принимать соединения по TLSv1.3
    """
    ssl_ctx.start_complex_balancer(ssl_protocols="tlsv1 tlsv1.1 tlsv1.2")

    conn = ssl_ctx.create_https_connection(tls1_3=True, check_closed=False)
    assert_closed(conn)


def test_not_disable_tlsv1_3(ssl_ctx):
    """
    Если tlsv1.3 в списке ssl_protocols, то балансер доолжен принимать соединения по TLSv1.3
    """
    ssl_ctx.start_complex_balancer(ssl_protocols="tlsv1 tlsv1.1 tlsv1.2 tlsv1.3")

    conn = ssl_ctx.create_https_connection(tls1_3=True)

    asserts.is_not_closed(conn.sock)
    unistat = ssl_ctx.get_unistat()
    assert unistat['ssl_sni-tls1_3_requests_summ'] == 1


def test_statistics_servername(ssl_ctx):
    """
    SEPE-7867
    Проверка статистики https запросов
    """
    count = 10
    ssl_ctx.start_complex_balancer()

    ssl_ctx.do_requests(count)
    ssl_ctx.do_requests(count, server_name='detroit.yandex.ru')
    ssl_ctx.do_requests(count, server_name='vegas.yandex.ru')

    for run in Multirun():
        with run:
            unistat = ssl_ctx.get_unistat()
            assert unistat['ssl_sni-https_requests_summ'] == 30
            assert unistat['ssl_sni-ssl_item-vegas-servername_selected_summ'] == 10
            assert unistat['ssl_sni-ssl_item-detroit-servername_selected_summ'] == 10
            assert unistat['ssl_sni-http_requests_summ'] == 0


def test_statistics_ciphers(ssl_ctx):
    """
    BALANCER-316
    Проверка статистики по используемым шифрам
    """
    count = 3
    ciphers = ['ECDHE-ECDSA-AES256-GCM-SHA384', 'ECDHE-ECDSA-AES128-SHA256']
    ssl_ctx.start_complex_balancer(log_ciphers_stats=True)
    for cipher in ciphers:
        ssl_ctx.do_requests(count, cipher=cipher)
        ssl_ctx.do_requests(count, cipher=cipher, server_name='detroit.yandex.ru')
        ssl_ctx.do_requests(count, cipher=cipher, server_name='vegas.yandex.ru')

    ssl_ctx.assert_ssllog(ssl_ctx.balancer.config.default_log, "name = ECDHE-ECDSA-AES128-SHA256 3", 1)
    ssl_ctx.assert_ssllog(ssl_ctx.balancer.config.detroit_log, "name = ECDHE-ECDSA-AES128-SHA256 3", 1)
    ssl_ctx.assert_ssllog(ssl_ctx.balancer.config.vegas_log, "name = ECDHE-ECDSA-AES128-SHA256 3", 1)
    ssl_ctx.assert_ssllog(ssl_ctx.balancer.config.default_log, "name = ECDHE-ECDSA-AES256-GCM-SHA384 3", 4)
    ssl_ctx.assert_ssllog(ssl_ctx.balancer.config.detroit_log, "name = ECDHE-ECDSA-AES256-GCM-SHA384 3", 4)
    ssl_ctx.assert_ssllog(ssl_ctx.balancer.config.vegas_log, "name = ECDHE-ECDSA-AES256-GCM-SHA384 3", 4)


SECRETS = re.compile(r"CLIENT_RANDOM\s+(\S+)\s+(\S+)\s*", re.VERBOSE | re.MULTILINE)

Secret = namedtuple('Secret', ['client_random', 'master_key'])


def parse_secrets_file(file_name):
    lines = ""
    with open(file_name) as file_:
        lines = file_.read()

    secrets = []
    for i in SECRETS.findall(lines):
        secrets.append(Secret(i[0], i[1]))

    return secrets


def get_secrets(file_name):
    return parse_secrets_file(file_name)


def get_timestamps(file_name):
    return [datetime.datetime.strptime(s[0], '%Y-%m-%dT%H:%M:%S.%fZ') for s in parse_secrets_file(file_name)]


def base_secrets_tests(ssl_ctx, count, secrets_log, server_name=None, expected_count=None):
    old_secrets = set(get_secrets(secrets_log))
    conn_secrets = list()

    ssl_options = {'msg': True}
    if server_name is not None:
        ssl_options['server_name'] = server_name
    for _ in range(count):
        conn = ssl_ctx.create_https_connection(**ssl_options)
        info = conn.sock.handshake_info
        conn_secrets.append((info.client_random,
                             info.master_key))

    if expected_count is None:
        expected_count = count

    for run in Multirun():
        with run:
            secrets = [s for s in get_secrets(secrets_log) if s not in old_secrets]
            assert expected_count == len(secrets)

            for conn_secret, secret in zip(conn_secrets, secrets):
                conn_client_random, conn_master_key = conn_secret
                assert conn_client_random.lower() == secret.client_random.lower()
                assert conn_master_key.lower() == secret.master_key.lower()


@pytest.mark.parametrize(
    "freq", [0, 1], ids=["off_on", "on_off"]
)
def test_secrets_single(ssl_ctx, freq):
    """
    BALANCER-298
    Проверка записи секретов в файл для одного соединения
    """
    rate_file = ssl_ctx.manager.fs.create_file('rate_file')
    ssl_ctx.manager.fs.rewrite(rate_file, "")
    balancer = ssl_ctx.start_complex_balancer(secrets_log_freq=freq, secrets_log_freq_file=rate_file)
    base_secrets_tests(ssl_ctx, 1, balancer.config.default_secrets_log, expected_count=bool(freq))
    freq = 1 - freq
    ssl_ctx.manager.fs.rewrite(rate_file, str(freq))
    time.sleep(3)
    base_secrets_tests(ssl_ctx, 1, balancer.config.default_secrets_log, expected_count=bool(freq))


def test_secrets_multiple(ssl_ctx):
    """
    BALANCER-298
    Проверка записи секретов в файл для нескольких соединений
    """
    balancer = ssl_ctx.start_complex_balancer()
    base_secrets_tests(ssl_ctx, 5, balancer.config.default_secrets_log)


def test_secrets_servername(ssl_ctx):
    """
    BALANCER-298
    Проверка записи секретов в файл для нескольких контекстов ssl_sni
    """
    balancer = ssl_ctx.start_complex_balancer()
    base_secrets_tests(ssl_ctx, 1, balancer.config.default_secrets_log)
    base_secrets_tests(ssl_ctx, 1, balancer.config.detroit_secrets_log, 'detroit.yandex.ru')
    base_secrets_tests(ssl_ctx, 1, balancer.config.vegas_secrets_log, 'vegas.yandex.ru')


def base_handshake_error_test(ssl_ctx, unknown_protocol_count=1, no_shared_cipher_count=1, curl_count=1):
    ssl_ctx.start_complex_balancer(ciphers='DHE-RSA-AES256-SHA256')
    for _ in range(unknown_protocol_count):
        # just -ssl2 not working: https://bugs.launchpad.net/ubuntu/+source/openssl/+bug/955675
        conn = ssl_ctx.create_https_connection(ssl3=False, tls1=False, tls1_1=False, tls1_2=False, check_closed=False)
        assert_closed(conn)

    for _ in range(no_shared_cipher_count):
        conn = ssl_ctx.create_https_connection(cipher='AES256-SHA256', check_closed=False)
        assert_closed(conn)

    for _ in range(curl_count):
        ssl_ctx.call(['curl', 'https://localhost:%s/' % ssl_ctx.balancer.config.port])


def test_stats_errors(ssl_ctx):
    base_handshake_error_test(ssl_ctx)

    unistat = ssl_ctx.get_unistat()
    assert unistat['ssl_error-tls_early_post_process_client_hello_unsupported_protocol_summ'] == 1
    assert unistat['ssl_error-ssl3_read_bytes_tlsv1_alert_unknown_ca_summ'] in (0, 1)
    assert unistat['ssl_error-tls_post_process_client_hello_no_shared_cipher_summ'] in (1, 2)
    assert unistat['ssl_error-total_summ'] in (2, 3)

    solomon = ssl_ctx.get_solomon()
    assert solomon['ssl_error-tls_early_post_process_client_hello_unsupported_protocol']['value'] == 1
    assert solomon['ssl_error-ssl3_read_bytes_tlsv1_alert_unknown_ca']['value'] in (0, 1)
    assert solomon['ssl_error-tls_post_process_client_hello_no_shared_cipher']['value'] in (1, 2)
    assert solomon['ssl_error-total']['value'] in (2, 3)


@pytest.mark.xfail(reason='BALANCER-534')
def test_handshake_error_log(ssl_ctx):
    """
    Ошибки ssl handshake-ов должны писаться в errorlog
    """
    base_handshake_error_test(ssl_ctx)
    for run in Multirun():
        with run:
            text = ssl_ctx.manager.fs.read_lines(ssl_ctx.balancer.config.errorlog)
            assert 'unknown protocol' in text[0]
            assert 'no shared cipher' in text[1]
            assert 'tlsv1 alert unknown ca' in text[2]


@pytest.mark.xfail(reason='BALANCER-534')
def test_handshake_error_stats(ssl_ctx):
    """
    BALANCER-448
    BALANCER-470
    Ошибки ssl handshake-ов должны появляться в статистике
    """
    unknown_protocol_count = 3
    no_shared_cipher_count = 4
    curl_count = 5
    total_count = unknown_protocol_count + no_shared_cipher_count + curl_count
    base_handshake_error_test(ssl_ctx, unknown_protocol_count, no_shared_cipher_count, curl_count)
    for run in Multirun():
        with run:
            unistat = ssl_ctx.get_unistat()
            assert unistat['ssl_error-tls_early_post_process_client_hello_unknown_protocol_summ'] == unknown_protocol_count
            assert unistat['ssl_error-tls_post_process_client_hello_no_shared_cipher_summ'] == no_shared_cipher_count
            assert unistat['ssl_error-ssl3_read_bytes_tlsv1_alert_unknown_ca_summ'] == curl_count
            assert unistat["ssl_errors-total_summ"] == total_count


@pytest.mark.parametrize('http2_alpn_freq', [0, 1], ids=["h2off", "h2on"])
@pytest.mark.parametrize('http2_alpn_rand_mode', ['rand', 'ip_hash'])
def test_alpn_h2(ssl_single_ctx, http2_alpn_freq, http2_alpn_rand_mode):
    """
    Проверка поддержки alpn http2
    """
    ssl_single_ctx.start_h2_balancer(http2_alpn_freq=http2_alpn_freq, http2_alpn_rand_mode=http2_alpn_rand_mode)
    conn = ssl_single_ctx.create_h2_ssl_connection(alpn='h2')

    if http2_alpn_freq == 1:
        assert 'h2' in (conn.sock.handshake_info.alpn_protocols or [])
    else:
        assert 'h2' not in (conn.sock.handshake_info.alpn_protocols or [])


def test_alpn_h2_ip_hash(ssl_single_ctx):
    """
    BALANCER-1686
    Проверка работы с весами http2_alpn_rand_mode = ip_hash
    """
    http2_alpn_file = ssl_single_ctx.manager.fs.create_file('http2_alpn_file')
    ssl_single_ctx.start_h2_balancer(http2_alpn_file=http2_alpn_file, http2_alpn_rand_mode="ip_hash")

    time.sleep(0.1)

    conn = ssl_single_ctx.create_h2_ssl_connection(alpn='h2')
    assert 'h2' not in (conn.sock.handshake_info.alpn_protocols or [])

    ssl_single_ctx.manager.fs.rewrite(http2_alpn_file, "0.2031")

    time.sleep(5)

    conn = ssl_single_ctx.create_h2_ssl_connection(alpn='h2')
    assert 'h2' not in (conn.sock.handshake_info.alpn_protocols or [])

    ssl_single_ctx.manager.fs.rewrite(http2_alpn_file, "0.2032")
    time.sleep(5)

    conn = ssl_single_ctx.create_h2_ssl_connection(alpn='h2')
    assert 'h2' in (conn.sock.handshake_info.alpn_protocols or [])


@pytest.mark.parametrize(
    ['rate', 'h2', 'header'],
    [
        ('2000,0.1,a', False, 'no'),
        ('2000,0.1,e', False, '1000,0,89'),
        ('2000,0.1,d', True, '2000,0,38')
    ],
    ids=["none", "cont", "exp"]
)
def test_alpn_h2_exp_static(ssl_single_ctx, rate, h2, header):
    """
    Checks that localhost is matched by the experiment
    """
    rate_file = ssl_single_ctx.manager.fs.create_file('rate_file')
    ssl_single_ctx.manager.fs.rewrite(rate_file, rate)
    ssl_single_ctx.start_h2_balancer(
        http2_alpn_rand_mode="exp_static",
        http2_alpn_exp_id="2000",
        exp_rate_file=rate_file,
    )

    conn = ssl_single_ctx.create_h2_ssl_connection(h2=h2, alpn='h2')

    if h2:
        assert 'h2' in (conn.sock.handshake_info.alpn_protocols or [])
        conn.write_preface()
        conn.write_frame(frames.Settings(length=None, flags=0, reserved=0, data=[],))
        conn.read_frame()
        conn.write_frame(frames.Settings(length=None, flags=flags.ACK, reserved=0, data=[],))
        resp = conn.perform_request(http2.request.get())
    else:
        assert 'h2' not in (conn.sock.handshake_info.alpn_protocols or [])
        resp = conn.perform_request(http.request.get())
    asserts.header_value(resp, 'Y-ExpStatic-Test', header)


@pytest.mark.parametrize('http2_alpn_freq', [0, 1], ids=["h2off", "h2on"])
def test_alpn_h2_freq_file_on(ssl_single_ctx, http2_alpn_freq):
    """
    При отстутсвии файла http2_alpn_file или наличии файла с числом,
    больше или равным 1.0, h2 в alpn выбирается в соответствии с дефолтом в http2_alpn_freq
    """
    http2_alpn_file = ssl_single_ctx.manager.fs.create_file('http2_alpn_file')
    ssl_single_ctx.manager.fs.remove(http2_alpn_file)

    ssl_single_ctx.start_h2_balancer(http2_alpn_file=http2_alpn_file, http2_alpn_freq=http2_alpn_freq)

    conn = ssl_single_ctx.create_h2_ssl_connection(alpn='h2')

    if http2_alpn_freq == 1:
        assert 'h2' in (conn.sock.handshake_info.alpn_protocols or [])
    else:
        assert 'h2' not in (conn.sock.handshake_info.alpn_protocols or [])

    for data in ['1.0', 'odin-odin-raz']:
        ssl_single_ctx.manager.fs.rewrite(http2_alpn_file, data)
        time.sleep(5)

        conn = ssl_single_ctx.create_h2_ssl_connection(alpn='h2')

        if http2_alpn_freq == 1 or data == '1.0':
            assert 'h2' in (conn.sock.handshake_info.alpn_protocols or [])
        else:
            assert 'h2' not in (conn.sock.handshake_info.alpn_protocols or [])


@pytest.mark.parametrize('http2_alpn_freq', [0, 1], ids=["h2off", "h2on"])
def test_alpn_h2_freq_file_off(ssl_single_ctx, http2_alpn_freq):
    """
    При наличии файла http2_alpn_file с 0.0,
    h2 в alpn не выбирается. При удалении файла h2
    сновая появляется в alpn в соответствии с дефолтом в http2_alpn_freq (BALANCER-1557)
    """
    http2_alpn_file = ssl_single_ctx.manager.fs.create_file('http2_alpn_file')

    ssl_single_ctx.start_h2_balancer(http2_alpn_file=http2_alpn_file, http2_alpn_freq=http2_alpn_freq)

    for data in ['0.0', ' 0.0', '0.0\n']:
        ssl_single_ctx.manager.fs.rewrite(http2_alpn_file, data)
        time.sleep(5)

        conn = ssl_single_ctx.create_h2_ssl_connection(alpn='h2')
        assert 'h2' not in (conn.sock.handshake_info.alpn_protocols or [])

    ssl_single_ctx.manager.fs.remove(http2_alpn_file)
    time.sleep(5)

    conn = ssl_single_ctx.create_h2_ssl_connection(alpn='h2')

    if http2_alpn_freq == 1:
        assert 'h2' in (conn.sock.handshake_info.alpn_protocols or [])
    else:
        assert 'h2' not in (conn.sock.handshake_info.alpn_protocols or [])


@pytest.mark.parametrize('mode', ['ip_hash', 'exp_static'])
def test_alpn_h2_mode_switch(ssl_single_ctx, mode):
    """
    BALANCER-2185
    Should be able to switch between the exp_static and ip_hash modes via the control file.
    """
    alpn_freq_file = ssl_single_ctx.manager.fs.create_file('alpn_file')
    exp_rate_file = ssl_single_ctx.manager.fs.create_file('rate_file')
    mode_file = ssl_single_ctx.manager.fs.create_file('mode_file')
    ssl_single_ctx.manager.fs.remove(mode_file)
    ssl_single_ctx.start_h2_balancer(
        http2_alpn_file=alpn_freq_file,
        http2_alpn_freq=0,
        http2_alpn_rand_mode=mode,
        http2_alpn_rand_mode_file=mode_file,
        http2_alpn_exp_id="2000",
        exp_rate_file=exp_rate_file,
    )

    time.sleep(0.1)
    # initially both modes are off
    conn = ssl_single_ctx.create_h2_ssl_connection(alpn='h2')
    assert 'h2' not in (conn.sock.handshake_info.alpn_protocols or [])

    if mode == 'exp_static':
        ssl_single_ctx.manager.fs.rewrite(alpn_freq_file, "1")
    else:
        ssl_single_ctx.manager.fs.rewrite(exp_rate_file, "2000,1")

    time.sleep(5)
    # the inactive mode's controls should be ignored
    conn = ssl_single_ctx.create_h2_ssl_connection(alpn='h2')
    assert 'h2' not in (conn.sock.handshake_info.alpn_protocols or [])

    if mode == 'exp_static':
        ssl_single_ctx.manager.fs.rewrite(exp_rate_file, "2000,1")
    else:
        ssl_single_ctx.manager.fs.rewrite(alpn_freq_file, "1")

    time.sleep(1)
    # the active mode's controls should be respected
    conn = ssl_single_ctx.create_h2_ssl_connection(alpn='h2')
    assert 'h2' in (conn.sock.handshake_info.alpn_protocols or [])

    if mode == 'exp_static':
        ssl_single_ctx.manager.fs.rewrite(mode_file, "ip_hash")
    else:
        ssl_single_ctx.manager.fs.rewrite(mode_file, "exp_static")

    time.sleep(1)
    # after the switch the new mode's state should be respected
    conn = ssl_single_ctx.create_h2_ssl_connection(alpn='h2')
    assert 'h2' in (conn.sock.handshake_info.alpn_protocols or [])

    if mode == 'exp_static':
        ssl_single_ctx.manager.fs.rewrite(alpn_freq_file, "0")
    else:
        ssl_single_ctx.manager.fs.rewrite(exp_rate_file, "2000,0")

    time.sleep(1)
    # the active mode's controls should be respected
    conn = ssl_single_ctx.create_h2_ssl_connection(alpn='h2')
    assert 'h2' not in (conn.sock.handshake_info.alpn_protocols or [])

    if mode == 'exp_static':
        ssl_single_ctx.manager.fs.rewrite(mode_file, "exp_static")
    else:
        ssl_single_ctx.manager.fs.rewrite(mode_file, "ip_hash")

    time.sleep(1)
    # after the switch the new mode's state should be respected
    conn = ssl_single_ctx.create_h2_ssl_connection(alpn='h2')
    assert 'h2' in (conn.sock.handshake_info.alpn_protocols or [])

    if mode == 'exp_static':
        ssl_single_ctx.manager.fs.rewrite(exp_rate_file, "2000,0")
    else:
        ssl_single_ctx.manager.fs.rewrite(alpn_freq_file, "0")

    time.sleep(1)
    # the active mode's controls should be respected
    conn = ssl_single_ctx.create_h2_ssl_connection(alpn='h2')
    assert 'h2' not in (conn.sock.handshake_info.alpn_protocols or [])


def test_http2_multi_stream_instance_buffer_overflow(ssl_single_ctx):
    """
    BALANCER-1161
    Balancer must send responses in all streams to client
    even if responses overflow instance buffer
    """
    stream_count = 30
    data_len = 80 * 1024
    data = 'A' * data_len
    ssl_single_ctx.start_h2_balancer(content=data, max_concurrent_streams=stream_count, http2_alpn_freq=1)
    conn = ssl_single_ctx.create_h2_ssl_connection(alpn='h2')
    conn.write_preface()
    conn.write_window_update(stream_count * data_len)
    streams = list()
    for _ in range(stream_count):
        stream = conn.create_stream()
        stream.write_message(http2.request.get(scheme='https').to_raw_request())
        stream.write_window_update(data_len)
        streams.append(stream)

    for stream in streams:
        resp = stream.read_message().to_response()
        asserts.content(resp, data)


def crl_none(ssl_client_ctx):
    return None


def crl_broken(ssl_client_ctx):
    return ssl_client_ctx.certs.abs_path('default_ticket.0.raw')


def crl_ok(ssl_client_ctx):
    return ssl_client_ctx.certs.abs_path('root_ca.crl.0.pem')


def crl_revoked(ssl_client_ctx):
    return ssl_client_ctx.certs.abs_path('root_ca.crl.1.pem')


def https_conn_no_cert(ssl_client_ctx, **kwargs):
    return ssl_client_ctx.create_https_connection(**kwargs)


def https_conn_cert(name):
    def conn_func(ssl_client_ctx, **kwargs):
        return ssl_client_ctx.create_https_connection(
            key=ssl_client_ctx.certs.abs_path('{}.key'.format(name)),
            cert=ssl_client_ctx.certs.abs_path('{}.crt'.format(name)),
            **kwargs
        )
    return conn_func


https_conn_ok = https_conn_cert('client')
https_conn_untrusted_ca = https_conn_cert('unknown')
https_conn_revoked = https_conn_cert('revoked')
https_conn_expired = https_conn_cert('expired')
https_conn_from_future = https_conn_cert('future')


@pytest.mark.parametrize(
    ['verify_peer', 'fail_if_no_peer_cert', 'crl', 'https_conn'],
    [
        (None, None, crl_none, https_conn_ok),
        (None, None, crl_ok, https_conn_ok),
        (None, False, crl_none, https_conn_ok),
        (None, False, crl_ok, https_conn_ok),
        (None, False, crl_none, https_conn_no_cert),
        (None, False, crl_ok, https_conn_no_cert),
        (False, False, crl_none, https_conn_ok),
        (False, False, crl_ok, https_conn_ok),
        (False, False, crl_none, https_conn_no_cert),
        (False, False, crl_ok, https_conn_no_cert),
        (False, False, crl_none, https_conn_untrusted_ca),
        (False, False, crl_ok, https_conn_untrusted_ca),
        (False, False, crl_revoked, https_conn_revoked),
        (False, False, crl_none, https_conn_expired),
        (False, False, crl_ok, https_conn_expired),
        (False, False, crl_none, https_conn_from_future),
        (False, False, crl_ok, https_conn_from_future),
    ],
    ids=[
        'no_crl',
        'crl',
        'not_fail_if_no_peer_cert,no_crl,cert_ok',
        'not_fail_if_no_peer_cert,crl,cert_ok',
        'not_fail_if_no_peer_cert,no_crl,no_cert',
        'not_fail_if_no_peer_cert,crl,no_cert',
        'not_verify_peer,no_crl,cert_ok',
        'not_verify_peer,crl,cert_ok',
        'not_verify_peer,no_crl,no_cert',
        'not_verify_peer,crl,no_cert',
        'not_verify_peer,no_crl,untrusted_ca',
        'not_verify_peer,crl,untrusted_ca',
        'not_verify_peer,revoked',
        'not_verify_peer,no_crl,expired',
        'not_verify_peer,crl,expired',
        'not_verify_peer,no_crl,from_future',
        'not_verify_peer,crl,from_future',
    ]
)
def test_client_ok(ssl_client_ctx, verify_peer, fail_if_no_peer_cert, crl, https_conn):
    """
    BALANCER-953
    Test in which balancer should accept incoming connections
    """
    ssl_client_ctx.start_simple_balancer(
        crl=crl(ssl_client_ctx),
        verify_peer=verify_peer,
        fail_if_no_peer_cert=fail_if_no_peer_cert,
    )
    conn = https_conn(ssl_client_ctx)
    resp = conn.perform_request(http.request.get())

    asserts.status(resp, 200)


@pytest.mark.parametrize(
    ['verify_peer', 'fail_if_no_peer_cert', 'crl', 'https_conn'],
    [
        (None, None, crl_none, https_conn_untrusted_ca),
        (None, None, crl_ok, https_conn_untrusted_ca),
        (None, None, crl_revoked, https_conn_revoked),
        (None, None, crl_none, https_conn_expired),
        (None, None, crl_ok, https_conn_expired),
        (None, None, crl_none, https_conn_from_future),
        (None, None, crl_ok, https_conn_from_future),
        (None, False, crl_none, https_conn_untrusted_ca),
        (None, False, crl_ok, https_conn_untrusted_ca),
        (None, False, crl_revoked, https_conn_revoked),
        (None, False, crl_none, https_conn_expired),
        (None, False, crl_ok, https_conn_expired),
        (None, False, crl_none, https_conn_from_future),
        (None, False, crl_ok, https_conn_from_future),
    ],
    ids=[
        'no_crl,untrusted_ca',
        'crl,untrusted_ca',
        'revoked',
        'no_crl,expired',
        'crl,expired',
        'no_crl,from_future',
        'crl,from_future',
        'not_fail_if_no_peer_cert,no_crl,untrusted_ca',
        'not_fail_if_no_peer_cert,crl,untrusted_ca',
        'not_fail_if_no_peer_cert,revoked',
        'not_fail_if_no_peer_cert,no_crl,expired',
        'not_fail_if_no_peer_cert,crl,expired',
        'not_fail_if_no_peer_cert,no_crl,from_future',
        'not_fail_if_no_peer_cert,crl,from_future',
    ]
)
def test_client_fail(ssl_client_ctx, verify_peer, fail_if_no_peer_cert, crl, https_conn):
    """
    BALANCER-953
    Test in which balancer should not accept incoming connections
    """
    ssl_client_ctx.start_simple_balancer(
        crl=crl(ssl_client_ctx),
        verify_peer=verify_peer,
        fail_if_no_peer_cert=fail_if_no_peer_cert,
    )
    conn = https_conn(ssl_client_ctx, check_closed=False)
    assert_closed(conn)


def snd_https_conn_no_cert(ssl_client_ctx, **kwargs):
    return ssl_client_ctx.create_secondary_https_connection(**kwargs)


def snd_https_conn_cert(name):
    def conn_func(ssl_client_ctx, **kwargs):
        return ssl_client_ctx.create_secondary_https_connection(
            key=ssl_client_ctx.certs.abs_path('{}.key'.format(name)),
            cert=ssl_client_ctx.certs.abs_path('{}.crt'.format(name)),
            **kwargs
        )
    return conn_func


snd_https_conn_ok = snd_https_conn_cert('client')
snd_https_conn_untrusted_ca = snd_https_conn_cert('unknown')
snd_https_conn_revoked = snd_https_conn_cert('revoked')
snd_https_conn_expired = snd_https_conn_cert('expired')
snd_https_conn_from_future = snd_https_conn_cert('future')


@pytest.mark.parametrize(
    ['verify_peer', 'fail_if_no_peer_cert', 'crl', 'https_conn'],
    [
        (None, None, crl_none, snd_https_conn_ok),
        (None, None, crl_ok, snd_https_conn_ok),
        (None, False, crl_none, snd_https_conn_ok),
        (None, False, crl_ok, snd_https_conn_ok),
        (None, False, crl_none, snd_https_conn_no_cert),
        (None, False, crl_ok, snd_https_conn_no_cert),
        (False, False, crl_none, snd_https_conn_ok),
        (False, False, crl_ok, snd_https_conn_ok),
        (False, False, crl_none, snd_https_conn_no_cert),
        (False, False, crl_ok, snd_https_conn_no_cert),
        (False, False, crl_none, snd_https_conn_untrusted_ca),
        (False, False, crl_ok, snd_https_conn_untrusted_ca),
        (False, False, crl_revoked, snd_https_conn_revoked),
        (False, False, crl_none, snd_https_conn_expired),
        (False, False, crl_ok, snd_https_conn_expired),
        (False, False, crl_none, snd_https_conn_from_future),
        (False, False, crl_ok, snd_https_conn_from_future),
    ],
    ids=[
        'no_crl',
        'crl',
        'not_fail_if_no_peer_cert,no_crl,cert_ok',
        'not_fail_if_no_peer_cert,crl,cert_ok',
        'not_fail_if_no_peer_cert,no_crl,no_cert',
        'not_fail_if_no_peer_cert,crl,no_cert',
        'not_verify_peer,no_crl,cert_ok',
        'not_verify_peer,crl,cert_ok',
        'not_verify_peer,no_crl,no_cert',
        'not_verify_peer,crl,no_cert',
        'not_verify_peer,no_crl,untrusted_ca',
        'not_verify_peer,crl,untrusted_ca',
        'not_verify_peer,revoked',
        'not_verify_peer,no_crl,expired',
        'not_verify_peer,crl,expired',
        'not_verify_peer,no_crl,from_future',
        'not_verify_peer,crl,from_future',
    ]
)
def test_client_secondary_ok(ssl_dualcerts_client_ctx, verify_peer, fail_if_no_peer_cert, crl, https_conn):
    """
    BALANCER-953
    Test in which balancer should accept incoming connections with SHA1
    """
    ssl_dualcerts_client_ctx.start_simple_balancer(
        crl=crl(ssl_dualcerts_client_ctx),
        verify_peer=verify_peer,
        fail_if_no_peer_cert=fail_if_no_peer_cert,
    )
    conn = https_conn(ssl_dualcerts_client_ctx)
    resp = conn.perform_request(http.request.get())

    asserts.status(resp, 200)
    ssl_dualcerts_client_ctx.assert_cert(conn, 'old.crt')


@pytest.mark.parametrize(
    ['verify_peer', 'fail_if_no_peer_cert', 'crl', 'https_conn'],
    [
        (None, None, crl_none, snd_https_conn_untrusted_ca),
        (None, None, crl_ok, snd_https_conn_untrusted_ca),
        (None, None, crl_revoked, snd_https_conn_revoked),
        (None, None, crl_none, snd_https_conn_expired),
        (None, None, crl_ok, snd_https_conn_expired),
        (None, None, crl_none, snd_https_conn_from_future),
        (None, None, crl_ok, snd_https_conn_from_future),
        (None, False, crl_none, snd_https_conn_untrusted_ca),
        (None, False, crl_ok, snd_https_conn_untrusted_ca),
        (None, False, crl_revoked, snd_https_conn_revoked),
        (None, False, crl_none, snd_https_conn_expired),
        (None, False, crl_ok, snd_https_conn_expired),
        (None, False, crl_none, snd_https_conn_from_future),
        (None, False, crl_ok, snd_https_conn_from_future),
    ],
    ids=[
        'no_crl,untrusted_ca',
        'crl,untrusted_ca',
        'revoked',
        'no_crl,expired',
        'crl,expired',
        'no_crl,from_future',
        'crl,from_future',
        'not_fail_if_no_peer_cert,no_crl,untrusted_ca',
        'not_fail_if_no_peer_cert,crl,untrusted_ca',
        'not_fail_if_no_peer_cert,revoked',
        'not_fail_if_no_peer_cert,no_crl,expired',
        'not_fail_if_no_peer_cert,crl,expired',
        'not_fail_if_no_peer_cert,no_crl,from_future',
        'not_fail_if_no_peer_cert,crl,from_future',
    ]
)
def test_client_secondary_fail(ssl_dualcerts_client_ctx, verify_peer, fail_if_no_peer_cert, crl, https_conn):
    """
    BALANCER-953
    Test in which balancer should not accept incoming connections with SHA1
    """
    ssl_dualcerts_client_ctx.start_simple_balancer(
        crl=crl(ssl_dualcerts_client_ctx),
        verify_peer=verify_peer,
        fail_if_no_peer_cert=fail_if_no_peer_cert,
    )
    conn = https_conn(ssl_dualcerts_client_ctx, check_closed=False)
    assert_closed(conn)


def oth_crl_ok(ssl_client_ctx):
    return ssl_client_ctx.certs.abs_path('other_root_ca.crl.0.pem')


def oth_crl_revoked(ssl_client_ctx):
    return ssl_client_ctx.certs.abs_path('other_root_ca.crl.1.pem')


def oth_https_conn_no_cert(ssl_client_ctx, **kwargs):
    return ssl_client_ctx.create_https_connection(server_name='other.yandex.ru', **kwargs)


def oth_https_conn_cert(name):
    def conn_func(ssl_client_ctx, **kwargs):
        return ssl_client_ctx.create_https_connection(
            server_name='other.yandex.ru',
            key=ssl_client_ctx.certs.abs_path('{}.key'.format(name)),
            cert=ssl_client_ctx.certs.abs_path('{}.crt'.format(name)),
            **kwargs
        )
    return conn_func


oth_https_conn_ok = oth_https_conn_cert('other_client')
oth_https_conn_untrusted_ca = oth_https_conn_cert('unknown')
oth_https_conn_revoked = oth_https_conn_cert('other_revoked')
oth_https_conn_expired = oth_https_conn_cert('other_expired')
oth_https_conn_from_future = oth_https_conn_cert('other_future')


@pytest.mark.parametrize(
    ['verify_peer', 'fail_if_no_peer_cert', 'crl', 'https_conn'],
    [
        (None, None, crl_none, oth_https_conn_ok),
        (None, None, oth_crl_ok, oth_https_conn_ok),
        (None, False, crl_none, oth_https_conn_ok),
        (None, False, oth_crl_ok, oth_https_conn_ok),
        (None, False, crl_none, oth_https_conn_no_cert),
        (None, False, oth_crl_ok, oth_https_conn_no_cert),
        (False, False, crl_none, oth_https_conn_ok),
        (False, False, oth_crl_ok, oth_https_conn_ok),
        (False, False, crl_none, oth_https_conn_no_cert),
        (False, False, oth_crl_ok, oth_https_conn_no_cert),
        (False, False, crl_none, oth_https_conn_untrusted_ca),
        (False, False, oth_crl_ok, oth_https_conn_untrusted_ca),
        (False, False, oth_crl_revoked, oth_https_conn_revoked),
        (False, False, crl_none, oth_https_conn_expired),
        (False, False, oth_crl_ok, oth_https_conn_expired),
        (False, False, crl_none, oth_https_conn_from_future),
        (False, False, oth_crl_ok, oth_https_conn_from_future),
    ],
    ids=[
        'no_crl',
        'crl',
        'not_fail_if_no_peer_cert,no_crl,cert_ok',
        'not_fail_if_no_peer_cert,crl,cert_ok',
        'not_fail_if_no_peer_cert,no_crl,no_cert',
        'not_fail_if_no_peer_cert,crl,no_cert',
        'not_verify_peer,no_crl,cert_ok',
        'not_verify_peer,crl,cert_ok',
        'not_verify_peer,no_crl,no_cert',
        'not_verify_peer,crl,no_cert',
        'not_verify_peer,no_crl,untrusted_ca',
        'not_verify_peer,crl,untrusted_ca',
        'not_verify_peer,revoked',
        'not_verify_peer,no_crl,expired',
        'not_verify_peer,crl,expired',
        'not_verify_peer,no_crl,from_future',
        'not_verify_peer,crl,from_future',
    ]
)
def test_client_sni_ok(ssl_multictx_client_ctx, verify_peer, fail_if_no_peer_cert, crl, https_conn):
    """
    BALANCER-953
    Test in which balancer should accept incoming SNI connections
    """
    ssl_multictx_client_ctx.start_simple_balancer(
        crl=crl_ok(ssl_multictx_client_ctx),
        other_crl=crl(ssl_multictx_client_ctx),
        verify_peer=verify_peer,
        fail_if_no_peer_cert=fail_if_no_peer_cert,
    )
    conn = https_conn(ssl_multictx_client_ctx)
    resp = conn.perform_request(http.request.get())

    asserts.status(resp, 200)
    ssl_multictx_client_ctx.assert_cert(conn, 'other.crt')


@pytest.mark.parametrize(
    ['verify_peer', 'fail_if_no_peer_cert', 'crl', 'https_conn'],
    [
        (None, None, crl_none, oth_https_conn_untrusted_ca),
        (None, None, oth_crl_ok, oth_https_conn_untrusted_ca),
        (None, None, oth_crl_revoked, oth_https_conn_revoked),
        (None, None, crl_none, oth_https_conn_expired),
        (None, None, oth_crl_ok, oth_https_conn_expired),
        (None, None, crl_none, oth_https_conn_from_future),
        (None, None, oth_crl_ok, oth_https_conn_from_future),
        (None, False, crl_none, oth_https_conn_untrusted_ca),
        (None, False, oth_crl_ok, oth_https_conn_untrusted_ca),
        (None, False, oth_crl_revoked, oth_https_conn_revoked),
        (None, False, crl_none, oth_https_conn_expired),
        (None, False, oth_crl_ok, oth_https_conn_expired),
        (None, False, crl_none, oth_https_conn_from_future),
        (None, False, oth_crl_ok, oth_https_conn_from_future),
    ],
    ids=[
        'no_crl,untrusted_ca',
        'crl,untrusted_ca',
        'revoked',
        'no_crl,expired',
        'crl,expired',
        'no_crl,from_future',
        'crl,from_future',
        'not_fail_if_no_peer_cert,no_crl,untrusted_ca',
        'not_fail_if_no_peer_cert,crl,untrusted_ca',
        'not_fail_if_no_peer_cert,revoked',
        'not_fail_if_no_peer_cert,no_crl,expired',
        'not_fail_if_no_peer_cert,crl,expired',
        'not_fail_if_no_peer_cert,no_crl,from_future',
        'not_fail_if_no_peer_cert,crl,from_future',
    ]
)
def test_client_sni_fail(ssl_multictx_client_ctx, verify_peer, fail_if_no_peer_cert, crl, https_conn):
    """
    BALANCER-953
    Test in which balancer should not accept incoming SNI connections
    """
    ssl_multictx_client_ctx.start_simple_balancer(
        crl=crl_ok(ssl_multictx_client_ctx),
        other_crl=crl(ssl_multictx_client_ctx),
        verify_peer=verify_peer,
        fail_if_no_peer_cert=fail_if_no_peer_cert,
    )
    conn = https_conn(ssl_multictx_client_ctx, check_closed=False)
    assert_closed(conn)


def test_client_sni_wrong_cert(ssl_multictx_client_ctx):
    """
    BALANCER-953
    Balancer should not accept connection with cert, signed by CA from another context
    """
    ssl_multictx_client_ctx.start_simple_balancer(
        crl=crl_ok(ssl_multictx_client_ctx),
        other_crl=oth_crl_ok(ssl_multictx_client_ctx),
    )
    conn1 = ssl_multictx_client_ctx.create_https_connection(
        server_name='other.yandex.ru',
        key=ssl_multictx_client_ctx.certs.abs_path('client.key'),
        cert=ssl_multictx_client_ctx.certs.abs_path('client.crt'),
        check_closed=False,
    )
    assert_closed(conn1)
    conn2 = ssl_multictx_client_ctx.create_https_connection(
        key=ssl_multictx_client_ctx.certs.abs_path('other_client.key'),
        cert=ssl_multictx_client_ctx.certs.abs_path('other_client.crt'),
        check_closed=False,
    )
    assert_closed(conn2)


def test_client_sni_default_ctx_only(ssl_multictx_client_ctx):
    """
    BALANCER-1144
    When default context does not have client cert, then
    default context's SNI requests should not require cert,
    and SNI context's requests should require and check cert.
    """
    ssl_multictx_client_ctx.start_simple_balancer(verify_peer=True, fail_if_no_peer_cert=True, erase_other_client=True)

    default_conn_cert = ssl_multictx_client_ctx.create_https_connection(
        key=ssl_multictx_client_ctx.certs.abs_path('client.key'),
        cert=ssl_multictx_client_ctx.certs.abs_path('client.crt'),
    )
    resp = default_conn_cert.perform_request(http.request.get())
    asserts.status(resp, 200)

    default_conn_no_cert = ssl_multictx_client_ctx.create_https_connection(
        check_closed=False,
    )
    assert_closed(default_conn_no_cert)

    other_conn_no_cert = ssl_multictx_client_ctx.create_https_connection(
        server_name='other.yandex.ru',
    )
    resp = other_conn_no_cert.perform_request(http.request.get())
    asserts.status(resp, 200)


def test_client_sni_other_ctx_only(ssl_multictx_client_ctx):
    """
    BALANCER-1144
    When sni context does not have client cert, but the default context does,
    then sni context must require cert, the default one must not.
    """
    ssl_multictx_client_ctx.start_simple_balancer(verify_peer=True, fail_if_no_peer_cert=True, erase_default_client=True)

    other_conn_cert = ssl_multictx_client_ctx.create_https_connection(
        server_name='other.yandex.ru',
        key=ssl_multictx_client_ctx.certs.abs_path('other.key'),
        cert=ssl_multictx_client_ctx.certs.abs_path('other.crt'),
    )
    resp = other_conn_cert .perform_request(http.request.get())
    asserts.status(resp, 200)

    other_conn_no_cert = ssl_multictx_client_ctx.create_https_connection(
        server_name='other.yandex.ru',
        check_closed=False,
    )
    assert_closed(other_conn_no_cert)

    default_conn_no_cert = ssl_multictx_client_ctx.create_https_connection()
    resp = default_conn_no_cert.perform_request(http.request.get())
    asserts.status(resp, 200)


@pytest.mark.parametrize(
    ['verify_peer', 'fail_if_no_peer_cert', 'crl'],
    [
        (None, None, crl_broken),
        (False, None, crl_ok),
    ],
    ids=[
        'broken_crl',
        'not_verify_peer,fail_if_no_peer_cert',
    ]
)
def test_client_broken_config(ssl_client_ctx, verify_peer, fail_if_no_peer_cert, crl):
    """
    BALANCER-953
    Balancer should not start with illegal parameters values
    """
    with pytest.raises(BalancerStartError):
        ssl_client_ctx.start_simple_balancer(
            crl=crl(ssl_client_ctx),
            verify_peer=verify_peer,
            fail_if_no_peer_cert=fail_if_no_peer_cert,
        )


@pytest.mark.parametrize('max_send_fragment', [16384, 4096, 512])
def test_max_send_fragment(ssl_max_send_fragment_ctx, max_send_fragment):
    """
    BALANCER-1687
    The less max_send_fragment is, the less TTFB is.
    """
    content = 'A' * 64 * 1024
    client_write_delay = 0.001
    client_write_size = 8

    min_time = (max_send_fragment / client_write_size) * client_write_delay

    ssl_max_send_fragment_ctx.start_simple_balancer(
        content=content,
        max_send_fragment=max_send_fragment,
        client_write_delay=client_write_delay,
        client_write_size=client_write_size,
    )

    with ssl_max_send_fragment_ctx.create_https_connection(conn_timeout=2 * min_time) as conn:
        stream = conn.create_stream()
        stream.write_request(http.request.raw_get())
        start = time.time()
        stream.read_response_line()
        stream.read_headers()
        conn.sock.recv(1)
        ttfb = time.time() - start
        assert ttfb >= min_time


@pytest.mark.parametrize(
    ['detroit_prob', 'vegas_prob', 'result_ctx'],
    [
        (0, 0, 'default'),
        (1, 0, 'detroit'),
        (0, 1, 'vegas'),
        (1, 1, 'default'),
    ],
    ids=['no_exp', 'exp', 'nested_exp', 'both_exps'])
def test_exp_contexts(ssl_exp_ctx, detroit_prob, vegas_prob, result_ctx):
    """
    BALANCER-1687
    If only one experiment is taken part in, experiment-local context will be selected.
    If either no or both experiments are taken part in, default context will be selected.
    """
    rate_file = ssl_exp_ctx.manager.fs.create_file('rate_file')
    ssl_exp_ctx.manager.fs.rewrite(rate_file, "2000,{}\n4000,{}".format(detroit_prob, vegas_prob))

    ssl_exp_ctx.start_complex_balancer(
        exp_id="2000",
        cont_id="1000",
        exp_id_nested="4000",
        cont_id_nested="1000",
        exp_rate_file=rate_file,
        detroit_server_name='.*\\.yandex\\.ru',
        vegas_server_name='.*\\.yandex\\.ru'
    )

    conn = ssl_exp_ctx.create_https_connection(server_name='unknown.yandex.ru')
    ssl_exp_ctx.assert_cert(conn, '{}.crt'.format(result_ctx))


def test_tickets_validation_normal(ssl_single_ctx):
    """
    Запускаем балансер в режиме валидации
    """
    ssl_single_ctx.certs.remove('default_ticket.0.key')
    ssl_single_ctx.certs.remove('default_ticket.1.key')
    ssl_single_ctx.certs.remove('default_ticket.2.key')

    ssl_single_ctx.certs.copy('default_ticket.0.pem', 'default_ticket.0.key')
    ssl_single_ctx.certs.copy('default_ticket.1.pem', 'default_ticket.1.key')
    ssl_single_ctx.certs.copy('default_ticket.2.pem', 'default_ticket.2.key')

    ssl_single_ctx.start_complex_balancer(tickets_validate=True)
    ssl_single_ctx.wait_ssllog_ticket_validate(ssl_single_ctx.balancer.config.default_log, 1)
    ssl_single_ctx.wait_ssllog_ticket_ok(ssl_single_ctx.balancer.config.default_log, 1)
    conn = ssl_single_ctx.create_https_connection()
    assert conn.sock.handshake_info.has_ticket


@pytest.mark.parametrize(
    ['idx'],
    [
        (0, ),
        (1, ),
        (2, ),
        (3, ),
    ],
    ids=['first', 'second', 'third', 'all'])
def test_tickets_validation_empty(ssl_single_ctx, idx):
    """
    Запускаем балансер в режиме валидации без одного ключа, а затем всех.
    Ожидаем что ключи не будут влюкчены.
    """
    ssl_single_ctx.certs.remove('default_ticket.0.key')
    ssl_single_ctx.certs.remove('default_ticket.1.key')
    ssl_single_ctx.certs.remove('default_ticket.2.key')

    if idx != 0 and idx != 3:
        ssl_single_ctx.certs.copy('default_ticket.0.pem', 'default_ticket.0.key')
    if idx != 1 and idx != 3:
        ssl_single_ctx.certs.copy('default_ticket.1.pem', 'default_ticket.1.key')
    if idx != 2 and idx != 3:
        ssl_single_ctx.certs.copy('default_ticket.2.pem', 'default_ticket.2.key')

    ssl_single_ctx.start_complex_balancer(tickets_validate=True)

    ssl_single_ctx.wait_ssllog_ticket_validate(ssl_single_ctx.balancer.config.default_log, 0)
    ssl_single_ctx.wait_ssllog_ticket_ok(ssl_single_ctx.balancer.config.default_log, 0)
    ssl_single_ctx.wait_ssllog_ticket_disable(ssl_single_ctx.balancer.config.default_log, 1)
    conn = ssl_single_ctx.create_https_connection()
    assert not conn.sock.handshake_info.has_ticket


@pytest.mark.parametrize(
    ['idx'],
    [
        (0, ),
        (1, ),
        (2, ),
        (3, ),
    ],
    ids=['first', 'second', 'third', 'all'])
def test_tickets_validation_broken(ssl_single_ctx, idx):
    """
    Запускаем балансер в режиме валидации с одним неправильный ключом, а затем всеми.
    Ожидаем что ключи не будут влюкчены.
    """
    new_key = "broken key"

    ssl_single_ctx.certs.remove('default_ticket.0.key')
    ssl_single_ctx.certs.remove('default_ticket.1.key')
    ssl_single_ctx.certs.remove('default_ticket.2.key')

    if idx != 0 and idx != 3:
        ssl_single_ctx.certs.copy('default_ticket.0.pem', 'default_ticket.0.key')
    else:
        ssl_single_ctx.certs.rewrite('default_ticket.0.key', new_key)
    if idx != 1 and idx != 3:
        ssl_single_ctx.certs.copy('default_ticket.1.pem', 'default_ticket.1.key')
    else:
        ssl_single_ctx.certs.rewrite('default_ticket.1.key', new_key)
    if idx != 2 and idx != 3:
        ssl_single_ctx.certs.copy('default_ticket.2.pem', 'default_ticket.2.key')
    else:
        ssl_single_ctx.certs.rewrite('default_ticket.2.key', new_key)

    ssl_single_ctx.start_complex_balancer(tickets_validate=True)

    ssl_single_ctx.wait_ssllog_ticket_validate(ssl_single_ctx.balancer.config.default_log, 0)
    ssl_single_ctx.wait_ssllog_ticket_ok(ssl_single_ctx.balancer.config.default_log, 0)
    ssl_single_ctx.wait_ssllog_ticket_disable(ssl_single_ctx.balancer.config.default_log, 1)
    conn = ssl_single_ctx.create_https_connection()
    assert not conn.sock.handshake_info.has_ticket


def test_tickets_validation_shift_one(ssl_single_ctx):
    """
    Запускаем балансер в режиме валидации. Проверяем, что
    балансер правильно определяет сдвиг на один ключ.
    """
    ssl_single_ctx.certs.remove('default_ticket.0.key')
    ssl_single_ctx.certs.remove('default_ticket.1.key')
    ssl_single_ctx.certs.remove('default_ticket.2.key')

    ssl_single_ctx.certs.copy('default_ticket.0.pem', 'default_ticket.0.key')
    ssl_single_ctx.certs.copy('default_ticket.1.pem', 'default_ticket.1.key')
    ssl_single_ctx.certs.copy('default_ticket.2.pem', 'default_ticket.2.key')

    ssl_single_ctx.start_complex_balancer(tickets_validate=True)
    ssl_single_ctx.wait_ssllog_ticket_validate(ssl_single_ctx.balancer.config.default_log, 1)
    ssl_single_ctx.wait_ssllog_ticket_ok(ssl_single_ctx.balancer.config.default_log, 1)
    conn = ssl_single_ctx.create_https_connection()
    assert conn.sock.handshake_info.has_ticket

    ssl_single_ctx.certs.copy('default_ticket.2.pem', 'default_ticket.0.key')
    ssl_single_ctx.certs.copy('default_ticket.0.pem', 'default_ticket.1.key')
    ssl_single_ctx.certs.copy('default_ticket.1.pem', 'default_ticket.2.key')
    resp = ssl_single_ctx.reload_tickets()

    conn1 = ssl_single_ctx.create_https_connection()
    ssl_single_ctx.wait_ssllog_ticket_validate(ssl_single_ctx.balancer.config.default_log, 2)
    ssl_single_ctx.wait_ssllog_ticket_ok(ssl_single_ctx.balancer.config.default_log, 2)
    ssl_single_ctx.assert_validation_status(resp, exp_statuses=[('default_ticket.0.key', 'OK'), ('default_ticket.1.key', 'OK'), ('default_ticket.2.key', 'OK')])
    ssl_single_ctx.assert_reload_status(resp, exp_statuses=[('default_ticket.0.key', 'OK'), ('default_ticket.1.key', 'OK'), ('default_ticket.2.key', 'OK')])
    assert conn1.sock.handshake_info.has_ticket


def test_tickets_validation_first_not_changed(ssl_single_ctx):
    """
    Запускаем балансер в режиме валидации. Проверяем, что
    балансер продолжает работать со старым ключом если
    новый сломан
    """
    ssl_single_ctx.certs.remove('default_ticket.0.key')
    ssl_single_ctx.certs.remove('default_ticket.1.key')
    ssl_single_ctx.certs.remove('default_ticket.2.key')

    ssl_single_ctx.certs.copy('default_ticket.0.pem', 'default_ticket.0.key')
    ssl_single_ctx.certs.copy('default_ticket.1.pem', 'default_ticket.1.key')
    ssl_single_ctx.certs.copy('default_ticket.2.pem', 'default_ticket.2.key')

    ssl_single_ctx.start_complex_balancer(tickets_validate=True)
    ssl_single_ctx.wait_ssllog_ticket_validate(ssl_single_ctx.balancer.config.default_log, 1)
    ssl_single_ctx.wait_ssllog_ticket_ok(ssl_single_ctx.balancer.config.default_log, 1)
    conn = ssl_single_ctx.create_https_connection()
    assert conn.sock.handshake_info.has_ticket

    ssl_single_ctx.certs.remove('default_ticket.0.key')
    resp = ssl_single_ctx.reload_tickets()

    conn1 = ssl_single_ctx.create_https_connection()
    ssl_single_ctx.wait_ssllog_ticket_validate(ssl_single_ctx.balancer.config.default_log, 2)
    ssl_single_ctx.wait_ssllog_ticket_ok(ssl_single_ctx.balancer.config.default_log, 2)
    ssl_single_ctx.assert_reload_status(resp, exp_statuses=[('default_ticket.0.key', 'failed'), ('default_ticket.1.key', 'OK'), ('default_ticket.2.key', 'OK')])
    assert conn1.sock.handshake_info.has_ticket
    ssl_single_ctx.assert_ticket(conn1, 'default_ticket.0.pem')


def test_tickets_validation_shift_one_fail(ssl_single_ctx):
    """
    Запускаем балансер в режиме валидации. Проверяем, что
    балансер правильно определяет неправильную последовательность
    сротированных ключей.
    """

    ssl_single_ctx.certs.remove('default_ticket.0.key')
    ssl_single_ctx.certs.remove('default_ticket.1.key')
    ssl_single_ctx.certs.remove('default_ticket.2.key')

    ssl_single_ctx.certs.copy('default_ticket.0.pem', 'default_ticket.0.key')
    ssl_single_ctx.certs.copy('default_ticket.1.pem', 'default_ticket.1.key')
    ssl_single_ctx.certs.copy('default_ticket.2.pem', 'default_ticket.2.key')

    ssl_single_ctx.start_complex_balancer(tickets_validate=True)
    ssl_single_ctx.wait_ssllog_ticket_validate(ssl_single_ctx.balancer.config.default_log, 1)
    ssl_single_ctx.wait_ssllog_ticket_ok(ssl_single_ctx.balancer.config.default_log, 1)
    conn = ssl_single_ctx.create_https_connection()
    assert conn.sock.handshake_info.has_ticket
    time.sleep(2)

    ssl_single_ctx.certs.copy('default_ticket.2.pem', 'default_ticket.0.key')
    ssl_single_ctx.certs.copy('default_ticket.1.pem', 'default_ticket.1.key')
    ssl_single_ctx.certs.copy('default_ticket.0.pem', 'default_ticket.2.key')
    resp = ssl_single_ctx.reload_tickets()

    time.sleep(2)
    ssl_single_ctx.assert_reload_status(resp, [('default_ticket.0.key', 'OK'), ('default_ticket.1.key', 'OK'), ('default_ticket.2.key', 'OK')])
    ssl_single_ctx.assert_validation_status(resp, [('default_ticket.0.key', 'OK'), ('default_ticket.1.key', 'failed'), ('default_ticket.2.key', 'failed')])

    ssl_single_ctx.wait_ssllog_ticket_validate(ssl_single_ctx.balancer.config.default_log, 1)
    ssl_single_ctx.wait_ssllog_ticket_ok(ssl_single_ctx.balancer.config.default_log, 1)
    ssl_single_ctx.wait_ssllog_ticket_restored(ssl_single_ctx.balancer.config.default_log, 1)

    ssl_single_ctx.assert_ticket(conn, 'default_ticket.0.pem')

    conn1 = ssl_single_ctx.create_https_connection()
    assert conn1.sock.handshake_info.has_ticket


def test_tickets_validation_shift_one_fail_force(ssl_single_ctx):
    """
    Запускаем балансер в режиме валидации. Проверяем, что
    балансер правильно определяет принудительную загрузку ключей.
    """
    ssl_single_ctx.certs.remove('default_ticket.0.key')
    ssl_single_ctx.certs.remove('default_ticket.1.key')
    ssl_single_ctx.certs.remove('default_ticket.2.key')

    ssl_single_ctx.certs.copy('default_ticket.0.pem', 'default_ticket.0.key')
    ssl_single_ctx.certs.copy('default_ticket.1.pem', 'default_ticket.1.key')
    ssl_single_ctx.certs.copy('default_ticket.2.pem', 'default_ticket.2.key')

    ssl_single_ctx.start_complex_balancer(tickets_validate=True)
    ssl_single_ctx.wait_ssllog_ticket_validate(ssl_single_ctx.balancer.config.default_log, 1)
    ssl_single_ctx.wait_ssllog_ticket_ok(ssl_single_ctx.balancer.config.default_log, 1)
    conn = ssl_single_ctx.create_https_connection()
    assert conn.sock.handshake_info.has_ticket
    ssl_single_ctx.assert_ticket(conn, 'default_ticket.0.pem')
    time.sleep(2)

    ssl_single_ctx.certs.copy('default_ticket.2.pem', 'default_ticket.0.key')
    ssl_single_ctx.certs.copy('default_ticket.1.pem', 'default_ticket.1.key')
    ssl_single_ctx.certs.copy('default_ticket.0.pem', 'default_ticket.2.key')
    resp = ssl_single_ctx.reload_tickets(force=True)

    time.sleep(2)
    ssl_single_ctx.assert_reload_status(resp, [('default_ticket.0.key', 'OK'), ('default_ticket.1.key', 'OK'), ('default_ticket.2.key', 'OK')])

    ssl_single_ctx.wait_ssllog_ticket_validate(ssl_single_ctx.balancer.config.default_log, 2)
    ssl_single_ctx.wait_ssllog_ticket_ok(ssl_single_ctx.balancer.config.default_log, 2)
    ssl_single_ctx.wait_ssllog_ticket_restored(ssl_single_ctx.balancer.config.default_log, 0)

    conn1 = ssl_single_ctx.create_https_connection()
    assert conn1.sock.handshake_info.has_ticket
    ssl_single_ctx.assert_ticket(conn1, 'default_ticket.2.pem')


def test_validate_cert_date_expired(ssl_single_ctx):
    """
    BALANCER-2576 Валидация конфига на старте. Не стартуем если сертификат истек.
    """
    with pytest.raises(BalancerStartError):
        ssl_single_ctx.start_simple_balancer(
            validate_cert_date=True,
            cert=ssl_single_ctx.certs.abs_path('expired.crt'),
            priv=ssl_single_ctx.certs.abs_path('expired.key'))


def test_validate_cert_date_future(ssl_single_ctx):
    """
    BALANCER-2576 Валидация конфига на старте. Не стартуем если сертификат еще не начал действовать.
    """
    with pytest.raises(BalancerStartError):
        ssl_single_ctx.start_simple_balancer(
            validate_cert_date=True,
            cert=ssl_single_ctx.certs.abs_path('future.crt'),
            priv=ssl_single_ctx.certs.abs_path('future.key'))


def test_early_data_http1(ssl_single_ctx):
    """
    BALANCER-1403 Проверка отправки early data(0RTT) для plain text HTTP
    """
    balancer_ctx = ssl_single_ctx.start_complex_balancer(ssl_protocols="tlsv1.3", early_data=True)

    earlydata.simple_http("::1", balancer_ctx.config.port)


def test_early_data_http1_default_item(ssl_single_ctx):
    """
    BALANCER-1403 Проверка, что работает выставление флага в TSslItem
    """
    balancer_ctx = ssl_single_ctx.start_complex_balancer(ssl_protocols="tlsv1.3", early_data=False, default_item_early_data=True)

    earlydata.simple_http("::1", balancer_ctx.config.port)


def test_early_data_http1_fail(ssl_single_ctx):
    """
    BALANCER-1403 Проверка отключения early data
    """
    balancer_ctx = ssl_single_ctx.start_complex_balancer(ssl_protocols="tlsv1.3", early_data=False, default_item_early_data=False)

    with pytest.raises(earlydata.WriteEarlyDataError):
        earlydata.simple_http("::1", balancer_ctx.config.port)


def test_early_data_http2(ssl_single_ctx):
    """
    BALANCER-1403 Проверка отправки early data для HTTP2 в нормальном режиме работы.
    Отправляем PREFACE и HEADER в одном фрейме early data.
    """
    balancer_ctx = ssl_single_ctx.start_complex_balancer(ssl_protocols="tlsv1.3", early_data=True, alpn_freq=1)

    earlydata.simple_http2("::1", balancer_ctx.config.port)


def test_early_data_http2_partial_preface(ssl_single_ctx):
    """
    BALANCER-1403 Фиксация поведения связанного с предполагаемым багом в Chrome.
    Если нам пришел PREFACE в early data без HEADER, то пытаемся вычитать данные
    из последующих стримов HTTP2.
    """
    balancer_ctx = ssl_single_ctx.start_complex_balancer(ssl_protocols="tlsv1.3", early_data=True, alpn_freq=1)

    earlydata.separate_preface_http2("::1", balancer_ctx.config.port)
