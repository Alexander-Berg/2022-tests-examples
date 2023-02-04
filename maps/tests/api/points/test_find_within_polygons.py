import json
import time

import pytest

from smb.common.multiruntime.lib.pytest import skip_in_arcadia

from maps_adv.points.proto.errors_pb2 import Error
from maps_adv.points.proto.points_in_polygons_pb2 import (
    PointsInPolygonsInput,
    PointsInPolygonsOutput,
)
from maps_adv.points.proto.primitives_pb2 import IdentifiedPoint, Point, Polygon
from maps_adv.points.server.lib.enums import PointType
from maps_adv.points.server.tests.generators import PointsGenerator, PolygonsGenerator

pytestmark = [pytest.mark.asyncio]


def url(point_type, version):
    return f"/api/v1/points/{point_type}/{version}/by-polygons/"


async def test_returns_points_within_polygons(factory, api):
    await factory.create_points()
    input_pb = PointsInPolygonsInput(
        polygons=[
            Polygon(
                points=[
                    Point(longitude="0.0", latitude="40.0"),
                    Point(longitude="80.0", latitude="40.0"),
                    Point(longitude="80.0", latitude="-40.0"),
                    Point(longitude="0.0", latitude="-40.0"),
                    Point(longitude="0.0", latitude="40.0"),
                ]
            ),
            Polygon(
                points=[
                    Point(longitude="130", latitude="40"),
                    Point(longitude="-150", latitude="40"),
                    Point(longitude="-150", latitude="-40"),
                    Point(longitude="130", latitude="-40"),
                    Point(longitude="130", latitude="40"),
                ]
            ),
        ]
    )

    got = await api.post(
        url("billboard", 1),
        proto=input_pb,
        decode_as=PointsInPolygonsOutput,
        expected_status=200,
    )

    assert got == PointsInPolygonsOutput(
        points=[
            IdentifiedPoint(longitude="25", latitude="25", id=got.points[0].id),
            IdentifiedPoint(longitude="55", latitude="20", id=got.points[1].id),
            IdentifiedPoint(longitude="30", latitude="-15", id=got.points[2].id),
            IdentifiedPoint(longitude="155", latitude="25", id=got.points[3].id),
            IdentifiedPoint(longitude="-175", latitude="20", id=got.points[4].id),
            IdentifiedPoint(longitude="160", latitude="-15", id=got.points[5].id),
        ]
    )


@pytest.mark.parametrize(
    "point_type, search_type",
    [
        (PointType.billboard, PointType.testing),
        (PointType.testing, PointType.billboard),
    ],
)
async def test_does_not_return_points_of_another_type(
    point_type, search_type, factory, api
):
    await factory.create_collection(search_type)
    await factory.create_points(point_type)

    input_pb = PointsInPolygonsInput(
        polygons=[
            Polygon(
                points=[
                    Point(longitude="0", latitude="40"),
                    Point(longitude="80", latitude="40"),
                    Point(longitude="80", latitude="-40"),
                    Point(longitude="0", latitude="-40"),
                    Point(longitude="0", latitude="40"),
                ]
            )
        ]
    )
    got = await api.post(
        url(search_type.value, 1),
        proto=input_pb,
        decode_as=PointsInPolygonsOutput,
        expected_status=200,
    )

    assert got == PointsInPolygonsOutput(points=[])


#   GEODISPLAY-1571
#   async def test_does_not_return_points_from_another_collections(factory, api):
#       await factory.create_collection(version=2)
#       await factory.create_points()

#       input_pb = PointsInPolygonsInput(
#           polygons=[
#               Polygon(
#                   points=[
#                       Point(longitude="0", latitude="40"),
#                       Point(longitude="80", latitude="40"),
#                       Point(longitude="80", latitude="-40"),
#                       Point(longitude="0", latitude="-40"),
#                       Point(longitude="0", latitude="40"),
#                   ]
#               )
#           ]
#       )

#       got = await api.post(
#           url("billboard", 2),
#           proto=input_pb,
#           decode_as=PointsInPolygonsOutput,
#           expected_status=200,
#       )

#       assert got == PointsInPolygonsOutput(points=[])


async def test_returns_404_for_incorrect_version(factory, api):
    await factory.create_points()
    input_pb = PointsInPolygonsInput(
        polygons=[
            Polygon(
                points=[
                    Point(longitude="0.0", latitude="40.0"),
                    Point(longitude="80.0", latitude="40.0"),
                    Point(longitude="80.0", latitude="-40.0"),
                    Point(longitude="0.0", latitude="-40.0"),
                    Point(longitude="0.0", latitude="40.0"),
                ]
            )
        ]
    )

    await api.post(url("billboard", "sometext"), proto=input_pb, expected_status=404)


