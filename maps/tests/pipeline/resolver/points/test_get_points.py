import pytest
from aiohttp.web import Response

from maps_adv.export.lib.pipeline.exceptions import StepException
from maps_adv.export.lib.pipeline.param import Param
from maps_adv.export.lib.pipeline.steps.resolve_points import ResolvePointsStep

from maps_adv.export.tests.tools import create_campaigns
from maps_adv.points.client.lib import ResultPoint
from maps_adv.points.proto import points_in_polygons_pb2, primitives_pb2

pytestmark = [pytest.mark.asyncio]


def setup_campaigns():
    data = create_campaigns(
        campaigns_data=[
            (
                4242,
                {
                    "version": 1,
                    "areas": [
                        {
                            "points": [
                                {"longitude": 25, "latitude": 25},
                                {"longitude": 55, "latitude": 20},
                                {"longitude": 30, "latitude": -15},
                            ]
                        }
                    ],
                },
            ),
            # campaign without polygons
            (6577, None, [6, 7, 8]),
            # campaign with several polygons
            (
                8765,
                {
                    "version": 1,
                    "areas": [
                        {
                            "points": [
                                {"longitude": 50, "latitude": 60},
                                {"longitude": 90, "latitude": 100},
                                {"longitude": 20, "latitude": -20},
                            ]
                        },
                        {
                            "points": [
                                {"longitude": 14, "latitude": 97},
                                {"longitude": 23, "latitude": -23},
                                {"longitude": -30, "latitude": -15},
                                {"longitude": 67, "latitude": -87},
                            ]
                        },
                    ],
                },
            ),
            # campaign without points in polygons
            (
                2355,
                {
                    "version": 1,
                    "areas": [
                        {
                            "points": [
                                {"longitude": 40, "latitude": 60},
                                {"longitude": 80, "latitude": 100},
                                {"longitude": -30, "latitude": -20},
                            ]
                        }
                    ],
                },
            ),
        ],
        places={
            33: ResultPoint(longitude="25.0", latitude="25.0", id=33),
            55: ResultPoint(longitude="-65.0", latitude="20.0", id=55),
        },
    )

    expected_data = create_campaigns(
        campaigns_data=[
            (
                4242,
                {
                    "version": 1,
                    "areas": [
                        {
                            "points": [
                                {"longitude": 25, "latitude": 25},
                                {"longitude": 55, "latitude": 20},
                                {"longitude": 30, "latitude": -15},
                            ]
                        }
                    ],
                },
                [1, 2, 3],
            ),
            # campaign without polygons
            (6577, None, [6, 7, 8]),
            # campaign with several polygons
            (
                8765,
                {
                    "version": 1,
                    "areas": [
                        {
                            "points": [
                                {"longitude": 50, "latitude": 60},
                                {"longitude": 90, "latitude": 100},
                                {"longitude": 20, "latitude": -20},
                            ]
                        },
                        {
                            "points": [
                                {"longitude": 14, "latitude": 97},
                                {"longitude": 23, "latitude": -23},
                                {"longitude": -30, "latitude": -15},
                                {"longitude": 67, "latitude": -87},
                            ]
                        },
                    ],
                },
                [34, 56],
            ),
            # campaign without points in polygons
            (
                2355,
                {
                    "version": 1,
                    "areas": [
                        {
                            "points": [
                                {"longitude": 40, "latitude": 60},
                                {"longitude": 80, "latitude": 100},
                                {"longitude": -30, "latitude": -20},
                            ]
                        }
                    ],
                },
                [],
            ),
        ],
        places={
            33: ResultPoint(longitude="25.0", latitude="25.0", id=33),
            55: ResultPoint(longitude="-65.0", latitude="20.0", id=55),
            # new points
            1: ResultPoint(longitude="155.0", latitude="25.0", id=1),
            2: ResultPoint(longitude="-175.0", latitude="20.0", id=2),
            3: ResultPoint(longitude="160.0", latitude="-15.0", id=3),
            34: ResultPoint(longitude="15.0", latitude="2.0", id=34),
            56: ResultPoint(longitude="-17.0", latitude="2.0", id=56),
        },
    )

    return data, expected_data


async def test_adds_points_to_payload_for_all_campaigns_with_polygons(
    config, mock_points, experimental_options
):
    data, expected_data = setup_campaigns()

    mock_points(
        Response(
            body=points_in_polygons_pb2.PointsInPolygonsOutput(
                points=[
                    primitives_pb2.IdentifiedPoint(
                        longitude="155.0", latitude="25.0", id=1
                    ),
                    primitives_pb2.IdentifiedPoint(
                        longitude="-175.0", latitude="20.0", id=2
                    ),
                    primitives_pb2.IdentifiedPoint(
                        longitude="160.0", latitude="-15.0", id=3
                    ),
                ]
            ).SerializeToString(),
            status=200,
        )
    )
    mock_points(
        Response(
            body=points_in_polygons_pb2.PointsInPolygonsOutput(
                points=[
                    primitives_pb2.IdentifiedPoint(
                        longitude="15.0", latitude="2.0", id=34
                    ),
                    primitives_pb2.IdentifiedPoint(
                        longitude="-17.0", latitude="2.0", id=56
                    ),
                ]
            ).SerializeToString(),
            status=200,
        )
    )
    mock_points(
        Response(
            body=points_in_polygons_pb2.PointsInPolygonsOutput(
                points=[]
            ).SerializeToString(),
            status=200,
        )
    )

    param = Param()
    with experimental_options({"EXPERIMENT_WITHOUT_CACHE_POINTS": True}):
        step = ResolvePointsStep(param, None, config)
        await step(data["campaigns"])
        data["places"].update(param.value)

    assert data == expected_data


async def test_doesnt_change_payload_if_no_polygons_in_campaign(
    config, mock_points, experimental_options
):
    def _handler(request):
        return pytest.fail("Attempt to call Points Client")

    data = create_campaigns(campaigns_data=[(4242,)])
    expected_data = create_campaigns(campaigns_data=[(4242,)])

    mock_points(_handler)

    param = Param()
    with experimental_options({"EXPERIMENT_WITHOUT_CACHE_POINTS": True}):
        step = ResolvePointsStep(param, None, config)
        await step(data["campaigns"])
        data["places"].update(param.value)

    assert data == expected_data


async def test_bad_points_client_response(config, mock_points, experimental_options):
    data = create_campaigns(
        campaigns_data=[
            (
                4242,
                {"version": 1, "areas": [{"points": []}]},
            )
        ]
    )
    mock_points(Response(status=500))

    param = Param()
    with experimental_options({"EXPERIMENT_WITHOUT_CACHE_POINTS": True}):
        step = ResolvePointsStep(param, None, config)

    with pytest.raises(StepException) as exc:
        await step(data["campaigns"])

    assert exc.type == StepException
    assert exc.value.args[0] == [4242]
