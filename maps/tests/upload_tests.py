import dotmap
from unittest import mock

from maps.pylibs.yandex_environment import environment as yenv

from maps.garden.sdk.core import Version
from maps.garden.sdk.core import arguments
from maps.garden.sdk.resources import file
from maps.garden.sdk.utils import GB

from maps.garden.sdk.ecstatic import const
from maps.garden.sdk.ecstatic import resources
from maps.garden.sdk.ecstatic import tasks


DATASET_VERSION = "123"
TEST_DIRECTORY = "path/to/test-directory"


def _create_and_upload_dataset(hold):
    dataset_dir = dotmap.DotMap()
    dataset_dir.path = lambda: TEST_DIRECTORY

    resource = resources.DatasetResource(
        "noname1",
        "zzz:{tag}",
        dataset_version_template="{dataset_version}"
    )
    resource.version = Version(
        properties={
            "tag": "v1",
            "dataset_version": DATASET_VERSION
        }
    )

    task = tasks.UploadDatasetTask(branch=const.STABLE_BRANCH, hold=hold)
    task(dataset_dir, resource)

    assert resource._yandex_environment_string == str(yenv.get_yandex_environment())
    assert resource.physically_exists


def test_predict_consumption():
    resource = file.FileResource("noname", "nopath")
    resource.exists = True
    resource.size = {
        "bytes": 31337
    }
    resource.version = Version()
    demands = arguments.Demands(resource)
    creates = arguments.Creates()
    task = tasks.UploadDatasetTask(branch=const.STABLE_BRANCH)
    consumption = task.predict_consumption(demands, creates)
    assert consumption["tmpfs"]
    assert consumption["ram"] == 31337 + 10 * GB


@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.upload_dataset")
@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.dataset_versions")
def test_uploading_dataset_without_hold(versions_mock, upload_mock):
    versions_mock.return_value = """
zzz:v1=000
zzz:v1=123
"""

    _create_and_upload_dataset(hold=False)

    upload_mock.assert_called_once_with(
        "zzz:v1",
        DATASET_VERSION,
        TEST_DIRECTORY,
        branches=["+stable"]
    )


@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.upload_dataset")
@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.dataset_versions")
def test_uploading_dataset_with_hold(versions_mock, upload_mock):
    versions_mock.return_value = """
zzz:v1=000
zzz:v1=123
"""

    _create_and_upload_dataset(hold=True)

    upload_mock.assert_called_once_with(
        "zzz:v1",
        DATASET_VERSION,
        TEST_DIRECTORY,
        branches=["+stable/hold"]
    )
