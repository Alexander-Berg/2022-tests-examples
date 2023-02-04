import pytest

from maps_adv.geosmb.crane_operator.server.lib.tasks import GenerateCouponsPoiDataTask
from maps_adv.geosmb.crane_operator.server.tests.shared_mock import (
    SharedCallableMock,
    SharedCallableMockManager,
    SharedCallableMockProxy,
)

pytestmark = [pytest.mark.asyncio]


class YqlRequestMockProxy(SharedCallableMockProxy):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

        self._exposed_props_get = self._exposed_props_get + ("run", "get_results")
        self._exposed_props_set = self._exposed_props_set + ("run", "get_results")


class YqlRequestResultsMockProxy(SharedCallableMockProxy):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

        self._exposed_props_get = self._exposed_props_get + ("status", "text")
        self._exposed_props_set = self._exposed_props_set + ("status", "text")


SharedCallableMockManager.register(
    "YqlRequestMock", SharedCallableMock, YqlRequestMockProxy
)
SharedCallableMockManager.register(
    "YqlRequestResultsMock", SharedCallableMock, YqlRequestResultsMockProxy
)


@pytest.fixture
def mp_mock_manager():
    with SharedCallableMockManager() as manager:
        yield manager


@pytest.fixture(autouse=True)
def mock_yql(mocker, mp_mock_manager):
    request_results = mp_mock_manager.YqlRequestResultsMock()
    request_results.status = mp_mock_manager.SharedCallableMock()
    request_results.text = mp_mock_manager.SharedCallableMock()

    request = mp_mock_manager.YqlRequestMock()
    request.run = mp_mock_manager.SharedCallableMock()
    request.get_results = request_results

    query = mp_mock_manager.SharedCallableMock(return_value=request)

    return {"query": mocker.patch("yql.api.v1.client.YqlClient.query", query)}


@pytest.fixture
def task(config):
    return GenerateCouponsPoiDataTask(config=config)


async def test_executes_with_correct_yql(task, mock_yql):
    await task

    mock_yql["query"].assert_called_with(
        """
        $client_coupons = (
            SELECT
                clients.biz_id AS biz_id,
                clients.passport_uid AS passport_uid,
                coupons.coupon_id AS coupon_id,
                coupons.poi_subscript AS poi_subscript
            FROM hahn.`//path/to/clients-table` AS clients
            JOIN hahn.`//path/to/coupons-with-segments-table` AS coupons
                ON coupons.biz_id = clients.biz_id
            WHERE NOT SetIsDisjoint(ToSet(clients.segments), ToSet(coupons.segments))
                AND clients.passport_uid IS NOT NULL
        );

        $distinct_client_coupons = (
            SELECT DISTINCT
                biz_id,
                passport_uid,
                first_value(poi_subscript) OVER w AS subscript
                FROM $client_coupons
                WINDOW w AS (
                    PARTITION BY biz_id, passport_uid
                    ORDER BY coupon_id
                )
        );

        INSERT INTO hahn.`//path/to/table-for-coupons-poi-data` WITH TRUNCATE
        SELECT
            gco.permalink AS permalink,
            'coupons' AS experiment_tag,
            dcc.passport_uid AS passport_uid,
            dcc.subscript AS subscript
        FROM $distinct_client_coupons AS dcc
        JOIN hahn.`//path/to/geoadv-campaigns-table` AS gc ON dcc.biz_id=gc.id
        JOIN hahn.`//path/to/geoadv-campaign-orgs-table` AS gco
            ON gco.campaign_id = gc.id;
    """,
        syntax_version=1,
    )
