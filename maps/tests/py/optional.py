from maps.garden.sdk.core import (
    Creates, Demands, Version, TaskGraphBuilder,
    MutagenGraphBuilder, Task)
from maps.garden.sdk.core.optional import OptionalResource, make_empty_resource
from maps.garden.sdk.resources.python import PythonResource
from maps.garden.sdk.extensions.optional import optional_task
from maps.garden.sdk.extensions.property_propagators import EnsureEqualProperties

from . import utils


@optional_task
class ProcessDataTask(Task):
    propagate_properties = EnsureEqualProperties(["region"])

    def __call__(self, raw_data, processed_data):
        # due to @optional_task decorator applied,
        # input resources MUST exists if this code is invoked
        assert raw_data
        processed_data.value = "processed_" + raw_data.properties["region"]


def build_region_processing_graph(graph_builder):
    """ Task graph that prepares data for single region """
    graph_builder.add_resource(
        OptionalResource(
            PythonResource("raw_data")
        )
    )
    graph_builder.add_resource(
        OptionalResource(
            PythonResource("processed_data")
        )
    )

    graph_builder.add_task(
        Demands(raw_data="raw_data"),
        Creates(processed_data="processed_data"),
        ProcessDataTask()
    )


# Describe how to glue all regions
class GlueRegions(Task):
    def __call__(self, *args):
        # Output argument is the last
        regions = args[:-1]
        glued_data = args[-1]

        glued_data.value = [
            region.value
            for region in regions
            if region
        ]


def build_glue_graph(graph_builder, region_names):
    graph_builder.add_resource(PythonResource("world"))

    graph_builder.add_task(
        Demands(*["processed_data_" + name for name in region_names]),
        Creates("world"),
        GlueRegions())


# Build big graph from small ones
def build_graph(graph_builder, region_names):
    for region in region_names:
        builder = MutagenGraphBuilder(graph_builder, suffix="_" + region)
        build_region_processing_graph(builder)
    build_glue_graph(graph_builder, region_names)


def test_optional_tasks_and_resources(resource_storage):
    graph_builder = TaskGraphBuilder()
    build_graph(graph_builder, ['europe', 'america'])

    raw_data_europe = PythonResource("raw_data_europe")
    raw_data_europe.version = Version(properties={'region': 'europe'})
    resource_storage.save(raw_data_europe)

    # We want to build data without America.
    # So we pass empty data for America.
    raw_data_america = make_empty_resource(
        name="raw_data_america",
        extra_properties={"region": "america"}
    )
    raw_data_america.key = raw_data_america.calculate_key()
    resource_storage.save(raw_data_america)

    result = utils.execute_graph(
        graph_builder,
        storage=resource_storage,
    )
    utils.check_result(result)

    for resource in result.values():
        if resource.name == "processed_data_america":
            assert resource.properties["is_empty"]
        elif resource.name == "processed_data_europe":
            assert resource.value == "processed_europe"
        elif resource.name == "world":
            assert resource.value == ["processed_europe"]
