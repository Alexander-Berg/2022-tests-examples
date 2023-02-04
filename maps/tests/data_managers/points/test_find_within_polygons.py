import pytest

from maps_adv.points.server.lib.data_managers.points import CollectionNotFound
from maps_adv.points.server.lib.enums import PointType
from maps_adv.points.server.tests import Any, make_points, make_polygons

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("point_type", list(PointType))
@pytest.mark.parametrize(
    "polygons, expected",
    (
        [["0 40, 80 40, 80 -40, 0 -40, 0 40"], ["25 25", "55 20", "30 -15"]],
        [
            ["130 40, -150 40, -150 -40, 130 -40, 130 40"],
            ["155 25", "-175 20", "160 -15"],
        ],
        [
            [
                "0 40, 80 40, 80 -40, 0 -40, 0 40",
                "130 40, -150 40, -150 -40, 130 -40, 130 40",
            ],
            ["25 25", "55 20", "30 -15", "155 25", "-175 20", "160 -15"],
        ],
    ),
)
async def test_returns_points_within_polygons(
    point_type, polygons, expected, factory, points_dm
):
    await factory.create_points(point_type)
    polygons = make_polygons(polygons)
    expected = make_points(expected, with_id=True)

    got = await points_dm.find_within_polygons(point_type, 1, polygons)

    assert got == expected


@pytest.mark.parametrize(
    "point_type, search_type",
    [
        (PointType.billboard, PointType.testing),
        (PointType.testing, PointType.billboard),
    ],
)
@pytest.mark.parametrize(
    "polygons",
    (
        ["0 40, 80 40, 80 -40, 0 -40, 0 40"],
        ["130 40, -150 40, -150 -40, 130 -40, 130 40"],
        [
            "0 40, 80 40, 80 -40, 0 -40, 0 40",
            "130 40, -150 40, -150 -40, 130 -40, 130 40",
        ],
    ),
)
async def test_does_not_return_points_of_another_type(
    point_type, search_type, polygons, factory, points_dm
):
    await factory.create_points(point_type)
    await factory.create_collection(search_type)
    polygons = make_polygons(polygons)

    got = await points_dm.find_within_polygons(search_type, 1, polygons)

    assert got == []


#   GEODISPLAY-1571
#   @pytest.mark.parametrize("point_type", list(PointType))
#   @pytest.mark.parametrize(
#       "polygons",
#       (
#           ["0 40, 80 40, 80 -40, 0 -40, 0 40"],
#           ["130 40, -150 40, -150 -40, 130 -40, 130 40"],
#           [
#               "0 40, 80 40, 80 -40, 0 -40, 0 40",
#               "130 40, -150 40, -150 -40, 130 -40, 130 40",
#           ],
#       ),
#   )
#   async def test_does_not_return_points_from_another_collections(
#       point_type, polygons, factory, points_dm
#   ):
#       await factory.create_collection(point_type, 2)
#       await factory.create_points(point_type)
#       polygons = make_polygons(polygons)

#       got = await points_dm.find_within_polygons(point_type, 2, polygons)

#       assert got == []


async def test_raises_for_unexistant_type_and_collection(points_dm):
    polygons = make_polygons(["0 40, 80 40, 80 -40, 0 -40, 0 40"])

    with pytest.raises(CollectionNotFound):
        await points_dm.find_within_polygons(PointType.billboard, 1, polygons)


async def test_returns_points_data(factory, points_dm):
    await factory.create_points()
    polygons = make_polygons(["0 40, 80 40, 80 -40, 0 -40, 0 40"])

    got = await points_dm.find_within_polygons(PointType.billboard, 1, polygons)

    assert got == [
        {"longitude": "25", "latitude": "25", "id": Any(int)},
        {"longitude": "55", "latitude": "20", "id": Any(int)},
        {"longitude": "30", "latitude": "-15", "id": Any(int)},
    ]
