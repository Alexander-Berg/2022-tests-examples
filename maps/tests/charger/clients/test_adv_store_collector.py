from decimal import Decimal

import pytest
from aiohttp.web import Response

from maps_adv.adv_store.api.proto.charger_api_pb2 import (
    CampaignForCharger,
    CampaignForChargerList,
    Money,
)
from maps_adv.stat_tasks_starter.lib.charger.clients import AdvStoreClient as Client
from maps_adv.stat_tasks_starter.lib.charger.clients.exceptions import UnknownResponse
from maps_adv.stat_tasks_starter.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_data_from_adv_store(adv_store_receive_active_campaigns_rmock):
    message = CampaignForChargerList(
        campaigns=[
            CampaignForCharger(
                campaign_id=4242,
                order_id=567382,
                cost=Money(value=30000),
                budget=Money(value=2000000),
                daily_budget=Money(value=200000),
                timezone="UTC",
            ),
            CampaignForCharger(
                campaign_id=4356,
                order_id=567382,
                cost=Money(value=40000),
                budget=Money(value=3000000),
                daily_budget=Money(value=300000),
                timezone="UTC",
            ),
            CampaignForCharger(
                campaign_id=1242,
                order_id=423773,
                cost=Money(value=50000),
                budget=Money(value=2000000),
                daily_budget=Money(value=300000),
                timezone="Europe/Moscow",
            ),
        ]
    ).SerializeToString()

    adv_store_receive_active_campaigns_rmock(Response(body=message, status=200))

    async with Client("http://somedomain.com") as client:
        result = await client.receive_active_campaigns(dt("2019-05-06 01:05:00"))

    assert result == [
        {
            "campaign_id": 4242,
            "order_id": 567382,
            "cost": Decimal(3),
            "budget": Decimal(200),
            "daily_budget": Decimal(20),
            "timezone": "UTC",
        },
        {
            "campaign_id": 4356,
            "order_id": 567382,
            "cost": Decimal(4),
            "budget": Decimal(300),
            "daily_budget": Decimal(30),
            "timezone": "UTC",
        },
        {
            "campaign_id": 1242,
            "order_id": 423773,
            "cost": Decimal(5),
            "budget": Decimal(200),
            "daily_budget": Decimal(30),
            "timezone": "Europe/Moscow",
        },
    ]


async def test_returns_nothing_if_no_data(adv_store_receive_active_campaigns_rmock):
    message = CampaignForChargerList(campaigns=[]).SerializeToString()

    adv_store_receive_active_campaigns_rmock(Response(body=message, status=200))

    async with Client("http://somedomain.com") as client:
        result = await client.receive_active_campaigns(dt("2019-05-06 01:05:00"))

    assert result == []


async def test_returns_data_if_no_daily_budget(
    adv_store_receive_active_campaigns_rmock,
):
    message = CampaignForChargerList(
        campaigns=[
            CampaignForCharger(
                campaign_id=4242,
                order_id=567382,
                cost=Money(value=30000),
                budget=Money(value=2000000),
                timezone="UTC",
            )
        ]
    ).SerializeToString()

    adv_store_receive_active_campaigns_rmock(Response(body=message, status=200))

    async with Client("http://somedomain.com") as client:
        result = await client.receive_active_campaigns(dt("2019-05-06 01:05:00"))

    assert result == [
        {
            "campaign_id": 4242,
            "order_id": 567382,
            "cost": Decimal(3),
            "budget": Decimal(200),
            "timezone": "UTC",
        }
    ]


async def test_returns_data_if_no_budget(adv_store_receive_active_campaigns_rmock):
    message = CampaignForChargerList(
        campaigns=[
            CampaignForCharger(
                campaign_id=4242,
                order_id=567382,
                cost=Money(value=30000),
                daily_budget=Money(value=300000),
                timezone="UTC",
            )
        ]
    ).SerializeToString()

    adv_store_receive_active_campaigns_rmock(Response(body=message, status=200))

    async with Client("http://somedomain.com") as client:
        result = await client.receive_active_campaigns(dt("2019-05-06 01:05:00"))

    assert result == [
        {
            "campaign_id": 4242,
            "order_id": 567382,
            "cost": Decimal(3),
            "daily_budget": Decimal(30),
            "timezone": "UTC",
        }
    ]


async def test_returns_data_if_both_budgets_missing(
    adv_store_receive_active_campaigns_rmock,
):
    message = CampaignForChargerList(
        campaigns=[
            CampaignForCharger(
                campaign_id=4242,
                order_id=567382,
                cost=Money(value=30000),
                timezone="UTC",
            )
        ]
    ).SerializeToString()

    adv_store_receive_active_campaigns_rmock(Response(body=message, status=200))

    async with Client("http://somedomain.com") as client:
        result = await client.receive_active_campaigns(dt("2019-05-06 01:05:00"))

    assert result == [
        {"campaign_id": 4242, "order_id": 567382, "cost": Decimal(3), "timezone": "UTC"}
    ]


async def test_timestamp_passed_as_query_parameter(
    adv_store_receive_active_campaigns_rmock,
):
    def handler(request):
        expected = "/v2/campaigns/charger/cpm/?active_at=1557104700"
        assert str(request.rel_url) == expected
        return Response(body=b"", status=200)

    adv_store_receive_active_campaigns_rmock(handler)

    async with Client("http://somedomain.com") as client:
        await client.receive_active_campaigns(dt("2019-05-06 01:05:00"))


async def test_raises_for_unknown_response(adv_store_receive_active_campaigns_rmock):
    adv_store_receive_active_campaigns_rmock(Response(body=b"{}", status=404))

    async with Client("http://somedomain.com") as client:
        with pytest.raises(UnknownResponse) as exc_info:
            await client.receive_active_campaigns(dt("2019-05-06 01:05:00"))

    assert "Status=404, payload=b'{}'" in exc_info.value.args


async def test_returns_data_for_campaign_with_optional_order(
    adv_store_receive_active_campaigns_rmock,
):
    message = CampaignForChargerList(
        campaigns=[
            # without order
            CampaignForCharger(
                campaign_id=4241,
                cost=Money(value=30000),
                budget=Money(value=2000000),
                daily_budget=Money(value=200000),
                timezone="UTC",
            ),
            # with order
            CampaignForCharger(
                campaign_id=4242,
                order_id=567382,
                cost=Money(value=30000),
                budget=Money(value=2000000),
                daily_budget=Money(value=200000),
                timezone="UTC",
            ),
        ]
    ).SerializeToString()

    adv_store_receive_active_campaigns_rmock(Response(body=message, status=200))

    async with Client("http://somedomain.com") as client:
        result = await client.receive_active_campaigns(dt("2019-05-06 01:05:00"))

    assert result == [
        {
            "campaign_id": 4241,
            "cost": Decimal(3),
            "budget": Decimal(200),
            "daily_budget": Decimal(20),
            "timezone": "UTC",
        },
        {
            "campaign_id": 4242,
            "order_id": 567382,
            "cost": Decimal(3),
            "budget": Decimal(200),
            "daily_budget": Decimal(20),
            "timezone": "UTC",
        },
    ]
