import pytest

from maps_adv.adv_store.api.proto import campaign_list_pb2, order_pb2

pytestmark = [pytest.mark.asyncio]


url = "/campaigns/list-short/"


@pytest.mark.real_db
async def test_returns_expected_campaigns(factory, api):
    campaign_1 = await factory.create_campaign(order_id=1111)
    campaign_2 = await factory.create_campaign(order_id=2222)
    campaign_3 = await factory.create_campaign(manul_order_id=3333)
    campaign_4 = await factory.create_campaign(manul_order_id=4444)
    # noise
    await factory.create_campaign(order_id=8888)
    await factory.create_campaign(manul_order_id=9999)

    input_pb = order_pb2.OrdersInput(
        order_ids=[1111, 2222], manul_order_ids=[3333, 4444]
    )

    got = await api.post(
        url,
        proto=input_pb,
        decode_as=campaign_list_pb2.ShortCampaignList,
        expected_status=200,
    )

    assert [campaign.id for campaign in got.campaigns] == [
        campaign_4["id"],
        campaign_3["id"],
        campaign_2["id"],
        campaign_1["id"],
    ]


@pytest.mark.parametrize(
    "creation_data, input_data, expected_order_data",
    (
        [
            dict(order_id=1111),
            dict(order_ids=[1111], manul_order_ids=[]),
            dict(order_id=1111),
        ],
        [
            dict(manul_order_id=1111),
            dict(order_ids=[], manul_order_ids=[1111]),
            dict(manul_order_id=1111),
        ],
    ),
)
async def test_returns_campaigns_in_expected_format(
    factory, api, creation_data: dict, input_data: dict, expected_order_data: dict
):
    campaign_1 = await factory.create_campaign(**creation_data)

    input_pb = order_pb2.OrdersInput(**input_data)

    got = await api.post(
        url,
        proto=input_pb,
        decode_as=campaign_list_pb2.ShortCampaignList,
        expected_status=200,
    )

    assert got == campaign_list_pb2.ShortCampaignList(
        campaigns=[
            campaign_list_pb2.ShortCampaign(
                id=campaign_1["id"], name="campaign0", **expected_order_data
            )
        ]
    )


@pytest.mark.parametrize(
    "input_data",
    (
        {"order_ids": [1111], "manul_order_ids": [2222]},
        {"order_ids": [1111], "manul_order_ids": []},
        {"order_ids": [], "manul_order_ids": [1111]},
        {"order_ids": [], "manul_order_ids": []},
    ),
)
async def test_returns_empty_proto_if_no_campaigns(api, input_data):
    input_pb = order_pb2.OrdersInput(**input_data)

    got = await api.post(
        url,
        proto=input_pb,
        decode_as=campaign_list_pb2.ShortCampaignList,
        expected_status=200,
    )

    assert got == campaign_list_pb2.ShortCampaignList(campaigns=[])


@pytest.mark.parametrize(
    "input_data",
    (
        {"order_ids": [9999], "manul_order_ids": [8888]},
        {"order_ids": [9999], "manul_order_ids": []},
        {"order_ids": [], "manul_order_ids": [9999]},
        {"order_ids": [], "manul_order_ids": []},
    ),
)
async def test_returns_empty_proto_for_unknown_orders(factory, api, input_data):
    await factory.create_campaign(order_id=1)
    await factory.create_campaign(manul_order_id=2)

    input_pb = order_pb2.OrdersInput(**input_data)

    got = await api.post(
        url,
        proto=input_pb,
        decode_as=campaign_list_pb2.ShortCampaignList,
        expected_status=200,
    )

    assert got == campaign_list_pb2.ShortCampaignList(campaigns=[])
