import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.crane_operator.server.lib.tasks import LoyaltyItemsYtExportTask

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def task(config, coupons_domain):
    return LoyaltyItemsYtExportTask(config=config, coupons_domain=coupons_domain)


async def test_creates_expected_table(task, mock_yt):
    await task

    assert mock_yt["create"].called
    assert mock_yt["create"].call_args[0][1] == "//path/to/loyalty-items-table"


async def test_creates_table_with_expected_schema(task, mock_yt):
    await task

    assert mock_yt["create"].call_args[1] == dict(
        attributes={
            "schema": [
                dict(name="client_id", type="uint64", required=True),
                dict(name="issued_at", type="timestamp", required=True),
                dict(name="id", type="uint64", required=True),
                dict(name="type", type="string", required=True),
                dict(name="data", type="any"),
            ]
        }
    )


async def test_writes_data_as_expected(task, mock_yt, config, facade):
    facade.get_loyalty_items_list_for_snapshot.seq = [
        [
            dict(
                client_id=111,
                issued_at=dt("2020-01-01 00:00:00"),
                id=1,
                type="COUPON",
                data={"key1": "value1"},
            ),
            dict(
                client_id=222,
                issued_at=dt("2020-02-02 00:00:00"),
                id=2,
                type="COUPON",
                data={"key2": "value2"},
            ),
        ]
    ]

    await task

    mock_yt["write_table"].assert_called_with(
        config["LOYALTY_ITEMS_YT_EXPORT_TABLE"],
        [
            dict(
                client_id=111,
                issued_at=1577836800000000,
                id=1,
                type="COUPON",
                data={"key1": "value1"},
            ),
            dict(
                client_id=222,
                issued_at=1580601600000000,
                id=2,
                type="COUPON",
                data={"key2": "value2"},
            ),
        ],
    )
