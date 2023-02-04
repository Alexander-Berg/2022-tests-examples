from typing import Optional

import pytest
from yt.yson.yson_types import YsonList, YsonUnicode

from maps_adv.geosmb.scenarist.server.lib.tasks import MessagesYtImportTask

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.mock_dm,
    pytest.mark.usefixtures("mock_yt"),
]


@pytest.fixture
def task(config, dm):
    return MessagesYtImportTask(config=config, dm=dm)


@pytest.fixture(autouse=True)
def mock_dm_consumer(dm):
    async def gen_consumer(gen):
        async for _ in gen:
            pass

    dm.copy_messages_from_generator.side_effect = gen_consumer


@pytest.fixture(autouse=True)
def common_yt_mocks(mock_yt):
    mock_yt["list"].return_value = YsonList(
        [
            yson_table_path("//some/dir/2020-02-03"),
            yson_table_path("//some/dir/2020-02-04"),
        ]
    )
    mock_yt["read_table"].side_effect = [
        [{"some": "data1"}, {"some": "data2"}],
        [{"some": "data3"}],
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


async def test_sends_data_to_dm(task, dm):
    await task

    dm.copy_messages_from_generator.assert_called()
