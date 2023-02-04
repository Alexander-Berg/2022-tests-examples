from unittest import mock
import json
import os
import pytest
import shutil

import yatest.common

from yt.wrapper.client import YtClient

from maps.garden.sdk.core import Version
from maps.garden.sdk.sandbox import storage

from maps.garden.sdk.yt import YtTableResource
from maps.garden.sdk.yt import YtSourcePathResource
from maps.garden.sdk.yt import yt_task
from maps.garden.sdk.yt import utils as yt_task_utils
from maps.garden.sdk.yt import geobase

YT_SERVER = "hahn"

TABLE_DATA = [
    {"id": 3, "data": "aaa"},
    {"id": 1, "data": "bbb"},
    {"id": 4, "data": "ccc"},
    {"id": 2, "data": "ddd"}
]


@mock.patch.object(YtClient, "read_file")
def test_download_geobase(read_mock, environment_settings):
    geobase4 = geobase.get_geobase4(environment_settings["yt_servers"][YT_SERVER])
    geobase5 = geobase.get_geobase5(environment_settings["yt_servers"][YT_SERVER])
    geobase6 = geobase.get_geobase6(environment_settings["yt_servers"][YT_SERVER])
    assert os.path.exists(geobase4)
    assert os.path.exists(geobase5)
    assert os.path.exists(geobase6)
    assert read_mock.call_count == 3


def test_download_tzdata(environment_settings, mocker):
    mocker.patch.object(storage.SandboxStorage, "get_latest_resource_info")
    mocker.patch.object(storage.SandboxStorage, "fetch_sandbox_resource_to_file")

    shutil.copyfile(
        yatest.common.binary_path("maps/data/test/tzdata/tzdata.tar.gz"),
        "tzdata.tar.gz",
    )

    zones_bin_path = geobase.get_tzdata_zones_bin(environment_settings)

    assert os.path.exists(zones_bin_path)
    assert zones_bin_path.endswith("tzdata/zones_bin")


@pytest.mark.use_local_yt(YT_SERVER)
def test_run_map(environment_settings):
    source_table = YtTableResource(
        name="test_run_map_source",
        path_template="/test_run_map_source",
        server=YT_SERVER)
    source_table.version = Version()
    source_table.load_environment_settings(environment_settings)
    source_table.write_table(TABLE_DATA)

    destination_table = YtTableResource(
        name="test_run_map_destination",
        path_template="/test_run_map_destination",
        server=YT_SERVER)
    destination_table.version = Version()
    destination_table.load_environment_settings(environment_settings)

    task = yt_task.YtTaskBase()
    task.load_environment_settings(environment_settings)

    task.run_map(
        YT_SERVER,
        _test_mapper,
        source_table=source_table.path,
        destination_table=destination_table.path
    )

    assert list(destination_table.read_table()) == TABLE_DATA


def _test_mapper(row):
    yield row


def test_source_path(environment_settings):
    TEST_YT_PATH = "//home/myhome/sweet_home"

    resource = YtSourcePathResource("source_table", server=YT_SERVER)
    resource.version = Version(properties={
        "yt_path": TEST_YT_PATH
    })

    assert resource.path == TEST_YT_PATH


def test_yt_spec_patching():
    core_spec = {}
    client_spec = {}
    with mock.patch.dict(os.environ, {"YT_SPEC": json.dumps(core_spec)}):
        yt_task_utils.patch_yt_spec(client_spec)
    assert client_spec == {}

    core_spec = {"annotations": {"key1": "value1"}}
    client_spec = {}
    with mock.patch.dict(os.environ, {"YT_SPEC": json.dumps(core_spec)}):
        yt_task_utils.patch_yt_spec(client_spec)
    assert client_spec == core_spec

    core_spec = {"annotations": {"key1": "value1"}}
    client_spec = {"annotations": {"key2": "value2"}, "key3": "value3"}
    with mock.patch.dict(os.environ, {"YT_SPEC": json.dumps(core_spec)}):
        yt_task_utils.patch_yt_spec(client_spec)
    # client's spec options are not touched
    assert client_spec == {"annotations": {"key1": "value1", "key2": "value2"}, "key3": "value3"}

    core_spec = {"annotations": {"key1": "value1"}}
    client_spec = {"annotations": {"key1": "value2"}}
    with mock.patch.dict(os.environ, {"YT_SPEC": json.dumps(core_spec)}):
        with pytest.raises(AssertionError, match=r"Overriding core annotations .* is not allowed"):
            yt_task_utils.patch_yt_spec(client_spec)
