from datetime import datetime, timedelta, timezone

import pytest

from maps_adv.adv_store.lib.data_managers.exceptions import CampaignNotFound
from maps_adv.adv_store.lib.data_managers.status_history import (
    append_status_history_entry,
)
from maps_adv.adv_store.tests.utils import Any
from maps_adv.adv_store.api.schemas.enums import CampaignStatusEnum

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("status", list(CampaignStatusEnum))
async def test_appends_entry(factory, status):
    campaign = await factory.create_campaign_with_any_status()

    await append_status_history_entry(
        campaign_id=campaign["id"],
        status=status,
        author_id=22,
        metadata={"some": "metadata"},
    )

    status_history = await factory.get_all_status_history_for_campaign(campaign["id"])
    assert status_history[1:] == [
        {
            "campaign_id": campaign["id"],
            "author_id": 22,
            "status": status,
            "metadata": {"some": "metadata"},
            "changed_datetime": Any(datetime),
        }
    ]


@pytest.mark.parametrize("status", list(CampaignStatusEnum))
async def test_not_removes_other_entries(faker, factory, status):
    past_dt = faker.past_datetime(tzinfo=timezone.utc)
    campaign = await factory.create_campaign()
    await factory.create_status_entry(
        campaign_id=campaign["id"],
        author_id=33,
        status=CampaignStatusEnum.DRAFT,
        metadata={"some1": "metadata1"},
        changed_datetime=past_dt - timedelta(days=2),
    )
    await factory.create_status_entry(
        campaign_id=campaign["id"],
        author_id=44,
        status=CampaignStatusEnum.ACTIVE,
        metadata={"some2": "metadata2"},
        changed_datetime=past_dt - timedelta(days=1),
    )

    await append_status_history_entry(
        campaign_id=campaign["id"],
        status=status,
        author_id=22,
        metadata={"some": "metadata"},
    )

    status_history = await factory.get_all_status_history_for_campaign(campaign["id"])
    assert status_history == [
        {
            "campaign_id": campaign["id"],
            "author_id": 33,
            "status": CampaignStatusEnum.DRAFT,
            "metadata": {"some1": "metadata1"},
            "changed_datetime": past_dt - timedelta(days=2),
        },
        {
            "campaign_id": campaign["id"],
            "author_id": 44,
            "status": CampaignStatusEnum.ACTIVE,
            "metadata": {"some2": "metadata2"},
            "changed_datetime": past_dt - timedelta(days=1),
        },
        {
            "campaign_id": campaign["id"],
            "author_id": 22,
            "status": status,
            "metadata": {"some": "metadata"},
            "changed_datetime": Any(datetime),
        },
    ]


@pytest.mark.parametrize("status", list(CampaignStatusEnum))
async def test_raises_for_inexistent_campaign(status):
    with pytest.raises(CampaignNotFound):
        await append_status_history_entry(
            campaign_id=987, status=status, author_id=22, metadata={"some": "metadata"}
        )


@pytest.mark.parametrize("status", list(CampaignStatusEnum))
async def test_metadata_is_optional(factory, status):
    campaign = await factory.create_campaign_with_any_status()

    await append_status_history_entry(
        campaign_id=campaign["id"], status=status, author_id=22
    )

    status_history = await factory.get_all_status_history_for_campaign(campaign["id"])
    assert status_history[1:] == [
        {
            "campaign_id": campaign["id"],
            "author_id": 22,
            "status": status,
            "metadata": {},
            "changed_datetime": Any(datetime),
        }
    ]


@pytest.mark.parametrize(
    ["params", "expected_system_metadata"],
    [
        ({}, {"action": "campaign.change_status"}),
        ({"change_log_action_name": "lololo"}, {"action": "lololo"}),
    ],
)
async def test_adds_change_log_record_for_changing_status(
    params, expected_system_metadata, factory
):
    campaign_id = (await factory.create_campaign())["id"]
    await factory.create_status_history(
        campaign_id=campaign_id,
        status=CampaignStatusEnum.DRAFT,
        changed_datetime=datetime(2019, 9, 9),
    )

    await append_status_history_entry(
        campaign_id=campaign_id,
        status=CampaignStatusEnum.PAUSED,
        author_id=132,
        **params,
    )

    result = await factory.list_campaign_change_log(campaign_id=campaign_id)
    assert result == [
        {
            "id": Any(int),
            "created_at": Any(datetime),
            "campaign_id": campaign_id,
            "author_id": 132,
            "status": "PAUSED",
            "system_metadata": expected_system_metadata,
            "state_before": Any(dict),
            "state_after": Any(dict),
            "is_latest": True,
        }
    ]
