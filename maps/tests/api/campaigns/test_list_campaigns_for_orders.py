from decimal import Decimal

import pytest

from maps_adv.adv_store.api.proto.billing_pb2 import Money
from maps_adv.adv_store.api.proto.campaign_list_pb2 import CampaignList, CampaignListItem
from maps_adv.adv_store.api.proto.campaign_pb2 import PublicationEnv
from maps_adv.adv_store.api.proto.campaign_status_pb2 import CampaignStatus
from maps_adv.adv_store.api.proto.order_pb2 import OrdersInput
from maps_adv.adv_store.api.schemas.enums import (
    CampaignStatusEnum,
    FixTimeIntervalEnum,
    PlatformEnum,
    PublicationEnvEnum,
)
from maps_adv.adv_store.v2.tests import dt
from maps_adv.common.helpers.enums import CampaignTypeEnum

pytestmark = [pytest.mark.asyncio]

url = "/campaigns/list/"


async def test_returns_campaigns_for_all_requested_orders(factory, api):
    campaign1_id = (await factory.create_campaign(order_id=1111))["id"]
    campaign2_id = (await factory.create_campaign(order_id=1111))["id"]
    campaign3_id = (await factory.create_campaign(manul_order_id=2222))["id"]
    campaign4_id = (await factory.create_campaign(manul_order_id=4444))["id"]
    # noise
    await factory.create_campaign(manul_order_id=4040)
    await factory.create_campaign(order_id=1010)

    input_pb = OrdersInput(order_ids=[1111, 9999], manul_order_ids=[2222, 4444, 8888])

    got = await api.post(
        url, proto=input_pb, decode_as=CampaignList, expected_status=200
    )

    assert sorted([campaign.id for campaign in got.campaigns]) == sorted(
        [campaign1_id, campaign2_id, campaign3_id, campaign4_id]
    )


@pytest.mark.parametrize(
    "billing_extra, expected_budget_value",
    (
        (
            {
                "cpm": {
                    "cost": Decimal("10.1234"),
                    "budget": Decimal("500"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
            {"budget": Money(value=5000000)},
        ),
        (
            {
                "cpa": {
                    "cost": Decimal("10.1234"),
                    "budget": Decimal("300"),
                    "daily_budget": Decimal("1000"),
                    "auto_daily_budget": False,
                }
            },
            {"budget": Money(value=3000000)},
        ),
        (
            {
                "fix": {
                    "time_interval": FixTimeIntervalEnum.DAILY,
                    "cost": Decimal("10.1234"),
                }
            },
            {},
        ),
    ),
)
async def test_returns_data_for_all_campaign_budget_types(
    billing_extra, expected_budget_value, factory, api
):
    campaign_id = (await factory.create_campaign(order_id=4242, **billing_extra))["id"]

    input_pb = OrdersInput(order_ids=[4242])

    got = await api.post(
        url, proto=input_pb, decode_as=CampaignList, expected_status=200
    )

    assert got == CampaignList(
        campaigns=[
            CampaignListItem(
                id=campaign_id,
                name="campaign0",
                start_datetime=dt("2019-01-01 00:00:00", as_proto=True),
                end_datetime=dt("2019-02-01 00:00:00", as_proto=True),
                publication_envs=[PublicationEnv.Enum.Value("DATA_TESTING")],
                timezone="UTC",
                status=CampaignStatus.Enum.Value("DRAFT"),
                **expected_budget_value,
            )
        ]
    )


@pytest.mark.real_db
@pytest.mark.parametrize("orders_extra", [{"order_id": 4242}, {"manul_order_id": 5555}])
async def test_returns_last_status_for_campaigns_in_order(orders_extra, factory, api):
    campaign1_id = (await factory.create_campaign(**orders_extra))["id"]
    await factory.set_status(campaign_id=campaign1_id, status=CampaignStatusEnum.ACTIVE)
    await factory.set_status(campaign_id=campaign1_id, status=CampaignStatusEnum.DONE)

    input_pb = OrdersInput(order_ids=[4242], manul_order_ids=[5555])

    got = await api.post(
        url, proto=input_pb, decode_as=CampaignList, expected_status=200
    )

    assert got.campaigns[0].status == CampaignStatus.Enum.Value("DONE")


@pytest.mark.parametrize(
    "order_extra, input_pb",
    [
        ({"order_id": 4242}, OrdersInput(manul_order_ids=[4242])),
        ({"manul_order_id": 4242}, OrdersInput(order_ids=[4242])),
    ],
)
async def test_does_not_return_campaigns_of_another_order_type(
    order_extra, input_pb, factory, api
):
    await factory.create_campaign(**order_extra)

    got = await api.post(
        url, proto=input_pb, decode_as=CampaignList, expected_status=200
    )

    assert got == CampaignList(campaigns=[])


@pytest.mark.real_db
async def test_returns_campaigns_data(factory, api):
    campaign_creation_kwargs = dict(
        name="campaign_name",
        author_id=123,
        publication_envs=[PublicationEnvEnum.DATA_TESTING],
        campaign_type=CampaignTypeEnum.ZERO_SPEED_BANNER,
        start_datetime=dt("2019-03-01 00:00:00"),
        end_datetime=dt("2019-06-01 00:00:00"),
        timezone="UTC",
        platforms=[PlatformEnum.METRO],
        order_id=4242,
        cpa={
            "cost": Decimal("10.1234"),
            "budget": Decimal("500"),
            "daily_budget": Decimal("1000"),
            "auto_daily_budget": False,
        },
        datatesting_expires_at=dt("2019-04-01 00:00:00"),
    )
    campaign_id = (await factory.create_campaign(**campaign_creation_kwargs))["id"]
    await factory.set_status(
        campaign_id=campaign_id, status=CampaignStatusEnum.REJECTED
    )

    input_pb = OrdersInput(order_ids=[4242])

    got = await api.post(
        url, proto=input_pb, decode_as=CampaignList, expected_status=200
    )

    assert got == CampaignList(
        campaigns=[
            CampaignListItem(
                id=campaign_id,
                name="campaign_name",
                start_datetime=dt("2019-03-01 00:00:00", as_proto=True),
                end_datetime=dt("2019-06-01 00:00:00", as_proto=True),
                publication_envs=[PublicationEnv.Enum.Value("DATA_TESTING")],
                timezone="UTC",
                status=CampaignStatus.Enum.Value("REJECTED"),
                budget=Money(value=5000000),
                datatesting_expires_at=dt("2019-04-01 00:00:00", as_proto=True),
            )
        ]
    )
