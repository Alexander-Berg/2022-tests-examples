import logging
import typing as tp
from pathlib import Path

import pytest

import yatest.common as yc
from yatest.common.network import PortManager
from maps.pylibs.test_helpers.yacare_runner import YacareApp


logger = logging.getLogger('test-logger')


class TestApp(YacareApp):
    def __init__(self) -> None:
        super().__init__(
            binary_dir=Path(yc.binary_path('maps/infra/yacare/testapp/bin/')),
            app_name='yacare-testapp'
        )


@pytest.fixture(scope='session')
def testapp() -> tp.Generator[TestApp, None, None]:
    yield TestApp()


@pytest.fixture
def freeport() -> tp.Generator[int, None, None]:
    with PortManager() as pm:
        yield pm.get_port()
