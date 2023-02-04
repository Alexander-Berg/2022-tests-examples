from datetime import datetime, timedelta
from unittest import mock

import pytest
import pytz

from maps_adv.adv_store.api.schemas.enums import CampaignStatusEnum

pytestmark = [pytest.mark.asyncio]


def make_row(log_id, campaign_id, created_at=1546300800.0):
    return {
        "id": log_id,
        "campaign_id": campaign_id,
        "author_id": 132,
        "created_at": created_at,
        "state_after": {},
        "state_before": {},
        "status": "DRAFT",
        "system_metadata": {},
    }


@pytest.fixture
def yt_mock(mocker):
    class YtClientMock:
        __init__ = mock.Mock(return_value=None)
        write_table = mock.Mock()

    return mocker.patch("yt.wrapper.YtClient", YtClientMock)


@pytest.fixture
def written_rows(yt_mock):
    write_table_rows = []

    def write_table(table, rows):
        nonlocal write_table_rows
        write_table_rows.append(list(rows))

    yt_mock.write_table.side_effect = write_table

    return write_table_rows


async def test_does_not_write_wrong_logs(factory, campaigns_dm, written_rows):
    campaign_id = (await factory.create_campaign())["id"]
    await factory.create_campaign_change_log(
        campaign_id=campaign_id,
        created_at=datetime.now(tz=pytz.utc) - timedelta(hours=23),
    )

    await campaigns_dm.backup_campaigns_change_log()

    assert written_rows == [[]]


async def test_does_not_write_with_no_logs(factory, campaigns_dm, written_rows):
    await factory.create_campaign()

    await campaigns_dm.backup_campaigns_change_log()

    assert written_rows == [[]]


@pytest.mark.parametrize(
    ("status",), [(CampaignStatusEnum.ACTIVE,), (CampaignStatusEnum.DONE,)]
)
async def test_writes_rows(factory, campaigns_dm, written_rows, status):
    created_at = datetime.now(tz=pytz.utc) - timedelta(hours=30)
    campaign_ids = [
        (await factory.create_campaign(status=status))["id"] for _ in range(3)
    ]
    log_ids = [
        (
            await factory.create_campaign_change_log(
                campaign_id=campaign_id, is_latest=True, created_at=created_at
            )
        )["id"]
        for campaign_id in campaign_ids
    ]

    await campaigns_dm.backup_campaigns_change_log()

    assert [sorted(rows, key=lambda r: r["id"]) for rows in written_rows] == [
        [
            make_row(log_id, campaign_id, created_at=created_at.timestamp())
            for log_id, campaign_id in zip(log_ids, campaign_ids)
        ]
    ]


@pytest.mark.freeze_time
async def test_writes_campaign_logs_created_day_ago(
    factory, campaigns_dm, written_rows
):
    created_at = datetime.now(tz=pytz.utc) - timedelta(hours=24)
    campaign_id = (await factory.create_campaign())["id"]
    log_id = (
        await factory.create_campaign_change_log(
            campaign_id=campaign_id, created_at=created_at
        )
    )["id"]

    await campaigns_dm.backup_campaigns_change_log()

    assert written_rows == [
        [make_row(log_id, campaign_id, created_at=created_at.timestamp())]
    ]


@pytest.mark.freeze_time
async def test_moves_change_log(factory, campaigns_dm, written_rows):
    campaign_id = (await factory.create_campaign())["id"]
    await factory.create_campaign_change_log(
        campaign_id=campaign_id,
        created_at=datetime.now(tz=pytz.utc) - timedelta(hours=24),
    )

    await campaigns_dm.backup_campaigns_change_log()

    change_log = await factory.list_campaign_change_log(campaign_id)
    assert len(change_log) == 0


async def test_does_not_remove_change_log_on_yt_error(factory, campaigns_dm, yt_mock):
    campaign_id = (await factory.create_campaign())["id"]
    change_log = await factory.create_campaign_change_log(
        campaign_id=campaign_id,
        created_at=datetime.now(tz=pytz.utc) - timedelta(hours=25),
    )

    def write_table(table, rows):
        list(rows)
        raise Exception()

    yt_mock.write_table.side_effect = write_table

    with pytest.raises(Exception):
        await campaigns_dm.backup_campaigns_change_log()

    assert (await factory.list_campaign_change_log(campaign_id)) == [change_log]
