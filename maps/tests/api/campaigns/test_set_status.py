import pytest

from maps_adv.adv_store.api.proto.campaign_status_pb2 import (
    CampaignStatus,
    CampaignStatusChangeInput,
)
from maps_adv.adv_store.api.proto.error_pb2 import Error
from maps_adv.adv_store.api.schemas.enums import CampaignStatusEnum

pytestmark = [pytest.mark.asyncio]

url = "/campaigns/{}/status/"


@pytest.mark.parametrize(
    ("status", "expected_status"),
    [
        (CampaignStatus.DRAFT, CampaignStatusEnum.DRAFT),
        (CampaignStatus.REVIEW, CampaignStatusEnum.REVIEW),
        (CampaignStatus.REJECTED, CampaignStatusEnum.REJECTED),
        (CampaignStatus.PAUSED, CampaignStatusEnum.PAUSED),
        (CampaignStatus.ACTIVE, CampaignStatusEnum.ACTIVE),
        (CampaignStatus.DONE, CampaignStatusEnum.DONE),
        (CampaignStatus.ARCHIVED, CampaignStatusEnum.ARCHIVED),
    ],
)
async def test_will_set_campaign_status(api, factory, con, status, expected_status):
    campaign_id = (await factory.create_campaign())["id"]

    input_pb = CampaignStatusChangeInput(
        initiator_id=1234, status=status, note="Я сменил статус"
    )
    await api.put(url.format(campaign_id), proto=input_pb, expected_status=200)

    sql = """
        SELECT EXISTS(
            SELECT *
            FROM status_history
            WHERE campaign_id = $1
                AND author_id = 1234
                AND status = $2
                AND metadata = '{"comment": "Я сменил статус"}'
        )
    """
    assert await con.fetchval(sql, campaign_id, expected_status.name) is True


async def test_default_note_value(api, factory, con):
    campaign_id = (await factory.create_campaign())["id"]

    input_pb = CampaignStatusChangeInput(
        initiator_id=1234, status=CampaignStatus.REVIEW
    )
    await api.put(url.format(campaign_id), proto=input_pb, expected_status=200)

    sql = """
        SELECT metadata
        FROM status_history
        WHERE campaign_id = $1
            AND author_id = 1234
            AND status = $2
    """
    assert await con.fetchval(sql, campaign_id, CampaignStatusEnum.REVIEW.name) == {}


async def test_does_not_change_previously_set_status(api, factory, con):
    campaign_id = (await factory.create_campaign())["id"]

    input_pb = CampaignStatusChangeInput(
        initiator_id=1234, status=CampaignStatus.REVIEW
    )
    await api.put(url.format(campaign_id), proto=input_pb, expected_status=200)
    input_pb = CampaignStatusChangeInput(
        initiator_id=5678, status=CampaignStatus.ACTIVE
    )
    await api.put(url.format(campaign_id), proto=input_pb, expected_status=200)

    sql = """
        SELECT EXISTS(
            SELECT *
            FROM status_history
            WHERE campaign_id = $1
                AND author_id = 1234
                AND status = 'REVIEW'
        )
    """
    assert await con.fetchval(sql, campaign_id) is True


async def test_will_create_new_status_record(api, factory, con):
    campaign_id = (await factory.create_campaign())["id"]

    input_pb = CampaignStatusChangeInput(
        initiator_id=1234, status=CampaignStatus.REVIEW
    )
    await api.put(url.format(campaign_id), proto=input_pb, expected_status=200)

    sql = """
        SELECT COUNT(*)
        FROM status_history
        WHERE campaign_id = $1
    """
    assert await con.fetchval(sql, campaign_id) == 2  # because created in draft


async def test_returns_nothing(api, factory):
    campaign_id = (await factory.create_campaign())["id"]

    input_pb = CampaignStatusChangeInput(
        initiator_id=5678, status=CampaignStatus.ACTIVE
    )
    got = await api.put(url.format(campaign_id), proto=input_pb, expected_status=200)

    assert got == b""


async def test_returns_error_for_nonexistent_campaign(api):
    input_pb = CampaignStatusChangeInput(
        initiator_id=5678, status=CampaignStatus.ACTIVE
    )
    got = await api.put(
        url.format(333), proto=input_pb, decode_as=Error, expected_status=404
    )

    assert got == Error(code=Error.CAMPAIGN_NOT_FOUND)
