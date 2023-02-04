import pytest
from aiohttp.web import Response

from maps_adv.stat_tasks_starter.lib.charger.clients import AdvStoreClient as Client
from maps_adv.stat_tasks_starter.lib.charger.clients.adv_store.enums import (
    ReasonsToStop,
)
from maps_adv.stat_tasks_starter.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_200(adv_store_send_campaigns_to_stop_rmock):
    campaigns = {
        "processed_at": dt(1566313386),
        "campaigns": [
            {"campaign_id": 9786, "reason_stopped": ReasonsToStop.daily_budget_limit},
            {"campaign_id": 8764, "reason_stopped": ReasonsToStop.budget_limit},
            {"campaign_id": 1234, "reason_stopped": ReasonsToStop.order_limit},
            {"campaign_id": 3523, "reason_stopped": ReasonsToStop.order_limit},
        ],
    }

    adv_store_send_campaigns_to_stop_rmock(Response(status=200))

    async with Client("http://somedomain.com") as client:
        await client.send_campaigns_to_stop(campaigns)


async def test_returns_200_if_empty_campaigns_list(
    adv_store_send_campaigns_to_stop_rmock
):
    campaigns = {"processed_at": dt(1566313386), "campaigns": []}

    adv_store_send_campaigns_to_stop_rmock(Response(status=200))

    async with Client("http://somedomain.com") as client:
        await client.send_campaigns_to_stop(campaigns)
