import tempfile
import unittest.mock

from maps.garden.sdk.core import Resource, Version
from maps.garden.sdk.module_rpc.proto import resource_pb2 as resource_proto
from maps.garden.sdk.resources.arc_source_path import ArcSourcePathResource
from maps.garden.sdk.resources.proto import resource_pb2 as resource_ext_proto


def test_there_and_back_again_arc_source_path():
    arc_source_path_resource = ArcSourcePathResource(
        name="test_arc_source_path_resource")

    proto = arc_source_path_resource.to_proto()
    assert isinstance(proto, resource_proto.Resource)
    assert proto.HasExtension(resource_ext_proto.arcSourcePath)

    assert proto.name == arc_source_path_resource.name

    decoded_arc_source_path = Resource.from_proto(proto)
    assert isinstance(decoded_arc_source_path, ArcSourcePathResource)
    assert decoded_arc_source_path.name == arc_source_path_resource.name


@unittest.mock.patch(
    "maps.garden.sdk.resources.arc_source_path.ArcClient", autospec=True)
def test_download_from_arc(MockArcClient):
    mock_arc_token = "mock arc token"
    mock_environment_settings = {"arcanum": {"token": mock_arc_token}}
    mock_file_content = b"mock file content"
    mock_file_path = "mock/file/path"
    mock_revision = "mockrevision"

    arc_source_path_resource = ArcSourcePathResource("mock_resource_name")
    arc_source_path_resource.version = Version(properties={
        "path": mock_file_path,
        "revision": mock_revision,
    })
    arc_source_path_resource.load_environment_settings(mock_environment_settings)

    assert arc_source_path_resource.path == mock_file_path
    assert arc_source_path_resource.revision == mock_revision

    mock_arc_client = MockArcClient.return_value

    mock_arc_client.get_file.return_value = mock_file_content
    assert arc_source_path_resource.download_to_bytes() == mock_file_content
    MockArcClient.assert_called_with(oauth_token=mock_arc_token)
    mock_arc_client.get_file.assert_called_with(file_path=mock_file_path)

    with tempfile.NamedTemporaryFile(mode="wb") as temp_file_path:
        arc_source_path_resource.download_to_file(temp_file_path.name)
        with open(temp_file_path.name, "rb") as input_file:
            content = input_file.read()
        assert content == mock_file_content
    MockArcClient.assert_called_with(oauth_token=mock_arc_token)
    mock_arc_client.get_file.assert_called_with(file_path=mock_file_path)
