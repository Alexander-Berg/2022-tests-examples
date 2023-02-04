import pytest
from aiohttp.web import Response

from maps_adv.geosmb.booking_yang.proto import orders_pb2

pytestmark = [pytest.mark.asyncio]


output_pb = orders_pb2.ClearOrdersForGdprOutput(cleared_order_ids=[111, 222])


async def test_sends_correct_request(client, mock_clear_orders_gdpr):
    request_path = None
    request_body = None

    async def _handler(request):
        nonlocal request_path, request_body
        request_path = request.path
        request_body = await request.read()
        return Response(status=200, body=output_pb.SerializeToString())

    mock_clear_orders_gdpr(_handler)

    await client.clear_orders_for_gdpr(passport_uid=123)

    assert request_path == "/internal/v1/clear_orders_for_gdpr/"
    assert (
        request_body
        == orders_pb2.ClearOrdersForGdprInput(passport_uid=123).SerializeToString()
    )


async def test_parses_response_correctly(client, mock_clear_orders_gdpr):
    mock_clear_orders_gdpr(
        lambda _: Response(status=200, body=output_pb.SerializeToString())
    )

    got = await client.clear_orders_for_gdpr(passport_uid=123)

    assert got == [111, 222]
