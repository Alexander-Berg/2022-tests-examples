from datetime import datetime

import pytest

from maps_adv.adv_store.api.proto.error_pb2 import Error
from maps_adv.adv_store.api.proto.moderation_pb2 import CampaignModerationReview
from maps_adv.adv_store.api.schemas.enums import (
    CampaignDirectModerationWorkflowEnum,
    CampaignStatusEnum,
)
from maps_adv.common.helpers import Any

pytestmark = [pytest.mark.asyncio]

url = "/moderation/review/"

STATUSES_EXCEPT_REVIEW = list(
    filter(lambda s: s is not CampaignStatusEnum.REVIEW, CampaignStatusEnum)
)


async def test_returns_error_for_nonexistent_campaign(api):
    input_pb = CampaignModerationReview(
        resolution=CampaignModerationReview.ReviewResolution.APPROVE,
        author_id=44,
        comment="comment",
        campaign_id=123,
    )
    got = await api.post(url, proto=input_pb, decode_as=Error, expected_status=404)

    assert got == Error(code=Error.CAMPAIGN_NOT_FOUND)


@pytest.mark.parametrize("status", STATUSES_EXCEPT_REVIEW)
async def test_returns_error_for_campaign_not_in_review(status, api, factory):
    campaign_id = (await factory.create_campaign(status=status))["id"]

    input_pb = CampaignModerationReview(
        resolution=CampaignModerationReview.ReviewResolution.APPROVE,
        author_id=44,
        comment="comment",
        campaign_id=campaign_id,
    )
    got = await api.post(url, proto=input_pb, decode_as=Error, expected_status=400)

    assert got == Error(code=Error.CAMPAIGN_NOT_IN_REVIEW)


async def test_returns_error_for_empty_comment(api, factory):
    campaign_id = (await factory.create_campaign(status=CampaignStatusEnum.REVIEW))[
        "id"
    ]

    input_pb = CampaignModerationReview(
        resolution=CampaignModerationReview.ReviewResolution.REJECT,
        author_id=44,
        comment="",
        campaign_id=campaign_id,
    )
    got = await api.post(url, proto=input_pb, decode_as=Error, expected_status=400)

    assert got == Error(code=Error.REVIEW_COMMENT_EMPTY)


async def test_returns_error_for_verdicts_on_approve(api, factory):
    campaign_id = (await factory.create_campaign(status=CampaignStatusEnum.REVIEW))[
        "id"
    ]

    input_pb = CampaignModerationReview(
        resolution=CampaignModerationReview.ReviewResolution.APPROVE,
        author_id=44,
        comment="",
        campaign_id=campaign_id,
        verdicts=[1, 2, 3],
    )
    got = await api.post(url, proto=input_pb, decode_as=Error, expected_status=400)

    assert got == Error(code=Error.INVALID_INPUT_DATA)


async def test_returns_nothing_on_success(api, factory):
    campaign_id = (await factory.create_campaign(status=CampaignStatusEnum.REVIEW))[
        "id"
    ]

    input_pb = CampaignModerationReview(
        resolution=CampaignModerationReview.ReviewResolution.APPROVE,
        author_id=44,
        comment="comment",
        campaign_id=campaign_id,
    )
    got = await api.post(url, proto=input_pb, expected_status=200)

    assert got == b""


@pytest.mark.parametrize(
    ("resolution", "comment", "expected_status"),
    [
        (
            CampaignModerationReview.ReviewResolution.APPROVE,
            "comment",
            CampaignStatusEnum.ACTIVE,
        ),
        (
            CampaignModerationReview.ReviewResolution.REJECT,
            "comment",
            CampaignStatusEnum.REJECTED,
        ),
    ],
)
async def test_will_create_new_status_record(
    resolution, comment, expected_status, api, factory, con
):
    campaign_id = (await factory.create_campaign(status=CampaignStatusEnum.REVIEW))[
        "id"
    ]

    input_pb = CampaignModerationReview(
        resolution=resolution, author_id=44, comment=comment, campaign_id=campaign_id
    )
    await api.post(url, proto=input_pb, expected_status=200)

    sql = """
        SELECT EXISTS(
            SELECT *
            FROM status_history
            WHERE campaign_id = $1
                AND author_id = 44
                AND status = $2
        )
    """
    assert await con.fetchval(sql, campaign_id, expected_status.name) is True


@pytest.mark.parametrize(
    ("resolution", "comment", "expected_status", "workflow", "verdicts"),
    [
        (
            CampaignModerationReview.ReviewResolution.APPROVE,
            "comment",
            CampaignStatusEnum.ACTIVE,
            CampaignDirectModerationWorkflowEnum.AUTO_ACCEPT,
            [],
        ),
        (
            CampaignModerationReview.ReviewResolution.REJECT,
            "comment",
            CampaignStatusEnum.REJECTED,
            CampaignDirectModerationWorkflowEnum.AUTO_REJECT,
            [],
        ),
        (
            CampaignModerationReview.ReviewResolution.REJECT,
            "comment",
            CampaignStatusEnum.REJECTED,
            CampaignDirectModerationWorkflowEnum.AUTO_REJECT,
            [1, 2, 3],
        ),
    ],
)
async def test_will_create_new_direct_moderation(
    resolution, comment, expected_status, workflow, verdicts, api, factory, con
):
    campaign_id = (await factory.create_campaign(status=CampaignStatusEnum.REVIEW))[
        "id"
    ]

    input_pb = CampaignModerationReview(
        resolution=resolution,
        author_id=44,
        comment=comment,
        campaign_id=campaign_id,
        verdicts=verdicts,
    )
    await api.post(url, proto=input_pb, expected_status=200)

    moderation = await factory.retrieve_actual_campaign_direct_moderation(campaign_id)

    assert moderation == {
        "id": Any(int),
        "campaign_id": campaign_id,
        "created_at": Any(datetime),
        "status": "NEW",
        "reviewer_uid": 44,
        "verdicts": verdicts,
        "workflow": workflow,
    }
