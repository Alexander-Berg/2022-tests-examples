from maps.garden.sdk.module_rpc.proto import resource_pb2 as resource_proto

from maps.garden.sdk.core import Resource, Version
from maps.garden.sdk.core.optional import OptionalResource, make_empty_resource


def test_there_and_back_again_optional_resource():
    name = "test_optional_resource"
    maybe_flag = OptionalResource(Resource(name))
    maybe_flag.version = Version(
        properties={
            "pattern": "stars and striped"
        }
    )
    maybe_flag.version.key = maybe_flag.calculate_key()
    assert maybe_flag.name == name

    proto = maybe_flag.to_proto()
    assert isinstance(proto, resource_proto.Resource)

    assert proto.name == name
    assert proto.type == "core:optional:core:resource"
    assert proto.HasField("version")

    decoded_flag = Resource.from_proto(proto)
    assert isinstance(decoded_flag, OptionalResource)
    assert decoded_flag
    assert decoded_flag.name == name
    assert isinstance(decoded_flag.resource, Resource)

    assert decoded_flag.properties == maybe_flag.properties
    assert decoded_flag.version.hash() == maybe_flag.version.hash()
    assert decoded_flag.version.key == maybe_flag.version.key


def test_there_and_back_again_empty_resource():
    name = "test_empty_resource"
    empty = make_empty_resource(
        name="test_empty_resource",
        extra_properties={"region": "void"},
    )
    assert empty.properties == {
        "is_empty": "TRUE",
        "region": "void",
    }
    assert empty.name == name

    proto = empty.to_proto()
    assert isinstance(proto, resource_proto.Resource)

    assert proto.name == name
    assert proto.type == "core:optional:core:empty"
    assert proto.HasField("version")

    decoded_empty = Resource.from_proto(proto)
    assert isinstance(decoded_empty, OptionalResource)
    assert not decoded_empty
    assert decoded_empty.name == name

    assert decoded_empty.properties == empty.properties
    assert decoded_empty.version.hash() == empty.version.hash()
    assert decoded_empty.calculate_key() == empty.calculate_key()
    assert decoded_empty.version.key == empty.version.key
