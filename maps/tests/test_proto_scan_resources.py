import json

from maps.garden.sdk.resources.scanners.common import SourceDataset, BuildExternalResource
from maps.garden.sdk.module_rpc.module_runner import ModuleProperties, process_protobuf_command

from maps.garden.sdk.module_rpc.proto import module_rpc_pb2 as module_rpc

from . import graph

_ENVIRONMENT_SETTINGS = {"foo": "bar"}


def _make_command():
    command = module_rpc.Input()
    command.command = module_rpc.Input.Command.SCAN_RESOURCES
    command.scanResourcesInput.environmentSettings = json.dumps(_ENVIRONMENT_SETTINGS)

    return command


def _scan_resources(environment_settings):
    yield SourceDataset(
        foreign_key={"foreign_key": "value"},
        resources=[BuildExternalResource(
            resource_name="name",
            properties={}
        )]
    )


def test_scan_resources():
    output = process_protobuf_command(
        _make_command(),
        ModuleProperties(
            module_name=graph.MODULE_NAME,
            fill_graph=None,
            scan_resources=_scan_resources,
        )
    )

    assert output.IsInitialized()
    assert output.HasField("result"), output.exception
    assert not output.HasField("exception")

    assert len(output.result.datasets) == 1
    dataset = output.result.datasets[0]
    assert dataset.foreignKey["foreign_key"] == "value"

    assert len(dataset.externalResources) == 1
    resource = BuildExternalResource.from_proto(dataset.externalResources[0])
    assert isinstance(resource, BuildExternalResource)
    assert resource.resource_name == "name"
