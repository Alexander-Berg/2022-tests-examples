import pytest

from maps.garden.libs_server.graph.versioned_task import VersionedTask
from maps.garden.libs_server.graph.graph_utils import ResourceVersionsPropagator, PropagateError
from maps.garden.sdk.core import Demands, Creates, Task, TaskGraphBuilder, Version
from maps.garden.sdk.extensions.property_propagators import EnsureEqualProperties
from maps.garden.sdk.resources import PythonResource

from maps.garden.libs_server.graph.request_storage import VersionSpecifier


class FirstTask(Task):
    propagate_properties = EnsureEqualProperties("region")


class SecondTask(Task):
    propagate_properties = EnsureEqualProperties("region")


class ThirdTask(Task):
    propagate_properties = EnsureEqualProperties("region")


def test_simple():
    graph_builder = TaskGraphBuilder()

    for name in ["target", "next_target", "finished"]:
        graph_builder.add_resource(PythonResource(name))

    source_resource = PythonResource("source")
    source_resource.version = Version(properties={"region": "europe"})
    graph_builder.add_resource(source_resource)

    graph_builder.add_task(Demands("source"), Creates("target"), FirstTask())
    graph_builder.add_task(Demands("target"), Creates("next_target"), SecondTask())
    graph_builder.add_task(Demands("next_target"), Creates("finished"), SecondTask())

    propagator = ResourceVersionsPropagator(graph_builder, "some_contour")
    result = propagator.propagate({source_resource.name: source_resource.version}, ["target", "next_target", "finished"])

    assert result
    assert result.graph
    assert result.target_versions
    assert not result.additional_versions

    graph, target_versions, additional_versions = result

    target_names = [t.name for t in target_versions]
    assert not additional_versions
    assert "target" in target_names
    assert "next_target" in target_names
    assert "finished" in target_names
    assert "source" not in target_names

    assert all(map(lambda x: isinstance(x, VersionedTask), graph))
    assert all(map(lambda x: isinstance(x.demands, Demands), graph))
    assert all(map(lambda x: isinstance(x.creates, Creates), graph))
    assert list(map(lambda x: type(x.task), graph.sources())) == [SecondTask]
    assert list(map(lambda x: type(x.task), graph.sinks())) == [FirstTask]
    assert all(map(lambda x: x.version.properties == {"region": "europe"}, target_versions))

    assert [VersionSpecifier.from_resource_meta(meta) for meta in target_versions]


def test_cycle():
    graph_builder = TaskGraphBuilder()

    for name in ["source", "target"]:
        graph_builder.add_resource(PythonResource(name))

    graph_builder.add_task(Demands("source"), Creates("target"), FirstTask())
    graph_builder.add_task(Demands("target"), Creates("source"), SecondTask())

    with pytest.raises(PropagateError) as ex:
        ResourceVersionsPropagator(graph_builder, "some_contour")

    assert "Task graph contains a cycle passing through" in str(ex.value)


def test_unknown_resource():
    graph_builder = TaskGraphBuilder()

    for name in ["source", "target", "next_target"]:
        graph_builder.add_resource(PythonResource(name))

    graph_builder.add_task(Demands("source"), Creates("target"), FirstTask())
    graph_builder.add_task(Demands("unknown_resource"), Creates("next_target"), SecondTask())

    with pytest.raises(PropagateError) as ex:
        ResourceVersionsPropagator(graph_builder, "some_contour")

    assert "Unknown demanded resource" in str(ex.value)


def test_two_ways():
    graph_builder = TaskGraphBuilder()

    for name in ["source", "target"]:
        graph_builder.add_resource(PythonResource(name))

    graph_builder.add_task(Demands("source"), Creates("target"), FirstTask())
    graph_builder.add_task(Demands("source"), Creates("target"), SecondTask())

    with pytest.raises(PropagateError) as ex:
        ResourceVersionsPropagator(graph_builder, "some_contour")

    assert "Resource target can be created in two different ways" in str(ex.value)


def test_wo_task():
    graph_builder = TaskGraphBuilder()

    source_resource = PythonResource("source")
    source_resource.version = Version(properties={"region": "europe"})
    graph_builder.add_resource(source_resource)

    propagator = ResourceVersionsPropagator(graph_builder, "some_contour")
    result = propagator.propagate({"source": source_resource.version}, ["source"])

    assert result
    assert not result.graph.vertices()
    assert result.target_versions

    graph, target_versions, additional_versions = result

    target_names = [t.name for t in target_versions]
    assert len(additional_versions) == 1
    assert len(target_names) == 1

    assert list(map(lambda x: type(x.task), graph.sources())) == []
    assert list(map(lambda x: type(x.task), graph.sinks())) == []

    assert all(map(lambda x: x.version.properties == {"region": "europe"}, target_versions))
