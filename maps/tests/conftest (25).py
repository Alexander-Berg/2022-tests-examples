import freezegun
import pytest

from maps_adv.geosmb.booking_yang.server.lib import Application
from maps_adv.geosmb.booking_yang.server.lib.db.engine import DB

# Hack to make asyncio.sleep() ignore frozen time
freezegun.configure(extend_ignore_list=["asyncio", "concurrent"])

pytest_plugins = [
    "aiohttp.pytest_plugin",
    "smb.common.aiotvm.pytest.plugin",
    "maps_adv.common.lasagna.pytest.plugin",
    "smb.common.pgswim.pytest.plugin",
    "maps_adv.common.shared_mock.pytest.plugin",
    "maps_adv.geosmb.clients.geosearch.pytest.plugin",
    "maps_adv.geosmb.doorman.client.pytest.plugin",
    "maps_adv.geosmb.booking_yang.server.tests.factory",
    "maps_adv.geosmb.booking_yang.server.tests.utils",
]

_config = dict(
    DATABASE_URL=(
        "postgresql://booking_yang:booking_yang@localhost:5433/booking_yang"
        "?master_as_replica=true"
    ),
    DOORMAN_TVM="doorman",
    DOORMAN_URL=None,
    GEO_PRODUCT_BIZ_OWNER_UID=123456789,
    GEO_PRODUCT_URL="http://geoproduct.test",
    GEO_SEARCH_URL="https://geosearch.test",
    TVM_DAEMON_URL="http://localhost:1",
    TVM_TOKEN="TESTING_TVM_TOKEN",
    TVM_GDPR_WHITELIST=[12345],
    WARDEN_URL=None,
    YANG_POOL_ID="999",
    YANG_TOKEN="YANG_TOKEN",
    YANG_URL="https://yang.test",
    YASMS_SENDER="yasms_sender",
    YASMS_URL="https://yasms.test",
    NEW_YANG_FORMAT=False,
    DISCONNECT_ORGS=False,
    YT_CLUSTER="hahn",
    YT_TOKEN="fake_yt_token",
    YT_EXPORT_CREATED_ORDERS="//path/to/created_orders_table",
    YT_EXPORT_ORDERS_PROCESSED_BY_YANG_TABLE="//path/to/processed_orders_table",
    YT_EXPORT_ORDERS_NOTIFIED_TO_USERS_TABLE="//path/to/notified_orders_table",
)


@pytest.fixture
def config(request):
    __config = _config.copy()

    config_mark = request.node.get_closest_marker("config")
    if config_mark:
        __config.update(config_mark.kwargs)

    return __config


@pytest.fixture(scope="session", autouse=True)
def pgswim_engine_cls():
    return DB


@pytest.fixture
def app(config):
    return Application(config)


@pytest.fixture
def mock_yt(mocker, shared_proxy_mp_manager):
    methods = "remove", "create", "exists", "write_table", "Transaction"
    return {
        method: mocker.patch(
            f"yt.wrapper.YtClient.{method}", shared_proxy_mp_manager.SharedMock()
        )
        for method in methods
    }
