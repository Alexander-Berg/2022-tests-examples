import pytest

from maps_adv.call_proxy.lib import Application
from maps_adv.call_proxy.lib.domain import Domain

pytest_plugins = [
    "aiohttp.pytest_plugin",
    "maps_adv.common.lasagna.pytest.plugin",
    "maps_adv.common.blackbox.pytest.plugin",
]

_config = dict(
    TVM_DAEMON_URL="http://tvm.daemon",
    TVM_TOKEN="tvm-token",
    BLACKBOX_URL="http://blackbox.api",
    BLACKBOX_SESSION_HOST="session.host",
)


@pytest.fixture(scope="session")
def config():
    return _config.copy()


@pytest.fixture
def app(config):
    return Application(config)


@pytest.fixture
def domain(blackbox_client):
    return Domain(blackbox_client=blackbox_client)
