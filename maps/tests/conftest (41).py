import copy
import logging
from unittest import mock

import pytest
from yt.wrapper import YtClient
from yt.wrapper.format import RowsIterator

from maps_adv.common.avatars import AvatarsClient, AvatarsInstallation
from maps_adv.common.helpers import coro_mock
from maps_adv.geosmb.clients.geobase import GeoBaseClient
from maps_adv.geosmb.landlord.server.lib import Application
from maps_adv.geosmb.landlord.server.lib.async_yt_client import AsyncYtClient
from maps_adv.geosmb.landlord.server.lib.data_manager import (
    BaseDataManager,
    DataManager,
)
from maps_adv.geosmb.landlord.server.lib.db import DB
from maps_adv.geosmb.landlord.server.lib.domain import Domain
from maps_adv.geosmb.landlord.server.lib.ext_feed_lb_writer import (
    ExtFeedLogbrokerWriter,
)
from maps_adv.geosmb.landlord.server.tests.shared_mock import SharedCallableMockManager
from maps_adv.geosmb.tuner.client import RequestsSettings, TunerClient

from . import PutImageResultMock

# uncomment this to run task tests on Mac OS
# from multiprocessing import set_start_method
# set_start_method("fork")

pytest_plugins = [
    "aiohttp.pytest_plugin",
    "smb.common.pgswim.pytest.plugin",
    "smb.common.aiotvm.pytest.plugin",
    "maps_adv.common.lasagna.pytest.plugin",
    "maps_adv.geosmb.clients.bunker.pytest.plugin",
    "maps_adv.geosmb.clients.bvm.pytest.plugin",
    "maps_adv.geosmb.clients.geosearch.pytest.plugin",
    "maps_adv.geosmb.clients.market.pytest.plugin",
    "maps_adv.common.ugcdb_client.pytest.plugin",
    "maps_adv.geosmb.landlord.server.tests.factory",
    "maps_adv.geosmb.landlord.server.tests.shared_mock",
]

