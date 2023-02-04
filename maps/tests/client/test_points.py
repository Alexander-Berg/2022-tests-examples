from unittest.mock import Mock

import pytest
from aiohttp.web import Response

from maps_adv.points.client.lib import (
    Client,
    CollectionNotFound,
    InvalidPolygon,
    InvalidVersion,
    NonClosedPolygon,
    NoPointsPassed,
    NoPolygonsPassed,
    NotFound,
    ResultPoint,
    UnknownError,
    UnknownResponseBody,
    ValidationError,
)
from maps_adv.points.proto.errors_pb2 import Error
from maps_adv.points.proto.points_in_polygons_pb2 import (
    PointsInPolygonsInput,
    PointsInPolygonsOutput,
)
from maps_adv.points.proto.primitives_pb2 import IdentifiedPoint, Point, Polygon

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    ("polygons", "expected_request_body"),
    [
        (
            [
                [
                    {"longitude": "25", "latitude": "25"},
                    {"longitude": "55", "latitude": "20"},
                    {"longitude": "30.0", "latitude": "-15.554"},
                ]
            ],
            PointsInPolygonsInput(
                polygons=[
                    Polygon(
                        points=[
                            Point(longitude="25", latitude="25"),
                            Point(longitude="55", latitude="20"),
                            Point(longitude="30.0", latitude="-15.554"),
                        ]
                    )
                ]
            ),
        ),
        (
            [
                [
                    {"longitude": "25", "latitude": "25"},
                    {"longitude": "55", "latitude": "20"},
                    {"longitude": "30", "latitude": "-15"},
                ],
                [
                    {"longitude": "14.0", "latitude": "97.0"},
                    {"longitude": "23.0", "latitude": "-23.0"},
                    {"longitude": "-30.0", "latitude": "-15.0"},
                    {"longitude": "67.0", "latitude": "-87.0"},
                ],
            ],
            PointsInPolygonsInput(
                polygons=[
                    Polygon(
                        points=[
                            Point(longitude="25", latitude="25"),
                            Point(longitude="55", latitude="20"),
                            Point(longitude="30", latitude="-15"),
                        ]
                    ),
                    Polygon(
                        points=[
                            Point(longitude="14.0", latitude="97.0"),
                            Point(longitude="23.0", latitude="-23.0"),
                            Point(longitude="-30.0", latitude="-15.0"),
                            Point(longitude="67.0", latitude="-87.0"),
                        ]
                    ),
                ]
            ),
        ),
        ([], PointsInPolygonsInput(polygons=[])),
    ],
)
async def test_requests_data_correctly(polygons, expected_request_body, mock_points):
    async def _handler(request):
        assert (
            PointsInPolygonsInput.FromString(await request.read())
            == expected_request_body
        )
        return Response(body=PointsInPolygonsOutput().SerializeToString(), status=200)

    mock_points(_handler)

    async with Client("http://points.server") as client:
        await client(polygons=polygons, points_version=1)


@pytest.mark.parametrize(
    ("response_pb", "expected_points"),
    [
        (
            PointsInPolygonsOutput(
                points=[IdentifiedPoint(id=1, longitude="155.0", latitude="25.0")]
            ),
            [ResultPoint(longitude="155.0", latitude="25.0", id=1)],
        ),
        (
            PointsInPolygonsOutput(
                points=[
                    IdentifiedPoint(id=1, longitude="155.0", latitude="25.0"),
                    IdentifiedPoint(id=2, longitude="-175.0", latitude="20.0"),
                    IdentifiedPoint(id=3, longitude="160.0", latitude="-15.0"),
                ]
            ),
            [
                ResultPoint(longitude="155.0", latitude="25.0", id=1),
                ResultPoint(longitude="-175.0", latitude="20.0", id=2),
                ResultPoint(longitude="160.0", latitude="-15.0", id=3),
            ],
        ),
        (PointsInPolygonsOutput(points=[]), []),
    ],
)
async def test_parses_response_correctly(response_pb, expected_points, mock_points):
    mock_points(Response(body=response_pb.SerializeToString(), status=200))

    async with Client("http://points.server") as client:
        result = await client(polygons=[[]], points_version=1)

    assert result == expected_points


@pytest.mark.parametrize(
    ["proto_error", "expected_exception", "expected_exception_args"],
    [
        (Error(code=Error.ERROR_CODE.INVALID_VERSION), InvalidVersion, tuple()),
        (Error(code=Error.ERROR_CODE.NO_POLYGONS_PASSED), NoPolygonsPassed, tuple()),
        (Error(code=Error.ERROR_CODE.INVALID_POLYGON), InvalidPolygon, tuple()),
        (Error(code=Error.ERROR_CODE.NON_CLOSED_POLYGON), NonClosedPolygon, tuple()),
        (Error(code=Error.ERROR_CODE.NO_POINTS_PASSED), NoPointsPassed, tuple()),
        (
            Error(code=Error.ERROR_CODE.COLLECTION_NOT_FOUND),
            CollectionNotFound,
            tuple(),
        ),
        (
            Error(
                code=Error.ERROR_CODE.VALIDATION_ERROR,
                description="point_type: Invalid value",
            ),
            ValidationError,
            ("point_type: Invalid value",),
        ),
    ],
)
async def test_raises_exception_for_protobuf_error(
    proto_error, expected_exception, expected_exception_args, mock_points
):
    mock_points(Response(body=proto_error.SerializeToString(), status=400))

    with pytest.raises(expected_exception) as exc_info:
        async with Client("http://points.server") as client:
            await client(polygons=[[]], points_version=1)

    assert exc_info.value.args == expected_exception_args


async def test_raises_for_unknown_proto_error(mock_points, mocker):
    def make_error_mock(_):
        error_mock = Mock()
        error_mock.code = 55555
        error_mock.description = "error description"
        return error_mock

    mocker.patch(
        "maps_adv.points.client.lib.client.Error.FromString", new=make_error_mock
    )

    async def _unknown_proto_error_handler(_):
        return Response(status=400)

    mock_points(_unknown_proto_error_handler)

    with pytest.raises(UnknownError):
        async with Client("http://points.server") as client:
            await client(polygons=[[]], points_version=1)


async def test_raises_for_bad_proto(mock_points):
    async def _bad_proto_handler(_):
        return Response(status=200, body=b"bad proto content")

    mock_points(_bad_proto_handler)

    with pytest.raises(UnknownResponseBody):
        async with Client("http://points.server") as client:
            await client(polygons=[[]], points_version=1)


async def test_raises_exception_for_404_status(mock_points):
    mock_points(Response(status=404))

    with pytest.raises(NotFound):
        async with Client("http://points.server") as client:
            await client(polygons=[[]], points_version=1)
