from operator import itemgetter

import pytest

from maps_adv.geosmb.promoter.server.lib.tasks import LeadsYtExportTask

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def mock_yt(mocker, shared_proxy_mp_manager):
    methods = "remove", "create", "exists", "write_table", "Transaction"
    return {
        method: mocker.patch(
            f"yt.wrapper.YtClient.{method}", shared_proxy_mp_manager.SharedMock()
        )
        for method in methods
    }


@pytest.fixture
def task(config, dm):
    return LeadsYtExportTask(config=config, dm=dm)


async def test_creates_table(task, mock_yt):
    await task
    assert mock_yt["create"].called
    assert mock_yt["create"].call_args[0][1] == "//path/to/table"


async def test_writes_data_as_expected(factory, task, mock_yt):
    lead1_id = await factory.create_lead_with_events(
        biz_id=100,
        name="lead_1",
        passport_uid="10000",
        device_id="101010",
        yandex_uid="1111",
        review_rating=5,
        clicks_on_phone=1,
    )
    lead2_id = await factory.create_lead_with_events(
        biz_id=200,
        name="lead_2",
        passport_uid="20000",
        yandex_uid="2222",
        clicks_on_phone=1,
        site_opens=4,
    )

    await task

    assert mock_yt["write_table"].called
    assert sorted(mock_yt["write_table"].call_args[0][1], key=itemgetter("biz_id")) == [
        {
            "promoter_id": lead1_id,
            "biz_id": 100,
            "passport_uid": "10000",
            "device_id": "101010",
            "yandex_uid": "1111",
            "name": "lead_1",
            "segments": ["LOYAL", "PROSPECTIVE"],
        },
        {
            "promoter_id": lead2_id,
            "biz_id": 200,
            "passport_uid": "20000",
            "device_id": None,
            "yandex_uid": "2222",
            "name": "lead_2",
            "segments": ["ACTIVE"],
        },
    ]
