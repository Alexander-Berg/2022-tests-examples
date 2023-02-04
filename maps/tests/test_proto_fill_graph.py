from maps.garden.sdk.resources import FlagResource
from maps.garden.sdk.utils import get_full_class_path

from maps.garden.sdk.module_rpc.module_runner import ModuleProperties, process_protobuf_command

from maps.garden.sdk.module_rpc.proto import module_rpc_pb2 as module_rpc
from maps.garden.sdk.module_rpc.proto import regions_pb2 as regions_proto

from . import graph


TEST_REGION_SLUG = "atlantis"
TEST_REGION_VENDOR = "poseidon"


def _make_command():
    command = module_rpc.Input()
    command.command = module_rpc.Input.Command.FILL_GRAPH
    region = regions_proto.YmapsdfRegion()
    region.slug = TEST_REGION_SLUG
    region.vendor = TEST_REGION_VENDOR
    command.fillGraphInput.ymapsdfRegions.append(region)

    return command


def test_fill_graph():
    output = process_protobuf_command(
        _make_command(),
        ModuleProperties(
            module_name=graph.MODULE_NAME,
            fill_graph=graph.fill_graph,
        )
    )

    assert output.IsInitialized()
    assert output.HasField("result")
    assert not output.HasField("exception")

    assert len(output.result.resources) == 1
    assert output.result.resources[0].type == FlagResource.TYPE
    assert output.result.resources[0].name == graph.CAPTURED_FLAG_RESOURCE_NAME

    assert len(output.result.tasks) == 1
    task = output.result.tasks[0]

    assert len(task.insertedFrom) > 0
    top_frame = task.insertedFrom[-1]
    assert top_frame.filename.endswith("graph.py")
    # NB: lineNumber can change upon code formatting
    assert top_frame.lineNumber == 50
    assert top_frame.functionName == "fill_graph"
    assert "graph_builder.add_task(" in top_frame.sourceCode

    assert task.name == "CaptureTheFlagTask"
    assert len(task.demands.args) == 0
    assert len(task.demands.kwargs) == 1
    assert task.demands.kwargs["plaque"] == graph.ANTIQUE_PLAQUE_RESOURCE_NAME
    assert len(task.creates.args) == 0
    assert len(task.creates.kwargs) == 1
    assert task.creates.kwargs["flag"] == graph.CAPTURED_FLAG_RESOURCE_NAME


def test_fill_graph_with_exception():
    def raise_exception(*args, **kwargs):
        raise graph.ExceptionalException("This is awful")

    output = process_protobuf_command(
        _make_command(),
        ModuleProperties(
            module_name=graph.MODULE_NAME,
            fill_graph=raise_exception,
        )
    )

    assert output.IsInitialized()
    assert not output.HasField("result")
    assert output.HasField("exception")

    assert output.exception.className.endswith(".graph.ExceptionalException")
    assert output.exception.classParents == [get_full_class_path(cls) for cls in graph.ExceptionalException.__mro__[1:]]
    assert output.exception.what == "This is awful"

    top_frame = output.exception.traceback[-1]
    assert top_frame.filename.endswith("test_proto_fill_graph.py")
    assert top_frame.lineNumber > 0
    assert top_frame.functionName == "raise_exception"
    assert top_frame.sourceCode == 'raise graph.ExceptionalException("This is awful")'
