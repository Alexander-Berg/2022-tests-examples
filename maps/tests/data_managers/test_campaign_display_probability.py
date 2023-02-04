from datetime import datetime
from decimal import Decimal

import pytest
from sqlalchemy.sql import select

from maps_adv.adv_store.lib.api_provider import CampaignStatusEnum
from maps_adv.adv_store.lib.data_managers.campaign_display_probability import (
    retrieve_campaigns_display_probability,
    update_campaign_display_probability,
)
from maps_adv.adv_store.lib.data_managers.create_campaign import create_campaign
from maps_adv.adv_store.v2.lib.db.tables import campaign as table
from maps_adv.common.helpers import Any

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def mock_yql_table_read_iterator(mocker):
    mocker.patch(
        "yql.api.v1.client.YqlTableReadIterator.__init__",
        new_callable=lambda: lambda _, table_name, cluster, column_names: None,
    )
    return mocker.patch("yql.api.v1.client.YqlTableReadIterator.__iter__")


@pytest.mark.parametrize(
    ["current_display_probability", "new_display_probability"],
    [
        (None, Decimal("1")),
        (None, Decimal("0.000001")),
        (Decimal("1.000000"), Decimal("0.000002")),
    ],
)
async def test_success_update_display_probability(
    current_display_probability, new_display_probability, db, client, faker
):
    campaign = await create_campaign(
        {
            "billing": {"cpm": {"cost": 1, "budget": 20, "daily_budget": None}},
            "creatives": [],
            "week_schedule": [],
            "actions": [],
            "placing": {"area": {"areas": [], "version": 1}},
            "author_id": 1,
            "name": "campaign name",
            "publication_envs": ["PRODUCTION"],
            "campaign_type": "ZERO_SPEED_BANNER",
            "status": CampaignStatusEnum.ACTIVE,
            "start_datetime": datetime.now(),
            "end_datetime": datetime.now(),
            "timezone": "Europe/Moscow",
            "platforms": ["MAPS"],
            "order_id": None,
            "comment": "Comment not found",
            "user_display_limit": None,
            "user_daily_display_limit": None,
            "targeting": {},
            "rubric": None,
            "display_probability": current_display_probability,
        }
    )

    await update_campaign_display_probability(campaign["id"], new_display_probability)

    result = await db.rw.fetch_val(
        select([table.columns.display_probability]).where(
            table.columns.id == campaign["id"]
        )
    )

    assert result == new_display_probability


@pytest.mark.parametrize(
    ["current_display_probability", "new_display_probability"],
    [
        (None, Decimal("1")),
        (None, Decimal("0.000001")),
        (Decimal("1.000000"), Decimal("0.000002")),
    ],
)
async def test_success_update_only_one_campaign_display_probability(
    current_display_probability, new_display_probability, db, client, faker
):
    campaign1 = await create_campaign(
        {
            "billing": {"cpm": {"cost": 1, "budget": 20, "daily_budget": None}},
            "creatives": [],
            "week_schedule": [],
            "actions": [],
            "placing": {"area": {"areas": [], "version": 1}},
            "author_id": 1,
            "name": "campaign name",
            "publication_envs": ["PRODUCTION"],
            "campaign_type": "ZERO_SPEED_BANNER",
            "status": CampaignStatusEnum.ACTIVE,
            "start_datetime": datetime.now(),
            "end_datetime": datetime.now(),
            "timezone": "Europe/Moscow",
            "platforms": ["MAPS"],
            "order_id": None,
            "comment": "Comment not found",
            "user_display_limit": None,
            "user_daily_display_limit": None,
            "targeting": {},
            "rubric": None,
            "display_probability": current_display_probability,
        }
    )
    campaign2 = await create_campaign(
        {
            "billing": {"cpm": {"cost": 1, "budget": 20, "daily_budget": None}},
            "creatives": [],
            "week_schedule": [],
            "actions": [],
            "placing": {"area": {"areas": [], "version": 1}},
            "author_id": 1,
            "name": "campaign name",
            "publication_envs": ["PRODUCTION"],
            "campaign_type": "ZERO_SPEED_BANNER",
            "status": CampaignStatusEnum.ACTIVE,
            "start_datetime": datetime.now(),
            "end_datetime": datetime.now(),
            "timezone": "Europe/Moscow",
            "platforms": ["MAPS"],
            "order_id": None,
            "comment": "Comment not found",
            "user_display_limit": None,
            "user_daily_display_limit": None,
            "targeting": {},
            "rubric": None,
            "display_probability": current_display_probability,
        }
    )

    await update_campaign_display_probability(campaign1["id"], new_display_probability)

    result = await db.rw.fetch_val(
        select([table.columns.display_probability]).where(
            table.columns.id == campaign2["id"]
        )
    )

    assert result == current_display_probability


async def test_returns_retrieve_display_probability_for_campaigns_as_expected(
    mock_yql_table_read_iterator
):
    mock_yql_table_read_iterator.return_value = iter(
        [(1, "1.0"), (2, "0.000001"), (3, "0")]
    )

    result = await retrieve_campaigns_display_probability()

    assert result == {1: Decimal("1"), 2: Decimal("0.000001"), 3: Decimal("0")}


async def test_raises_for_none_value_in_field_new_probability(
    mock_yql_table_read_iterator
):
    mock_yql_table_read_iterator.return_value = iter([(1, None)])
    with pytest.raises(TypeError):
        await retrieve_campaigns_display_probability()


async def test_adds_change_log_record_for_updated_campaign(
    factory, mock_yql_table_read_iterator
):
    mock_yql_table_read_iterator.return_value = iter([(1, "1.0")])
    campaign1 = await factory.create_campaign_with_any_status(
        author_id=123,
        status=CampaignStatusEnum.ACTIVE,
        display_probability=Decimal("0"),
    )
    campaign2 = await factory.create_campaign_with_any_status(
        author_id=123,
        status=CampaignStatusEnum.ACTIVE,
        display_probability=Decimal("0"),
    )

    await update_campaign_display_probability(campaign1["id"], Decimal("0.000002"))

    result1 = await factory.list_campaign_change_log(campaign_id=campaign1["id"])
    result2 = await factory.list_campaign_change_log(campaign_id=campaign2["id"])

    assert result1 == [
        {
            "id": Any(int),
            "created_at": Any(datetime),
            "campaign_id": campaign1["id"],
            "author_id": 0,
            "status": "ACTIVE",
            "system_metadata": {"action": "campaign.refresh_display_probability"},
            "state_before": Any(dict),
            "state_after": Any(dict),
            "is_latest": True,
        }
    ]
    assert result2 == []
