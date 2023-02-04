import pytest
from aiohttp.web import Response
from tenacity import RetryError

from maps_adv.points.client.lib import Client, ResultPoint, UnknownResponseCode
from maps_adv.points.proto.points_in_polygons_pb2 import PointsInPolygonsOutput
from maps_adv.points.proto.primitives_pb2 import IdentifiedPoint

pytestmark = [pytest.mark.asyncio]


@pytest.fixture()
async def points_client(mocker):
    mocker.patch("maps_adv.points.client.lib.client.REQUEST_MAX_ATTEMPTS", new=2)
    mocker.patch("maps_adv.points.client.lib.client.RETRY_WAIT_MULTIPLIER", new=0.001)

    yield Client("http://points.server")


@pytest.mark.parametrize("response_code", [502, 503])
async def test_raises_if_retry_attempts_exceeds_max(
    points_client, response_code, mock_points
):
    request_count = 0

    async def _handler(_):
        nonlocal request_count
        request_count += 1
        return Response(status=response_code)

    async def _handler_success(_):
        return Response(status=200, body=PointsInPolygonsOutput().SerializeToString())

    mock_points(_handler)
    mock_points(_handler)
    mock_points(_handler_success)

    with pytest.raises(RetryError):
        async with points_client as client:
            await client(polygons=[], points_version=1)

    assert request_count == 2


@pytest.mark.parametrize("response_code", [502, 503])
async def test_retries_request_for_expected_response_status_code(
    points_client, response_code, mock_points
):
    request_count = 0

    async def _handler(_):
        nonlocal request_count
        request_count += 1
        return Response(status=response_code)

    async def _handler_success(_):
        return Response(
            status=200,
            body=PointsInPolygonsOutput(
                points=[IdentifiedPoint(id=1, longitude="155.0", latitude="25.0")]
            ).SerializeToString(),
        )

    mock_points(_handler)
    mock_points(_handler_success)

    async with points_client as client:
        result = await client(polygons=[[]], points_version=1)

    assert request_count == 1
    assert result == [ResultPoint(longitude="155.0", latitude="25.0", id=1)]


@pytest.mark.parametrize("response_code", [504, 403])
async def test_does_not_retry_if_unexpected_response_status_code(
    response_code, mock_points, points_client
):
    request_count = 0

    async def _handler(_):
        nonlocal request_count
        request_count += 1

        return Response(
            status=response_code, body=PointsInPolygonsOutput().SerializeToString()
        )

    async def _handler_success(_):
        return Response(
            status=200,
            body=PointsInPolygonsOutput(
                points=[IdentifiedPoint(id=1, longitude="155.0", latitude="25.0")]
            ).SerializeToString(),
        )

    mock_points(_handler)
    mock_points(_handler_success)

    with pytest.raises(UnknownResponseCode):
        async with points_client as client:
            await client(polygons=[], points_version=1)

    assert request_count == 1
