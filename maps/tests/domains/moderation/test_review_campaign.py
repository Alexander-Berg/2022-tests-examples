import pytest

from maps_adv.adv_store.v2.lib.data_managers.campaigns import (
    CampaignsChangeLogActionName,
)
from maps_adv.adv_store.v2.lib.data_managers.exceptions import CampaignNotFound
from maps_adv.adv_store.api.schemas.enums import (
    CampaignStatusEnum,
    ModerationResolutionEnum,
)
from maps_adv.adv_store.v2.lib.domains.moderation import (
    CampaignNotInReview,
    ReviewCommentEmpty,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]

STATUSES_EXCEPT_REVIEW = list(
    filter(lambda s: s is not CampaignStatusEnum.REVIEW, CampaignStatusEnum)
)


@pytest.mark.parametrize(
    ("resolution", "comment", "expected_status"),
    [
        (ModerationResolutionEnum.APPROVE, "comment", CampaignStatusEnum.ACTIVE),
        (ModerationResolutionEnum.REJECT, "comment", CampaignStatusEnum.REJECTED),
    ],
)
async def test_calls_data_manager(
    resolution, comment, expected_status, moderation_domain, campaigns_dm
):
    campaigns_dm.campaign_exists.coro.return_value = True
    campaigns_dm.get_status.coro.return_value = CampaignStatusEnum.REVIEW

    await moderation_domain.review_campaign(987, resolution, 44, comment)

    campaigns_dm.campaign_exists.assert_called_with(987)
    campaigns_dm.get_status.assert_called_with(987)
    campaigns_dm.set_status.assert_called_with(
        987,
        author_id=44,
        status=expected_status,
        change_log_action_name=CampaignsChangeLogActionName.CAMPAIGN_REVIEWED,
        metadata={"comment": comment},
    )


@pytest.mark.parametrize("resolution", list(ModerationResolutionEnum))
async def test_raises_for_nonexistent_campaign(
    resolution, moderation_domain, campaigns_dm
):
    campaigns_dm.campaign_exists.coro.return_value = False

    with pytest.raises(CampaignNotFound):
        await moderation_domain.review_campaign(
            987, resolution, 44, "Resolution comment"
        )


async def test_comment_not_required_if_accepted(moderation_domain, campaigns_dm):
    campaigns_dm.campaign_exists.coro.return_value = True
    campaigns_dm.get_status.coro.return_value = CampaignStatusEnum.REVIEW

    await moderation_domain.review_campaign(987, ModerationResolutionEnum.APPROVE, 44)

    assert campaigns_dm.set_status.called


@pytest.mark.parametrize("params", [{}, {"comment": ""}])
async def test_comment_required_if_rejected(params, moderation_domain, campaigns_dm):
    campaigns_dm.campaign_exists.coro.return_value = True
    campaigns_dm.get_status.coro.return_value = CampaignStatusEnum.REVIEW

    with pytest.raises(ReviewCommentEmpty):
        await moderation_domain.review_campaign(
            987, ModerationResolutionEnum.REJECT, 44, **params
        )


@pytest.mark.parametrize("status", STATUSES_EXCEPT_REVIEW)
@pytest.mark.parametrize("resolution", list(ModerationResolutionEnum))
async def test_raises_if_campaign_not_in_review_status(
    status, resolution, moderation_domain, campaigns_dm
):
    campaigns_dm.campaign_exists.coro.return_value = True
    campaigns_dm.get_status.coro.return_value = status

    with pytest.raises(CampaignNotInReview):
        await moderation_domain.review_campaign(987, resolution, 44, "comment")
