from decimal import Decimal

import pytest
from aiohttp.web import Response

from maps_adv.adv_store.client import (
    Client,
    UnknownResponse,
)
from maps_adv.adv_store.api.proto.charger_api_pb2 import (
    CampaignForChargerCpa,
    CampaignForChargerCpaList,
    Money,
)
from maps_adv.common.client.lib.client import REQUEST_MAX_ATTEMPTS
from maps_adv.common.client.lib.exceptions import (
    BadGateway,
    ServiceUnavailable,
)
from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]

example_proto = CampaignForChargerCpaList(
    campaigns=[
        CampaignForChargerCpa(
            campaign_id=4242,
            order_id=567382,
            cost=Money(value=34500),
            budget=Money(value=2345600),
            daily_budget=Money(value=200000),
            timezone="UTC",
            paid_events_names=[
                "ACTION_MAKE_ROUTE",
            ],
        ),
        CampaignForChargerCpa(
            campaign_id=4356,
            order_id=567382,
            cost=Money(value=45600),
            budget=Money(value=3000000),
            daily_budget=Money(value=345600),
            timezone="UTC",
            paid_events_names=[
                "ACTION_MAKE_ROUTE",
            ],
        ),
        CampaignForChargerCpa(
            campaign_id=1242,
            order_id=423773,
            cost=Money(value=50000),
            budget=Money(value=2345600),
            daily_budget=Money(value=345600),
            timezone="Europe/Moscow",
            paid_events_names=[
                "ACTION_MAKE_ROUTE",
            ],
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
        "paid_events_names": [
            "ACTION_MAKE_ROUTE",
        ],
    },
    {
        "campaign_id": 4356,
        "order_id": 567382,
        "cost": Decimal("4.56"),
        "budget": Decimal("300"),
        "daily_budget": Decimal("34.56"),
        "timezone": "UTC",
        "paid_events_names": [
            "ACTION_MAKE_ROUTE",
        ],
    },
    {
        "campaign_id": 1242,
        "order_id": 423773,
        "cost": Decimal("5"),
        "budget": Decimal("234.56"),
        "daily_budget": Decimal("34.56"),
        "timezone": "Europe/Moscow",
        "paid_events_names": [
            "ACTION_MAKE_ROUTE",
        ],
    },
]


@pytest.mark.parametrize(
    "on_datetime, expected",
    ([dt("1970-01-01 00:02:00"), 120], [dt("2020-02-26 15:28:00"), 1582730880]),
)
async def test_requests_data_correctly(on_datetime, expected, mock_charger_cpa_api):
    req_details = {}

    async def _handler(request):
        req_details.update(path=request.path, active_at=request.query.get("active_at"))
        return Response(status=200, body=example_proto.SerializeToString())

    mock_charger_cpa_api(_handler)

    async with Client("http://adv_store.server") as client:
        await client.list_active_cpa_campaigns(on_datetime)

    assert req_details["path"] == "/v2/campaigns/charger/cpa/"
    assert req_details["active_at"] == str(expected)


async def test_returns_empty_list_if_server_returns_nothing(mock_charger_cpa_api):
    proto = CampaignForChargerCpaList(campaigns=[])
    mock_charger_cpa_api(Response(status=200, body=proto.SerializeToString()))

    async with Client("http://adv_store.server") as client:
        got = await client.list_active_cpa_campaigns(dt("2020-02-26 15:28:00"))

    assert got == []


async def test_parse_response_data_correctly(mock_charger_cpa_api):
    mock_charger_cpa_api(Response(status=200, body=example_proto.SerializeToString()))

    async with Client("http://adv_store.server") as client:
        got = await client.list_active_cpa_campaigns(dt("2020-02-26 15:28:00"))

    assert got == example_result


async def test_raises_for_unexpected_status(mock_charger_cpa_api):
    mock_charger_cpa_api(Response(status=409))

    async with Client("http://adv_store.server") as client:

        with pytest.raises(UnknownResponse):
            await client.list_active_cpa_campaigns(dt("2020-02-26 15:28:00"))


@pytest.mark.parametrize(
    "status, expected_exc", ([502, BadGateway], [503, ServiceUnavailable])
)
async def test_raises_for_expected_statuses_if_retrying_fails(
    status, expected_exc, mock_charger_cpa_api
):
    for _ in range(REQUEST_MAX_ATTEMPTS):
        mock_charger_cpa_api(Response(status=status))

    async with Client("http://adv_store.server") as client:

        with pytest.raises(expected_exc):
            await client.list_active_cpa_campaigns(dt("2020-02-26 15:28:00"))


@pytest.mark.parametrize("status", (502, 503))
async def test_returns_result_if_retries_successfully(status, mock_charger_cpa_api):
    for _ in range(REQUEST_MAX_ATTEMPTS - 1):
        mock_charger_cpa_api(Response(status=status))
    mock_charger_cpa_api(Response(status=200, body=example_proto.SerializeToString()))

    async with Client("http://adv_store.server") as client:
        got = await client.list_active_cpa_campaigns(dt("2020-02-26 15:28:00"))

    assert got == example_result


async def test_returns_none_for_order_id_if_campaign_without_order_id(
    mock_charger_cpa_api,
):
    mock_charger_cpa_api(
        Response(
            status=200,
            body=CampaignForChargerCpaList(
                campaigns=[
                    CampaignForChargerCpa(
                        campaign_id=3242,
                        cost=Money(value=50000),
                        budget=Money(value=2345600),
                        daily_budget=Money(value=345600),
                        timezone="UTC",
                        paid_events_names=[
                            "ACTION_MAKE_ROUTE",
                        ],
                    )
                ]
            ).SerializeToString(),
        )
    )

    async with Client("http://adv_store.server") as client:
        result = await client.list_active_cpa_campaigns(dt("1970-01-01 00:02:00"))

    assert result == [
        {
            "campaign_id": 3242,
            "order_id": None,
            "cost": Decimal("5"),
            "budget": Decimal("234.56"),
            "daily_budget": Decimal("34.56"),
            "timezone": "UTC",
            "paid_events_names": [
                "ACTION_MAKE_ROUTE",
            ],
        }
    ]


async def test_returns_paid_events_names_as_list(mock_charger_cpa_api):
    mock_charger_cpa_api(
        Response(
            status=200,
            body=CampaignForChargerCpaList(
                campaigns=[
                    CampaignForChargerCpa(
                        campaign_id=3242,
                        cost=Money(value=50000),
                        budget=Money(value=2345600),
                        daily_budget=Money(value=345600),
                        timezone="UTC",
                        paid_events_names=[],
                    )
                ]
            ).SerializeToString(),
        )
    )

    async with Client("http://adv_store.server") as client:
        result = await client.list_active_cpa_campaigns(dt("1970-01-01 00:02:00"))

    assert isinstance(result[0]["paid_events_names"], list)


async def test_returns_none_for_budget_and_daily_budget_if_campaign_without_them(
    mock_charger_cpa_api,
):
    mock_charger_cpa_api(
        Response(
            status=200,
            body=CampaignForChargerCpaList(
                campaigns=[
                    CampaignForChargerCpa(
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
        result = await client.list_active_cpa_campaigns(dt("1970-01-01 00:02:00"))

    assert result == [
        {
            "campaign_id": 3242,
            "order_id": None,
            "cost": Decimal("5"),
            "budget": None,
            "daily_budget": None,
            "paid_events_names": [],
            "timezone": "UTC",
        }
    ]
