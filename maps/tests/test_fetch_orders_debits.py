from decimal import Decimal
import pytest
from aiohttp.web import Response
from maps_adv.billing_proxy.proto.orders_for_stat_pb2 import (
    OrdersDebitsInfoInput,
    OrdersDebitsInfo,
    OrderDebitsInfo,
    DebitInfo,
)
from maps_adv.billing_proxy.client import (
    Client,
    UnknownResponse,
)
from maps_adv.common.client.lib.exceptions import (
    BadGateway,
    ServiceUnavailable,
)
from maps_adv.common.client.lib.client import REQUEST_MAX_ATTEMPTS
from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]

BILLING_API_URL = "http://billing_proxy.server"

PROTO_RESULT = OrdersDebitsInfo(
    orders_debits=[
        OrderDebitsInfo(
            order_id=1,
            debits=[
                DebitInfo(
                    billed_at=dt("2022-01-10 00:00:00", as_proto=True),
                    amount="10.0000000000",
                ),
                DebitInfo(
                    billed_at=dt("2022-01-20 00:00:00", as_proto=True),
                    amount="100.0000000000",
                ),
                DebitInfo(
                    billed_at=dt("2022-01-20 00:00:01", as_proto=True),
                    amount="10.0000000000",
                ),
            ],
        ),
        OrderDebitsInfo(
            order_id=2,
            debits=[
                DebitInfo(
                    billed_at=dt("2022-01-22 00:00:00", as_proto=True),
                    amount="200.0000000000",
                ),
            ],
        ),
    ]
)

EXPECTED_RESULT = {
    1: [
        {"amount": Decimal("10"), "billed_at": dt("2022-01-10 00:00:00")},
        {"amount": Decimal("100"), "billed_at": dt("2022-01-20 00:00:00")},
        {"amount": Decimal("10"), "billed_at": dt("2022-01-20 00:00:01")},
    ],
    2: [
        {"amount": Decimal("200"), "billed_at": dt("2022-01-22 00:00:00")},
    ],
}


async def test_requests_data_correctly(mock_fetch_orders_debits_api):
    req_details = {}

    async def _handler(request):
        req_details.update(path=request.path, body=await request.read())
        return Response(status=200, body=PROTO_RESULT.SerializeToString())

    mock_fetch_orders_debits_api(_handler)

    async with Client(BILLING_API_URL) as client:
        result = await client.fetch_orders_debits([1, 2], dt("2022-01-01 00:00:00"))

    assert req_details["path"] == "/orders/debits/"
    assert OrdersDebitsInfoInput.FromString(
        req_details["body"]
    ) == OrdersDebitsInfoInput(
        order_ids=[1, 2],
        billed_after=dt("2022-01-01 00:00:00", as_proto=True),
    )
    assert result == EXPECTED_RESULT


async def test_raises_for_unexpected_status(mock_fetch_orders_debits_api):
    mock_fetch_orders_debits_api(Response(status=409))

    async with Client(BILLING_API_URL) as client:
        with pytest.raises(UnknownResponse):
            await client.fetch_orders_debits([1, 2], dt("2022-01-01 00:00:00"))


@pytest.mark.parametrize(
    "status, expected_exc", ([502, BadGateway], [503, ServiceUnavailable])
)
async def test_raises_for_expected_statuses_if_retrying_fails(
    status, expected_exc, mock_fetch_orders_debits_api
):
    for _ in range(REQUEST_MAX_ATTEMPTS):
        mock_fetch_orders_debits_api(Response(status=status))

    async with Client(BILLING_API_URL) as client:
        with pytest.raises(expected_exc):
            await client.fetch_orders_debits([1, 2], dt("2022-01-01 00:00:00"))


@pytest.mark.parametrize("status", (502, 503))
async def test_returns_result_if_retries_successfully(
    status, mock_fetch_orders_debits_api
):
    for _ in range(REQUEST_MAX_ATTEMPTS - 1):
        mock_fetch_orders_debits_api(Response(status=status))
    mock_fetch_orders_debits_api(
        Response(status=200, body=PROTO_RESULT.SerializeToString())
    )

    async with Client(BILLING_API_URL) as client:
        result = await client.fetch_orders_debits([1, 2], dt("2022-01-01 00:00:00"))

    assert result == EXPECTED_RESULT
