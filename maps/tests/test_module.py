import json
from unittest import mock
import tempfile

import pytest
import yatest.common

from maps.garden.sdk.resources.scanners import SourceDataset
from maps.garden.libs_server.module.module import Module, Attributes

import yt.wrapper as yt


MODULE_TO_TEST = "maps/garden/libs_server/test_utils/test_module/test_module"
SRC_MODULE_TO_TEST = "maps/garden/libs_server/test_utils/test_src_module/test_src_module"
TEST_MODULE_NAME = "test_module"
TEST_MODULE_VERSION = "31337"
TEST_MODULE_TRAITS = {"name": TEST_MODULE_NAME, "type": "source", "sort_options": [{"key_pattern": "test_pattern"}]}


@pytest.mark.use_local_yt("hahn")
def test_module_upload(environment_settings):

    yt_config = environment_settings["yt_servers"]["hahn"]["yt_config"]

    with tempfile.NamedTemporaryFile() as module_file:
        module_file.write(b"fake_module_content")
        module_file.flush()

        module = Module(
            module_name=TEST_MODULE_NAME,
            module_version=TEST_MODULE_VERSION,
            yt_client_config=yt_config,
            module_traits=TEST_MODULE_TRAITS,
            local_path=module_file.name
        )
        assert module.local_path == module_file.name
        assert module.remote_path is None

        module_ypath = "//home/garden/unittests/test_module/LOCAL_123"
        module.upload_to(module_ypath)

        assert module.local_path == module_file.name
        assert module.remote_path == module_ypath

        yt_client = yt.YtClient(config=yt_config)
        module_yson = yt_client.get(module_ypath, attributes=Attributes.ALL)
        assert module_yson.attributes[Attributes.NAME] == TEST_MODULE_NAME
        assert module_yson.attributes[Attributes.VERSION] == TEST_MODULE_VERSION
        traits = json.loads(module_yson.attributes[Attributes.TRAITS])
        assert traits["name"] == TEST_MODULE_TRAITS["name"]
        assert traits["type"] == TEST_MODULE_TRAITS["type"]

        module._name = "blah"
        module._version = "100500"
        # Expecting no upload to happen
        module.upload_to(module_ypath)

        module_yson = yt_client.get(module_ypath, attributes=Attributes.ALL)
        assert module_yson.attributes[Attributes.NAME] == TEST_MODULE_NAME
        assert module_yson.attributes[Attributes.VERSION] == TEST_MODULE_VERSION


def test_scan_resources():
    module_path = yatest.common.binary_path(SRC_MODULE_TO_TEST)
    module = Module(
        module_name=TEST_MODULE_NAME,
        module_version=TEST_MODULE_VERSION,
        yt_client_config={},
        module_traits=TEST_MODULE_TRAITS,
        local_path=module_path
    )

    datasets_proto = module.scan_resources({}, contour_name="contour_name")
    assert len(datasets_proto) > 0
    assert isinstance(datasets_proto[0], SourceDataset)
    assert datasets_proto[0].foreign_key
    assert datasets_proto[0].resources or datasets_proto[0].externalResources


@pytest.mark.use_local_yt("hahn")
def test_module_download_from_yt(environment_settings):

    yt_config = environment_settings["yt_servers"]["hahn"]["yt_config"]

    with tempfile.NamedTemporaryFile() as module_file:
        module_file.write(b"fake_module_content")
        module_file.flush()

        module = Module(
            module_name=TEST_MODULE_NAME,
            module_version=TEST_MODULE_VERSION,
            yt_client_config=yt_config,
            module_traits=TEST_MODULE_TRAITS,
            local_path=module_file.name
        )
        module_ypath = "//home/garden/unittests/test_module/LOCAL_123"
        module.upload_to(module_ypath)

    module = Module(
        module_name=TEST_MODULE_NAME,
        module_version=TEST_MODULE_VERSION,
        yt_client_config=yt_config,
        module_traits=TEST_MODULE_TRAITS,
        remote_path=module_ypath
    )
    local_file = "./module_download_from_yt_filename"
    module.download_to(local_file)
    with open(local_file, "rb") as f:
        assert f.readlines() == [b"fake_module_content"]


@pytest.mark.use_local_yt("hahn")
def test_module_download_from_sandbox(environment_settings):

    sandbox_storage_mock = mock.Mock()
    with mock.patch("maps.garden.libs_server.module.module.sandbox_storage.SandboxStorage",
                    return_value=sandbox_storage_mock):
        sandbox_storage_mock.get_task_resource.return_value = {"id": "1234"}

        yt_config = environment_settings["yt_servers"]["hahn"]["yt_config"]

        module = Module(
            module_name=TEST_MODULE_NAME,
            module_version=TEST_MODULE_VERSION,
            yt_client_config=yt_config,
            module_traits=TEST_MODULE_TRAITS,
            remote_path="//home/garden/unittests/test_module/LOCAL_123",
            sandbox_task_id="123"
        )
        local_file = "./module_download_from_sandbox_filename"
        module.download_to(local_file)

        sandbox_storage_mock.get_task_resource.assert_called_with(
            task_id="123",
            resource_type="ARCADIA_PROJECT",
            scan_child_tasks=True
        )
        sandbox_storage_mock.fetch_sandbox_resource_to_file.assert_called_with(
            {"id": "1234"}, f"{local_file}.tmp"
        )
