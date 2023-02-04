import hashlib
import os

from unittest import mock
import pytest

from maps.garden.sdk.module_rpc.proto import resource_pb2 as resource_proto
from maps.garden.sdk.resources.proto import resource_pb2 as core_resource_proto

from maps.garden.sdk.core import GardenError, Resource, Version
from maps.garden.sdk.resources import FileResource
from maps.garden.sdk.resources.file import FileStorage, S3FileStorage
from maps.garden.sdk.utils import MB


@pytest.mark.parametrize(
    "file_resource_settings",
    ["file_storage_settings_local", "file_storage_settings_s3"],
    indirect=["file_resource_settings"],
)
class TestFileResource:

    FILES = {
        "simple_file": ("file.ext", b"aaaaa"),
        "subfolder_file": ("folder1/subfolder1/subfolder2/file2.ext", b"bbbb"),
        "weird_chars_file": ("file<2.ext", b"ccc"),
        "empty_file": ("empty.txt", b""),

        # WARNING:
        # The size of the following file
        # - should be > max(multipart_threshold, multipart_chunksize) in current
        #   S3FileStorage implementation
        # - must be less than DEFAULT_KEY_BUFFER_SIZE in moto's S3 backend
        "big_file": ("big.txt", b"q" * 150 * MB),
    }

    def _create_resource(self, file_resource_settings, name="simple_file"):

        path, content = self.FILES[name]
        resource = FileResource(name, path)
        resource.version = Version()
        resource.load_environment_settings(file_resource_settings)

        assert resource._temp_root is None
        assert not resource.exists
        assert not resource.sha1
        assert not resource.size

        with resource.open("wb") as file:
            file.write(content)

        assert os.path.exists(resource.path())
        assert resource.path().endswith(resource.relative_path())

        resource.logged_commit()

        assert resource.exists
        assert resource.size["bytes"] == len(content)
        assert resource.physically_exists
        assert resource.sha1 == hashlib.sha1(content).hexdigest()

        storage_type = resource.get_storage_type()
        if storage_type:
            assert (
                (storage_type == S3FileStorage.Type) ==
                ("file_storage_s3" in file_resource_settings)
            )

        return resource

    @pytest.mark.parametrize("name", list(FILES.keys()))
    def test_lifecycle(self, file_resource_settings, name):
        # __init__, .version, load_environment_settings(), writing content, logged_commit()
        resource = self._create_resource(file_resource_settings, name)

        # Fields to compare after serialization-deserialization
        sha1 = resource.sha1
        uri = resource.uri
        relative_path = resource.relative_path()
        storage_type = resource.get_storage_type()

        resource.clean()

        assert resource._temp_root is None

        # The resource must exist even after cleaning
        assert resource.exists

        # Proto-unproto
        resource = Resource.from_proto(resource.to_proto())
        assert sha1 == resource.sha1
        assert relative_path == resource.relative_path()
        assert storage_type == resource.get_storage_type()
        assert resource.exists

        # Emulating garden
        resource.load_environment_settings(file_resource_settings)
        assert uri == resource.uri
        assert resource.physically_exists

        # Creating local copy
        resource.ensure_available()
        _, content = self.FILES[name]
        assert sha1 == hashlib.sha1(content).hexdigest()

        # ... task code usually runs here ...

        resource.clean()

        # In order to remove file, we need to load environment file_resource_settings
        resource.load_environment_settings(file_resource_settings)
        resource.remove()
        assert not resource.physically_exists

        assert resource._temp_root is None

    def test_without_ensure_available(self, file_resource_settings):
        resource = self._create_resource(file_resource_settings)
        resource.clean()
        resource.load_environment_settings(file_resource_settings)

        with pytest.raises(AssertionError):
            resource.path()

        resource.remove()

    def test_commit_nothing(self, file_resource_settings):
        resource = FileResource("nothing", "nothing")
        resource.version = Version()
        resource.load_environment_settings(file_resource_settings)

        with pytest.raises(GardenError):
            resource.logged_commit()

    def test_no_tmp_dir(self, file_resource_settings):
        res = self._create_resource(file_resource_settings)
        res.clean()

        with mock.patch('tempfile.mkdtemp') as mocked_mkdtemp:
            res.load_environment_settings(file_resource_settings)
            res.remove()
            mocked_mkdtemp.assert_not_called()

    def test_there_and_back_again_file_resource(self, file_resource_settings):
        if "file_storage_s3" in file_resource_settings:
            EXPECTED_STORAGE_TYPE = S3FileStorage.Type
            EXPECTED_STORAGE_NAMESPACE = file_resource_settings["file_storage_s3"]["bucket"]
            EXPECTED_STORAGE_KEY_PREFIX = os.path.join(file_resource_settings["file_storage_s3"]["prefix"], "file.ext")
        else:
            EXPECTED_STORAGE_TYPE = FileStorage.Type
            EXPECTED_STORAGE_NAMESPACE = ""
            EXPECTED_STORAGE_KEY_PREFIX = "file.ext"

        test_file_name = "simple_file"

        file = self._create_resource(file_resource_settings, name=test_file_name)

        proto = file.to_proto()

        assert isinstance(proto, resource_proto.Resource)
        assert proto.IsInitialized()
        assert proto.HasExtension(core_resource_proto.file)
        file_metadata = proto.Extensions[core_resource_proto.file]

        assert proto.name == file.name
        assert file_metadata.filenameTemplate == file._filename_template
        assert file_metadata.storageType == EXPECTED_STORAGE_TYPE
        assert file_metadata.storageNamespace == EXPECTED_STORAGE_NAMESPACE
        assert file_metadata.storageKey.startswith(EXPECTED_STORAGE_KEY_PREFIX)
        assert file_metadata.checksumSha1 == hashlib.sha1(self.FILES[test_file_name][1]).digest()

        decoded_file = Resource.from_proto(proto)
        assert isinstance(decoded_file, FileResource)

        assert decoded_file.name == file.name
        assert decoded_file._filename_template == file._filename_template
        assert decoded_file._storage_key == file._storage_key
