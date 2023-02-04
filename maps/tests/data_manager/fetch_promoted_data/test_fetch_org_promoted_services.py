from decimal import Decimal

import pytest

from maps_adv.geosmb.landlord.server.lib.enums import ServiceItemType

pytestmark = [pytest.mark.asyncio]


async def test_returns_promoted_services_for_permalink(dm, factory):
    await factory.create_promoted_service_list(services=[4, 8, 15, 16])
    await factory.create_promoted_service_list(services=[16, 23])
    await factory.create_promoted_service_list(services=[42])
    for idx in (4, 8, 15, 16, 23, 42):
        await factory.create_promoted_service(
            service_id=idx,
            title=f"title_{idx}",
            cost=str(idx),
            image=f"image_link_{idx}",
            url=f"https://url_{idx}.com",
            description=f"desc_{idx}",
        )

    got = await dm.fetch_org_promoted_services(biz_id=15)

    assert got == [
        dict(
            type=ServiceItemType.PROMOTED,
            id=idx,
            name=f"title_{idx}",
            cost=Decimal(idx),
            image=f"image_link_{idx}",
            url=f"https://url_{idx}.com",
            description=f"desc_{idx}",
        )
        for idx in (4, 8, 15, 16, 23, 42)
    ]


async def test_does_not_return_services_which_are_not_in_service_lists(dm, factory):
    await factory.create_promoted_service_list(services=[4, 8, 15])
    for idx in (4, 8, 15, 16, 23, 42):
        await factory.create_promoted_service(
            service_id=idx,
            title=f"title_{idx}",
            cost=str(idx),
            image=f"image_link_{idx}",
            url=f"https://url_{idx}.com",
            description=f"desc_{idx}",
        )

    got = await dm.fetch_org_promoted_services(biz_id=15)

    assert got == [
        dict(
            type=ServiceItemType.PROMOTED,
            id=idx,
            name=f"title_{idx}",
            cost=Decimal(idx),
            image=f"image_link_{idx}",
            url=f"https://url_{idx}.com",
            description=f"desc_{idx}",
        )
        for idx in (4, 8, 15)
    ]


async def test_does_not_return_services_if_no_service_lists(dm, factory):
    await factory.create_promoted_service(service_id=4362)

    got = await dm.fetch_org_promoted_services(biz_id=54321)

    assert got == []


async def test_does_not_return_services_if_no_services(dm, factory):
    await factory.create_promoted_service_list()

    got = await dm.fetch_org_promoted_services(biz_id=54321)

    assert got == []


@pytest.mark.parametrize("service_list_biz_id, service_biz_id", [(15, 51), (51, 15)])
async def test_does_not_return_other_org_services(
    service_list_biz_id, service_biz_id, dm, factory
):
    await factory.create_promoted_service_list(
        biz_id=service_list_biz_id, services=[100]
    )
    await factory.create_promoted_service(biz_id=service_biz_id, service_id=100)

    got = await dm.fetch_org_promoted_services(biz_id=15)

    assert got == []
