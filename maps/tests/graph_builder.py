import pytest

from maps.garden.sdk.core import Demands, Creates, Task, Resource, TaskGraphBuilder
from maps.garden.sdk.core.graph_builder import UnknownResourceError


def test_graph_builder():
    graph_builder = TaskGraphBuilder()

    for num in range(1, 5):
        graph_builder.add_resource(Resource(str(num)))

    for num in range(1, 4):
        graph_builder.add_task(
            Demands(str(num)),
            Creates(str(num + 1)),
            Task())

    assert len(graph_builder.resources) == 4
    assert len(graph_builder.tasks) == 3


def test_graph_builder_resources():
    graph_builder = TaskGraphBuilder()

    resource = Resource("input")
    graph_builder.add_resource(resource)

    assert id(graph_builder.peek_resource("input")) == id(resource)

    new_resource = graph_builder.make_resource("input")
    assert id(new_resource) != id(resource)
    assert new_resource.name == resource.name

    with pytest.raises(KeyError):
        graph_builder.peek_resource("unknown_resource")

    with pytest.raises(UnknownResourceError):
        graph_builder.make_resource("unknown_resource")


def test_input_output_resources():
    graph_builder = TaskGraphBuilder()
    graph_builder.add_resource(Resource("isolated"))
    graph_builder.add_resource(Resource("input"))
    graph_builder.add_resource(Resource("output"))
    graph_builder.add_task(
        Demands("input"),
        Creates("output"),
        Task())

    assert graph_builder.input_resources() == set(["isolated", "input"])
    assert graph_builder.output_resources() == set(["isolated", "input", "output"])
