import hashlib
from unittest import mock
import os
import pytest
import shutil

from maps.garden.tools.garden_cli_launcher.lib import updater


def test_updater(requests_mock):
    root_dir = "."

    test_file = "garden"
    with open(test_file, "w+") as f:
        f.write("some fake data")

    with open(test_file, mode="rb") as fin:
        md5_hash = hashlib.md5()
        md5_hash.update(fin.read(1024))
        test_file_md5 = md5_hash.hexdigest()

    def tar_file_callback(request, context):
        return open(test_file, "rb")

    requests_mock.get("http://some_url.com", body=tar_file_callback)

    resource = {
        "md5": test_file_md5,
        "http": {"proxy": "http://some_url.com"},
        "id": 1234
    }

    with mock.patch(
            "maps.garden.sdk.sandbox.storage.SandboxStorage.get_latest_resource_info",
            return_value=resource):

        updater.GardenCliUpdater(root_dir, "garden").update()
        assert os.path.exists("./releases/1234/garden")
        assert os.path.islink("./garden")
        assert os.readlink("./garden") == "./releases/1234/garden"
        with open("./releases/1234/garden", "r") as f:
            assert f.readlines() == ["some fake data"]


@pytest.mark.parametrize(
    ("all_versions", "current_version", "result_versions"),
    [
        ([1, 2, 3], 3, [2, 3]),
        ([1, 2, 3], 2, [1, 2]),
        ([1], 1, [1]),
    ],
)
def test_cleanup_old_binaries(all_versions, current_version, result_versions):
    cli_updater = updater.GardenCliUpdater(".", "garden")
    release_dir = "cli_releases"
    shutil.rmtree(release_dir, ignore_errors=True)
    for version in all_versions:
        os.makedirs(f"{release_dir}/{version}/subdir")
    cli_updater._garden_cli_releases_dir = release_dir
    cli_updater._cleanup_old_binaries(current_version)
    actual_versions = [int(f.name) for f in os.scandir(release_dir) if f.is_dir()]
    assert set(actual_versions) == set(result_versions)
