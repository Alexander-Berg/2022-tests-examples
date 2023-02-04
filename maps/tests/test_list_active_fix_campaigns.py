from decimal import Decimal

import pytest
from aiohttp.web import Response

from maps_adv.adv_store.client import (
    Client,
    UnknownResponse,
)
from maps_adv.adv_store.api.schemas.enums import FixTimeIntervalEnum
from maps_adv.adv_store.api.proto.billing_pb2 import Fix
from maps_adv.adv_store.api.proto.charger_api_pb2 import (
    CampaignForChargerFix,
    CampaignForChargerFixList,
    Money,
)
from maps_adv.common.client.lib.client import REQUEST_MAX_ATTEMPTS
from maps_adv.common.client.lib.exceptions import (
    BadGateway,
    ServiceUnavailable,
)
from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]

example_proto = CampaignForChargerFixList(
    campaigns=[
        CampaignForChargerFix(
            campaign_id=101,
            order_id=201,
            cost=Money(value=123400),
            time_interval=Fix.TimeIntervalEnum.Value("DAILY"),
            paid_till=dt("2020-03-01 00:00:00", as_proto=True),
            timezone="Europe/Moscow",
        ),
        CampaignForChargerFix(
            campaign_id=102,
            order_id=201,
            cost=Money(value=567800),
            time_interval=Fix.TimeIntervalEnum.Value("WEEKLY"),
            paid_till=dt("2020-03-02 00:00:00", as_proto=True),
            timezone="UTC",
        ),
        CampaignForChargerFix(
            campaign_id=103,
            order_id=202,
            cost=Money(value=900000),
            time_interval=Fix.TimeIntervalEnum.Value("MONTHLY"),
            paid_till=dt("2020-03-03 00:00:00", as_proto=True),
            timezone="Europe/Berlin",
        ),
    ]
)

example_result = [
    {
        "campaign_id": 101,
        "order_id": 201,
        "cost": Decimal("12.34"),
        "time_interval": FixTimeIntervalEnum.DAILY,
        "paid_till": dt("2020-03-01 00:00:00"),
        "timezone": "Europe/Moscow",
    },
    {
        "campaign_id": 102,
        "order_id": 201,
        "cost": Decimal("56.78"),
        "time_interval": FixTimeIntervalEnum.WEEKLY,
        "paid_till": dt("2020-03-02 00:00:00"),
        "timezone": "UTC",
    },
    {
        "campaign_id": 103,
        "order_id": 202,
        "cost": Decimal("90"),
        "time_interval": FixTimeIntervalEnum.MONTHLY,
        "paid_till": dt("2020-03-03 00:00:00"),
        "timezone": "Europe/Berlin",
    },
]


@pytest.mark.parametrize(
    "on_datetime, expected",
    ([dt("1970-01-01 00:02:00"), 120], [dt("2020-02-26 15:28:00"), 1582730880]),
)
async def test_requests_data_correctly(on_datetime, expected, mock_charger_fix_api):
    req_details = {}

    async def _handler(request):
        req_details.update(path=request.path, active_at=request.query.get("active_at"))
        return Response(status=200, body=example_proto.SerializeToString())

    mock_charger_fix_api(_handler)

    async with Client("http://adv_store.server") as client:
        await client.list_active_fix_campaigns(on_datetime)

    assert req_details["path"] == "/v2/campaigns/charger/fix/"
    assert req_details["active_at"] == str(expected)


async def test_returns_empty_list_if_server_returns_nothing(mock_charger_fix_api):
    proto = CampaignForChargerFixList(campaigns=[])
    mock_charger_fix_api(Response(status=200, body=proto.SerializeToString()))

    async with Client("http://adv_store.server") as client:
        got = await client.list_active_fix_campaigns(dt("2020-02-26 15:28:00"))

    assert got == []


async def test_parse_response_data_correctly(mock_charger_fix_api):
    mock_charger_fix_api(Response(status=200, body=example_proto.SerializeToString()))

    async with Client("http://adv_store.server") as client:
        got = await client.list_active_fix_campaigns(dt("2020-02-26 15:28:00"))

    assert got == example_result


async def test_raises_for_unexpected_status(mock_charger_fix_api):
    mock_charger_fix_api(Response(status=409))

    async with Client("http://adv_store.server") as client:

        with pytest.raises(UnknownResponse):
            await client.list_active_fix_campaigns(dt("2020-02-26 15:28:00"))


@pytest.mark.parametrize(
    "status, expected_exc", ([502, BadGateway], [503, ServiceUnavailable])
)
async def test_raises_for_expected_statuses_if_retrying_fails(
    status, expected_exc, mock_charger_fix_api
):
    for _ in range(REQUEST_MAX_ATTEMPTS):
        mock_charger_fix_api(Response(status=status))

    async with Client("http://adv_store.server") as client:

        with pytest.raises(expected_exc):
            await client.list_active_fix_campaigns(dt("2020-02-26 15:28:00"))


@pytest.mark.parametrize("status", (502, 503))
async def test_returns_result_if_retries_successfully(status, mock_charger_fix_api):
    for _ in range(REQUEST_MAX_ATTEMPTS - 1):
        mock_charger_fix_api(Response(status=status))
    mock_charger_fix_api(Response(status=200, body=example_proto.SerializeToString()))

    async with Client("http://adv_store.server") as client:
        got = await client.list_active_fix_campaigns(dt("2020-02-26 15:28:00"))

    assert got == example_result
