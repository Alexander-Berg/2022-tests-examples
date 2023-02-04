from maps.garden.sdk.core import Resource
from maps.garden.sdk.resources.url import UrlResource
from maps.garden.sdk.module_rpc.proto import resource_pb2 as resource_proto
from maps.garden.sdk.resources.proto import resource_pb2 as core_resource_proto


def test_there_and_back_again_url_resource():
    url = UrlResource(
        name="test_url_resource",
        url_template="https://example.com/{tic}/{tac}/{toe}"
    )

    proto = url.to_proto()

    assert isinstance(proto, resource_proto.Resource)
    assert proto.HasExtension(core_resource_proto.url)

    assert proto.name == url.name
    assert proto.Extensions[core_resource_proto.url].urlTemplate == url.url_template

    decoded_url = Resource.from_proto(proto)

    assert isinstance(decoded_url, UrlResource)

    assert decoded_url.name == url.name
    assert decoded_url.url_template == url.url_template
