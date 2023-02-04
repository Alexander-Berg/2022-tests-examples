import json
import socket

from maps.garden.sdk.core import Resource, Version
from maps.garden.sdk.resources import PythonResource
from maps.garden.sdk.utils import pickle_utils, get_full_class_path

from maps.garden.sdk.module_rpc.module_runner import ModuleProperties, process_protobuf_command

from maps.garden.sdk.module_rpc.proto import module_rpc_pb2 as module_rpc

from . import graph


def _make_invoke_task_command(task):
    command = module_rpc.Input()
    command.command = module_rpc.Input.Command.INVOKE_TASK
    command.taskOperationInput.serializedTask = pickle_utils.dumps(task)

    version = Version(
        properties={
            "inscription": "instruction"
        }
    )

    plaque = PythonResource(name=graph.ANTIQUE_PLAQUE_RESOURCE_NAME)
    plaque.version = version
    plaque.value = 21
    # WARN: workaround the fact that messages cannot be directly assigned into a map value
    command.taskOperationInput.demands.kwargs["plaque"].CopyFrom(plaque.to_proto())

    flag = PythonResource(name=graph.CAPTURED_FLAG_RESOURCE_NAME)
    flag.version = version
    # WARN: workaround the fact that messages cannot be directly assigned into a map value
    command.taskOperationInput.creates.kwargs["flag"].CopyFrom(flag.to_proto())

    environment_settings = {}
    command.taskOperationInput.environmentSettings = json.dumps(environment_settings)

    return command


def test_invoke_task():
    input_command = _make_invoke_task_command(task=graph.CaptureTheFlagTask())
    output = process_protobuf_command(
        input_command,
        ModuleProperties(
            module_name=graph.MODULE_NAME,
            fill_graph=graph.fill_graph,
        )
    )

    assert output.IsInitialized()
    assert output.HasField("result")
    assert not output.HasField("exception")

    assert output.timings.HasField("mainProcessStartedAt")
    assert output.timings.HasField("demandsPreparationStartedAt")
    assert output.timings.HasField("taskInvocationStartedAt")
    assert output.timings.HasField("resourceCommitmentStartedAt")
    assert output.timings.HasField("mainProcessFinishedAt")
    assert len(output.timings.prepareDurations) == 1
    assert len(output.timings.commitDurations) == 1

    output_resource = Resource.from_proto(output.result.creates.kwargs["flag"])
    assert isinstance(output_resource, PythonResource)
    assert output_resource.value == 42

    assert output.context.hostname == socket.gethostname()


def test_invoke_task_with_exception():
    input_command = _make_invoke_task_command(task=graph.ExceptionalTask())
    output = process_protobuf_command(
        input_command,
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
    assert output.exception.what == "Another Brick in the Wall"

    top_frame = output.exception.traceback[-1]
    assert top_frame.filename.endswith("graph.py")
    assert top_frame.lineNumber == 41
    assert top_frame.functionName == "__call__"
    assert top_frame.sourceCode == 'raise ExceptionalException("Another Brick in the Wall")'
