import pytest

from maps_adv.points.server.lib.data_managers.forecasts import (
    BaseForecastsDataManager,
    ForecastsDataManager,
)
from maps_adv.points.server.lib.data_managers.points import (
    BasePointsDataManager,
    PointsDataManager,
)
from maps_adv.points.server.tests import coro_mock


@pytest.fixture
async def points_dm(db, request):
    if request.node.get_closest_marker("mock_dm"):
        return request.getfixturevalue("_mock_points_dm")
    return request.getfixturevalue("_points_dm")


@pytest.fixture
def _points_dm(db):
    return PointsDataManager(db)


@pytest.fixture
def _mock_points_dm():
    class MockDm(BasePointsDataManager):
        find_within_polygons = coro_mock()

    return MockDm()


@pytest.fixture
async def forecasts_dm(db, request):
    if request.node.get_closest_marker("mock_dm"):
        return request.getfixturevalue("_mock_forecasts_dm")
    return request.getfixturevalue("_forecasts_dm")


@pytest.fixture
def _forecasts_dm(config, db):
    return ForecastsDataManager(
        db,
        yt_config={
            "yt_token": config["YT_TOKEN"],
            "yt_table": config["YT_FORECASTS_TABLE"],
            "yt_cluster": config["YT_CLUSTER"],
        },
    )


@pytest.fixture
def _mock_forecasts_dm():
    class MockDm(BaseForecastsDataManager):
        forecast_billboard = coro_mock()
        forecast_zerospeed = coro_mock()
        forecast_pins = coro_mock()
        sync_forecasts = coro_mock()

    return MockDm()


@pytest.fixture
def yql_table_read_iterator(mocker):
    mocker.patch(
        "yql.api.v1.client.YqlTableReadIterator.__init__",
        new_callable=lambda: lambda _, table_name, cluster, column_names: None,
    )
    return mocker.patch("yql.api.v1.client.YqlTableReadIterator.__iter__")
