import pytest
from yt.yson.yson_types import YsonBoolean

from maps_adv.common.yt_utils.lib import BaseYtExportTask

pytestmark = [pytest.mark.asyncio]


class ExampleTask(BaseYtExportTask):
    TABLE_SCHEMA = [
        dict(name="column0", type="string", required=True),
        dict(name="column1", type="uint64", required=False),
    ]

    ITER_SIZE: int = 10
    CHUNKED_ITERATOR_METHOD_NAME = "iter_some_data"

    TIMEOUT = 3


@pytest.fixture
def task(dm):
    return ExampleTask(
        cluster="cluster", table="//path/to/table", token="fake_token", data_producer=dm
    )


@pytest.mark.parametrize("recreate_table_every_run", (True, False))
async def test_create_table_if_not_exists(task, mock_yt, recreate_table_every_run):
    mock_yt["exists"].return_value = YsonBoolean(0)
    task.RECREATE_TABLE_EVERY_RUN = recreate_table_every_run

    await task

    mock_yt["remove"].assert_not_called()
    mock_yt["create"].assert_called_with(
        "table",
        "//path/to/table",
        attributes={
            "schema": [
                dict(name="column0", type="string", required=True),
                dict(name="column1", type="uint64", required=False),
            ]
        },
    )


async def test_recreates_existed_table_if_recreate_mode_on(task, mock_yt):
    mock_yt["exists"].return_value = YsonBoolean(1)
    task.RECREATE_TABLE_EVERY_RUN = True

    await task

    mock_yt["remove"].assert_called_with("//path/to/table")
    mock_yt["create"].assert_called_with(
        "table",
        "//path/to/table",
        attributes={
            "schema": [
                dict(name="column0", type="string", required=True),
                dict(name="column1", type="uint64", required=False),
            ]
        },
    )


async def test_does_not_recreates_existed_table_if_recreate_mode_off(task, mock_yt):
    mock_yt["exists"].return_value = YsonBoolean(1)
    task.RECREATE_TABLE_EVERY_RUN = False

    await task

    mock_yt["remove"].assert_not_called()
    mock_yt["create"].assert_not_called()


@pytest.mark.parametrize("iter_size", [10, 20])
async def test_requests_data_in_chunks_of_specified_size(dm, task, mocker, iter_size):
    mocker.patch.object(ExampleTask, "ITER_SIZE", iter_size)

    await task

    assert dm.iter_some_data.args == (iter_size,)


@pytest.mark.parametrize("recreate_table_every_run", (True, False))
async def test_writes_data_got_from_data_manager(
    dm, task, mock_yt, recreate_table_every_run
):
    dm.iter_some_data.seq = [list(range(10)) for _ in range(3)]
    task.RECREATE_TABLE_EVERY_RUN = recreate_table_every_run

    await task

    assert mock_yt["write_table"].call_count == 3
    assert mock_yt["write_table"].call_args[0][1] == list(range(10))


@pytest.mark.parametrize("recreate_table_every_run", (True, False))
async def test_sorts_data_when_iteration_finished_if_has_sorting_params(
    dm, task, mock_yt, recreate_table_every_run
):
    dm.iter_some_data.seq = [list(range(10)) for _ in range(3)]
    task.RECREATE_TABLE_EVERY_RUN = recreate_table_every_run
    task.SORTING_PARAMS = ["column1", "column0"]

    await task

    assert mock_yt["run_sort"].call_count == 1
    assert mock_yt["run_sort"].call_args[1] == {
        "sort_by": ["column1", "column0"],
        "source_table": "//path/to/table",
    }


@pytest.mark.parametrize("recreate_table_every_run", (True, False))
@pytest.mark.parametrize("sorting_params", [None, []])
async def test_does_not_sort_data_if_no_sorting_params(
    dm, task, mock_yt, recreate_table_every_run, sorting_params
):
    dm.iter_some_data.seq = [list(range(10)) for _ in range(3)]
    task.RECREATE_TABLE_EVERY_RUN = recreate_table_every_run
    task.SORTING_PARAMS = sorting_params

    await task

    mock_yt["run_sort"].assert_not_called()


async def test_transaction_started(task, yt_transaction_mock):
    await task

    yt_transaction_mock.assert_enter_called()


async def test_transaction_committed_after_all_data_written(task, yt_transaction_mock):
    await task

    yt_transaction_mock.assert_exit_called_without_exception()


@pytest.mark.parametrize("method_to_raise", ["remove", "create", "write_table"])
async def test_transaction_not_committed_on_yt_exception(
    task, dm, mock_yt, yt_transaction_mock, method_to_raise
):
    dm.iter_some_data.seq = [list(range(10)) for _ in range(3)]
    mock_yt[method_to_raise].side_effect = Exception

    with pytest.raises(Exception):
        await task

    yt_transaction_mock.assert_exit_called_with_exception()


async def test_raises_if_write_fails_during_data_iteration(dm, task, mock_yt):
    dm.iter_some_data.seq = [list(range(10)) for _ in range(3)]
    mock_yt["write_table"].side_effect = Exception("YT write fails")

    with pytest.raises(Exception, match="YT write fails"):
        await task


async def test_raises_if_write_fails_after_data_iteration(dm, task, mock_yt):
    dm.iter_some_data.seq = [list(range(10))]
    mock_yt["write_table"].side_effect = Exception("YT write fails")

    with pytest.raises(Exception, match="YT write fails"):
        await task


async def test_raises_if_data_iteration_fails(dm, task, mock_yt):
    dm.iter_some_data.seq = [Exception("Boom!")]

    with pytest.raises(Exception, match="Boom!"):
        await task
