from typing import Optional

import pytest
from yt.yson.yson_types import YsonList, YsonUnicode

from maps_adv.geosmb.scenarist.server.lib.tasks import (
    CertificateMailingStatYtImportTask,
)

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.mock_dm,
    pytest.mark.usefixtures("mock_yt"),
]


@pytest.fixture
def task(config, dm):
    return CertificateMailingStatYtImportTask(config=config, dm=dm)


@pytest.fixture(autouse=True)
def mock_dm_consumer(dm):
    async def gen_consumer(gen):
        async for _ in gen:
            pass

    dm.import_certificate_mailing_stats.side_effect = gen_consumer


@pytest.fixture(autouse=True)
def common_yt_mocks(mock_yt):
    mock_yt["list"].return_value = YsonList(
        [
            yson_table_path("//some/dir/2020-02-03"),
            yson_table_path("//some/dir/2020-02-04"),
        ]
    )
    mock_yt["read_table"].side_effect = [
        [
            {
                "biz_id": 6543,
                "coupon_id": 1000,
                "scenario": "ENGAGE_PROSPECTIVE",
                "clicked": 0,
                "opened": 2,
                "sent": 14,
                "dt": "2020-12-05",
            },
            {
                "biz_id": 7854,
                "coupon_id": 2000,
                "scenario": "DISCOUNT_FOR_LOST",
                "clicked": 2,
                "opened": 5,
                "sent": 12,
                "dt": "2020-12-05",
            },
        ],
        [
            {
                "biz_id": 1975,
                "coupon_id": 3000,
                "scenario": "THANK_THE_LOYAL",
                "clicked": 9,
                "opened": 6,
                "sent": 3,
                "dt": "2020-12-04",
            },
        ],
    ]


def yson_table_path(path: str, attributes: Optional[dict] = None):
    yson_path = YsonUnicode(path)
    yson_path.attributes["type"] = "table"
    if attributes:
        yson_path.attributes.update(attributes)

    return yson_path


async def test_reads_data_from_tables(mock_yt, task):
    await task

    assert mock_yt["read_table"].call_args_list == [
        (("//some/dir/2020-02-03",), {}),
        (("//some/dir/2020-02-04",), {}),
    ]


async def test_sends_all_data_to_dm(task, dm, mock_yt):
    rows_written = []

    async def consumer(generator):
        nonlocal rows_written
        async for records in generator:
            rows_written.extend(records)

        return rows_written

    dm.import_certificate_mailing_stats.side_effect = consumer

    await task

    dm.import_certificate_mailing_stats.assert_called()
    assert rows_written == [
        {
            "biz_id": 6543,
            "coupon_id": 1000,
            "scenario": "ENGAGE_PROSPECTIVE",
            "clicked": 0,
            "opened": 2,
            "sent": 14,
            "dt": "2020-12-05",
        },
        {
            "biz_id": 7854,
            "coupon_id": 2000,
            "scenario": "DISCOUNT_FOR_LOST",
            "clicked": 2,
            "opened": 5,
            "sent": 12,
            "dt": "2020-12-05",
        },
        {
            "biz_id": 1975,
            "coupon_id": 3000,
            "scenario": "THANK_THE_LOYAL",
            "clicked": 9,
            "opened": 6,
            "sent": 3,
            "dt": "2020-12-04",
        },
    ]
