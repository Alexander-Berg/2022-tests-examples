import pytest

from maps.garden.sdk.core import DataValidationWarning, Version
from maps.garden.sdk.resources import FlagResource

from maps.garden.sdk.test_utils import execute
from maps.garden.sdk.test_utils.internal.task_handler import TaskError

from maps.garden.sdk.extensions.validation_tools import add_validation_task


def _simple_validation(*, flag_resource):
    raise DataValidationWarning(flag_resource.name)


def test_add_validation(cook):
    cook.target_builder().add_resource(FlagResource("flag_resource"))
    resource = cook.create_input_resource("flag_resource")
    resource.version = Version()

    add_validation_task(
        graph_builder=cook.target_builder(),
        validation_function=_simple_validation,
        flag_resource="flag_resource",
    )

    with pytest.raises(TaskError) as ex:
        execute(cook)

    assert isinstance(ex.value.original_exception, DataValidationWarning)
    assert str(ex.value.original_exception) == resource.name
