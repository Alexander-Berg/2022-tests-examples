import pytest

from maps_adv.points.proto.errors_pb2 import Error
from maps_adv.points.proto.forecasts_pb2 import ForecastOutput, ForecastPointsInput
from maps_adv.points.proto.primitives_pb2 import Point

pytestmark = [pytest.mark.asyncio]

url = "/api/v1/forecasts/pins/"


async def test_returns_forecast_for_passed_points(fill_shows_forecasts, api):
    input_pb = ForecastPointsInput(
        points=[
            Point(longitude="150", latitude="-10"),
            Point(longitude="-170", latitude="-5"),
            Point(longitude="60", latitude="40"),
        ]
    )

    got = await api.post(
        url, proto=input_pb, decode_as=ForecastOutput, expected_status=200
    )

    assert got == ForecastOutput(shows=303020)


async def test_ignores_duplicates_if_points_in_one_geohash(fill_shows_forecasts, api):
    input_pb = ForecastPointsInput(
        points=[
            Point(longitude="70", latitude="30"),
            Point(longitude="69.9993", latitude="30.00094"),
            Point(longitude="70", latitude="30"),
            Point(longitude="69.9999", latitude="30.0009493"),
        ]
    )

    got = await api.post(
        url, proto=input_pb, decode_as=ForecastOutput, expected_status=200
    )

    assert got == ForecastOutput(shows=50)


async def test_returns_zero_if_nothing_found(fill_shows_forecasts, api):
    input_pb = ForecastPointsInput(
        points=[
            Point(longitude="80", latitude="80"),
            Point(longitude="0", latitude="0"),
            Point(longitude="20", latitude="-80"),
        ]
    )

    got = await api.post(
        url, proto=input_pb, decode_as=ForecastOutput, expected_status=200
    )

    assert got == ForecastOutput(shows=0)


async def test_raises_if_no_points_passed(fill_shows_forecasts, api):
    input_pb = ForecastPointsInput(points=[])

    got = await api.post(url, proto=input_pb, decode_as=Error, expected_status=400)

    assert got == Error(code=Error.NO_POINTS_PASSED)
