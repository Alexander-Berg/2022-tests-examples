import pytest

from maps_adv.common.helpers import coro_mock
from maps_adv.warden.client.lib.exceptions import UnknownWardenStatus
from maps_adv.warden.client.lib.task_master.pipeline import Pipeline, Step

pytestmark = [pytest.mark.asyncio]


def generate_step_functions(*values):
    result = []
    for value in values:
        step = coro_mock()
        step.coro.return_value = value
        result.append(step)

    return tuple(result)


def generate_three_step_functions():
    return generate_step_functions({"key": "return step1"}, "return step2", 3)


@pytest.mark.parametrize(
    "steps", [[], [("s1", {"key": "return step1"}), ("s2", "return step2"), ("s3", 3)]]
)
async def test_success_processing_full_pipeline(steps, mock_context):
    mock_context.client._client.update_task.coro.return_value = {}
    step_list = [Step(item[0], generate_step_functions(item[1])[0]) for item in steps]

    await Pipeline(step_list)(mock_context)

    call_args_list = mock_context.client._client.update_task.call_args_list
    for index in range(len(steps)):
        assert call_args_list[index][1] == {
            "task_id": 1,
            "status": steps[index][0],
            "metadata": steps[index][1],
        }

    assert mock_context.client._client.update_task.call_count == len(steps)


async def test_expected_context_metadata_on_each_step_of_pipeline(mock_context):
    mock_context.metadata = {}
    steps = generate_three_step_functions()

    await Pipeline([Step("s1", steps[0]), Step("s2", steps[1]), Step("s3", steps[2])])(
        mock_context
    )

    for step in steps:
        step.assert_called_once()

    assert steps[0].call_args[0][0].metadata == {}
    assert steps[1].call_args[0][0].metadata == {"key": "return step1"}
    assert steps[2].call_args[0][0].metadata == "return step2"
    assert mock_context.client._client.update_task.call_count == 3


async def test_restore_processing_of_pipeline(mock_context):
    mock_context.status = "s1"

    _, step2, step3 = generate_three_step_functions()

    await Pipeline([Step("s1", coro_mock()), Step("s2", step2), Step("s3", step3)])(
        mock_context
    )

    call_args_list = mock_context.client._client.update_task.call_args_list
    assert call_args_list[0][1] == {
        "task_id": 1,
        "status": "s2",
        "metadata": "return step2",
    }
    assert call_args_list[1][1] == {"task_id": 1, "status": "s3", "metadata": 3}
    assert mock_context.client._client.update_task.call_count == 2


@pytest.mark.parametrize("steps", [[], [("s1", {})]])
async def test_raises_exception_not_found_step(steps, mock_context):
    mock_context.client._client.update_task.coro.return_value = {}
    step_list = [Step(item[0], generate_step_functions(item[1])[0]) for item in steps]

    mock_context.status = "impossible status"
    with pytest.raises(UnknownWardenStatus):
        await Pipeline(step_list)(mock_context)
