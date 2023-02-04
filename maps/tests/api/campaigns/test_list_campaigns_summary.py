from operator import attrgetter

import pytest

from maps_adv.adv_store.api.proto import order_pb2
from maps_adv.adv_store.api.schemas.enums import CampaignStatusEnum

pytestmark = [pytest.mark.asyncio, pytest.mark.real_db]


url = "/orders/campaigns-summary/"


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
    factory, api, campaigns_kwargs
):
    for campaign_kwargs in campaigns_kwargs:
        await factory.create_campaign(**campaign_kwargs)

    input_pb = order_pb2.OrdersInput(order_ids=[1, 2], manul_order_ids=[2, 3])
    got = await api.post(
        url, proto=input_pb, decode_as=order_pb2.OrderSummaryList, expected_status=200
    )

    assert sorted(filter(None, map(attrgetter("order_id"), got.orders))) == [1, 2]
    assert sorted(filter(None, map(attrgetter("manul_order_id"), got.orders))) == [2, 3]


async def test_returns_zeros_if_no_campaigns_in_order(api):
    input_pb = order_pb2.OrdersInput(order_ids=[1], manul_order_ids=[2])
    got = await api.post(
        url, proto=input_pb, decode_as=order_pb2.OrderSummaryList, expected_status=200
    )

    assert got == order_pb2.OrderSummaryList(
        orders=[
            order_pb2.OrderSummary(order_id=1, active=0, total=0),
            order_pb2.OrderSummary(manul_order_id=2, active=0, total=0),
        ]
    )


async def test_returns_campaigns_summary(factory, api):
    for _ in range(2):
        await factory.create_campaign(order_id=1, status=CampaignStatusEnum.ACTIVE)
    for _ in range(3):
        await factory.create_campaign(order_id=1)

    for _ in range(1):
        await factory.create_campaign(
            manul_order_id=2, status=CampaignStatusEnum.ACTIVE
        )
    for _ in range(2):
        await factory.create_campaign(manul_order_id=2)

    input_pb = order_pb2.OrdersInput(order_ids=[1, 2], manul_order_ids=[2])
    got = await api.post(
        url, proto=input_pb, decode_as=order_pb2.OrderSummaryList, expected_status=200
    )

    assert got == order_pb2.OrderSummaryList(
        orders=[
            order_pb2.OrderSummary(order_id=1, active=2, total=5),
            order_pb2.OrderSummary(manul_order_id=2, active=1, total=3),
            order_pb2.OrderSummary(order_id=2, active=0, total=0),
        ]
    )
