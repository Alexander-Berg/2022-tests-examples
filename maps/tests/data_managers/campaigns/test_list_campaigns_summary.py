import pytest

from maps_adv.adv_store.api.schemas.enums import (
    CampaignStatusEnum,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.real_db]


@pytest.mark.parametrize(
    "campaigns_kwargs",
    [
        [],
        [{"order_id": 1}],
        [{"manul_order_id": 3}],
        [{"order_id": 1}, {"order_id": 2}, {"manul_order_id": 2}],
        [
            {"order_id": 1},
            {"order_id": 2},
            {"manul_order_id": 2},
            {"manul_order_id": 3},
        ],
    ],
)
async def test_returns_all_order_ids_and_manul_order_ids(
    factory, campaigns_dm, campaigns_kwargs
):
    for campaign_kwargs in campaigns_kwargs:
        await factory.create_campaign(**campaign_kwargs)

    got = await campaigns_dm.list_campaigns_summary(
        order_ids=[1, 2], manul_order_ids=[2, 3]
    )

    assert sorted(filter(None, map(lambda v: v.get("order_id"), got))) == [1, 2]
    assert sorted(filter(None, map(lambda v: v.get("manul_order_id"), got))) == [2, 3]


@pytest.mark.parametrize(
    ("order_type", "dm_arg"),
    [("order_id", "order_ids"), ("manul_order_id", "manul_order_ids")],
)
async def test_returns_aggregates_for_order(factory, campaigns_dm, order_type, dm_arg):
    await factory.create_campaign(status=CampaignStatusEnum.ACTIVE, **{order_type: 1})
    campaign2_id = (
        await factory.create_campaign(
            status=CampaignStatusEnum.ACTIVE, **{order_type: 1}
        )
    )["id"]
    await factory.set_status(campaign2_id, status=CampaignStatusEnum.DONE)
    await factory.create_campaign(status=CampaignStatusEnum.DRAFT, **{order_type: 1})

    dm_kwargs = {"order_ids": [], "manul_order_ids": []}
    dm_kwargs[dm_arg] = [1]
    got = await campaigns_dm.list_campaigns_summary(**dm_kwargs)

    expected = [{"active": 1, "total": 3}]
    expected[0][order_type] = 1
    assert got == expected


async def test_returns_zeros_if_no_campaigns_in_order(campaigns_dm):
    got = await campaigns_dm.list_campaigns_summary(order_ids=[1], manul_order_ids=[2])

    assert got == [
        {"order_id": 1, "active": 0, "total": 0},
        {"manul_order_id": 2, "active": 0, "total": 0},
    ]


@pytest.mark.parametrize(
    ["reason_stopped", "expected_active"],
    [
        ("", 0),
    ],
)
async def test_returns_campaigns_as_active_if_them_have_technical_pause_status(
    factory, campaigns_dm, reason_stopped: str, expected_active: int
):
    campaign = await factory.create_campaign(order_id=1)
    await factory.set_status(
        campaign["id"],
        status=CampaignStatusEnum.PAUSED,
        metadata={"reason_stopped": reason_stopped},
    )

    got = await campaigns_dm.list_campaigns_summary(order_ids=[1], manul_order_ids=[])

    assert got == [{"order_id": 1, "active": expected_active, "total": 1}]
