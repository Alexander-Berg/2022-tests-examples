from decimal import Decimal

import pytest

from maps_adv.geosmb.clients.market import ClientAction
from maps_adv.geosmb.landlord.server.lib.enums import ServiceItemType

pytestmark = [pytest.mark.asyncio]


async def test_returns_market_int_services_for_biz_id(dm, factory):

    for idx in (4, 8, 15, 16, 23, 42):
        await factory.create_market_int_service(
            service_id=idx,
            biz_id=15,
            service_data={
                "action_type": "request",
                "categories": ["category1"],
                "description": f"desc_{idx}",
                "id": idx,
                "image": f"image_link_{idx}",
                "min_cost": idx,
                "min_duration": idx,
                "name": f"title_{idx}",
            },
        )

    got = await dm.fetch_org_market_int_services(biz_id=15)

    assert got == [
        dict(
            type=ServiceItemType.MARKET,
            id=idx,
            name=f"title_{idx}",
            min_cost=Decimal(idx),
            min_duration=Decimal(idx),
            image=f"image_link_{idx}",
            description=f"desc_{idx}",
            action_type=ClientAction.REQUEST,
            categories=["category1"],
        )
        for idx in (4, 8, 15, 16, 23, 42)
    ]


async def test_does_not_return_services_if_no_services(dm, factory):

    got = await dm.fetch_org_market_int_services(biz_id=54321)

    assert got == []


async def test_does_not_return_other_org_services(dm, factory):

    await factory.create_market_int_service(biz_id=100500)

    got = await dm.fetch_org_promoted_services(biz_id=15)

    assert got == []
