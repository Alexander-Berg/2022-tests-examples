import logging
import pytest

from maps.infra.sedem.machine.lib.token import Token
from maps.infra.sedem.machine.tests.integration_tests.fixtures.machine_fixture import MachineFixture
from maps.pylibs.fixtures.api_fixture import ApiFixture
from maps.pylibs.fixtures.arc_fixture import ArcFixture
from maps.pylibs.fixtures.abc_fixture import ABCFixture
from maps.pylibs.fixtures.arcadia.fixture import ArcadiaFixture

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)


@pytest.fixture(scope='function', autouse=True)
def sedem_machine_fixture(monkeypatch, fixture_factory):
    monkeypatch.setattr(Token, 'get', lambda *_: 'Fake token')
    fixture_factory(ArcFixture)         # ensure arc mocked
    fixture_factory(ABCFixture)         # ensure abc mocked
    fixture_factory(ArcadiaFixture)     # ensure arcadia mocked
    fixture_factory(ApiFixture)         # ensure api mocked
    fixture_factory(MachineFixture)     # ensure machine mocked
