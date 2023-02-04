import datetime

import pytest

from maps_adv.adv_store.lib.data_managers import CampaignStatusEnum
from maps_adv.adv_store.lib.data_managers.status_history import (
    get_campaign_status_history,
)
from maps_adv.adv_store.v2.lib.db import tables

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "statuses, expected",
    [
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
async def test_gets_status_history(db, faker, factory, statuses, expected):
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
