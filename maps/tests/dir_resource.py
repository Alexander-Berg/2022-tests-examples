import hashlib
import os

from unittest import mock
import pytest

from maps.pylibs.utils.lib.system import create_dir

from maps.garden.sdk.module_rpc.proto import resource_pb2 as resource_proto
from maps.garden.sdk.resources.proto import resource_pb2 as core_resource_proto

from maps.garden.sdk.core import Resource, Version
from maps.garden.sdk.resources import DirResource
from maps.garden.sdk.resources.file import S3FileStorage, FileStorage
from maps.garden.sdk.utils import MB


FILES = [
    ("folder1/file.ext", b"aaa"),
    ("folder1/subfolder1/subfolder2/file2.ext", b"Bbb" + b"ca" * 100),
    ("a/b/c/empty_file", b""),
    ("a/b/c/file2", b"b"),

    # WARNING:
    # The size of the following file
    # - should be greater than the default chunk size in
    #   boto3's TransferConfig
    # - must be less than DEFAULT_KEY_BUFFER_SIZE in moto's S3 backend
    ("big_file.ext", b"a" * 10 * MB),
]


@pytest.mark.parametrize(
    "file_resource_settings",
    ["file_storage_settings_local", "file_storage_settings_s3"],
    indirect=["file_resource_settings"],
)
class TestDirResource:

    def _verify_dir(self, folder, files_expected):
        files_actual = []
        for root_dir, dirnames, filenames in os.walk(folder):
            for filename in filenames:
                path = os.path.join(root_dir, filename)
                rel_path = os.path.relpath(path, folder)
                with open(path, "rb") as f:
                    files_actual.append((rel_path, f.read()))

        assert set(files_actual) == set(files_expected)

    def _construct_dir(self, file_resource_settings, name, path, files):

        res = DirResource(name, path)
        res.version = Version()
        res.load_environment_settings(file_resource_settings)

        assert res._temp_root is None
        assert not res.exists

        for rel_path, content in files:
            file_path = os.path.join(res.path(), rel_path)
            create_dir(os.path.dirname(file_path))

            with open(file_path, "wb") as f:
                f.write(content)

        assert res.path().endswith(res.relative_path())
        self._verify_dir(res.path(), files)

        res.logged_commit()
        assert res.exists

        storage_type = res.get_storage_type()
        is_mds = storage_type == S3FileStorage.Type
        if storage_type:
            assert is_mds == ("file_storage_s3" in file_resource_settings)

        assert res.physically_exists

        if is_mds:
            assert res.uri == "http://localhost.unittest/dir/" + res.key
        else:
            assert res.uri is None

        return res

    @pytest.mark.parametrize(
        ("name", "path", "files"),
        [
            ("dir_full", "test/dir_full", FILES),
            ("dir_empty", "test/dir_empty", []),
        ],
    )
    def test_dir_lifecycle(self, file_resource_settings, name, path, files):

        # __init__, .version, load_environment_settings(), writing content, logged_commit()
        res = self._construct_dir(file_resource_settings, name, path, files)

        # Fields to compare after serialization-deserialization
        uri = res.uri
        relative_path = res.relative_path()
        content = res.content()
        storage_type = res.get_storage_type()
        if storage_type is not None:
            assert (
                (storage_type == S3FileStorage.Type) ==
                ("file_storage_s3" in file_resource_settings)
            )

        res.clean()

        assert res._temp_root is None

        # The resource must exist even after cleaning
        assert res.exists

        # Proto-unproto
        res = Resource.from_proto(res.to_proto())
        assert relative_path == res.relative_path()
        assert storage_type == res.get_storage_type()
        assert res.exists

        # Emulating garden
        res.load_environment_settings(file_resource_settings)
        assert uri == res.uri
        assert res.physically_exists

        # We can check the content only after environment file_resource_settings loading because DirResource must initialize its
        # storage to compute url in case of remote resources
        assert sorted(content) == sorted(res.content())

        res.ensure_available()
        self._verify_dir(res.path(), files)

        # ... task code usually runs here ...

        res.clean()

        # In order to remove dir, we need to load environment file_resource_settings
        res.load_environment_settings(file_resource_settings)
        res.remove()
        assert res._temp_root is None

    def test_create(self, file_resource_settings):
        path = "test/dir_to_create"

        d = self._construct_dir(file_resource_settings, "dir_to_create", path, FILES)
        content = d.content()
        assert len(content["files"]) == len(FILES)

        d.remove()

    def test_empty_dir2(self, file_resource_settings):
        """ Test commiting a directory resource without calling path().
            In that case, an empty dir should be commited.
        """

        name = "test_create_dir2"
        path = "aa/bb/cc"

        res = DirResource(name, path)
        res.version = Version()
        res.load_environment_settings(file_resource_settings)

        res.logged_commit()

        assert res.physically_exists

        res.remove()

    def test_without_prepare(self, file_resource_settings):
        name = "test_no_prepare"
        rel_path = "aa/bb/no_prepare"

        res = self._construct_dir(file_resource_settings, name, rel_path, FILES)
        res.clean()

        res.load_environment_settings(file_resource_settings)

        assert name == res.name
        if "file_storage_s3" in file_resource_settings:
            with pytest.raises(AssertionError):
                res.path()

        res.remove()

    def test_no_tmp_dir(self, file_resource_settings):
        name = "test_no_tmp_dir"
        rel_path = name
        res = self._construct_dir(file_resource_settings, name, rel_path, FILES)
        res.clean()

        with mock.patch('tempfile.mkdtemp') as mocked_mkdtemp:
            res.load_environment_settings(file_resource_settings)
            res.remove()
            mocked_mkdtemp.assert_not_called()

    def test_there_and_back_again_dir_resource(self, file_resource_settings):
        path = "aa/bb/cc"

        if "file_storage_s3" in file_resource_settings:
            EXPECTED_STORAGE_TYPE = S3FileStorage.Type
            EXPECTED_STORAGE_NAMESPACE = file_resource_settings["file_storage_s3"]["bucket"]
            EXPECTED_STORAGE_KEY_PREFIX = os.path.join(file_resource_settings["file_storage_s3"]["prefix"], path)
        else:
            EXPECTED_STORAGE_TYPE = FileStorage.Type
            EXPECTED_STORAGE_NAMESPACE = ""
            EXPECTED_STORAGE_KEY_PREFIX = path

        dir = self._construct_dir(
            file_resource_settings,
            name="test_dir_resource",
            path=path,
            files=FILES,
        )

        proto = dir.to_proto()

        assert isinstance(proto, resource_proto.Resource)
        assert proto.IsInitialized()
        assert proto.HasExtension(core_resource_proto.dir)
        dir_metadata = proto.Extensions[core_resource_proto.dir]

        assert proto.name == dir.name
        assert dir_metadata.dirnameTemplate == dir._dirname_template
        assert dir_metadata.storageType == EXPECTED_STORAGE_TYPE
        assert dir_metadata.storageNamespace == EXPECTED_STORAGE_NAMESPACE

        # Sort files by relative paths for the ease of comparison
        sorted_files = sorted(dir_metadata.files, key=lambda file_descriptor: file_descriptor.relativePath)
        sorted_prototypes = sorted(FILES, key=lambda file_prototype: file_prototype[0])
        assert len(sorted_files) == len(sorted_prototypes)
        for file_descriptor, file_prototype in zip(sorted_files, sorted_prototypes):
            assert file_descriptor.relativePath == file_prototype[0]
            expected_prefix = os.path.join(EXPECTED_STORAGE_KEY_PREFIX, file_prototype[0])
            assert file_descriptor.storageKey.startswith(expected_prefix)
            assert file_descriptor.checksumSha1 == hashlib.sha1(file_prototype[1]).digest()

        decoded_dir = Resource.from_proto(proto)
        assert isinstance(decoded_dir, DirResource)

        assert decoded_dir.name == dir.name
        assert decoded_dir._dirname_template == dir._dirname_template

        for (relative_path, content) in FILES:
            assert decoded_dir._storage_keys[relative_path] == dir._storage_keys[relative_path]
            assert decoded_dir._content.file_hashes[relative_path] == dir._content.file_hashes[relative_path]
