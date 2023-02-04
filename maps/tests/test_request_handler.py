import pytest

from maps.garden.sdk.core import Version, Task, Demands, Creates
from maps.garden.sdk.resources import FlagResource
from maps.garden.sdk.test_utils import execute
from maps.garden.sdk.test_utils.internal.task_handler import TaskError


class _CustomException(Exception):
    pass


class _TestTask(Task):
    def __init__(self, raise_on_call=None):
        self._raise_on_call = raise_on_call
        super().__init__()

    def __call__(self, *args, **kwargs):
        if self._raise_on_call:
            raise self._raise_on_call


class _RaiseOnCommitResource(FlagResource):
    def _commit(self):
        raise _CustomException("on_commit")


class _RaiseOnLoadResource(FlagResource):
    def load_environment_settings(self, *args, **kwargs):
        raise _CustomException("on_load")


class _RaiseOnEnsureAvailableResource(FlagResource):
    def _ensure_available(self):
        raise _CustomException("on_ensure_available")


def _run_graph(cook, task, input_resource=None, output_resource=None):
    if not input_resource:
        input_resource = FlagResource("input_resource")

    if not output_resource:
        output_resource = FlagResource("output_resource")

    cook.target_builder().add_resource(input_resource)
    resource = cook.create_input_resource(input_resource.name)
    resource.version = Version()

    cook.target_builder().add_resource(output_resource)

    cook.target_builder().add_task(
        Demands(input_resource=input_resource.name),
        Creates(output_resource=output_resource.name),
        task,
    )

    execute(cook)


def test_no_exception(cook):
    _run_graph(cook, _TestTask())


def test_exception_on_task(cook):
    with pytest.raises(TaskError) as ex:
        _run_graph(cook, _TestTask(_CustomException("custom_exception")))

    assert isinstance(ex.value.original_exception, _CustomException)
    assert str(ex.value.original_exception) == "custom_exception"
    assert "_TestTask" in str(ex.value)
    return str(ex.value)


def test_exception_on_load(cook):
    with pytest.raises(TaskError) as ex:
        _run_graph(cook, _TestTask(), output_resource=_RaiseOnLoadResource("output_resource"))

    assert isinstance(ex.value.original_exception, _CustomException)
    assert str(ex.value.original_exception) == "on_load"
    assert "_TestTask" in str(ex.value)
    return str(ex.value)


def test_exception_on_commit(cook):
    with pytest.raises(TaskError) as ex:
        _run_graph(cook, _TestTask(), output_resource=_RaiseOnCommitResource("output_resource"))

    assert isinstance(ex.value.original_exception, _CustomException)
    assert str(ex.value.original_exception) == "on_commit"
    assert "_TestTask" in str(ex.value)
    return str(ex.value)


def test_exception_on_ensure_available(cook):
    with pytest.raises(TaskError) as ex:
        _run_graph(cook, _TestTask(), input_resource=_RaiseOnEnsureAvailableResource("input_resource"))

    assert isinstance(ex.value.original_exception, _CustomException)
    assert str(ex.value.original_exception) == "on_ensure_available"
    assert "_TestTask" in str(ex.value)
    return str(ex.value)
