import asyncio
import inspect
from typing import Optional

import pytest
from yt.yson.yson_types import YsonBoolean, YsonList, YsonUnicode

from maps_adv.common.yt_utils.lib import BaseYtImportDirTask

pytestmark = [pytest.mark.asyncio]


def yson_table_path(path: str, attributes: Optional[dict] = None):
    yson_path = YsonUnicode(path)
    yson_path.attributes["type"] = "table"
    if attributes:
        yson_path.attributes.update(attributes)

    return yson_path


class ExampleTask(BaseYtImportDirTask):
    ITER_SIZE: int = 3
    CHUNKED_WRITER_METHOD_NAME = "write_some_data"
    PROCESSED_ATTR = "_geosmb_example_processed"

    TIMEOUT = 2


@pytest.fixture
def task(dm):
    return ExampleTask(
        cluster="cluster",
        yt_token="fake_yt_token",
        yt_dir="//some/dir",
        data_consumer=dm,
    )


async def test_reads_from_yt_tables_in_dir(task, mock_yt):
    mock_yt["list"].return_value = YsonList(
        [
            yson_table_path("//some/dir/2020-02-03"),
            yson_table_path("//some/dir/2020-02-04"),
        ]
    )

    await task

    assert mock_yt["list"].called
    assert mock_yt["read_table"].mock_calls == [
        (("//some/dir/2020-02-03",), {}),
        (("//some/dir/2020-02-04",), {}),
    ]


async def test_not_reads_from_tables_marked_as_processed(task, mock_yt):
    mock_yt["list"].return_value = YsonList(
        [
            yson_table_path("//some/dir/2020-02-03"),
            yson_table_path(
                "//some/dir/2020-02-04",
                {"_geosmb_example_processed": YsonBoolean(True)},
            ),
            yson_table_path(
                "//some/dir/2020-02-05", {"_some_other_attribute": YsonBoolean(True)}
            ),
        ]
    )

    await task

    assert mock_yt["list"].called
    assert mock_yt["read_table"].mock_calls == [
        (("//some/dir/2020-02-03",), {}),
        (("//some/dir/2020-02-05",), {}),
    ]


async def test_calls_dm(task, dm, mock_yt):
    mock_yt["list"].return_value = YsonList([yson_table_path("//some/dir/2020-02-03")])

    await task

    dm.write_some_data.assert_called()
    assert inspect.isasyncgen(dm.write_some_data.call_args[0][0])


@pytest.mark.parametrize(
    ("rows_in_table", "expected_chunks_count"), [(1, 1), (3, 1), (4, 2), (6, 2), (7, 3)]
)
async def test_sends_data_to_dm_in_chunks(
    task, dm, mock_yt, rows_in_table, expected_chunks_count
):
    mock_yt["list"].return_value = YsonList([yson_table_path("//some/dir/2020-02-03")])
    mock_yt["read_table"].return_value = list(
        {"field": f"value{i}"} for i in range(rows_in_table)
    )
    chunks_written = []

    async def consumer(generator):
        nonlocal chunks_written
        async for records in generator:
            chunks_written.append(records)

        return chunks_written

    dm.write_some_data.side_effect = consumer

    await task

    assert len(chunks_written) == expected_chunks_count


async def test_sends_all_data_to_dm(task, dm, mock_yt):
    rows_written = []
    mock_yt["list"].return_value = YsonList([yson_table_path("//some/dir/2020-02-03")])
    mock_yt["read_table"].return_value = [
        {"field": "value1"},
        {"field": "value2"},
        {"field": "value3"},
        {"field": "value4"},
    ]

    async def consumer(generator):
        nonlocal rows_written
        async for records in generator:
            rows_written.extend(records)

        return rows_written

    dm.write_some_data.side_effect = consumer

    await task

    assert rows_written == [
        {"field": "value1"},
        {"field": "value2"},
        {"field": "value3"},
        {"field": "value4"},
    ]


async def test_sets_attribute_on_tables_if_processed_successfully(task, mock_yt):
    mock_yt["list"].return_value = YsonList(
        [
            yson_table_path("//some/dir/2020-02-03"),
            yson_table_path("//some/dir/2020-02-04"),
        ]
    )

    await task

    mock_yt["set_attribute"].assert_any_call(
        "//some/dir/2020-02-03", "_geosmb_example_processed", True
    )
    mock_yt["set_attribute"].assert_any_call(
        "//some/dir/2020-02-04", "_geosmb_example_processed", True
    )


async def test_sets_attribute_for_processed_table_on_other_table_exception(
    task, mock_yt
):
    mock_yt["list"].return_value = YsonList(
        [
            yson_table_path("//some/dir/2020-02-03"),
            yson_table_path("//some/dir/2020-02-04"),
            yson_table_path("//some/dir/2020-02-05"),
        ]
    )
    mock_yt["read_table"].side_effect = [
        [{"field": "value1"}, {"field": "value2"}],
        Exception,
    ]

    with pytest.raises(Exception):
        await task

    assert mock_yt["set_attribute"].mock_calls == [
        (("//some/dir/2020-02-03", "_geosmb_example_processed", True), {})
    ]


@pytest.mark.parametrize("read_before_exception", range(0, 3))
async def test_does_not_set_attribute_on_dm_exception(
    task, dm, mock_yt, read_before_exception
):
    mock_yt["list"].return_value = YsonList([yson_table_path("//some/dir/2020-02-03")])
    mock_yt["read_table"].return_value = [
        {"field": "value1"},
        {"field": "value2"},
        {"field": "value3"},
    ]

    async def consumer(generator):
        rows_written_number = 0

        async for _ in generator:
            await asyncio.sleep(0.1)

            if rows_written_number >= read_before_exception:
                raise Exception

            rows_written_number += 1

    dm.write_some_data.side_effect = consumer
    task.ITER_SIZE = 1
    task.TIMEOUT = 1

    with pytest.raises(Exception):
        await task

    mock_yt["set_attribute"].assert_not_called()


@pytest.mark.parametrize("processed_tables_count", range(3))
async def test_sends_empty_generator_to_dm_if_not_tables_to_process(
    task, dm, mock_yt, processed_tables_count
):
    tables = list(
        yson_table_path(
            f"//some/dir/2020-02-0{i + 1}",
            {"_geosmb_example_processed": YsonBoolean(True)},
        )
        for i in range(processed_tables_count)
    )

    mock_yt["list"].return_value = tables
    rows_written = []

    async def consumer(generator):
        nonlocal rows_written
        async for records in generator:
            rows_written.extend(records)

        return rows_written

    dm.write_some_data.side_effect = consumer

    await task

    assert rows_written == []


async def test_raises_on_iter_table_exception(task, mock_yt):
    mock_yt["list"].return_value = YsonList([yson_table_path("//some/dir/2020-02-03")])
    mock_yt["read_table"].side_effect = Exception

    with pytest.raises(Exception):
        await task


async def test_raises_on_dm_write_timeout(task, dm, mock_yt):
    mock_yt["list"].return_value = YsonList([yson_table_path("//some/dir/2020-02-03")])
    mock_yt["read_table"].return_value = [{"field": "value1"}, {"field": "value2"}]

    async def slow_consumer(generator):
        await asyncio.sleep(3)
        _ = []
        async for records in generator:
            _.append(records)

    dm.write_some_data.side_effect = slow_consumer

    with pytest.raises(Exception):
        await task
