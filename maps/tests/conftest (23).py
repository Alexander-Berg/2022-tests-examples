import os
import pathlib
import tempfile
from contextlib import contextmanager
from unittest.mock import patch

import aiohttp.pytest_plugin
import pytest
from aresponses import ResponsesMockServer
from faker import Faker

from smb.common.multiruntime.lib.basics import is_arcadia_python

from maps_adv.export.lib import Application
from maps_adv.export.lib.config import config as app_config
from maps_adv.export.lib.core.url_signer import ActionUrlSigner

from . import coro_mock

pytest_plugins = ["aiohttp.pytest_plugin"]
del aiohttp.pytest_plugin.loop  # DEVTOOLS-5496


def pytest_configure(config):
    config.addinivalue_line("markers", "experiments")


@pytest.fixture
def loop(event_loop):
    return event_loop


DEFAULT_XML_SCHEMA_PATH = "schemas/biz/advert/1.x/pins.xsd"
DEFAULT_W3C_SCHEMA_PATH = "schemas/w3c.xsd"

RESOURCE_EXAMPLE_PINS_PATH = "schemas/biz/advert/1.x/examples/pins.xml"

if not is_arcadia_python:
    ARCADIA_ROOT = str(
        pathlib.Path(
            pathlib.Path(__file__).absolute().parent, "..", "..", ".."
        ).resolve()
    )

    DEFAULT_XML_SCHEMA_PATH = str(
        pathlib.Path(ARCADIA_ROOT, "maps/doc", DEFAULT_XML_SCHEMA_PATH).resolve()
    )

    DEFAULT_W3C_SCHEMA_PATH = str(
        pathlib.Path(
            ARCADIA_ROOT, "maps_adv/export/resources", DEFAULT_W3C_SCHEMA_PATH
        ).resolve()
    )

    RESOURCE_EXAMPLE_PINS_PATH = str(
        pathlib.Path(ARCADIA_ROOT, "maps/doc", RESOURCE_EXAMPLE_PINS_PATH).resolve()
    )


_config = {
    "RELAUNCH_INTERVAL": {"default": 60, "converter": int},
    "AVATARS_NAMESPACE": {"default": "test-avatars-namespace"},
    "ADV_STORE_URL": {"default": "https://example.com/"},
    "BILLING_URL": {"default": "https://example.com/"},
    "OLD_GEOADV_URL": {"default": "https://example.com/"},
    "WARDEN_URL": {"default": "https://example.com/"},
    "POINTS_URL": {"default": "https://example.com/"},
    "DASHBOARD_URL": {"default": "https://example.com/"},
    "SANDBOX_USER": {"default": "TEST_USER"},
    "SANDBOX_TOKEN": {"default": "test-sandbox-token"},
    "SANDBOX_URL": {"default": "https://example.com/"},
    "SANDBOX_PROXY_URL": {"default": "https://example.com/"},
    "SANDBOX_EXPORT_RESOURCE_TYPE": {"default": "TEST_RESOURCE"},
    "SANDBOX_EXPORT_RELEASE_TYPE": {"default": "testing"},
    "FILENAME_XML": {"default": "adv.xml"},
    "XML_SCHEMA_PATH": {"default": DEFAULT_XML_SCHEMA_PATH},
    "W3C_SCHEMA_PATH": {"default": DEFAULT_W3C_SCHEMA_PATH},
    # settings for retrying requests to any server
    "RETRY_MAX_ATTEMPTS": {"default": 1},
    "RETRY_WAIT_MULTIPLIER": {"default": 0.1},
    "MONKEY_PATCH_MERGE_CATEGORY_CAMPAIGNS": {"default": {1356: [200000]}},
    "NAVI_CLIENT_ID": {"default": 261, "converter": int},
    "NAVI_SIGNATURE_FILE": {"default": ""},
    "NANNY_SERVICE_ID": {"default": "test"},
    "INSTANCE_TAG_CTYPE": {"default": "testing"},
}

