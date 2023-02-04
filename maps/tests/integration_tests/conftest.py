import logging
import typing as tp
from pathlib import Path

import pytest

import yatest.common as yc
from maps.pylibs.test_helpers.nginx_runner import Nginx
from maps.pylibs.test_helpers.pycare_runner import PycareApp
from maps.pylibs.test_helpers.roquefort_runner import Roquefort
from yatest.common import network


logger = logging.getLogger('test-logger')


class TestApp(PycareApp):
    def __init__(self, listen_port: int) -> None:
        super().__init__(
            binary_dir=Path(yc.binary_path('maps/infra/pycare/example/bin/')),
            app_name='example',
            listen_port=listen_port
        )

    def _process_name(self) -> str:
        return 'testapp'


@pytest.fixture(scope='session')
def port_manager() -> tp.Iterator[network.PortManager]:
    with network.PortManager() as manager:
        yield manager


@pytest.fixture(scope='session')
def testapp(port_manager: network.PortManager) -> tp.Iterator[TestApp]:
    port = port_manager.get_port()
    testapp = TestApp(port)
    try:
        testapp.start()
        yield testapp
    finally:
        testapp.stop(timeout=30)


@pytest.fixture(scope='function')
def testapp_once(port_manager: network.PortManager) -> tp.Iterator[TestApp]:
    port = port_manager.get_port()
    testapp = TestApp(port)
    try:
        testapp.start()
        yield testapp
    finally:
        testapp.stop(timeout=30)


@pytest.fixture(scope='session')
def nginx(port_manager: network.PortManager, testapp: TestApp) -> tp.Iterator[Nginx]:
    listen_port = port_manager.get_port()
    nginx = Nginx(listen_port)
    try:
        nginx.add_service_config(
            name='example.conf',
            config=testapp.nginx_config()
        )
        nginx.start()
        yield nginx
    finally:
        nginx.stop()


@pytest.fixture(scope='session')
def roquefort(testapp: TestApp) -> tp.Iterator[Roquefort]:
    roquefort = Roquefort()
    roquefort.add_service_config(
        name='example.conf',
        config=testapp.roquefort_config()
    )
    yield roquefort
