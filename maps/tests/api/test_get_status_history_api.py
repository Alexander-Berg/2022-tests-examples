import datetime

import pytest

from maps_adv.adv_store.api.proto import campaign_status_pb2, error_pb2
from maps_adv.adv_store.tests.utils import pb_datetime, status_db_to_pb
from maps_adv.adv_store.v2.lib.db import tables
from maps_adv.adv_store.api.schemas.enums import CampaignStatusEnum

pytestmark = [pytest.mark.asyncio]


API_URL = "/campaign/{campaign_id}/status_history/"


async def _get_status_history(client, campaign_id):
    response = await client.get(API_URL.format(campaign_id=campaign_id))

    assert response.status == 200

    content = await response.read()
    output_pb = campaign_status_pb2.CampaignStatusHistory()
    output_pb.ParseFromString(content)
    return output_pb


@pytest.mark.parametrize(
    "statuses",
    [
        (),
        ((CampaignStatusEnum.DRAFT, True),),
        ((CampaignStatusEnum.DRAFT, True), (CampaignStatusEnum.ACTIVE, True)),
        ((CampaignStatusEnum.DRAFT, True), (CampaignStatusEnum.DRAFT, False)),
        (
            (CampaignStatusEnum.DRAFT, True),
            (CampaignStatusEnum.ACTIVE, True),
            (CampaignStatusEnum.ACTIVE, False),
        ),
        (
            (CampaignStatusEnum.DRAFT, True),
            (CampaignStatusEnum.DRAFT, False),
            (CampaignStatusEnum.ACTIVE, True),
        ),
        (
            (CampaignStatusEnum.PAUSED, True),
            (CampaignStatusEnum.PAUSED, False),
            (CampaignStatusEnum.ACTIVE, True),
            (CampaignStatusEnum.ACTIVE, False),
        ),
        (
            (CampaignStatusEnum.PAUSED, True),
            (CampaignStatusEnum.ACTIVE, True),
            (CampaignStatusEnum.ACTIVE, False),
            (CampaignStatusEnum.PAUSED, True),
            (CampaignStatusEnum.ACTIVE, True),
        ),
    ],
)
async def test_get_campaign_campaign_status_history_in_api(
    db, faker, factory, client, statuses
):
    campaign_id = (await factory.create_campaign())["id"]
    dt = datetime.datetime.now()
    expected = campaign_status_pb2.CampaignStatusHistory()
    for status, is_expected in statuses:
        author_id = faker.u64()
        await db.rw.execute(
            tables.status_history.insert().values(
                campaign_id=campaign_id,
                author_id=author_id,
                status=status,
                changed_datetime=dt,
                metadata={"some": "metadata"},
            )
        )
        if is_expected:
            expected.entries.extend(
                [
                    campaign_status_pb2.CampaignStatusLogEntry(
                        initiator_id=author_id,
                        status=status_db_to_pb(status),
                        changed_at=pb_datetime(dt),
                        note='{"some": "metadata"}',
                    )
                ]
            )
        dt += datetime.timedelta(days=1)

    got = await _get_status_history(client, campaign_id)

    assert got == expected


async def test_get_campaign_history_raises_campaign_not_found_in_api(client):
    response = await client.get(API_URL.format(campaign_id=1234))

    assert response.status == 400

    content = await response.read()
    got = error_pb2.Error()
    got.ParseFromString(content)

    assert got.code == error_pb2.Error.CAMPAIGN_NOT_FOUND
