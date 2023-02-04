import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("fill_shows_forecasts")]


async def test_returns_sum_of_shows(forecasts_dm):
    points = [
        dict(longitude="150", latitude="-10"),
        dict(longitude="-170", latitude="-5"),
        dict(longitude="60", latitude="40"),
    ]

    got = await forecasts_dm.forecast_pins(points)

    assert got == 303020


async def test_ignores_duplicates_if_points_in_one_geohash(forecasts_dm):
    points = [
        dict(longitude="70", latitude="30"),
        dict(longitude="69.9993", latitude="30.00094"),
        dict(longitude="70", latitude="30"),
        dict(longitude="69.9999", latitude="30.0009493"),
    ]

    got = await forecasts_dm.forecast_pins(points)

    assert got == 50


async def test_returns_zero_if_geohash_not_in_table(forecasts_dm):
    points = [
        dict(longitude="80", latitude="80"),
        dict(longitude="0", latitude="0"),
        dict(longitude="20", latitude="-80"),
    ]

    got = await forecasts_dm.forecast_pins(points)

    assert got == 0


async def test_returns_zero_if_empty_points_list(forecasts_dm):
    points = []

    got = await forecasts_dm.forecast_pins(points)

    assert got == 0
