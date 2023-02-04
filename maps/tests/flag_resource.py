from maps.garden.sdk.resources.flag import FlagResource
from maps.garden.sdk.core import Resource

from maps.garden.sdk.module_rpc.proto import resource_pb2 as resource_proto
from maps.garden.sdk.resources.proto import resource_pb2 as core_resource_proto


def test_there_and_back_again_flag_resource():
    flag = FlagResource(
        name="test_python_resource"
    )

    proto = flag.to_proto()

    assert isinstance(proto, resource_proto.Resource)
    assert proto.HasExtension(core_resource_proto.flag)

    assert proto.name == flag.name

    decoded_flag = Resource.from_proto(proto)
    assert isinstance(decoded_flag, FlagResource)

    assert decoded_flag.name == flag.name
