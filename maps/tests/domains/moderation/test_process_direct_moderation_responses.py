import pytest

from maps_adv.adv_store.v2.lib.core.direct_moderation.schema import (
    DirectModerationIncoming,
    DirectModerationMeta,
)
from maps_adv.adv_store.v2.lib.data_managers.campaigns import (
    CampaignsChangeLogActionName,
)
from maps_adv.adv_store.api.schemas.enums import (
    CampaignDirectModerationStatusEnum,
    CampaignDirectModerationWorkflowEnum,
    CampaignStatusEnum,
    YesNoEnum,
)
from maps_adv.common.helpers import dt

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.mock_dm,
    pytest.mark.mock_direct_moderation_client,
]


@pytest.mark.parametrize(
    ("moderation_response", "moderation_call_args", "campaign_call_args"),
    [
        (
            DirectModerationIncoming(
                meta=DirectModerationMeta(campaign_id=123, version_id=555),
                verdict=YesNoEnum.YES,
                reasons=[],
            ),
            [555, CampaignDirectModerationStatusEnum.ACCEPTED],
            {
                "author_id": 100500,
                "campaign_id": 123,
                "change_log_action_name": CampaignsChangeLogActionName.CAMPAIGN_REVIEWED,
                "metadata": {"direct_moderation": 555},
                "status": CampaignStatusEnum.ACTIVE,
            },
        ),
        (
            DirectModerationIncoming(
                meta=DirectModerationMeta(campaign_id=123, version_id=555),
                verdict=YesNoEnum.NO,
                reasons=[100500, 200600, 300700],
            ),
            [
                555,
                CampaignDirectModerationStatusEnum.REJECTED,
                [100500, 200600, 300700],
            ],
            {
                "author_id": 100500,
                "campaign_id": 123,
                "change_log_action_name": CampaignsChangeLogActionName.CAMPAIGN_REVIEWED,
                "metadata": {"direct_moderation": 555},
                "status": CampaignStatusEnum.REJECTED,
            },
        ),
    ],
)
async def test_calls_direct_moderation_client_and_update_moderation_status(
    moderation_domain,
    moderation_dm,
    campaigns_dm,
    direct_moderation_client,
    moderation_response,
    moderation_call_args,
    campaign_call_args,
):
    async def responses():
        yield moderation_response

    direct_moderation_client.retrieve_direct_responses.side_effect = responses
    moderation_dm.retrieve_direct_moderation.coro.return_value = {
        "id": 555,
        "created_at": dt("2020-10-01 00:00:00"),
        "campaign_id": 123,
        "reviewer_uid": 100500,
        "status": CampaignDirectModerationStatusEnum.PROCESSING,
        "workflow": CampaignDirectModerationWorkflowEnum.COMMON,
        "verdicts": [],
    }
    campaigns_dm.retrieve_campaign.coro.return_value = {
        "id": 123,
        "direct_moderation_id": 555,
    }

    await moderation_domain.process_direct_moderation_responses()

    moderation_dm.update_direct_moderation.assert_called_with(*moderation_call_args)
    campaigns_dm.set_status.assert_called_with(**campaign_call_args)


async def test_does_nothing_on_empty_response(
    moderation_domain, moderation_dm, campaigns_dm, direct_moderation_client
):
    async def responses():
        return
        yield

    direct_moderation_client.retrieve_direct_responses.side_effect = responses

    await moderation_domain.process_direct_moderation_responses()

    moderation_dm.update_direct_moderation.assert_not_called()
    campaigns_dm.set_status.assert_not_called()


async def test_does_not_change_campaign_status_on_old_moderation_results(
    moderation_domain, moderation_dm, campaigns_dm, direct_moderation_client
):
    async def responses():
        yield DirectModerationIncoming(
            meta=DirectModerationMeta(campaign_id=123, version_id=555),
            verdict=YesNoEnum.YES,
            reasons=[],
        )

    direct_moderation_client.retrieve_direct_responses.side_effect = responses
    moderation_dm.retrieve_direct_moderation.coro.return_value = {
        "id": 555,
        "created_at": dt("2020-10-01 00:00:00"),
        "campaign_id": 123,
        "reviewer_uid": 100500,
        "status": CampaignDirectModerationStatusEnum.PROCESSING,
        "workflow": CampaignDirectModerationWorkflowEnum.COMMON,
        "verdicts": [],
    }
    campaigns_dm.retrieve_campaign.coro.return_value = {
        "id": 123,
        "direct_moderation_id": 556,
    }

    await moderation_domain.process_direct_moderation_responses()

    moderation_dm.update_direct_moderation.assert_called_with(
        555, CampaignDirectModerationStatusEnum.ACCEPTED
    )
    campaigns_dm.set_status.assert_not_called()


async def test_does_not_activate_campaign_if_positive_verdict_is_not_beign_awaited(
    moderation_domain, moderation_dm, campaigns_dm, direct_moderation_client
):
    async def responses():
        yield DirectModerationIncoming(
            meta=DirectModerationMeta(campaign_id=123, version_id=555),
            verdict=YesNoEnum.YES,
            reasons=[],
        )

    direct_moderation_client.retrieve_direct_responses.side_effect = responses
    moderation_dm.retrieve_direct_moderation.coro.return_value = {
        "id": 555,
        "created_at": dt("2020-10-01 00:00:00"),
        "campaign_id": 123,
        "reviewer_uid": 100500,
        "status": CampaignDirectModerationStatusEnum.ACCEPTED,
        "workflow": CampaignDirectModerationWorkflowEnum.COMMON,
        "verdicts": [],
    }
    campaigns_dm.retrieve_campaign.coro.return_value = {
        "id": 123,
        "direct_moderation_id": 555,
    }

    await moderation_domain.process_direct_moderation_responses()

    moderation_dm.update_direct_moderation.assert_not_called()
    campaigns_dm.set_status.assert_not_called()
