import pytest

from maps.garden.sdk.resources.python import PythonResource
from maps.garden.sdk.core import Resource
from maps.garden.sdk.module_rpc.proto import resource_pb2 as resource_proto
from maps.garden.sdk.resources.proto import resource_pb2 as core_resource_proto


VALUES_TO_TEST = [
    31337,
    {
        "plain": "values are ok",
        "nested": {
            "values": {
                "are also": "ok"
            }
        }
    }
]


@pytest.mark.parametrize("value", VALUES_TO_TEST)
def test_there_and_back_again_python_resource(value):
    r = PythonResource(
        name="test_python_resource",
        value=value
    )

    proto = r.to_proto()

    assert isinstance(proto, resource_proto.Resource)
    assert proto.HasExtension(core_resource_proto.pythonResource)

    meta = proto.Extensions[core_resource_proto.pythonResource]

    assert proto.name == r.name
    assert len(meta.pickledValue) > 0

    decoded_r = Resource.from_proto(proto)
    assert isinstance(decoded_r, PythonResource)

    assert decoded_r.name == r.name
    assert decoded_r.value == r.value
