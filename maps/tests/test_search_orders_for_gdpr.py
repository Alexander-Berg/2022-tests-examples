import pytest
from aiohttp.web import Response

from maps_adv.geosmb.booking_yang.proto import orders_pb2

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(client, mock_search_orders_gdpr):
    request_path = None
    request_body = None

    async def _handler(request):
        nonlocal request_path, request_body
        request_path = request.path
        request_body = await request.read()
        return Response(
            status=200,
            body=orders_pb2.SearchOrdersForGdprOutput(
                orders_exist=True
            ).SerializeToString(),
        )

    mock_search_orders_gdpr(_handler)

    await client.search_orders_for_gdpr(passport_uid=123)

    assert request_path == "/internal/v1/search_orders_for_gdpr/"
    assert (
        request_body
        == orders_pb2.SearchOrdersForGdprInput(passport_uid=123).SerializeToString()
    )


@pytest.mark.parametrize("orders_exists", [True, False])
async def test_parses_response_correctly(
    client, mock_search_orders_gdpr, orders_exists
):
    mock_search_orders_gdpr(
        lambda _: Response(
            status=200,
            body=orders_pb2.SearchOrdersForGdprOutput(
                orders_exist=orders_exists
            ).SerializeToString(),
        )
    )

    got = await client.search_orders_for_gdpr(passport_uid=123)

    assert got is orders_exists
