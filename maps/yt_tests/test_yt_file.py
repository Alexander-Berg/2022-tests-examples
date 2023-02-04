import copy
import datetime
from unittest import mock
import os
import pytest

import yatest.common

from yt.packages.requests import HTTPError
from yt.wrapper.client import YtClient
from yt.wrapper.ypath import ypath_join, ypath_dirname

from maps.garden.sdk.core import Version

from maps.garden.sdk.yt import YtFileResource
from maps.garden.sdk.yt import UploadToYtTask


YT_SERVER = "hahn"


@pytest.fixture
def yt_path(environment_settings):
    prefix = environment_settings["yt_servers"][YT_SERVER]["prefix"]
    return ypath_join(prefix, "path/name_suffix")


@pytest.fixture
def file_resource(environment_settings):
    resource = YtFileResource(
        name="test_file",
        filename_template="/path/name_{property}",
        server=YT_SERVER)
    resource.version = Version(properties={"property": "suffix"})
    resource.load_environment_settings(environment_settings)
    return resource


def test_path(file_resource, yt_path):
    assert file_resource.path == yt_path
    assert str(file_resource) == "YtFileResource(path=" + yt_path + ")"


@mock.patch.object(YtClient, "create")
def test_create_path(create_mock, file_resource, yt_path):
    file_resource.create_path()
    create_mock.assert_called_with("map_node", ypath_dirname(yt_path), recursive=True, ignore_existing=True)


@mock.patch.object(YtClient, "exists")
def test_physically_exists(exists_mock, file_resource, yt_path):
    exists_mock.side_effect = [HTTPError(), True]

    assert file_resource.physically_exists

    assert exists_mock.call_count == 2
    exists_mock.assert_called_with(yt_path)


@mock.patch("yt.wrapper.cypress_commands.remove_with_empty_dirs")
def test_remove(remove_mock, file_resource):
    remove_mock.side_effect = [HTTPError(), None]

    file_resource.remove()

    assert remove_mock.call_count == 2


@pytest.mark.use_local_yt(YT_SERVER)
def test_file_upload(environment_settings, file_resource, yt_path):
    yt_client = file_resource.get_yt_client()

    local_file_path = yatest.common.test_source_path("data/file1")

    local_file_mock = mock.Mock()
    local_file_mock.path.return_value = local_file_path

    task = UploadToYtTask()
    task.load_environment_settings(environment_settings)
    task(local_file_mock, file_resource)

    file_resource.logged_commit()
    file_resource.calculate_size()

    assert yt_client.exists(yt_path)

    expected_data = open(local_file_path, "rb").read()

    assert yt_client.read_file(file_resource.path).read() == expected_data

    data_size = yt_client.get(
        ypath_join(file_resource.path, "@uncompressed_data_size"))
    assert data_size > 0
    assert file_resource.size["bytes"] == data_size
    assert len(expected_data) == data_size

    annotation = yt_client.get(ypath_join(file_resource.path, "@annotation"))
    assert "UploadToYtTask" in annotation

    # Test resource copying

    another_prefix = "//home/another_prefix"
    another_environment_settings = copy.deepcopy(environment_settings)
    another_environment_settings["yt_servers"]["hahn"]["prefix"] = another_prefix
    another_environment_settings["yt_servers"]["hahn"]["tmp_dir"] = another_prefix + "/tmp"


@pytest.mark.use_local_yt(YT_SERVER)
def test_directory_upload(environment_settings, file_resource, yt_path):
    yt_client = file_resource.get_yt_client()

    local_dir_mock = mock.Mock()
    local_dir_mock.path.return_value = yatest.common.test_source_path("data")

    task = UploadToYtTask()
    task.load_environment_settings(environment_settings)
    task(local_dir_mock, file_resource)

    file_resource.logged_commit()
    file_resource.calculate_size()

    assert yt_client.exists(yt_path + "/file1")
    assert yt_client.exists(yt_path + "/file2")
    assert yt_client.exists(yt_path + "/dir1")
    assert yt_client.exists(yt_path + "/dir1/file11")
    assert yt_client.exists(yt_path + "/dir1/file12")
    assert yt_client.exists(yt_path + "/dir2")
    assert yt_client.exists(yt_path + "/dir2/file21")
    assert yt_client.exists(yt_path + "/empty_dir")

    def local_directory_size(path):
        result = 0
        for dirpath, dirs, files in os.walk(path):
            for file_name in files:
                result += os.path.getsize(os.path.join(dirpath, file_name))
        return result

    assert file_resource.size["bytes"] == local_directory_size(local_dir_mock.path())


@pytest.mark.use_local_yt(YT_SERVER)
def test_expiration_time(file_resource, yt_path):
    yt_client = file_resource.get_yt_client()

    expiration_time = datetime.datetime.utcnow() + datetime.timedelta(hours=1)

    yt_client.create("file", file_resource.path, recursive=True, ignore_existing=True)

    file_resource.expiration_time = expiration_time
    file_resource.logged_commit()
    assert yt_client.exists(yt_path + "/@expiration_time")


def test_download_to(mocker, file_resource):
    local_file_path = yatest.common.test_source_path("data/file1")
    local_file_copy_path = "file1_copy"
    with open(local_file_path, "rb") as f:
        mocker.patch.object(YtFileResource, "read_file", new=lambda _: f)
        file_resource.download_to(local_file_copy_path)

    with open(local_file_path, "r") as f, open(local_file_copy_path, "r") as f_copy:
        assert f.read() == f_copy.read()
