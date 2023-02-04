"""
Tests for identity's DB unavailability retrying functionality
"""

from .conftest import IdentityApp
from .local_pg import create_local_pg
from .util import TEST_APIKEY, KeycloakUser

import logging
import time
from typing import Iterator

from contextlib import contextmanager
import pytest
import selectors
import socket
import socketserver
import threading

from yatest.common import network


logger = logging.getLogger(__name__)


class ProxyingTCPHandler(socketserver.BaseRequestHandler):
    def handle(self):
        with socket.socket(socket.AF_INET6, socket.SOCK_STREAM) as target_sock, selectors.DefaultSelector() as selector:
            if not self.server.proxy_target:
                raise ValueError('Proxy target must be set!')

            target_sock.connect(self.server.proxy_target)
            selector.register(target_sock, selectors.EVENT_READ, self.request)
            selector.register(self.request, selectors.EVENT_READ, target_sock)

            while True:
                events = selector.select()
                for key, _ in events:
                    data = key.fileobj.recv(1024)
                    if not data or self.server.should_drop():
                        return
                    key.data.sendall(data)


class ProxyingTCPServer(socketserver.ThreadingTCPServer):
    daemon_threads = True
    block_on_close = False
    address_family = socket.AF_INET6

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.proxy_target = None
        self.drop_until = time.monotonic()

    def drop_connections_for_next_seconds(self, seconds: float) -> None:
        self.drop_until = time.monotonic() + seconds

    def should_drop(self) -> bool:
        return time.monotonic() < self.drop_until

    def verify_request(self, request, client_address):
        return not self.should_drop()


@contextmanager
def create_proxy_server(listen_address: tuple[str, int]) -> ProxyingTCPServer:
    server = ProxyingTCPServer(listen_address, ProxyingTCPHandler)
    t = threading.Thread(target=server.serve_forever)
    t.daemon = True
    t.start()
    logger.info(f'TCP proxy server created: {server.server_address}')
    yield server

    logger.info('TCP proxy server teardown...')
    server.shutdown()
    t.join()
    server.server_close()
    logger.info('TCP proxy server teardown finished')


@pytest.fixture(scope='module')
def proxied_pg_instance(port_manager: network.PortManager) -> Iterator[tuple[dict[str, str], ProxyingTCPServer]]:
    with (
        create_proxy_server(listen_address=('localhost', port_manager.get_port())) as proxy,
        create_local_pg() as pg_config,
    ):
        proxy.proxy_target = pg_config['host'], pg_config['port']
        pg_config['host'], pg_config['port'], _, _ = proxy.server_address
        logger.info(f'Proxied DB created: {(pg_config["host"], pg_config["port"])} -> {proxy.proxy_target}')
        yield pg_config, proxy


@pytest.fixture(scope='module')
def identity_app_with_proxied_db(
    proxied_pg_instance: tuple[dict[str, str], ProxyingTCPServer], port_manager: network.PortManager
) -> Iterator[IdentityApp]:
    pg_config, _ = proxied_pg_instance
    port = port_manager.get_port()
    identity_app = IdentityApp(port, pg_config)

    try:
        identity_app.start()
        yield identity_app
    finally:
        identity_app.stop(timeout=30)
        assert 'Using PostgreSQL storage' in identity_app.stderr.decode('utf-8')


@pytest.fixture(scope='module')
def request_definitions_all(identity_app_with_proxied_db):
    company_id = 2  # Some guesswork involved here
    yield {
        'company_token': (f'/internal/tokens/company?apikey={TEST_APIKEY}', {}),
        'user_token': ('/internal/tokens/user', {'headers': KeycloakUser('app', 'app_uid').jwt_headers()}),
        'all_companies': (
            '/companies',
            {'headers': KeycloakUser('superuser', 'superuser_uid').authenticate(identity_app_with_proxied_db)},
        ),
        'create_company': (
            '/companies',
            {
                'headers': KeycloakUser('superuser', 'superuser_uid').authenticate(identity_app_with_proxied_db),
                'method': 'POST',
                'json': {'apikey': '7732f91b-278b-4846-b283-83c433499637'},
            },
        ),
        'get_company': (
            f'/companies/{company_id}',
            {'headers': KeycloakUser('superuser', 'superuser_uid').authenticate(identity_app_with_proxied_db)},
        ),
        'patch_company': (
            f'/companies/{company_id}',
            {
                'headers': KeycloakUser('superuser', 'superuser_uid').authenticate(identity_app_with_proxied_db),
                'method': 'PATCH',
                'json': {'apikey': '1786cdf1-08f2-43ad-a90b-05008d366f48'},
            },
        ),
        'delete_company': (
            f'/companies/{company_id}',
            {
                'headers': KeycloakUser('superuser', 'superuser_uid').authenticate(identity_app_with_proxied_db),
                'method': 'DELETE',
            },
        ),
    }


@pytest.fixture(
    scope='module',
    params=[
        'company_token',
        'user_token',
        'all_companies',
        'create_company',
        'get_company',
        'patch_company',
        'delete_company',
    ],
)
def request_definition(request, request_definitions_all):
    yield request_definitions_all[request.param]


def test_db_retrying(identity_app_with_proxied_db, proxied_pg_instance, request_definition):
    _, db_proxy_server = proxied_pg_instance
    route, request_kwargs = request_definition

    db_proxy_server.drop_connections_for_next_seconds(2.0)
    identity_app_with_proxied_db.checked_request(route, read_timeout=10, **request_kwargs)
