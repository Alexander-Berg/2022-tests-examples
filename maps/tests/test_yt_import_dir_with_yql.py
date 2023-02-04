from typing import Optional

import pytest
from yt.yson.yson_types import YsonList, YsonUnicode

from maps_adv.common.yt_utils.lib import BaseYtImportDirWithYqlTask
from maps_adv.common.yt_utils.lib.tasks import YqlOperationError

pytestmark = [pytest.mark.asyncio]


def yson_table_path(path: str, attributes: Optional[dict] = None):
    yson_path = YsonUnicode(path)
    yson_path.attributes["type"] = "table"
    if attributes:
        yson_path.attributes.update(attributes)

    return yson_path


class ExampleTask(BaseYtImportDirWithYqlTask):
    YQL = "SELECT * FROM kek"
    ITER_SIZE: int = 3
    CHUNKED_WRITER_METHOD_NAME = "write_some_data"
    PROCESSED_ATTR = "_geosmb_example_processed"

    TIMEOUT = 2

    @classmethod
    def _yt_row_decode(cls, row):
        return [row[0], int(row[1])]


@pytest.fixture
def task(dm):
    return ExampleTask(
        cluster="cluster",
        yt_token="fake_yt_token",
        yql_token="fake_yql_token",
        yt_dir="//some/dir",
        data_consumer=dm,
    )


async def test_will_execute_yql(task, mock_yt, mock_yql):
    mock_yt["list"].return_value = YsonList([yson_table_path("//some/dir/2020-02-03")])

    await task

    mock_yql["query"].assert_called_with("SELECT * FROM kek", syntax_version=1)
    assert mock_yql["request_run"].called is True


async def test_sends_all_data_decoded_to_dm(task, dm, mock_yt, mock_yql):
    rows_written = []
    mock_yt["list"].return_value = YsonList([yson_table_path("//some/dir/2020-02-03")])
    mock_yql["table_get_iterator"].return_value = [
        ("value1", "1"),
        ("value2", "2"),
        ("value3", "3"),
        ("value4", "4"),
    ]

    async def consumer(generator):
        nonlocal rows_written
        async for records in generator:
            rows_written.extend(records)

        return rows_written

    dm.write_some_data.side_effect = consumer

    await task

    assert rows_written == [["value1", 1], ["value2", 2], ["value3", 3], ["value4", 4]]


async def test_raises_if_yql_operation_errored(task, mock_yt, mock_yql):
    mock_yt["list"].return_value = YsonList([yson_table_path("//some/dir/2020-02-03")])
    mock_yql["request_get_results"].status = "ERROR"
    mock_yql["request_get_results"].text = "some error details"

    with pytest.raises(YqlOperationError, match="some error details"):
        await task
