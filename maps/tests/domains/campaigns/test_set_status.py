from datetime import datetime, timedelta
from unittest.mock import ANY

import pytest

from maps_adv.adv_store.api.schemas.enums import CampaignStatusEnum
from maps_adv.adv_store.v2.lib.domains.campaigns import CampaignNotFound

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize(
    ("note", "expected_metadata"),
    [("", {}), ("Комментарий эксперта", {"comment": "Комментарий эксперта"})],
)
async def test_calls_data_manager(
    campaigns_domain, campaigns_dm, note, expected_metadata
):
    campaigns_dm.campaign_exists.coro.return_value = True

    await campaigns_domain.set_status(
        campaign_id=333, status=CampaignStatusEnum.ACTIVE, initiator_id=123, note=note
    )

    campaigns_dm.campaign_exists.assert_called_with(333)
    campaigns_dm.set_status.assert_called_with(
        campaign_id=333,
        status=CampaignStatusEnum.ACTIVE,
        author_id=123,
        metadata=expected_metadata,
    )


async def test_raises_for_nonexistent_campaign(campaigns_domain, campaigns_dm):
    campaigns_dm.campaign_exists.coro.return_value = False

    with pytest.raises(CampaignNotFound):
        await campaigns_domain.set_status(
            campaign_id=333,
            status=CampaignStatusEnum.ACTIVE,
            initiator_id=123,
            note="Коммент",
        )


async def test_calls_event_data_manager_paused(
    campaigns_domain, campaigns_dm, events_dm
):
    campaigns_dm.campaign_exists.coro.return_value = True
    campaigns_dm.get_status.coro.return_value = CampaignStatusEnum.ACTIVE

    await campaigns_domain.set_status(
        campaign_id=333,
        status=CampaignStatusEnum.PAUSED,
        initiator_id=123,
        note="Paused",
    )
    campaigns_dm.get_status.called_with(333)

    events_dm.create_event_stopped_manually.assert_called_once_with(
        timestamp=ANY, campaign_id=333, initiator_id=123, metadata={"comment": "Paused"}
    )

    # check that timestamp is aproximately now
    timestamp = events_dm.create_event_stopped_manually.call_args[1]["timestamp"]
    assert datetime.now() - timedelta(seconds=5) <= timestamp <= datetime.now()


async def test_calls_event_data_manager_draft(
    campaigns_domain, campaigns_dm, events_dm
):
    campaigns_dm.campaign_exists.coro.return_value = True
    campaigns_dm.get_status.coro.return_value = CampaignStatusEnum.ACTIVE

    await campaigns_domain.set_status(
        campaign_id=333,
        status=CampaignStatusEnum.DRAFT,
        initiator_id=123,
        note="Drafted",
    )
    campaigns_dm.get_status.called_with(333)

    events_dm.create_event_stopped_manually.assert_called_once_with(
        timestamp=ANY,
        campaign_id=333,
        initiator_id=123,
        metadata={"comment": "Drafted"},
    )

    # check that timestamp is aproximately now
    timestamp = events_dm.create_event_stopped_manually.call_args[1]["timestamp"]
    assert datetime.now() - timedelta(seconds=5) <= timestamp <= datetime.now()


@pytest.mark.parametrize(
    ("prev_status", "new_status"),
    [
        (CampaignStatusEnum.ACTIVE, CampaignStatusEnum.DONE),
        (CampaignStatusEnum.PAUSED, CampaignStatusEnum.ACTIVE),
        (CampaignStatusEnum.PAUSED, CampaignStatusEnum.DONE),
        (CampaignStatusEnum.PAUSED, CampaignStatusEnum.DRAFT),
        (CampaignStatusEnum.PAUSED, CampaignStatusEnum.ARCHIVED),
        (CampaignStatusEnum.DRAFT, CampaignStatusEnum.ACTIVE),
        (CampaignStatusEnum.DRAFT, CampaignStatusEnum.ARCHIVED),
        (CampaignStatusEnum.DRAFT, CampaignStatusEnum.REVIEW),
        (CampaignStatusEnum.REVIEW, CampaignStatusEnum.REJECTED),
    ],
)
async def test_does_not_call_event_data_manager(
    campaigns_domain, campaigns_dm, events_dm, prev_status, new_status
):
    campaigns_dm.campaign_exists.coro.return_value = True
    campaigns_dm.get_status.coro.return_value = prev_status

    await campaigns_domain.set_status(
        campaign_id=333, status=new_status, initiator_id=123, note="Paused"
    )

    campaigns_dm.get_status.called_with(333)
    events_dm.create_event_stopped_manually.assert_not_called()


async def test_creates_new_direct_moderation(
    campaigns_domain, campaigns_dm, moderation_dm
):
    campaigns_dm.campaign_exists.coro.return_value = True
    campaigns_dm.get_status.coro.return_value = CampaignStatusEnum.DRAFT
    moderation_dm.create_direct_moderation_for_campaign.coro.return_value = 1

    await campaigns_domain.set_status(
        campaign_id=333,
        status=CampaignStatusEnum.REVIEW,
        initiator_id=123,
        note="Ready",
    )

    moderation_dm.create_direct_moderation_for_campaign.assert_called_with(
        campaign_id=333, reviewer_uid=123
    )
    campaigns_dm.set_direct_moderation.assert_called_with(
        campaign_id=333, moderation_id=1
    )


@pytest.mark.parametrize(
    "new_status",
    [
        CampaignStatusEnum.ACTIVE,
        CampaignStatusEnum.PAUSED,
        CampaignStatusEnum.DRAFT,
        CampaignStatusEnum.ARCHIVED,
        CampaignStatusEnum.REJECTED,
    ],
)
async def test_does_not_create_new_direct_moderation(
    campaigns_domain, campaigns_dm, moderation_dm, new_status
):
    campaigns_dm.campaign_exists.coro.return_value = True
    campaigns_dm.get_status.coro.return_value = CampaignStatusEnum.DRAFT

    await campaigns_domain.set_status(
        campaign_id=333, status=new_status, initiator_id=123, note="Ready"
    )

    moderation_dm.create_direct_moderation_for_campaign.assert_not_called()


@pytest.mark.parametrize(
    "prev_status",
    [
        CampaignStatusEnum.ACTIVE,
        CampaignStatusEnum.PAUSED,
        CampaignStatusEnum.ARCHIVED,
        CampaignStatusEnum.REJECTED,
    ],
)
async def test_resets_direct_moderation(
    campaigns_domain, campaigns_dm, moderation_dm, prev_status
):
    campaigns_dm.campaign_exists.coro.return_value = True
    campaigns_dm.get_status.coro.return_value = prev_status

    await campaigns_domain.set_status(
        campaign_id=333, status=CampaignStatusEnum.DRAFT, initiator_id=123, note="Draft"
    )

    campaigns_dm.set_direct_moderation.assert_called_with(
        campaign_id=333, moderation_id=None
    )
