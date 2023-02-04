from datetime import datetime

import pytest

from maps_adv.adv_store.api.proto.charger_api_pb2 import (
    CampaignsToStopList,
    CampaignToStop,
    ReasonStopped,
)
from maps_adv.adv_store.api.proto.error_pb2 import Error
from maps_adv.adv_store.api.schemas.enums import (
    CampaignStatusEnum,
    ReasonCampaignStoppedEnum,
)
from maps_adv.adv_store.v2.tests import Any, dt

pytestmark = [pytest.mark.asyncio]

url = "/campaigns/stop/"


async def test_returns_200_if_nothing_to_stop(api):
    await api.put(
        url,
        proto=CampaignsToStopList(
            processed_at=dt("2020-02-20 11:00:00", as_proto=True), campaigns=[]
        ),
        expected_status=200,
    )


async def test_returns_200(api, factory):
    campaign_id1 = (await factory.create_campaign())["id"]
    campaign_id2 = (await factory.create_campaign())["id"]
    await factory.create_campaign()

    await api.put(
        url,
        proto=CampaignsToStopList(
            processed_at=dt("2020-02-20 11:00:00", as_proto=True),
            campaigns=[
                CampaignToStop(
                    campaign_id=campaign_id1,
                    reason_stopped=ReasonStopped.BUDGET_REACHED,
                ),
                CampaignToStop(
                    campaign_id=campaign_id2,
                    reason_stopped=ReasonStopped.DAILY_BUDGET_REACHED,
                ),
            ],
        ),
        expected_status=200,
    )


async def test_returns_nothing(api, factory):
    campaign_id = (await factory.create_campaign())["id"]

    got = await api.put(
        url,
        proto=CampaignsToStopList(
            processed_at=dt("2020-02-20 11:00:00", as_proto=True),
            campaigns=[
                CampaignToStop(
                    campaign_id=campaign_id, reason_stopped=ReasonStopped.BUDGET_REACHED
                )
            ],
        ),
        expected_status=200,
    )

    assert got == b""


async def test_stops_only_listed_campaigns(api, factory):
    campaign_id1 = (await factory.create_campaign(status=CampaignStatusEnum.ACTIVE))[
        "id"
    ]
    campaign_id2 = (await factory.create_campaign(status=CampaignStatusEnum.ACTIVE))[
        "id"
    ]
    campaign_id3 = (await factory.create_campaign(status=CampaignStatusEnum.ACTIVE))[
        "id"
    ]

    await api.put(
        url,
        proto=CampaignsToStopList(
            processed_at=dt("2020-02-20 11:00:00", as_proto=True),
            campaigns=[
                CampaignToStop(
                    campaign_id=campaign_id1,
                    reason_stopped=ReasonStopped.ORDER_LIMIT_REACHED,
                ),
                CampaignToStop(
                    campaign_id=campaign_id2,
                    reason_stopped=ReasonStopped.BUDGET_REACHED,
                ),
            ],
        ),
        expected_status=200,
    )

    campaign1_status = (await factory.fetch_last_campaign_status_data(campaign_id1))[
        "status"
    ]
    campaign2_status = (await factory.fetch_last_campaign_status_data(campaign_id2))[
        "status"
    ]
    campaign3_status = (await factory.fetch_last_campaign_status_data(campaign_id3))[
        "status"
    ]

    assert campaign1_status == CampaignStatusEnum.ACTIVE.name
    assert campaign2_status == CampaignStatusEnum.DONE.name
    assert campaign3_status == CampaignStatusEnum.ACTIVE.name


@pytest.mark.parametrize(
    "reason_stopped_proto, reason_stopped_enum, expected_status_enum",
    [
        (
            ReasonStopped.BUDGET_REACHED,
            ReasonCampaignStoppedEnum.BUDGET_REACHED,
            CampaignStatusEnum.DONE,
        ),
        (
            ReasonStopped.DAILY_BUDGET_REACHED,
            ReasonCampaignStoppedEnum.DAILY_BUDGET_REACHED,
            CampaignStatusEnum.ACTIVE,
        ),
        (
            ReasonStopped.ORDER_LIMIT_REACHED,
            ReasonCampaignStoppedEnum.ORDER_LIMIT_REACHED,
            CampaignStatusEnum.ACTIVE,
        ),
    ],
)
async def test_updates_campaign_status_history(
    reason_stopped_proto, reason_stopped_enum, expected_status_enum, api, factory
):
    campaign_id = (await factory.create_campaign(status=CampaignStatusEnum.ACTIVE))[
        "id"
    ]

    await api.put(
        url,
        proto=CampaignsToStopList(
            processed_at=dt("2020-02-20 11:00:00", as_proto=True),
            campaigns=[
                CampaignToStop(
                    campaign_id=campaign_id, reason_stopped=reason_stopped_proto
                )
            ],
        ),
        expected_status=200,
    )

    campaign_status_data = await factory.fetch_last_campaign_status_data(campaign_id)

    assert campaign_status_data == dict(
        campaign_id=campaign_id,
        author_id=0,
        status=expected_status_enum.name,
        metadata={
            "processed_at": 1582196400,
            "reason_stopped": reason_stopped_enum.name,
        },
        changed_datetime=Any(datetime),
    )


async def test_returns_404_if_one_of_campaigns_not_found(api, factory):
    campaign_id = (await factory.create_campaign(status=CampaignStatusEnum.ACTIVE))[
        "id"
    ]

    got = await api.put(
        url,
        proto=CampaignsToStopList(
            processed_at=dt("2020-02-20 11:00:00", as_proto=True),
            campaigns=[
                CampaignToStop(
                    campaign_id=campaign_id,
                    reason_stopped=ReasonStopped.ORDER_LIMIT_REACHED,
                ),
                CampaignToStop(
                    campaign_id=1234, reason_stopped=ReasonStopped.ORDER_LIMIT_REACHED
                ),
                CampaignToStop(
                    campaign_id=4321, reason_stopped=ReasonStopped.ORDER_LIMIT_REACHED
                ),
            ],
        ),
        decode_as=Error,
        expected_status=404,
    )

    assert got == Error(code=Error.CAMPAIGN_NOT_FOUND, description="1234, 4321")
