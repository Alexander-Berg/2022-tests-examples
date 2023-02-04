"""
Why is this needed:
We run a pginx binary on localhost during tests, and we need to be able to make HTTPS requests to it and check
cert validity. But SNI gets in the way, because we make connections to a local IP address, which doesn't match
hostname in the certificate. To work around this, we use a custom HTTPS adapter.

Based on https://github.com/Roadmaster/forcediphttpsadapter
"""


from __future__ import absolute_import

import collections
import functools
from socket import error as SocketError, timeout as SocketTimeout

import requests
from requests.adapters import HTTPAdapter, DEFAULT_POOLSIZE, DEFAULT_RETRIES, DEFAULT_POOLBLOCK
from urllib3.connection import HTTPSConnection
from urllib3.exceptions import ConnectTimeoutError, NewConnectionError
from urllib3.poolmanager import (
    PoolManager,
    HTTPSConnectionPool,
    _default_key_normalizer,
    _key_fields
)
from urllib3.util import connection, ssl_
from six.moves.urllib import parse as urlparse

from awtest.network import is_ipv6_address


def modify_url(url, hostname=None, port=None, port_offset=0, scheme=None):
    assert not port or not port_offset
    u = urlparse.urlparse(url)

    return urlparse.ParseResult(
        scheme=scheme or u.scheme,
        netloc=u'{}:{}'.format(
            url_escape_hostname(hostname or u.hostname),
            u.port + port_offset if port is None else port
        ),
        path=u.path, params=u.params, query=u.query, fragment=u.fragment).geturl()


def get_https_session(balancer, balancer_url, hostname, ciphers=None, ssl_version=None, verify=True):
    https_url = modify_url(balancer_url, hostname=hostname, port=balancer.https_port, scheme=u'https')
    s = requests.Session()
    s.mount(https_url, AwacsHTTPSAdapter(hostname, balancer_url, ciphers=ciphers, ssl_version=ssl_version, verify=verify))
    s.headers = {
        b'Host': hostname,
    }
    return s, https_url


def url_escape_hostname(hostname):
    if is_ipv6_address(hostname):
        return u'[{}]'.format(hostname)
    else:
        return hostname


def get_url_hostname(url):
    u = urlparse.urlparse(url)
    return u.hostname


class AwacsHTTPSAdapter(HTTPAdapter):
    def __init__(self, fqdn, balancer_url, ciphers=None, ssl_version=None,
                 pool_connections=DEFAULT_POOLSIZE, pool_maxsize=DEFAULT_POOLSIZE,
                 max_retries=DEFAULT_RETRIES, pool_block=DEFAULT_POOLBLOCK,
                 verify=True):
        self.fqdn = fqdn
        self.dest_ip = url_escape_hostname(get_url_hostname(balancer_url))
        self.ciphers = ciphers
        self.ssl_version = ssl_version
        self.verify = verify
        super(AwacsHTTPSAdapter, self).__init__(
            pool_connections=pool_connections,
            pool_maxsize=pool_maxsize,
            max_retries=max_retries,
            pool_block=pool_block
        )

    def init_poolmanager(self, connections, maxsize, block=DEFAULT_POOLBLOCK, **pool_kwargs):
        if self.ciphers:
            ssl_context = ssl_.create_urllib3_context(ciphers=self.ciphers)
            if not self.verify:
                ssl_context.check_hostname = False
                ssl_context.verify_mode = ssl_.CERT_NONE
            pool_kwargs['ssl_context'] = ssl_context
        if self.verify:
            pool_kwargs['assert_hostname'] = str(self.fqdn)
        else:
            pool_kwargs['assert_hostname'] = False
        pool_kwargs['dest_ip'] = self.dest_ip
        pool_kwargs['ssl_version'] = self.ssl_version
        self.poolmanager = AwacsHTTPSPoolManager(connections, maxsize, block=block, **pool_kwargs)


class AwacsHTTPSPoolManager(PoolManager):
    AwacsPoolKey = collections.namedtuple(u'PoolKey', list(_key_fields) + [u'key_dest_ip', ])

    def __init__(self, *args, **kwargs):
        self.dest_ip = kwargs.pop('dest_ip', None)
        super(AwacsHTTPSPoolManager, self).__init__(*args, **kwargs)
        self.key_fn_by_scheme[u'http'] = functools.partial(_default_key_normalizer, self.AwacsPoolKey)
        self.key_fn_by_scheme[u'https'] = functools.partial(_default_key_normalizer, self.AwacsPoolKey)

    def _new_pool(self, scheme, host, port, request_context=None):
        kwargs = self.connection_pool_kw
        assert scheme == u'https'
        kwargs[u'dest_ip'] = self.dest_ip
        return AwacsHTTPSConnectionPool(host, port, **kwargs)


class AwacsHTTPSConnectionPool(HTTPSConnectionPool):
    def __init__(self, *args, **kwargs):
        self.dest_ip = kwargs.pop('dest_ip', None)
        super(AwacsHTTPSConnectionPool, self).__init__(*args, **kwargs)

    def _new_conn(self):
        self.num_connections += 1

        actual_host = self.host
        actual_port = self.port
        if self.proxy is not None:
            actual_host = self.proxy.host
            actual_port = self.proxy.port

        self.conn_kw = getattr(self, u'conn_kw', {})
        self.conn_kw[u'dest_ip'] = self.dest_ip
        conn = AwacsHTTPSConnection(
            host=actual_host, port=actual_port,
            timeout=self.timeout.connect_timeout,
            strict=self.strict, **self.conn_kw)
        pc = self._prepare_conn(conn)
        return pc

    def __str__(self):
        return u'%s(host=%r, port=%r, dest_ip=%s)' % (type(self).__name__, self.host, self.port, self.dest_ip)


class AwacsHTTPSConnection(HTTPSConnection, object):
    def __init__(self, *args, **kwargs):
        self.dest_ip = kwargs.pop('dest_ip', None)
        super(AwacsHTTPSConnection, self).__init__(*args, **kwargs)

    def _new_conn(self):
        extra_kw = {}
        if self.source_address:
            extra_kw[u'source_address'] = self.source_address
        if getattr(self, u'socket_options', None):
            extra_kw[u'socket_options'] = self.socket_options
        dest_host = self.dest_ip if self.dest_ip else self.host
        try:
            conn = connection.create_connection((dest_host, self.port), self.timeout, **extra_kw)
        except SocketTimeout:
            raise ConnectTimeoutError(
                self, u'Connection to {} timed out. (connect timeout={})'.format(self.host, self.timeout))
        except SocketError as e:
            raise NewConnectionError(
                self, u'Failed to establish a new connection: {}'.format(e))
        return conn