async def test_raises_for_nonexistent_type_and_collection(api):
    input_pb = PointsInPolygonsInput(
        polygons=[
            Polygon(
                points=[
                    Point(longitude="0", latitude="40"),
                    Point(longitude="80", latitude="40"),
                    Point(longitude="80", latitude="-40"),
                    Point(longitude="0", latitude="-40"),
                    Point(longitude="0", latitude="40"),
                ]
            )
        ]
    )

    got = await api.post(
        url("billboard", 1), proto=input_pb, decode_as=Error, expected_status=400
    )

    assert got == Error(code=Error.COLLECTION_NOT_FOUND)


async def test_raises_for_invalid_version(factory, api):
    await factory.create_points()
    input_pb = PointsInPolygonsInput(
        polygons=[
            Polygon(
                points=[
                    Point(longitude="10", latitude="10"),
                    Point(longitude="20", latitude="10"),
                    Point(longitude="10", latitude="0"),
                    Point(longitude="10", latitude="10"),
                ]
            )
        ]
    )

    got = await api.post(
        url("billboard", 0), proto=input_pb, decode_as=Error, expected_status=400
    )

    assert got == Error(code=Error.INVALID_VERSION)


async def test_raises_if_no_polygons_passed(factory, api):
    await factory.create_points()
    input_pb = PointsInPolygonsInput(polygons=[])

    got = await api.post(
        url("billboard", 1), proto=input_pb, decode_as=Error, expected_status=400
    )

    assert got == Error(code=Error.NO_POLYGONS_PASSED)


async def test_raises_for_non_closed_polygon(factory, api):
    await factory.create_points()
    input_pb = PointsInPolygonsInput(
        polygons=[
            Polygon(
                points=[
                    Point(longitude="10", latitude="10"),
                    Point(longitude="20", latitude="10"),
                    Point(longitude="10", latitude="0"),
                    Point(longitude="10", latitude="50"),
                ]
            )
        ]
    )

    got = await api.post(
        url("billboard", 1), proto=input_pb, decode_as=Error, expected_status=400
    )

    assert got == Error(code=Error.NON_CLOSED_POLYGON)


async def test_raises_for_invalid_polygon(factory, api):
    await factory.create_points()
    input_pb = PointsInPolygonsInput(
        polygons=[
            Polygon(
                points=[
                    Point(longitude="10", latitude="10"),
                    Point(longitude="20", latitude="10"),
                    Point(longitude="10", latitude="0"),
                ]
            )
        ]
    )

    got = await api.post(
        url("billboard", 1), proto=input_pb, decode_as=Error, expected_status=400
    )

    assert got == Error(code=Error.INVALID_POLYGON)


async def test_raises_for_validation_error(factory, api):
    await factory.create_points()
    input_pb = PointsInPolygonsInput(
        polygons=[
            Polygon(
                points=[
                    Point(longitude="10", latitude="10"),
                    Point(longitude="20", latitude="10"),
                    Point(longitude="10", latitude="0"),
                    Point(longitude="10", latitude="10"),
                ]
            )
        ]
    )

    got = await api.post(
        url("sometext", 1), proto=input_pb, decode_as=Error, expected_status=400
    )

    assert got == Error(
        code=Error.VALIDATION_ERROR,
        description=json.dumps({"point_type": ["Invalid enum member sometext"]}),
    )


@pytest.mark.slow
@pytest.mark.real_db
@skip_in_arcadia
async def test_completes_in_expected_time(api, con):
    points_generator = PointsGenerator()
    await points_generator(PointType.billboard, 1, con)

    polygons = [Polygon(points=el) for el in PolygonsGenerator()]
    input_pb = PointsInPolygonsInput(polygons=polygons)

    now = time.time()
    await api.post(
        url("billboard", 1),
        proto=input_pb,
        decode_as=PointsInPolygonsOutput,
        expected_status=200,
    )
    spent = time.time() - now

    assert spent < 45


@pytest.mark.slow
@pytest.mark.real_db
@skip_in_arcadia
async def test_returns_expected_count(api, con):
    points_generator = PointsGenerator()
    await points_generator(PointType.billboard, 1, con)

    polygons = [Polygon(points=el) for el in PolygonsGenerator()]
    input_pb = PointsInPolygonsInput(polygons=polygons)

    got = await api.post(
        url("billboard", 1),
        proto=input_pb,
        decode_as=PointsInPolygonsOutput,
        expected_status=200,
    )

    assert len(got.points) == 2280
