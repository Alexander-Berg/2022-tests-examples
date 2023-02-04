from datetime import datetime, timedelta
from decimal import Decimal

import pytest

from maps_adv.adv_store.api.proto.billing_pb2 import Money
from maps_adv.adv_store.api.proto.campaign_list_pb2 import (
    CampaignBudgetAnalysisData,
    CampaignBudgetAnalysisList,
)
from maps_adv.adv_store.api.schemas.enums import (
    CampaignStatusEnum,
    FixTimeIntervalEnum,
    PublicationEnvEnum,
)
from maps_adv.adv_store.v2.tests import dt

pytestmark = [pytest.mark.asyncio]

_cpm = {
    "cost": Decimal("50"),
    "budget": Decimal("66000"),
    "daily_budget": Decimal("5000"),
    "auto_daily_budget": False,
}

API_URL = "/campaigns/budget-analysis/"


async def test_campaign_returned_if_active(api, factory):
    campaign_id = (
        await factory.create_campaign(
            cpm=_cpm,
            start_datetime=dt("2019-01-01 00:00:00"),
            end_datetime=datetime.now() + timedelta(days=5),
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(
        campaign_id,
        status=CampaignStatusEnum.ACTIVE,
        changed_datetime=dt("2019-10-10 00:00:00"),
    )

    got = await api.get(
        API_URL, decode_as=CampaignBudgetAnalysisList, expected_status=200
    )

    assert got == CampaignBudgetAnalysisList(
        campaigns=[
            CampaignBudgetAnalysisData(
                id=campaign_id,
                budget=Money(value=660000000),
                daily_budget=Money(value=50000000),
                days_left=5,
            )
        ]
    )


@pytest.mark.parametrize("reason_stopped", ["BUDGET_REACHED", "END_DATETIME"])
async def test_campaign_not_returned_if_paused_other_reason(
    factory, api, reason_stopped
):
    campaign_id = (
        await factory.create_campaign(
            cpm=_cpm,
            start_datetime=dt("2019-01-01 00:00:00"),
            end_datetime=datetime.now() + timedelta(days=5),
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(
        campaign_id,
        status=CampaignStatusEnum.PAUSED,
        changed_datetime=dt("2019-10-10 00:00:00"),
        metadata={"reason_stopped": reason_stopped},
    )

    got = await api.get(
        API_URL, decode_as=CampaignBudgetAnalysisList, expected_status=200
    )

    assert got == CampaignBudgetAnalysisList()


@pytest.mark.parametrize(
    "current_status",
    [
        CampaignStatusEnum.DRAFT,
        CampaignStatusEnum.REVIEW,
        CampaignStatusEnum.REJECTED,
        CampaignStatusEnum.DONE,
        CampaignStatusEnum.ARCHIVED,
    ],
)
async def test_campaign_not_returned_because_of_status(api, factory, current_status):
    campaign_id = (
        await factory.create_campaign(
            cpm=_cpm,
            start_datetime=dt("2019-01-01 00:00:00"),
            end_datetime=datetime.now() + timedelta(days=5),
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(
        campaign_id, status=current_status, changed_datetime=dt("2019-10-10 00:00:00")
    )

    got = await api.get(
        API_URL, decode_as=CampaignBudgetAnalysisList, expected_status=200
    )

    assert got == CampaignBudgetAnalysisList()


@pytest.mark.parametrize(
    ("start_datetime", "end_datetime"),
    [
        (dt("2019-09-01 00:00:00"), datetime.now() - timedelta(days=5)),
        (datetime.now() + timedelta(days=3), datetime.now() + timedelta(days=5)),
    ],
)
async def test_campaign_not_returned_if_not_active_by_dates(
    api, factory, start_datetime, end_datetime
):
    campaign_id = (
        await factory.create_campaign(
            cpm=_cpm,
            start_datetime=start_datetime,
            end_datetime=end_datetime,
            publication_envs=[PublicationEnvEnum.PRODUCTION],
        )
    )["id"]
    await factory.set_status(
        campaign_id,
        status=CampaignStatusEnum.ACTIVE,
        changed_datetime=dt("2019-10-09 00:00:00"),
    )

    got = await api.get(
        API_URL, decode_as=CampaignBudgetAnalysisList, expected_status=200
    )

    assert got == CampaignBudgetAnalysisList()


@pytest.mark.parametrize(
    "billing_kwargs",
    [
        {
            "cpa": {
                "cost": Decimal("55.66"),
                "budget": Decimal("66000"),
                "daily_budget": Decimal("5000"),
                "auto_daily_budget": False,
            }
        },
        {"fix": {"cost": Decimal("55.66"), "time_interval": FixTimeIntervalEnum.DAILY}},
    ],
)
async def test_not_cpm_campaigns_not_returned(api, factory, billing_kwargs):
    await factory.create_campaign(
        start_datetime=dt("2019-01-01 00:00:00"),
        end_datetime=datetime.now() + timedelta(days=5),
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        **billing_kwargs,
    )

    got = await api.get(
        API_URL, decode_as=CampaignBudgetAnalysisList, expected_status=200
    )

    assert got == CampaignBudgetAnalysisList()


@pytest.mark.parametrize(
    "publication_envs",
    [
        [PublicationEnvEnum.PRODUCTION],
        [PublicationEnvEnum.PRODUCTION, PublicationEnvEnum.DATA_TESTING],
    ],
)
async def test_production_campaign_returned(api, factory, publication_envs):
    campaign_id = (
        await factory.create_campaign(
            cpm=_cpm,
            start_datetime=dt("2019-01-01 00:00:00"),
            end_datetime=datetime.now() + timedelta(days=5),
            publication_envs=publication_envs,
        )
    )["id"]
    await factory.set_status(
        campaign_id,
        status=CampaignStatusEnum.ACTIVE,
        changed_datetime=dt("2019-10-10 00:00:00"),
    )

    got = await api.get(
        API_URL, decode_as=CampaignBudgetAnalysisList, expected_status=200
    )

    assert len(got.campaigns) == 1


async def test_datatesting_campaign_not_returned(api, factory):
    campaign_id = (
        await factory.create_campaign(
            cpm=_cpm,
            start_datetime=dt("2019-01-01 00:00:00"),
            end_datetime=datetime.now() + timedelta(days=5),
            publication_envs=[PublicationEnvEnum.DATA_TESTING],
        )
    )["id"]
    await factory.set_status(
        campaign_id,
        status=CampaignStatusEnum.ACTIVE,
        changed_datetime=dt("2019-10-10 00:00:00"),
    )

    got = await api.get(
        API_URL, decode_as=CampaignBudgetAnalysisList, expected_status=200
    )

    assert len(got.campaigns) == 0


@pytest.mark.parametrize(
    "billing_kwargs",
    [
        {
            "cpm": {
                "cost": Decimal("50"),
                "budget": None,
                "daily_budget": Decimal("5000"),
                "auto_daily_budget": False,
            }
        },
        {
            "cpm": {
                "cost": Decimal("50"),
                "budget": Decimal("600000"),
                "daily_budget": None,
                "auto_daily_budget": False,
            }
        },
    ],
)
async def test_campaign_not_returned_if_no_budget(factory, api, billing_kwargs):
    campaign_id = (
        await factory.create_campaign(
            start_datetime=dt("2019-01-01 00:00:00"),
            end_datetime=datetime.now() + timedelta(days=5),
            publication_envs=[PublicationEnvEnum.PRODUCTION],
            **billing_kwargs,
        )
    )["id"]
    await factory.set_status(
        campaign_id,
        status=CampaignStatusEnum.ACTIVE,
        changed_datetime=dt("2019-10-10 00:00:00"),
    )

    got = await api.get(
        API_URL, decode_as=CampaignBudgetAnalysisList, expected_status=200
    )

    assert len(got.campaigns) == 0
