from maps.garden.sdk.core import Version
from maps.garden.sdk.resources import FlagResource
from maps.garden.sdk.utils import pickle_utils, get_full_class_path, KB

from maps.garden.sdk.module_rpc.module_runner import ModuleProperties, process_protobuf_command
from maps.garden.sdk.module_rpc.proto import module_rpc_pb2 as module_rpc

from . import graph


def _make_command(task):
    command = module_rpc.Input()
    command.command = module_rpc.Input.Command.PREDICT_CONSUMPTION
    command.taskOperationInput.serializedTask = pickle_utils.dumps(task)

    plaque = FlagResource(name=graph.ANTIQUE_PLAQUE_RESOURCE_NAME)
    plaque.version = Version(
        properties={
            "inscription": "instruction"
        }
    )
    plaque.size = {
        "bytes": 31337
    }
    # WARN: workaround the fact that messages cannot be directly assigned into a map value
    command.taskOperationInput.demands.kwargs["plaque"].CopyFrom(plaque.to_proto())

    flag = FlagResource(name=graph.CAPTURED_FLAG_RESOURCE_NAME)
    flag.version = Version(properties={})
    # WARN: workaround the fact that messages cannot be directly assigned into a map value
    command.taskOperationInput.creates.kwargs["flag"].CopyFrom(flag.to_proto())

    return command


def test_predict_consumption():
    output = process_protobuf_command(
        _make_command(graph.CaptureTheFlagTask()),
        ModuleProperties(
            module_name=graph.MODULE_NAME,
            fill_graph=graph.fill_graph,
        )
    )

    assert output.result.tmpfs
    assert not output.result.portoLayer
    assert output.result.cpuCores == 31337
    assert output.result.ramBytes == 640 * KB
    assert output.result.operations == 1


def test_predict_consumption_with_exception():
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

    assert output.exception.className == get_full_class_path(graph.ExceptionalException())
    assert output.exception.classParents == [get_full_class_path(cls) for cls in graph.ExceptionalException.__mro__[1:]]
    assert output.exception.what == "Shine on You Crazy Diamond"

    top_frame = output.exception.traceback[-1]
    assert top_frame.filename.endswith("graph.py")
    assert top_frame.lineNumber > 0
    assert top_frame.functionName == "predict_consumption"
    assert top_frame.sourceCode == 'raise ExceptionalException("Shine on You Crazy Diamond")'
