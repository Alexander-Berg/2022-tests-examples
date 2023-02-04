from unittest import mock
import os

import pytest

from maps.pylibs.utils.lib.system import create_dir

from maps.garden.sdk.core import Demands, Creates, Task, Resource, OptionalResource, Version
from maps.garden.sdk.resources import (
    FileResource, DirResource, UrlResource, PythonResource
)
from maps.garden.sdk.test_utils.graph import (
    GraphCook,
    execute,
)
from maps.garden.sdk.test_utils.resources import make_resources_accessible


class EmptyTask(Task):
    def __call__(self, *args, **kwargs):
        pass


class ConvertUrlToDirTask(Task):
    def __init__(self, dir_files):
        self._dir_files = dir_files
        Task.__init__(self)

    def __call__(self, url, dir):
        for rel_path in self._dir_files:
            path = os.path.join(dir.path(), rel_path)
            create_dir(os.path.dirname(path))

            with open(path, "w") as f:
                f.write(url.url)


class ConvertUrlToFileTask(Task):
    def __call__(self, url, file):
        with file.open("w") as f:
            f.write(url.url)


class PredictConsumptionTask(Task):
    def __init__(self, consumption):
        self._consumption = consumption
        super(PredictConsumptionTask, self).__init__()

    def predict_consumption(self, demands, creates):
        return self._consumption

    def __call__(self, *args, **kwargs):
        pass


def test_cook_module_with_existing_input_resources(environment_settings):
    cook = GraphCook(environment_settings)
    cook.target_builder().add_resource(
        UrlResource("url", "localhost"))
    cook.target_builder().add_resource(
        FileResource("file", "file_name"))
    cook.create_input_resource("url").version = Version()
    cook.target_builder().add_task(
        Demands("url"),
        Creates("file"),
        ConvertUrlToFileTask())
    result = execute(cook)
    assert result["file"].physically_exists


def test_cook_failure_on_resource_in_multiple_builders():
    cook = GraphCook({})
    cook.input_builder().add_resource(
        UrlResource("url", "localhost"))
    cook.target_builder().add_resource(
        UrlResource("url", "localhost"))
    with pytest.raises(Exception):
        cook.create_input_resource("url")


def test_input_file_resource(environment_settings):
    cook = GraphCook(environment_settings)
    cook.target_builder().add_resource(
        PythonResource("result"))
    cook.target_builder().add_resource(
        FileResource("file", "file_name"))

    file = cook.create_input_resource("file")
    file.version = Version()
    with file.open() as f:
        f.write(b"data")

    cook.target_builder().add_task(
        Demands("file"),
        Creates("result"),
        EmptyTask())
    execute(cook)


def test_resource_availability(environment_settings):
    file_data = "localhost_aaa"
    dir_files = ("subd/a", "bfile")

    cook = GraphCook(environment_settings)
    cook.target_builder().add_resource(
        UrlResource("url", file_data)
    )

    cook.target_builder().add_resource(
        FileResource("file", "some/file_name")
    )

    cook.target_builder().add_resource(
        DirResource("dir", "some/dir_name")
    )

    cook.create_input_resource("url").version = Version()

    cook.target_builder().add_task(
        Demands("url"),
        Creates("file"),
        ConvertUrlToFileTask()
    )

    cook.target_builder().add_task(
        Demands("url"),
        Creates("dir"),
        ConvertUrlToDirTask(dir_files)
    )

    result = execute(cook)

    resources = [
        v for k, v in result.items()
        if k in ["file", "dir"]
    ]

    with make_resources_accessible(environment_settings, *resources):
        for res in resources:
            if isinstance(res, FileResource):
                assert res.open("r").read() == file_data
            else:
                for rel_path in dir_files:
                    path = os.path.join(res.path(), rel_path)
                    assert open(path, "r").read() == file_data


def test_create_resources_with_version(environment_settings):
    cook = GraphCook(environment_settings)

    def _check_resource_creation(name, resource_wrapper=lambda x: x):
        # create resource
        created_resource = Resource(name)
        created_resource.load_environment_settings = mock.MagicMock()
        cook.target_builder().add_resource(resource_wrapper(created_resource))
        version = Version()
        resource = cook.create_input_resource(name, version=version)
        assert resource.version == version
        resource.load_environment_settings.assert_called_once_with(environment_settings)

    _check_resource_creation("basic_resource")
    _check_resource_creation("optional_resource", lambda r: OptionalResource(r))
