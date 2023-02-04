import datetime

import pytest

from maps_adv.adv_store.lib.domain.campaign import get_campaign_status_history
from maps_adv.adv_store.lib.domain.exceptions import CampaignNotFound
from maps_adv.adv_store.v2.lib.db import tables
from maps_adv.adv_store.api.schemas.enums import CampaignStatusEnum

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "statuses, expected",
    [
        ([], []),
        ([CampaignStatusEnum.DRAFT], [CampaignStatusEnum.DRAFT]),
        (
            [CampaignStatusEnum.DRAFT, CampaignStatusEnum.ACTIVE],
            [CampaignStatusEnum.DRAFT, CampaignStatusEnum.ACTIVE],
        ),
        (
            [CampaignStatusEnum.DRAFT, CampaignStatusEnum.DRAFT],
            [CampaignStatusEnum.DRAFT],
        ),
        (
            [
                CampaignStatusEnum.DRAFT,
                CampaignStatusEnum.ACTIVE,
                CampaignStatusEnum.ACTIVE,
            ],
            [CampaignStatusEnum.DRAFT, CampaignStatusEnum.ACTIVE],
        ),
        (
            [
                CampaignStatusEnum.DRAFT,
                CampaignStatusEnum.DRAFT,
                CampaignStatusEnum.ACTIVE,
            ],
            [CampaignStatusEnum.DRAFT, CampaignStatusEnum.ACTIVE],
        ),
        (
            [
                CampaignStatusEnum.PAUSED,
                CampaignStatusEnum.PAUSED,
                CampaignStatusEnum.ACTIVE,
                CampaignStatusEnum.ACTIVE,
            ],
            [CampaignStatusEnum.PAUSED, CampaignStatusEnum.ACTIVE],
        ),
        (
            [
                CampaignStatusEnum.PAUSED,
                CampaignStatusEnum.ACTIVE,
                CampaignStatusEnum.ACTIVE,
                CampaignStatusEnum.PAUSED,
                CampaignStatusEnum.ACTIVE,
            ],
            [
                CampaignStatusEnum.PAUSED,
                CampaignStatusEnum.ACTIVE,
                CampaignStatusEnum.PAUSED,
                CampaignStatusEnum.ACTIVE,
            ],
        ),
    ],
)
async def test_get_campaign_campaign_status_history_in_domain(
    db, faker, factory, statuses, expected
):
    campaign_id = (await factory.create_campaign())["id"]
    dt = datetime.datetime.now()
    for status in statuses:
        await db.rw.execute(
            tables.status_history.insert().values(
                campaign_id=campaign_id,
                author_id=faker.u64(),
                status=status,
                changed_datetime=dt,
            )
        )
        dt += datetime.timedelta(days=1)

    history = await get_campaign_status_history(campaign_id)

    assert [c["status"] for c in history] == expected


async def test_get_campaign_history_raises_campaign_not_found_in_domain(db, faker):
    with pytest.raises(CampaignNotFound):
        await get_campaign_status_history(42)
