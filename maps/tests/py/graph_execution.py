import more_itertools
import pytest

from maps.garden.sdk.core import (
    TaskGraphBuilder,
    Task, Demands, Creates, Version)
from maps.garden.sdk.resources.python import PythonResource
from maps.garden.libs_server.resource_storage.resource_meta import ResourceMeta

from . import utils


class FillResourceTask(Task):
    def __call__(self, *args):
        target = args[-1]
        target.value["calculated"] = int(target.name)


RESOURCE_NAMES = [str(idx) for idx in range(1, 6)]


@pytest.fixture
def graph_executor(resource_storage):
    builder = TaskGraphBuilder()
    builder.add_resource(
        PythonResource(RESOURCE_NAMES[0], value={"environment_settings": 42}),
    )
    for prev, curr in more_itertools.windowed(RESOURCE_NAMES, 2):
        builder.add_resource(
            PythonResource(curr, value={"environment_settings": 42}),
        )
        builder.add_task(
            Demands(prev),
            Creates(curr),
            FillResourceTask()
        )

    def execute_graph(hash_string=""):
        source_resource = builder.make_resource(RESOURCE_NAMES[0])
        source_resource.version = Version(hash_string=hash_string)
        source_resource.value["calculated"] = 1
        resource_storage.save(source_resource)

        return utils.execute_graph(
            builder,
            storage=resource_storage,
            input_name_to_version={source_resource.name: source_resource.version},
            target_names=RESOURCE_NAMES[1:]
        )

    return execute_graph


def test_storage_state(resource_storage, graph_executor):
    graph_executor()
    names_in_storage = sorted(resource.name for key, resource in resource_storage)
    assert names_in_storage == RESOURCE_NAMES


def test_resources_content(resource_storage, graph_executor):
    graph_executor()
    for key, resource in resource_storage:
        expected_value = {
            "calculated": int(resource.name),
            "environment_settings": 42
        }
        assert key == resource.key
        assert resource.value == expected_value


def test_name_pattern_search(resource_storage, graph_executor):
    graph_executor("v1")
    graph_executor("v2")

    for _, resource in resource_storage:
        assert resource.name in RESOURCE_NAMES

    def resources_number_by_pattern(pattern):
        return more_itertools.ilen(resource_storage.find_versions(pattern))

    for name in RESOURCE_NAMES:
        assert resources_number_by_pattern(name) == 2

    assert resources_number_by_pattern(".*") == 10
    assert resources_number_by_pattern("[1-4]") == 8

    existing_source_versions = set(resource_storage.find_versions("1"))
    correct_source_versions = {
        ResourceMeta("1", Version(hash_string=hash_string))
        for hash_string in ["v1", "v2"]
    }
    assert existing_source_versions == correct_source_versions