_config = dict(
    DATABASE_URL="postgresql://landlord:landlord@localhost:5433/landlord",
    TVM_DAEMON_URL="http://tvm.daemon",
    TVM_TOKEN="tvm-token",
    BVM_URL="http://bvm.server",
    MARKET_INT_URL="http://market.server",
    GEOSEARCH_URL="http://geosearch.server",
    GEOSEARCH_TVM_ID="geosearch",
    FETCH_DATA_TOKEN="fetch_data_token",
    AVATARS_INSTALLATION="debug",
    AVATARS_STORE_NAMESPACE="store",
    BASE_MAPS_URL="http://maps-url",
    BASE_WIDGET_REQUEST_URL="http://widget_url",
    UGCDB_TVM_ID="ugcdb",
    UGCDB_URL="http://ugcdb.server",
    GEOBASE_URL="http://geobase.server",
    PROMOS_YT_TABLE="//path/to/yt_promos_table",
    GEOADV_CAMPAIGNS_YT_TABLE="//path/to/yt_campaigns_table",
    GEOADV_ORGS_YT_TABLE="//path/to/yt_orgs_table",
    GEOADV_BASE_YT_PREFIX="//path/to/geoadv_tables",
    LANDLORD_BASE_YT_PREFIX="//path/to/landlord_tables",
    MARKET_INT_STREAMING_YT_FOLDER="//path/to/market_int_tables",
    YT_GOODS_TABLE="//path/to/yt_goods_table",
    YT_BIZ_STATE_TABLE="//path/to/biz_state_table",
    YT_CLUSTER="hahn",
    YQL_TOKEN="fake_yql_token",
    YT_TOKEN="fake_yt_token",
    AVATARS_UPLOAD_TIMEOUT=3,
    TVM_DELETE_LANDING_WHITELIST=(12345,),
    DISABLE_PROMOTED_SERVICES=False,
    YT_GOOGLE_COUNTERS_TABLE="//home/counters",
    YT_TIKTOK_PIXELS_TABLE="//home/pixels",
    YT_VK_PIXELS_TABLE="//home/vk_pixels",
    EXPERIMENT_GOOGLE_COUNTERS=[54321],
    LANDING_CONFIG_BUNKER_NODE="/landlord/config",
    LANDING_CONFIG_BUNKER_NODE_VERSION="latest",
    BUNKER_URL="http://bunker.server",
    TUNER_URL="http://tuner.server",
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
def dm(request):
    if request.node.get_closest_marker("mock_dm"):
        return request.getfixturevalue("_mock_dm")
    return request.getfixturevalue("_dm")


@pytest.fixture
def _dm(config, db):
    return DataManager(db=db)


@pytest.fixture
def _mock_dm():
    class MockDM(BaseDataManager):
        fetch_biz_state = coro_mock()
        check_slug_is_free = coro_mock()
        fetch_biz_state_by_slug = coro_mock()
        create_biz_state = coro_mock()
        update_biz_state_slug = coro_mock()
        fetch_landing_data_for_crm = coro_mock()
        save_landing_data_for_biz_id = coro_mock()
        delete_landing_by_biz_id = coro_mock()
        fetch_landing_data_by_slug = coro_mock()
        set_landing_publicity = coro_mock()
        fetch_org_promos = coro_mock()
        fetch_promoted_cta = coro_mock()
        fetch_org_promoted_services = coro_mock()
        fetch_substitution_phone = coro_mock()
        fetch_landing_phone = coro_mock()
        update_biz_state_permalink = coro_mock()
        fetch_cached_landing_config = coro_mock()
        fetch_cached_landing_config_feature = coro_mock()
        set_cached_landing_config = coro_mock()
        update_biz_state_set_blocked = coro_mock()
        create_instagram_landing = coro_mock()
        save_instagram_landing = coro_mock()
        fetch_google_counters_for_permalink = coro_mock()
        fetch_avatars = coro_mock()
        fetch_all_published_permalinks = coro_mock()
        update_landing_data_with_geosearch = coro_mock()
        update_instagram_landing_data_with_geosearch = coro_mock()
        fetch_branches_for_permalink = coro_mock()
        fetch_published_slugs = coro_mock()
        fetch_landing_photos = coro_mock()
        hide_landing_photos = coro_mock()
        fetch_org_market_int_services = coro_mock()
        fetch_tiktok_pixels_for_permalink = coro_mock()
        fetch_goods_data_for_permalink = coro_mock()
        fetch_vk_pixels_for_permalink = coro_mock()
        update_permalink_from_geosearch = coro_mock()

        import_promos_from_yt = coro_mock()
        import_promoted_cta_from_yt = coro_mock()
        import_promoted_services_from_yt = coro_mock()
        import_promoted_service_lists_from_yt = coro_mock()
        import_call_tracking_from_yt = coro_mock()
        import_google_counters_from_yt = coro_mock()
        import_avatars_from_yt = coro_mock()
        import_market_int_services_from_yt = coro_mock()
        import_tiktok_pixels_from_yt = coro_mock()
        import_goods_data_from_yt = coro_mock()
        import_vk_pixels_from_yt = coro_mock()
        sync_permalinks_from_yt = coro_mock()

    return MockDM()


@pytest.fixture
def domain(
    dm,
    config,
    bvm,
    market_int,
    geosearch,
    avatars_client,
    ugcdb_client,
    geobase_client,
    async_yt_client,
    bunker_client,
    tuner_client,
    ext_feed_writer,
):
    return Domain(
        dm=dm,
        config=config,
        bunker_client=bunker_client,
        bvm_client=bvm,
        geosearch_client=geosearch,
        market_client=market_int,
        avatars_client=avatars_client,
        geobase_client=geobase_client,
        ugcdb_client=ugcdb_client,
        yt_client=async_yt_client,
        tuner_client=tuner_client,
        ext_feed_writer=ext_feed_writer,
        fetch_data_token=config["FETCH_DATA_TOKEN"],
        base_maps_url=config["BASE_MAPS_URL"],
        base_widget_request_url=config["BASE_WIDGET_REQUEST_URL"],
        avatars_upload_timeout=config["AVATARS_UPLOAD_TIMEOUT"],
        landing_config_bunker_node=config["LANDING_CONFIG_BUNKER_NODE"],
        landing_config_bunker_node_version=config["LANDING_CONFIG_BUNKER_NODE_VERSION"],
    )


@pytest.fixture(autouse=True)
async def geobase_client(mocker, config):
    fetch_linguistics_for_region = mocker.patch(
        "maps_adv.geosmb.clients.geobase.GeoBaseClient.fetch_linguistics_for_region",
        coro_mock(),
    )

    fetch_linguistics_for_region.coro.side_effect = [
        {"preposition": "в", "prepositional_case": "Ростове-на-Дону"},
        {"preposition": "в", "prepositional_case": "Ростовской области"},
    ]

    async with GeoBaseClient(url=config["GEOBASE_URL"]) as client:
        yield client


@pytest.fixture(autouse=True)
async def avatars_client(mocker, config, aiotvm):
    mocker.patch.dict(
        "maps_adv.common.avatars.avatars_installations",
        {
            "debug": AvatarsInstallation(
                outer_read_url="http://avatars-outer-read.server",
                inner_read_url="http://avatars-inner-read.server",
                write_url="http://avatars-write.server",
            )
        },
    )

    put_image_by_url = mocker.patch(
        "maps_adv.common.avatars.AvatarsClient.put_image_by_url", coro_mock()
    )

    put_image_by_url.coro.return_value = PutImageResultMock(
        url_template="http://avatars-outer-read.server/get-store/603/green-leaves/%s"
    )

    async with AvatarsClient(
        installation=config["AVATARS_INSTALLATION"],
        namespace=config["AVATARS_STORE_NAMESPACE"],
        tvm_client=aiotvm,
        tvm_destination="avatars",
    ) as client:
        yield client


def yt_rows_interator(rows):
    return RowsIterator(
        rows=rows,
        extract_control_attributes=lambda x: None,
        tablet_index_attribute_name=None,
        table_index_attribute_name=None,
        row_index_attribute_name=None,
        range_index_attribute_name=None,
        key_switch_attribute_name=None,
    )


@pytest.fixture(autouse=True)
def yt_client(mocker, config):
    client = mocker.patch("yt.wrapper.YtClient.select_rows", mock.Mock())
    client.select_rows.return_value = yt_rows_interator([])
    return YtClient()


@pytest.fixture()
async def async_yt_client(mocker, config):
    get_google_counters_for_permalink = mocker.patch(
        "maps_adv.geosmb.landlord.server.lib.async_yt_client.AsyncYtClient.get_google_counters_for_permalink",  # noqa: E501
        coro_mock(),
    )
    get_google_counters_for_permalink.coro.return_value = None

    return AsyncYtClient(config["YT_TOKEN"], config["YT_GOOGLE_COUNTERS_TABLE"])


@pytest.fixture(autouse=True)
async def tuner_client(mocker, config, aiotvm):
    fetch_settings = mocker.patch(
        "maps_adv.geosmb.tuner.client.TunerClient.fetch_settings", coro_mock()
    )
    fetch_settings.coro.return_value = {
        "requests": RequestsSettings(enabled=True, button_text="Push me gently")
    }

    async with await TunerClient(
        url=config["TUNER_URL"], tvm=aiotvm, tvm_destination="tuner"
    ) as client:
        yield client


@pytest.fixture
def app(config):
    return Application(config)


@pytest.fixture
def logging_warning(caplog):
    # This removes ya.test noise from caplog
    caplog.set_level(logging.WARNING)


@pytest.fixture
def mp_mock_manager():
    with SharedCallableMockManager() as manager:
        yield manager


@pytest.fixture
def geosearch_moved_perm(geosearch, geosearch_resp):
    geosearch_resp_2 = copy.deepcopy(geosearch_resp)

    geosearch_resp["permalink_moved_to"] = "98765"
    geosearch_resp_2.permalink = "11111"

    geosearch.resolve_org.coro.side_effect = (geosearch_resp, geosearch_resp_2)


@pytest.fixture(autouse=True)
def mock_yql(mocker, mp_mock_manager):
    table_get_iterator = mp_mock_manager.SharedCallableMock()
    table_get_iterator.return_value = []

    results_table = mp_mock_manager.YqlResultTableMock()
    results_table.get_iterator = table_get_iterator

    request_results = mp_mock_manager.YqlRequestResultsMock()
    request_results.status = mp_mock_manager.SharedCallableMock()
    request_results.text = mp_mock_manager.SharedCallableMock()
    request_results.table = results_table

    request = mp_mock_manager.YqlRequestMock()
    request.run = mp_mock_manager.SharedCallableMock()
    request.get_results = request_results

    query = mp_mock_manager.SharedCallableMock(return_value=request)

    return {
        "query": mocker.patch("yql.api.v1.client.YqlClient.query", query),
        "request_run": request.run,
        "request_get_results": request_results,
        "table_get_iterator": table_get_iterator,
    }


@pytest.fixture
async def ext_feed_writer(config):
    yield ExtFeedLogbrokerWriter(config)
