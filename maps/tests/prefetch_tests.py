from unittest import mock
import os

import pytest

from maps.garden.sdk.core import Version, Demands

from maps.garden.sdk.ecstatic import resources
from maps.garden.sdk.ecstatic import tasks


class DatasetDependentTask(tasks.PrefetchingTaskBase):
    def __call__(self, *args, **kwargs):
        # Do not do anything
        pass


@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.dataset_versions")
@mock.patch("maps.infra.ecstatic.tool.ecstatic_api.ecstatic_api.EcstaticAPI.download_datasets")
def test_dataset_download(download_mock, versions_mock):
    resource = resources.DatasetResource(
        name="a_dataset",
        dataset_name_template="dataset_name",
        dataset_version_template="1"
    )
    resource._version = Version(properties={})
    resource.save_yandex_environment()
    resource.exists = True

    versions_mock.return_value = "dataset_name=1"
    # Emulate resource downloading by creating empty directory
    download_mock.side_effect = lambda paths: os.mkdir(paths[0][2])

    task = DatasetDependentTask()
    assert task._tmp_dir is None

    task.ensure_available(Demands(resource))
    assert os.path.isdir(task._tmp_dir)
    expected_save_dir = os.path.join(task._tmp_dir, "dataset_name_1")
    download_mock.assert_called_once_with(
        [("dataset_name", "1", expected_save_dir)]
    )
    assert os.path.isdir(resource.path())
    assert resource.path() == expected_save_dir

    resource.clean()
    with pytest.raises(AssertionError):
        resource.path()

    task.clean(demands=None, creates=None)
    assert not os.path.exists(task._tmp_dir)
