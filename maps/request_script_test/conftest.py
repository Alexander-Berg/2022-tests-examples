import logging
import typing as tp
from pathlib import Path

import pytest

import yatest.common as yc
from maps.pylibs.test_helpers.yacare_runner import YacareApp


logger = logging.getLogger('test_logger')


class TestApp(YacareApp):
    def __init__(self) -> None:
        super().__init__(
            binary_dir=Path(yc.binary_path('maps/infra/yacare/testapp/bin/')),
            app_name='yacare-testapp'
        )
        if self.listen_socket_path.exists():
            self.listen_socket_path.unlink()

    def healthcheck(self) -> bool:
        # Replace default healthcheck as it use the lib we are testing here
        if self.listen_socket_path.exists():
            return True
        logger.warning(f'{self.app_name} still has no unix socket: {self.listen_socket_path}')
        return False


@pytest.fixture(scope='session')
def testapp() -> tp.Generator[TestApp, None, None]:
    testapp = TestApp()
    try:
        testapp.start()
        yield testapp
    finally:
        # insreased timeout for test_receive_timeout
        # testapp handler will sleep 30s and yacare won't stop until handler finish
        testapp.stop(timeout=60)


@pytest.fixture(scope='session')
def unix_socket_path(testapp: TestApp) -> str:
    return str(testapp.listen_socket_path)
