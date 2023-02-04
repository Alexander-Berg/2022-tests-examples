import json

from maps.garden.sdk.module_rpc import common
from maps.garden.sdk.module_rpc import module_runner

from maps.garden.sdk.utils import get_full_class_path

from maps.garden.sdk.module_rpc.proto import module_rpc_pb2 as module_rpc
from . import graph


def _make_command():
    command = module_rpc.Input()
    command.command = module_rpc.Input.Command.MODULE_INFO
    command.moduleInfoInput.stub = 0

    return command


def test_module_info(mocker):
    traits = {"key": "value"}

    mocker.patch(
        "maps.garden.sdk.module_traits.module_traits.load_traits_dict_from_resource",
        return_value=traits,
    )

    output = module_runner.process_protobuf_command(
        _make_command(),
        module_runner.ModuleProperties(
            module_name=graph.MODULE_NAME,
            fill_graph=None,
        )
    )

    assert output.IsInitialized()
    assert output.HasField("result")
    assert not output.HasField("exception")

    assert output.result.name == graph.MODULE_NAME
    assert output.result.version
    assert output.result.capabilities == common.Capabilities.ALL
    assert output.result.traits == json.dumps(traits)


def test_module_info_with_exception(mocker):
    def raise_exception(*args, **kwargs):
        raise graph.ExceptionalException("This is awful")

    mocker.patch(
        "maps.garden.sdk.module_traits.module_traits.load_traits_dict_from_resource",
        side_effect=raise_exception
    )

    output = module_runner.process_protobuf_command(
        _make_command(),
        module_runner.ModuleProperties(
            module_name=graph.MODULE_NAME,
            fill_graph=None,
        )
    )

    assert output.IsInitialized()
    assert not output.HasField("result")
    assert output.HasField("exception")

    assert output.exception.className.endswith(".graph.ExceptionalException")
    assert output.exception.classParents == [get_full_class_path(cls) for cls in graph.ExceptionalException.__mro__[1:]]
    assert output.exception.what == "This is awful"

    top_frame = output.exception.traceback[-1]
    assert top_frame.filename.endswith("test_proto_module_info.py")
    assert top_frame.lineNumber > 0
    assert top_frame.functionName == "raise_exception"
    assert top_frame.sourceCode == 'raise graph.ExceptionalException("This is awful")'
