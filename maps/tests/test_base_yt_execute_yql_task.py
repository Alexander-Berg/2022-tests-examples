import pytest

from maps_adv.common.yt_utils.lib import BaseExecuteYqlTask
from maps_adv.common.yt_utils.lib.tasks import YqlOperationError

pytestmark = [pytest.mark.asyncio]


class ExampleTaskWithFormatting(BaseExecuteYqlTask):
    YQL = "SELECT {param1}, {param2} FROM kek"


class ExampleTaskWithoutFormatting(BaseExecuteYqlTask):
    YQL = "SELECT * FROM kek"


async def test_will_execute_yql(mock_yql):
    task = ExampleTaskWithFormatting(
        yql_token="fake_yql_token",
        yql_format_kwargs=dict(param1="something", param2="anything"),
    )

    await task

    mock_yql["query"].assert_called_with(
        "SELECT something, anything FROM kek", syntax_version=1
    )
    assert mock_yql["request_run"].called is True


async def test_will_execute_not_formatted_yql(mock_yql):
    task = ExampleTaskWithoutFormatting(yql_token="fake_yql_token")

    await task

    mock_yql["query"].assert_called_with("SELECT * FROM kek", syntax_version=1)
    assert mock_yql["request_run"].called is True


async def test_raises_if_yql_operation_errored(mock_yql):
    mock_yql["request_get_results"].status = "ERROR"
    mock_yql["request_get_results"].text = "some error details"

    task = ExampleTaskWithoutFormatting(yql_token="fake_yql_token")

    with pytest.raises(YqlOperationError, match="some error details"):
        await task
