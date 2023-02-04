from maps.garden.sdk.core import Resource, Version
from maps.garden.sdk.utils import pickle_utils, get_full_class_path

from maps.garden.sdk.module_rpc.module_runner import ModuleProperties, process_protobuf_command

from maps.garden.sdk.module_rpc.proto import module_rpc_pb2 as module_rpc

from . import graph


def _make_command(task):
    command = module_rpc.Input()
    command.command = module_rpc.Input.Command.PROPAGATE_PROPERTIES
    command.taskOperationInput.serializedTask = pickle_utils.dumps(task)

    plaque = Resource(name=graph.ANTIQUE_PLAQUE_RESOURCE_NAME)
    plaque.version = Version(
        properties={
            "inscription": "instruction"
        }
    )
    # WARN: workaround the fact that messages cannot be directly assigned into a map value
    command.taskOperationInput.demands.kwargs["plaque"].CopyFrom(plaque.to_proto())

    flag = Resource(name=graph.CAPTURED_FLAG_RESOURCE_NAME)
    flag.version = Version(properties={})
    # WARN: workaround the fact that messages cannot be directly assigned into a map value
    command.taskOperationInput.creates.kwargs["flag"].CopyFrom(flag.to_proto())

    return command


def test_properties_propagation():
    output = process_protobuf_command(
        _make_command(graph.CaptureTheFlagTask()),
        ModuleProperties(
            module_name=graph.MODULE_NAME,
            fill_graph=graph.fill_graph,
        )
    )

    proto_flag = output.result.creates.kwargs["flag"]
    assert proto_flag.HasField("version")
    assert proto_flag.HasField("propertiesJson")

    assert proto_flag.version.propertiesJson == proto_flag.propertiesJson

    propagated_flag = Resource.from_proto(proto_flag)
    assert propagated_flag.version is not None
    assert propagated_flag.properties["inscription"] == "instruction"


def test_properties_propagation_with_exception():
    output = process_protobuf_command(
        _make_command(graph.ExceptionalTask()),
        ModuleProperties(
            module_name=graph.MODULE_NAME,
            fill_graph=graph.fill_graph,
        )
    )

    assert output.IsInitialized()
    assert not output.HasField("result")
    assert output.HasField("exception")

    assert output.exception.className.endswith(".graph.ExceptionalException")
    assert output.exception.classParents == [get_full_class_path(cls) for cls in graph.ExceptionalException.__mro__[1:]]
    assert output.exception.what == "Wish You Were Here"

    top_frame = output.exception.traceback[-1]
    assert top_frame.filename.endswith("graph.py")
    assert top_frame.lineNumber > 0
    assert top_frame.functionName == "propagate_properties"
    assert top_frame.sourceCode == 'raise ExceptionalException("Wish You Were Here")'
