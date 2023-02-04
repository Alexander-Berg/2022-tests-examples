from maps.garden.sdk.module_rpc.proto import resource_pb2 as resource_proto

from maps.garden.sdk.core import Resource, Version


def test_there_and_back_again_resource():
    r = Resource(name="test_resource")
    r.version = Version(
        properties={
            u"red": u"alert",
            u"blue": u"shift",
            u"nested": [
                u"properties",
                u"are",
                u"supported",
                {
                    u"though": u"this",
                    u"was": u"a bad idea"
                }
            ]
        }
    )
    r.size = {
        "bytes": 100500
    }
    r.version.key = r.calculate_key()

    proto = r.to_proto()

    assert isinstance(proto, resource_proto.Resource)

    assert proto.type == Resource.TYPE
    assert proto.name == r.name
    assert proto.HasField("version")
    assert proto.HasField("sizeInBytes")
    assert proto.sizeInBytes == 100500

    decoded = Resource.from_proto(proto)

    assert type(decoded) is Resource
    assert decoded.name == r.name
    assert decoded.properties == r.properties
    assert decoded.size == r.size
    assert decoded.version.hash() == r.version.hash()

    assert decoded.version.key == r.version.key
