from maps.garden.sdk.core import Resource
from maps.garden.sdk.resources.build_params import BuildParamsResource
from maps.garden.sdk.module_rpc.proto import resource_pb2 as resource_proto
from maps.garden.sdk.resources.proto import resource_pb2 as core_resource_proto


def test_there_and_back_again_build_params_resource():
    build_params = BuildParamsResource(name="test_build_params")

    proto = build_params.to_proto()

    assert isinstance(proto, resource_proto.Resource)
    assert proto.HasExtension(core_resource_proto.buildParams)

    assert proto.name == build_params.name

    decoded_build_params = Resource.from_proto(proto)

    assert isinstance(decoded_build_params, BuildParamsResource)
    assert decoded_build_params.name == build_params.name
