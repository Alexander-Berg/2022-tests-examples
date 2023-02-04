import pytest

from maps_adv.geosmb.crane_operator.server.lib.tasks import GenerateDataForSnippet
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
    return GenerateDataForSnippet(config=config)


async def test_executes_with_correct_yql(task, mock_yql):
    await task

    mock_yql["query"].assert_called_with(
        """
        $size = Yson::From(AsStruct(300 AS height, 200 AS width));

        INSERT INTO hahn.`//path/to/snippet-table` WITH TRUNCATE
        SELECT coalesce(t1.permalink, t2.permalink) AS permalink,
        Yson::From(AsStruct(
                CASE WHEN t1.booking_url IS NOT NULL
                    THEN Yson::From(AsStruct($size AS size, t1.booking_url AS url))
                    ELSE NULL
                    END AS booking_widget,
                CASE WHEN Yson::Lookup(Yson::Parse(t2.showcase), 'value') IS NOT NULL
                    THEN Yson::From(AsStruct(
                        $size AS size,
                        Yson::Lookup(Yson::Parse(t2.showcase), 'value') AS url)
                    )
                    ELSE NULL
                    END AS coupon_widget
        )) AS showcase
        FROM hahn.`//path/to/orgs-with-booking-dir` AS t1
        FULL JOIN hahn.`//path/to/orgs-with-coupons-dir` AS t2
        USING (permalink);
    """,
        syntax_version=1,
    )
