import os
import filecmp
import hashlib
from unittest import mock
import pytest
import yatest.common

from maps.garden.sdk.core import Resource, Version
from maps.garden.sdk.resources import RemoteDirResource
from maps.garden.sdk.module_rpc.proto import resource_pb2 as resource_proto
from maps.garden.sdk.resources.proto import resource_pb2 as core_resource_proto


def _generate_file_list_property(data_dir):
    file_with_md5 = os.path.join(data_dir, 'file1')
    md5 = hashlib.md5(open(file_with_md5, 'rb').read()).hexdigest()

    file_with_sha256 = os.path.join(data_dir, 'file2')
    sha256 = hashlib.sha256(open(file_with_sha256, 'rb').read()).hexdigest()

    file_with_md5_sha256 = os.path.join(data_dir, 'file3')
    file3_md5 = hashlib.md5(open(file_with_md5_sha256, 'rb').read()).hexdigest()
    file3_sha256 = hashlib.sha256(open(file_with_md5_sha256, 'rb').read()).hexdigest()

    return [
        {'name': 'file0', 'url': 'http://localhost/file0'},
        {'name': 'file1', 'url': 'http://localhost/file1', 'md5': md5},
        {'name': 'file2', 'url': 'http://localhost/file2', 'sha256': sha256},
        {'name': 'file3', 'url': 'http://localhost/file3',
         'sha256': file3_sha256, 'md5': file3_md5}
    ]


def _urlopen(url):
    data_dir = yatest.common.test_source_path("data/remote_dir")
    filename = url.split("/")[-1]
    filepath = os.path.join(data_dir, filename)
    return open(filepath, 'rb')


def _construct_remote_dir_resource(file_list):
    resource = RemoteDirResource('test')
    resource.version = Version(
        properties={'file_list': file_list}
    )
    resource.ensure_available()
    return resource


def _assert_expectations(resource, expectations_dir):
    path = resource.path()
    assert os.path.exists(path)
    files = set(os.listdir(path))
    expected_files = set(os.listdir(expectations_dir))
    print('Got files:      {}'.format(files))
    print('Expected files: {}'.format(expected_files))
    assert files == expected_files
    for f in files:
        resource_file = os.path.join(path, f)
        expected_file = os.path.join(expectations_dir, f)
        assert os.path.exists(expected_file)
        assert filecmp.cmp(resource_file, expected_file)


@mock.patch("maps.garden.sdk.resources.remote_dir.urlopen", side_effect=_urlopen)
def test_create_and_clean(urlopen_mock):
    data_dir = yatest.common.test_source_path("data/remote_dir")

    file_list = _generate_file_list_property(data_dir)
    resource = _construct_remote_dir_resource(file_list)
    _assert_expectations(resource, data_dir)

    path = resource.path()
    resource.clean()
    with pytest.raises(AssertionError):
        resource.path()
    assert not os.path.exists(path)


def test_empty_dir():
    resource = _construct_remote_dir_resource([])
    path = resource.path()
    assert os.path.exists(path)
    assert not len(os.listdir(path))


def test_there_and_back_again_remote_dir_resource():
    dir = _construct_remote_dir_resource([])

    proto = dir.to_proto()

    assert isinstance(proto, resource_proto.Resource)
    assert proto.HasExtension(core_resource_proto.remoteDir)

    assert proto.name == dir.name

    decoded_dir = Resource.from_proto(proto)

    assert isinstance(decoded_dir, RemoteDirResource)
    assert decoded_dir.name == dir.name
