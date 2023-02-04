import pytest

from maps_adv.points.proto.errors_pb2 import Error
from maps_adv.points.proto.forecasts_pb2 import ForecastOutput, ForecastPolygonsInput
from maps_adv.points.proto.primitives_pb2 import Point, Polygon

pytestmark = [pytest.mark.asyncio]

url = "/api/v1/forecasts/overview/"


async def test_returns_forecast_for_passed_polygons(fill_shows_forecasts, api):
    input_pb = ForecastPolygonsInput(
        polygons=[
            Polygon(
                points=[
                    Point(longitude="140", latitude="-20"),
                    Point(longitude="140", latitude="30"),
                    Point(longitude="-160", latitude="30"),
                    Point(longitude="-160", latitude="-20"),
                    Point(longitude="140", latitude="-20"),
                ]
            ),
            Polygon(
                points=[
                    Point(longitude="50", latitude="50"),
                    Point(longitude="80", latitude="50"),
                    Point(longitude="80", latitude="20"),
                    Point(longitude="50", latitude="20"),
                    Point(longitude="50", latitude="50"),
                ]
            ),
        ]
    )

    got = await api.post(
        url, proto=input_pb, decode_as=ForecastOutput, expected_status=200
    )

    assert got == ForecastOutput(shows=22222270)


async def test_returns_forecast_for_overlapping_polygons(fill_shows_forecasts, api):
    input_pb = ForecastPolygonsInput(
        polygons=[
            Polygon(
                points=[
                    Point(longitude="140", latitude="-20"),
                    Point(longitude="140", latitude="30"),
                    Point(longitude="-160", latitude="30"),
                    Point(longitude="-160", latitude="-20"),
                    Point(longitude="140", latitude="-20"),
                ]
            ),
            Polygon(
                points=[
                    Point(longitude="120", latitude="10"),
                    Point(longitude="120", latitude="60"),
                    Point(longitude="170", latitude="60"),
                    Point(longitude="170", latitude="10"),
                    Point(longitude="120", latitude="10"),
                ]
            ),
        ]
    )

    got = await api.post(
        url, proto=input_pb, decode_as=ForecastOutput, expected_status=200
    )

    assert got == ForecastOutput(shows=22222227)


async def test_returns_zero_if_nothing_found(fill_shows_forecasts, api):
    input_pb = ForecastPolygonsInput(
        polygons=[
            Polygon(
                points=[
                    Point(longitude="20", latitude="40"),
                    Point(longitude="40", latitude="40"),
                    Point(longitude="40", latitude="20"),
                    Point(longitude="20", latitude="40"),
                ]
            )
        ]
    )

    got = await api.post(
        url, proto=input_pb, decode_as=ForecastOutput, expected_status=200
    )

    assert got == ForecastOutput(shows=0)


@pytest.mark.parametrize(
    "input_polygons, expected_error_code",
    [
        ([], Error.NO_POLYGONS_PASSED),
        (
            [
                Polygon(
                    points=[
                        Point(longitude="10", latitude="10"),
                        Point(longitude="20", latitude="10"),
                        Point(longitude="10", latitude="0"),
                        Point(longitude="10", latitude="50"),
                    ]
                )
            ],
            Error.NON_CLOSED_POLYGON,
        ),
        (
            [
                Polygon(
                    points=[
                        Point(longitude="10", latitude="10"),
                        Point(longitude="20", latitude="10"),
                        Point(longitude="10", latitude="0"),
                    ]
                )
            ],
            Error.INVALID_POLYGON,
        ),
    ],
)
async def test_raises_for_errors(input_polygons, expected_error_code, api):
    input_pb = ForecastPolygonsInput(polygons=input_polygons)

    got = await api.post(url, proto=input_pb, decode_as=Error, expected_status=400)

    assert got == Error(code=expected_error_code)
