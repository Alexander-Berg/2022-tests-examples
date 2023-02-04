import pytest

from maps_adv.common.helpers import coro_mock
from maps_adv.geosmb.scenarist.server.lib import Application
from maps_adv.geosmb.scenarist.server.lib.data_manager import (
    BaseDataManager,
    DataManager,
)
from maps_adv.geosmb.scenarist.server.lib.db import DB
from maps_adv.geosmb.scenarist.server.lib.domain import Domain

pytest_plugins = [
    "aiohttp.pytest_plugin",
    "smb.common.pgswim.pytest.plugin",
    "smb.common.aiotvm.pytest.plugin",
    "maps_adv.common.lasagna.pytest.plugin",
    "maps_adv.common.shared_mock.pytest.plugin",
    "maps_adv.geosmb.clients.facade.pytest.plugin",
    "maps_adv.geosmb.scenarist.server.tests.factory",
]

_config = dict(
    DATABASE_URL="postgresql://scenarist:scenarist@localhost:5433/scenarist",
    FACADE_URL="http://facade.server",
    TVM_TOKEN="qloud-tvm-token",
    SUBSCRIPTIONS_YT_EXPORT_TABLE="//path/to/sub-table",
    SUBSCRIPTIONS_VERSIONS_YT_EXPORT_TABLE="//path/to/sub-version-table",
    MESSAGES_YT_IMPORT_DIR="//path/to/msg_dir",
    CERTIFICATE_MAILING_STATS_YT_IMPORT_DIR="//path/to/certificates_stat_dir",
    TVM_DAEMON_URL="http://tvm.daemon",
    WARDEN_TASKS=[],
    WARDEN_URL=None,
    YT_CLUSTER="hahn",
    YT_TOKEN="fake_token",
    EMAIL_CLIENT_API_URL="http://localhost/email",
    EMAIL_CLIENT_ACCOUNT_SLUG="email_slug",
    EMAIL_CLIENT_ACCOUNT_TOKEN="email_token",
    EMAIL_CLIENT_CAMPAIGN_TITLE="Автокампания",
    EMAIL_CLIENT_FROM_NAME="Предложения от партнёров",
    EMAIL_CLIENT_FROM_EMAIL="from@yandex.ru",
    EMAIL_CLIENT_UNSUBSCRIBE_LIST_SLUG="yandex_clients",
    EMAIL_CLIENT_ALLOWED_STAT_DOMAINS=("yandex.ru", "ya.ru"),
    SEND_REAL_EMAILS=True,
)


@pytest.fixture
def config():
    return _config.copy()


@pytest.fixture(scope="session", autouse=True)
def pgswim_engine_cls():
    return DB


@pytest.fixture
def dm(request):
    if request.node.get_closest_marker("mock_dm"):
        return request.getfixturevalue("_mock_dm")
    return request.getfixturevalue("_dm")


@pytest.fixture
def _dm(config, db):
    return DataManager(db=db)


@pytest.fixture
def email_client():
    class EmailSenderMock:
        schedule_promo_campaign = coro_mock()

        async def __aenter__(self):
            return self

        __aexit__ = coro_mock()

    return EmailSenderMock()


@pytest.fixture
def _mock_dm():
    class MockDM(BaseDataManager):
        list_scenarios = coro_mock()
        create_subscription = coro_mock()
        retrieve_subscription = coro_mock()
        retrieve_subscription_current_state = coro_mock()
        update_subscription_status = coro_mock()
        update_subscriptions_statuses = coro_mock()
        replace_subscription_coupon = coro_mock()
        iter_subscriptions_for_export = coro_mock()
        iter_subscriptions_versions_for_export = coro_mock()
        copy_messages_from_generator = coro_mock()
        list_unprocessed_email_messages = coro_mock()
        mark_messages_processed = coro_mock()
        import_certificate_mailing_stats = coro_mock()

    return MockDM()


@pytest.fixture
def domain(config, dm, email_client, facade):
    return Domain(
        dm=dm,
        email_client=email_client,
        facade_client=facade,
        schedule_promo_campaign_params=dict(
            title=config["EMAIL_CLIENT_CAMPAIGN_TITLE"],
            from_name=config["EMAIL_CLIENT_FROM_NAME"],
            from_email=config["EMAIL_CLIENT_FROM_EMAIL"],
            unsubscribe_list_slug=config["EMAIL_CLIENT_UNSUBSCRIBE_LIST_SLUG"],
            allowed_stat_domains=list(config["EMAIL_CLIENT_ALLOWED_STAT_DOMAINS"])
            if config["EMAIL_CLIENT_ALLOWED_STAT_DOMAINS"] is not None
            else None,
        ),
        allowed_button_url_domains=config["EMAIL_CLIENT_ALLOWED_STAT_DOMAINS"],
    )


@pytest.fixture
def app(config):
    return Application(config)


@pytest.fixture
def mock_yt(mocker, shared_proxy_mp_manager):
    methods = (
        "list",
        "remove",
        "create",
        "exists",
        "write_table",
        "read_table",
        "set_attribute",
        "Transaction",
    )
    return {
        method: mocker.patch(
            f"yt.wrapper.YtClient.{method}", shared_proxy_mp_manager.SharedMock()
        )
        for method in methods
    }
