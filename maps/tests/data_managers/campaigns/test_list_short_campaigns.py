import pytest

pytestmark = [pytest.mark.asyncio]


@pytest.mark.real_db
async def test_returns_expected_campaigns(factory, campaigns_dm):
    campaign_1 = await factory.create_campaign(order_id=1111)
    campaign_2 = await factory.create_campaign(order_id=2222)
    campaign_3 = await factory.create_campaign(manul_order_id=3333)
    campaign_4 = await factory.create_campaign(manul_order_id=4444)
    await factory.create_campaign(order_id=8888)
    await factory.create_campaign(manul_order_id=9999)

    got = await campaigns_dm.list_short_campaigns(
        order_ids=[1111, 2222], manul_order_ids=[3333, 4444]
    )

    assert [item["id"] for item in got] == [
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
    factory,
    campaigns_dm,
    creation_data: dict,
    input_data: dict,
    expected_order_data: dict,
):
    campaign_1 = await factory.create_campaign(**creation_data)

    got = await campaigns_dm.list_short_campaigns(**input_data)

    expected_result = [
        dict(id=campaign_1["id"], name="campaign0", **expected_order_data)
    ]
    assert got == expected_result


@pytest.mark.parametrize(
    "input_data",
    (
        {"order_ids": [1111], "manul_order_ids": [2222]},
        {"order_ids": [1111], "manul_order_ids": []},
        {"order_ids": [], "manul_order_ids": [1111]},
        {"order_ids": [], "manul_order_ids": []},
    ),
)
async def test_returns_nothing_if_no_campaigns(campaigns_dm, input_data):
    got = await campaigns_dm.list_short_campaigns(**input_data)

    assert got == []


@pytest.mark.parametrize(
    "input_data",
    (
        {"order_ids": [9999], "manul_order_ids": [8888]},
        {"order_ids": [9999], "manul_order_ids": []},
        {"order_ids": [], "manul_order_ids": [9999]},
        {"order_ids": [], "manul_order_ids": []},
    ),
)
async def test_returns_nothing_for_unknown_orders(factory, campaigns_dm, input_data):
    await factory.create_campaign(order_id=1)
    await factory.create_campaign(manul_order_id=2)

    got = await campaigns_dm.list_short_campaigns(**input_data)

    assert got == []
