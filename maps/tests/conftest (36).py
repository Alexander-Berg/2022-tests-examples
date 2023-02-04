from unittest.mock import Mock

import pytest
from tvmauth.mock import TvmClientPatcher

from maps_adv.common.helpers import AsyncIterator
from maps_adv.geosmb.crane_operator.server.lib import Application
from maps_adv.geosmb.crane_operator.server.lib.domains.coupons_domain import (
    CouponsDomain,
)
from maps_adv.geosmb.crane_operator.server.lib.domains.poi_domain import PoiDomain

from .utils import AsyncContextManagerMock, coro_mock

pytest_plugins = [
    "aiohttp.pytest_plugin",
    "smb.common.aiotvm.pytest.plugin",
    "maps_adv.common.lasagna.pytest.plugin",
    "maps_adv.common.shared_mock.pytest.plugin",
    "maps_adv.geosmb.booking_yang.client.pytest.plugin",
    "maps_adv.geosmb.clients.bvm.pytest.plugin",
    "maps_adv.geosmb.clients.facade.pytest.plugin",
    "maps_adv.geosmb.clients.geosearch.pytest.plugin",
    "maps_adv.geosmb.clients.market.pytest.plugin",
    "maps_adv.geosmb.crane_operator.server.tests.shared_mock",
    "maps_adv.geosmb.doorman.client.pytest.plugin",
]


_config = dict(
    WARDEN_TASKS=[],
    WARDEN_URL=None,
    FACADE_URL="http://facade.url",
    LOYALTY_URL="http://loyalty.url",
    TVM_DAEMON_URL="http://tvm.url",
    BOOKING_YANG_URL="http://booking-yang.url",
    DOORMAN_URL="http://doorman.url",
    GEOSEARCH_URL="http://geosearch.url",
    MARKET_INT_URL="http://market.url",
    BVM_URL="http://bvm.url",
    TVM_SELF_ALIAS="self",
    TVM_TOKEN="qloud-tvm-token",
    YT_CLUSTER="hahn",
    YT_TOKEN="fake_token",
    YT_TABLE_FOR_ORDERS_POI="//path/to/table-for-orders-poi-data",
    YT_TABLE_FOR_COUPONS_POI="//path/to/table-for-coupons-poi-data",
    YT_TABLE_CLIENTS="//path/to/clients-table",
    YT_TABLE_GEOADV_CAMPAIGNS="//path/to/geoadv-campaigns-table",
    YT_TABLE_GEOADV_CAMPAIGN_ORGS="//path/to/geoadv-campaign-orgs-table",
    ORGS_WITH_COUPONS_YT_EXPORT_TABLE="//path/to/orgs-with-coupons-dir",
    ORGS_WITH_BOOKING_YT_EXPORT_TABLE="//path/to/orgs-with-booking-dir",
    LOYALTY_ITEMS_YT_EXPORT_TABLE="//path/to/loyalty-items-table",
    BUSINESS_COUPONS_YT_EXPORT_TABLE="//path/to/business-coupons-table",
    COUPONS_WITH_SEGMENTS_YT_EXPORT_TABLE="//path/to/coupons-with-segments-table",
    COUPON_PROMOTIONS_YT_EXPORT_TABLE="//path/to/coupon-promotions-table",
    LOGBROKER_WRITER_HOST="logbroker.host",
    LOGBROKER_READER_HOSTS=["sas.logbroker.host"],
    LOGBROKER_PORT="1234",
    LOGBROKER_DEFAULT_SOURCE_ID=b"TestCraneOperatorDefaultSourceId",
    LOGBROKER_COUPONS_TO_REVIEW_TOPIC_OUT="coupons-to-review-topic-out",
    LOGBROKER_COUPONS_REVIEWED_TOPIC_IN="coupons-reviewed-topic-in",
    LOGBROKER_CONSUMER_ID="any-valid-consumer-id",
    LOGBROKER_MESSAGE_COUNT_THRESHOLD=100,
    YQL_TOKEN="any_yql_token",
    YT_TABLE_FOR_SNIPPET="//path/to/snippet-table",
    BOOKING_YANG_ORDERS_POI_EXPERIMENT="booking_yang",
    MARKET_INT_ORDERS_POI_EXPERIMENT="market_int",
    COUPONS_POI_EXPERIMENT_TAG="coupons",
    YT_REPLICATION_CLUSTER="arnold",
)


@pytest.fixture
def config(request):
    __config = _config.copy()

    config_mark = request.node.get_closest_marker("config")
    if config_mark:
        __config.update(config_mark.kwargs)

    return __config


@pytest.fixture
def app(config, mock_lb_client, mocker):
    mocker.patch(
        "maps_adv.geosmb.crane_operator.server.lib.LogbrokerClient",
        return_value=mock_lb_client,
    )

    with TvmClientPatcher():
        yield Application(config)


@pytest.fixture
async def db():
    return None


@pytest.fixture
async def mock_loyalty_client():
    class LoyaltyClientMock:
        get_coupons_list_for_review = AsyncIterator([])
        confirm_coupons_sent_to_review = coro_mock()
        submit_coupons_reviews_list = coro_mock()

    return LoyaltyClientMock()


@pytest.fixture
def mock_topic_writer():
    class TopicWriterMock(AsyncContextManagerMock):
        write_one = coro_mock()

    return TopicWriterMock()


@pytest.fixture
def mock_topic_reader():
    class TopicReaderMock(AsyncContextManagerMock):
        read_batch = AsyncIterator([])
        commit = Mock()
        finish_reading = coro_mock()

    return TopicReaderMock()


@pytest.fixture
def mock_lb_client(config, mock_topic_writer, mock_topic_reader):
    class LogbrokerClientMock:
        start = coro_mock()
        close = coro_mock()
        create_writer = Mock(return_value=mock_topic_writer)
        create_reader = Mock(return_value=mock_topic_reader)

    return LogbrokerClientMock()


@pytest.fixture
def coupons_domain(
    config,
    facade,
    mock_loyalty_client,
    mock_lb_client,
):
    return CouponsDomain(
        facade_client=facade,
        loyalty_client=mock_loyalty_client,
        logbroker_writer_client=mock_lb_client,
        logbroker_reader_clients=[mock_lb_client],
        coupons_to_review_topic_out=config["LOGBROKER_COUPONS_TO_REVIEW_TOPIC_OUT"],
        coupons_reviewed_topic_in=config["LOGBROKER_COUPONS_REVIEWED_TOPIC_IN"],
        logbroker_consumer_id=config["LOGBROKER_CONSUMER_ID"],
        logbroker_message_count_threshold=config["LOGBROKER_MESSAGE_COUNT_THRESHOLD"],
    )


@pytest.fixture
def poi_domain(config, booking_yang, doorman, market_int, bvm, geosearch):
    return PoiDomain(
        booking_yang=booking_yang,
        booking_yang_poi_experiment=config["BOOKING_YANG_ORDERS_POI_EXPERIMENT"],
        market_poi_experiment=config["MARKET_INT_ORDERS_POI_EXPERIMENT"],
        doorman_client=doorman,
        market_client=market_int,
        bvm_client=bvm,
        geosearch_client=geosearch,
    )
