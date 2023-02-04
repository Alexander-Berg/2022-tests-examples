import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("fill_shows_forecasts")]


async def test_returns_sum_of_shows_for_all_passed_polygons(forecasts_dm):
    polygons = [
        [
            dict(longitude="140", latitude="-20"),
            dict(longitude="140", latitude="30"),
            dict(longitude="-160", latitude="30"),
            dict(longitude="-160", latitude="-20"),
            dict(longitude="140", latitude="-20"),
        ],
        [
            dict(longitude="50", latitude="50"),
            dict(longitude="80", latitude="50"),
            dict(longitude="80", latitude="20"),
            dict(longitude="50", latitude="20"),
            dict(longitude="50", latitude="50"),
        ],
    ]

    got = await forecasts_dm.forecast_overview(polygons)

    assert got == 22222270


async def test_returns_zero_if_geohash_not_in_table(forecasts_dm):
    polygons = [
        [
            dict(longitude="20", latitude="40"),
            dict(longitude="40", latitude="40"),
            dict(longitude="40", latitude="20"),
            dict(longitude="20", latitude="40"),
        ]
    ]

    got = await forecasts_dm.forecast_overview(polygons)

    assert got == 0


async def test_returns_zero_if_empty_polygons_list(forecasts_dm):
    polygons = []

    got = await forecasts_dm.forecast_overview(polygons)

    assert got == 0


async def test_ignores_duplicates_for_crossing_polygons(forecasts_dm):
    polygons = [
        [
            dict(longitude="140", latitude="-20"),
            dict(longitude="140", latitude="30"),
            dict(longitude="-160", latitude="30"),
            dict(longitude="-160", latitude="-20"),
            dict(longitude="140", latitude="-20"),
        ],
        [
            dict(longitude="120", latitude="10"),
            dict(longitude="120", latitude="60"),
            dict(longitude="170", latitude="60"),
            dict(longitude="170", latitude="10"),
            dict(longitude="120", latitude="10"),
        ],
    ]

    got = await forecasts_dm.forecast_overview(polygons)

    assert got == 22222227
