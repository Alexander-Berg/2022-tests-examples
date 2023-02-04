from unittest import mock

from maps.garden.sdk.core import Version, Resource
from maps.garden.sdk.module_rpc.proto import resource_pb2 as resource_proto

from maps.garden.sdk.ecstatic import resources
from maps.garden.sdk.ecstatic.proto import resource_pb2 as ecstatic_resource_proto


def _make_ecstatic_dataset_resource():
    """
    Tests DatasetResource and its properties.
    Some of these properties invoke ecstatic.versions, hence we have to mock it out.
    """
    resource = resources.DatasetResource(
        name="noname",
        dataset_name_template="zzz:{tag}",
        dataset_version_template="{dataset_version}"
    )
    resource.version = Version(properties={
        "tag": "v1",
        "dataset_version": "123"
    })
    resource.save_yandex_environment()
    return resource


@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.dataset_versions")
def test_ecstatic_dataset_resource(versions_mock):
    resource = _make_ecstatic_dataset_resource()
    assert resource.dataset_name == "zzz:v1"
    assert resource.dataset_version == "123"

    # dataset does not exist
    versions_mock.return_value = "\n\n"
    assert not resource.physically_exists
    versions_mock.assert_called_once_with(dataset="zzz")
    versions_mock.reset_mock()

    # dataset and tag match, but version is different
    versions_mock.return_value = "\nzzz:v1=345\n"
    assert not resource.physically_exists
    versions_mock.assert_called_once_with(dataset="zzz")
    versions_mock.reset_mock()

    # dataset and version match, but tag is different
    versions_mock.return_value = "\nzzz:v1=345\nzzz:v2=123\n"
    assert not resource.physically_exists
    versions_mock.assert_called_once_with(dataset="zzz")
    versions_mock.reset_mock()

    # dataset, tag and version match
    versions_mock.return_value = "\nzzz:v1=345\nzzz:v2=123\nzzz:v1=123"
    assert resource.physically_exists
    versions_mock.assert_called_once_with(dataset="zzz")
    versions_mock.reset_mock()


@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.dataset_versions")
@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.download_datasets")
def test_dataset_download(download_mock, versions_mock):
    resource = _make_ecstatic_dataset_resource()
    versions_mock.return_value = "zzz:v1=123"
    resource.download_to("path_to_temporary_dir")

    versions_mock.assert_called_once_with(dataset="zzz")
    download_mock.assert_called_once_with(
        [("zzz:v1", "123", "path_to_temporary_dir/zzz:v1_123")]
    )
    assert resource.path() == "path_to_temporary_dir/zzz:v1_123"


@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.remove_dataset_version")
def test_dataset_removal(remove_mock):
    resource = _make_ecstatic_dataset_resource()
    resource.remove()
    remove_mock.assert_called_once_with("zzz:v1", "123")


def test_there_and_back_again_ecstatic_dataset():
    dataset = _make_ecstatic_dataset_resource()

    proto = dataset.to_proto()

    assert isinstance(proto, resource_proto.Resource)
    assert proto.HasExtension(ecstatic_resource_proto.dataset)

    assert proto.name == dataset.name
    assert proto.state == resource_proto.Resource.State.NOT_CREATED
    assert proto.Extensions[ecstatic_resource_proto.dataset].nameTemplate == dataset._dataset_name_template
    assert proto.Extensions[ecstatic_resource_proto.dataset].versionTemplate == dataset._dataset_version_template

    decoded_dataset = Resource.from_proto(proto)
    assert isinstance(decoded_dataset, resources.DatasetResource)
    assert decoded_dataset.name == dataset.name
    assert decoded_dataset._dataset_name_template == dataset._dataset_name_template
    assert decoded_dataset._dataset_version_template == dataset._dataset_version_template
