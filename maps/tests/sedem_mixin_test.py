import pytest

from maps.infra.sandbox import SedemManagedMixin, ReleaseSpec
from sandbox import sdk2


def test_mixin_defaults() -> None:
    class ExampleTask(SedemManagedMixin, sdk2.Task):
        pass

    parameters = ExampleTask.Parameters
    assert parameters is SedemManagedMixin.Parameters
    assert parameters.push_tasks_resource.default is True
    assert isinstance(parameters.release_spec(), ReleaseSpec)

    requirements = ExampleTask.Requirements
    assert requirements is SedemManagedMixin.Requirements
    assert requirements.cores.default == 1
    assert requirements.disk_space.default == 1024
    assert requirements.ram.default == 2048
    assert requirements.Caches.__getstate__() == {}  # no caches


def test_missing_sdk2_inheritance() -> None:
    with pytest.raises(Exception, match='ExampleTask must inherit from sdk2.Task'):
        class ExampleTask(SedemManagedMixin):
            pass


def test_wrong_inheritance_order() -> None:
    with pytest.raises(Exception, match='Invalid inheritance order'):
        class ExampleTask(sdk2.Task, SedemManagedMixin):
            pass


def test_bad_parameters_inheritance() -> None:
    with pytest.raises(Exception, match='ExampleTask.Parameters must inherit from SedemManagedMixin.Parameters'):
        class ExampleTask(SedemManagedMixin, sdk2.Task):
            class Parameters(sdk2.Parameters):
                pass


def test_bad_release_spec() -> None:
    with pytest.raises(Exception, match='ExampleTask.Parameters.release_spec must return an instance of ReleaseSpec'):
        class ExampleTask(SedemManagedMixin, sdk2.Task):
            class Parameters(SedemManagedMixin.Parameters):
                @classmethod
                def release_spec(cls) -> None:
                    return None
