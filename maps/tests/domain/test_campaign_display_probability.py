from datetime import datetime
from decimal import Decimal

import pytest
from sqlalchemy.sql import select

from maps_adv.adv_store.lib.api_provider import CampaignStatusEnum
from maps_adv.adv_store.lib.data_managers.create_campaign import create_campaign
from maps_adv.adv_store.lib.domain.campaign import refresh_campaigns_display_probability
from maps_adv.adv_store.v2.lib.db.tables import campaign as table

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
async def test_success_update_display_probability_for_campaign(
    current_display_probability,
    new_display_probability,
    mock_yql_table_read_iterator,
    db,
    client,
    faker,
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

    mock_yql_table_read_iterator.return_value = iter(
        [(campaign["id"], str(new_display_probability))]
    )

    await refresh_campaigns_display_probability()

    result = await db.rw.fetch_val(
        select([table.columns.display_probability]).where(
            table.columns.id == campaign["id"]
        )
    )

    assert result == new_display_probability


async def test_updated_campaigns_with_values_in_external_table(
    mock_yql_table_read_iterator, db, client, faker
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
            "display_probability": None,
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
            "display_probability": None,
        }
    )

    campaign3 = await create_campaign(
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
            "display_probability": None,
        }
    )

    mock_yql_table_read_iterator.return_value = iter(
        [(campaign1["id"], "1.0"), (campaign2["id"], "0.000001")]
    )

    await refresh_campaigns_display_probability()

    results = list(
        map(
            lambda item: item[0],
            await db.rw.fetch_all(
                select([table.columns.display_probability])
                .where(
                    table.columns.id.in_(
                        [campaign1["id"], campaign2["id"], campaign3["id"]]
                    )
                )
                .order_by(table.columns.id)
            ),
        )
    )

    assert results == [Decimal("1"), Decimal("0.000001"), None]
