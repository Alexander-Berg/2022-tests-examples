import pytest
from yql.client.parameter_value_builder import YqlParameterValueBuilder

from maps_adv.common.yt_utils.lib import BaseImportWithYqlTask
from maps_adv.common.yt_utils.lib.tasks import YqlOperationError

pytestmark = [pytest.mark.asyncio]


class ExampleTask(BaseImportWithYqlTask):
    YQL = "SELECT {param1}, {param2} FROM kek"
    ITER_SIZE: int = 3
    CHUNKED_WRITER_METHOD_NAME = "write_some_data"

    TIMEOUT = 2

    @classmethod
    def _yt_row_decode(cls, row):
        return [row[0], int(row[1])]


class ExampleNoFormattingTask(BaseImportWithYqlTask):
    YQL = "SELECT * FROM kek"
    ITER_SIZE: int = 3
    CHUNKED_WRITER_METHOD_NAME = "write_some_data"

    TIMEOUT = 2


class ExampleWithQueryParamsTask(BaseImportWithYqlTask):
    YQL = """
        DECLARE $id AS Int64;
        SELECT * FROM kek WHERE id=$id;
    """
    ITER_SIZE: int = 3
    CHUNKED_WRITER_METHOD_NAME = "write_some_data"

    TIMEOUT = 2

    async def fetch_yql_query_params(self) -> dict:
        return {"$id": YqlParameterValueBuilder.make_int64(123)}


@pytest.fixture
def task(dm):
    return ExampleTask(
        yql_token="fake_yql_token",
        yql_format_kwargs=dict(param1="something", param2="anything"),
        data_consumer=dm,
    )


async def test_will_execute_yql(task, mock_yql):
    await task()

    mock_yql["query"].assert_called_with(
        "SELECT something, anything FROM kek", syntax_version=1
    )
    assert mock_yql["request_run"].called is True


async def test_will_execute_not_formatted_yql(dm, mock_yql):
    task = ExampleNoFormattingTask(yql_token="fake_yql_token", data_consumer=dm)

    await task()

    mock_yql["query"].assert_called_with("SELECT * FROM kek", syntax_version=1)
    assert mock_yql["request_run"].called is True


async def test_will_execute_with_query_params(dm, mock_yql):
    task = ExampleWithQueryParamsTask(yql_token="fake_yql_token", data_consumer=dm)

    await task()

    mock_yql["query"].assert_called_with(
        """
        DECLARE $id AS Int64;
        SELECT * FROM kek WHERE id=$id;
    """,
        syntax_version=1,
    )
    mock_yql["request_run"].assert_called_with(parameters={"$id": '{"Data": "123"}'})


async def test_sends_decoded_data_to_dm(task, dm, mock_yql):
    rows_written = []
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

    await task()

    assert rows_written == [["value1", 1], ["value2", 2], ["value3", 3], ["value4", 4]]


async def test_raises_if_yql_operation_errored(task, mock_yql):
    mock_yql["request_get_results"].status = "ERROR"
    mock_yql["request_get_results"].text = "some error details"

    with pytest.raises(YqlOperationError, match="some error details"):
        await task()
