""" Checks exception is raised when different resources have the same key. """
import unittest

import mongomock
import pytest

from maps.garden.sdk.core import (
    GardenError, TaskGraphBuilder, Task, Demands, Creates, Version)
from maps.garden.sdk.extensions.property_propagators import EnsureEqualProperties

from maps.garden.sdk.resources.python import PythonResource
from maps.garden.libs_server.resource_storage.storage import ResourceStorage
from maps.garden.libs_server.test_utils.task_handler_stubs import EnvironmentSettingsProviderSimple

from . import utils


class CustomKeyResource(PythonResource):
    def _calculate_key(self, version):
        return version.properties["key"]


class KeyPropagateTask(Task):
    def __call__(self, *args, **kwargs):
        pass
    propagate_properties = EnsureEqualProperties("key")


class MakeCollectionTask(Task):
    def __call__(self, *args, **kwargs):
        pass

    def propagate_properties(self, demands, creates):
        creates[0] = [{"key": 1}, {"key": 1}, {"key": 2}]


class KeysCollisionTest(unittest.TestCase):
    def setUp(self):
        self._graph_builder = TaskGraphBuilder()
        self._fill_graph()

        database = mongomock.MongoClient(tz_aware=True).db
        self._storage = ResourceStorage(
            database,
            environment_settings_provider=EnvironmentSettingsProviderSimple({}),
        )

    def _fill_graph(self):
        graph_builder = self._graph_builder

        add_resource = graph_builder.add_resource
        add_resource(PythonResource("in"))
        add_resource(CustomKeyResource("mid"))
        add_resource(CustomKeyResource("out"))
        add_resource(CustomKeyResource("collection"))
        add_resource(CustomKeyResource("runtime_collection"))

        add_task = graph_builder.add_task
        add_task(Demands("in"), Creates("mid"), KeyPropagateTask())
        add_task(Demands("mid"), Creates("out"), KeyPropagateTask())
        add_task(Demands("in"), Creates("collection"), MakeCollectionTask())

    def test_keys_collision(self):
        input_name_to_version = {"in": Version(properties={"key": 1})}

        with pytest.raises(GardenError):
            utils.execute_graph(
                self._graph_builder,
                storage=self._storage,
                input_name_to_version=input_name_to_version
            )

    def test_key_collision_in_collection(self):
        in_resource = self._graph_builder.make_resource("in")
        in_resource.version = Version(properties={"key": 1})
        self._storage.save(in_resource)
        input_name_to_version = {in_resource.name: in_resource.version}

        with pytest.raises(GardenError):
            utils.execute_graph(
                self._graph_builder,
                storage=self._storage,
                input_name_to_version=input_name_to_version
            )
