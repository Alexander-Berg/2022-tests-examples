from decimal import Decimal

import pytest
from aiohttp.web import Response

from maps_adv.adv_store.client import (
    Client,
    UnknownResponse,
)
from maps_adv.adv_store.api.proto.charger_api_pb2 import (
    CampaignForCharger,
    CampaignForChargerList,
    Money,
)
from maps_adv.common.client.lib.client import REQUEST_MAX_ATTEMPTS
from maps_adv.common.client.lib.exceptions import (
    BadGateway,
    ServiceUnavailable,
)
from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]

example_proto = CampaignForChargerList(
    campaigns=[
        CampaignForCharger(
            campaign_id=4242,
            order_id=567382,
            cost=Money(value=34500),
            budget=Money(value=2345600),
            daily_budget=Money(value=200000),
            timezone="UTC",
        ),
        CampaignForCharger(
            campaign_id=4356,
            order_id=567382,
            cost=Money(value=45600),
            budget=Money(value=3000000),
            daily_budget=Money(value=345600),
            timezone="UTC",
        ),
        CampaignForCharger(
            campaign_id=1242,
            order_id=423773,
            cost=Money(value=50000),
            budget=Money(value=2345600),
            daily_budget=Money(value=345600),
            timezone="Europe/Moscow",
        ),
    ]
)

example_result = [
    {
        "campaign_id": 4242,
        "order_id": 567382,
        "cost": Decimal("3.45"),
        "budget": Decimal("234.56"),
        "daily_budget": Decimal("20"),
        "timezone": "UTC",
    },
    {
        "campaign_id": 4356,
        "order_id": 567382,
        "cost": Decimal("4.56"),
        "budget": Decimal("300"),
        "daily_budget": Decimal("34.56"),
        "timezone": "UTC",
    },
    {
        "campaign_id": 1242,
        "order_id": 423773,
        "cost": Decimal("5"),
        "budget": Decimal("234.56"),
        "daily_budget": Decimal("34.56"),
        "timezone": "Europe/Moscow",
    },
]


@pytest.mark.parametrize(
    "on_datetime, expected",
    ([dt("1970-01-01 00:02:00"), 120], [dt("2020-02-26 15:28:00"), 1582730880]),
)
async def test_requests_data_correctly(on_datetime, expected, mock_charger_cpm_api):
    req_details = {}

    async def _handler(request):
        req_details.update(path=request.path, active_at=request.query.get("active_at"))
        return Response(status=200, body=example_proto.SerializeToString())

    mock_charger_cpm_api(_handler)

    async with Client("http://adv_store.server") as client:
        await client.list_active_cpm_campaigns(on_datetime)

    assert req_details["path"] == "/v2/campaigns/charger/cpm/"
    assert req_details["active_at"] == str(expected)


async def test_returns_empty_list_if_server_returns_nothing(mock_charger_cpm_api):
    proto = CampaignForChargerList(campaigns=[])
    mock_charger_cpm_api(Response(status=200, body=proto.SerializeToString()))

    async with Client("http://adv_store.server") as client:
        got = await client.list_active_cpm_campaigns(dt("2020-02-26 15:28:00"))

    assert got == []


async def test_parse_response_data_correctly(mock_charger_cpm_api):
    mock_charger_cpm_api(Response(status=200, body=example_proto.SerializeToString()))

    async with Client("http://adv_store.server") as client:
        got = await client.list_active_cpm_campaigns(dt("2020-02-26 15:28:00"))

    assert got == example_result


async def test_raises_for_unexpected_status(mock_charger_cpm_api):
    mock_charger_cpm_api(Response(status=409))

    async with Client("http://adv_store.server") as client:

        with pytest.raises(UnknownResponse):
            await client.list_active_cpm_campaigns(dt("2020-02-26 15:28:00"))


@pytest.mark.parametrize(
    "status, expected_exc", ([502, BadGateway], [503, ServiceUnavailable])
)
async def test_raises_for_expected_statuses_if_retrying_fails(
    status, expected_exc, mock_charger_cpm_api
):
    for _ in range(REQUEST_MAX_ATTEMPTS):
        mock_charger_cpm_api(Response(status=status))

    async with Client("http://adv_store.server") as client:

        with pytest.raises(expected_exc):
            await client.list_active_cpm_campaigns(dt("2020-02-26 15:28:00"))


@pytest.mark.parametrize("status", (502, 503))
async def test_returns_result_if_retries_successfully(status, mock_charger_cpm_api):
    for _ in range(REQUEST_MAX_ATTEMPTS - 1):
        mock_charger_cpm_api(Response(status=status))
    mock_charger_cpm_api(Response(status=200, body=example_proto.SerializeToString()))

    async with Client("http://adv_store.server") as client:
        got = await client.list_active_cpm_campaigns(dt("2020-02-26 15:28:00"))

    assert got == example_result


async def test_returns_none_for_order_id_if_campaign_without_order_id(
    mock_charger_cpm_api,
):
    mock_charger_cpm_api(
        Response(
            status=200,
            body=CampaignForChargerList(
                campaigns=[
                    CampaignForCharger(
                        campaign_id=3242,
                        cost=Money(value=50000),
                        budget=Money(value=2345600),
                        daily_budget=Money(value=345600),
                        timezone="UTC",
                    )
                ]
            ).SerializeToString(),
        )
    )

    async with Client("http://adv_store.server") as client:
        result = await client.list_active_cpm_campaigns(dt("1970-01-01 00:02:00"))

    assert result == [
        {
            "campaign_id": 3242,
            "order_id": None,
            "cost": Decimal("5"),
            "budget": Decimal("234.56"),
            "daily_budget": Decimal("34.56"),
            "timezone": "UTC",
        }
    ]


async def test_returns_none_for_budget_and_daily_budget_if_campaign_without_them(
    mock_charger_cpm_api,
):
    mock_charger_cpm_api(
        Response(
            status=200,
            body=CampaignForChargerList(
                campaigns=[
                    CampaignForCharger(
                        campaign_id=3242,
                        cost=Money(value=50000),
                        budget=None,
                        daily_budget=None,
                        timezone="UTC",
                    )
                ]
            ).SerializeToString(),
        )
    )

    async with Client("http://adv_store.server") as client:
        result = await client.list_active_cpm_campaigns(dt("1970-01-01 00:02:00"))

    assert result == [
        {
            "campaign_id": 3242,
            "order_id": None,
            "cost": Decimal("5"),
            "budget": None,
            "daily_budget": None,
            "timezone": "UTC",
        }
    ]
