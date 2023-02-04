import logging
import typing as tp
from pathlib import Path

import pytest

from maps.pylibs.test_helpers.nginx_runner import Nginx
from maps.pylibs.test_helpers.yacare_runner import YacareApp
import yatest.common as yc
from yatest.common import network


logger = logging.getLogger('test-logger')


class TestApp(YacareApp):
    def __init__(self) -> None:
        super().__init__(
            binary_dir=Path(yc.binary_path('maps/infra/yacare/testapp/bin/')),
            app_name='yacare-testapp'
        )


@pytest.fixture(scope='session')
def port_manager() -> tp.Generator[network.PortManager, None, None]:
    with network.PortManager() as manager:
        yield manager


@pytest.fixture(scope='session')
def testapp() -> tp.Generator[TestApp, None, None]:
    testapp = TestApp()
    try:
        testapp.start()
        yield testapp
    finally:
        testapp.stop()


@pytest.fixture(scope='session')
def nginx(port_manager: network.PortManager, testapp: TestApp) -> tp.Generator[Nginx, None, None]:
    listen_port = port_manager.get_port()
    nginx = Nginx(listen_port)
    try:
        nginx.add_service_config(
            name='yacare-testapp.conf',
            config=testapp.nginx_config()
        )
        nginx.start()
        yield nginx
    finally:
        nginx.stop()