NAVI_SIGNATURE_KEY = """-----BEGIN RSA PRIVATE KEY-----
MIIBPAIBAAJBAJ8IMm24PQVV0T0tD7z61iytXoUbeRwhr8uYuG+Y5VDccEXv2XDZ
TzNCPhJcoTYNdx44KP3dzfgb+wPECphKr8sCAwEAAQJAN70tv5sFeCs97Q0wKPJZ
wsr5B/o7FosQDHH4otSZ+x7ai+g1SdRdO6+ynLhmpVpnPCgm3Dnv5gXo3AzVOmFV
IQIhAND+zJlQNF5jDQJxkPjNqyLygtMiRyEwzUYm72e6lq+pAiEAwsywzPR4MWL/
xz9mRJibm34ILP7/l6A2iMu81q03XFMCIQC4nz3Seb2pW8rUS8qLX/Q8TQswxGkd
cuUDgcWfVn9i8QIhAMCXUu0vfm2FbVB2lAuZva67qiWibtxf38rbc3Xjh2pVAiEA
gH6ZgIDedboWGEv8oROh5XIwxPp92tJ1sm7JL09RBWI=
-----END RSA PRIVATE KEY-----"""


@pytest.fixture(scope="session")
def signature_file():
    signature_file = tempfile.NamedTemporaryFile(delete=False)
    with open(signature_file.name, "w") as f:
        f.write(NAVI_SIGNATURE_KEY)
    yield signature_file
    os.remove(signature_file.name)


@pytest.fixture(scope="session")
def config(signature_file):
    _config["NAVI_SIGNATURE_FILE"] = {"default": signature_file.name}
    with patch.dict(app_config.options_map, _config):
        app_config.init()
        yield app_config


@pytest.fixture
def experimental_options(config):
    @contextmanager
    def wrapped(flags: dict):
        patch_config = {key: {"default": value} for key, value in flags.items()}

        try:
            with patch.dict(config.options_map, patch_config):
                app_config.init()
                yield app_config
        finally:
            app_config.init()

    return wrapped


@pytest.fixture
async def setup_app(config, aiohttp_client):
    async def _make_app():
        app = Application()
        await app.setup()

        return await aiohttp_client(app.api)

    yield _make_app


@pytest.fixture
async def api_client(setup_app, mocker):
    mocker.patch(
        "maps_adv.export.lib.application.export_pipeline",
        new_callable=lambda: coro_mock,
    )
    return await setup_app()


@pytest.fixture(scope="session")
def faker():
    fake = Faker("ru_RU")
    return fake


@pytest.fixture
async def aresponses(event_loop):
    async with ResponsesMockServer(loop=event_loop) as server:
        yield server


@pytest.fixture
async def rmock(aresponses):
    return lambda *a, **b: aresponses.add("example.com", *a, **b)


@pytest.fixture
def mock_points(rmock):
    return lambda h: rmock("/api/v1/points/billboard/1/by-polygons/", "POST", h)


@pytest.fixture
def mock_billing(rmock):
    return lambda h: rmock("/orders/active/", "POST", h)


@pytest.fixture
def mock_billing_orders_discounts(rmock):
    return lambda h: rmock("/orders/discounts/", "POST", h)


@pytest.fixture
def mock_adv_store(rmock):
    return lambda h: rmock("/v2/campaigns/export/", "GET", h)


@pytest.fixture
def mock_old_geoadv(rmock, config):
    return lambda h: rmock("/campaigns/api/v2/organizations/resolve", "POST", h)


@pytest.fixture
def mock_dashboard(rmock):
    return lambda h: rmock("/statistics/campaigns/events/", "POST", h)


@pytest.fixture
def xml_example_data():
    with open(RESOURCE_EXAMPLE_PINS_PATH, "rb") as inp:
        return inp.read()


@pytest.fixture
def navi_uri_signer(config):
    return ActionUrlSigner(config.NAVI_CLIENT_ID, config.NAVI_SIGNATURE_FILE)
