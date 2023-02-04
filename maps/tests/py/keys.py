import unittest

import mongomock

from maps.garden.sdk.core import TaskGraphBuilder, Task, Demands, Creates, Version
from maps.garden.sdk.resources import PythonResource

from maps.garden.libs_server.resource_storage.storage import ResourceStorage
from maps.garden.libs_server.test_utils.task_handler_stubs import EnvironmentSettingsProviderSimple

from . import utils


class NameResource(PythonResource):
    def __init__(self, name, hash_string):
        super(NameResource, self).__init__(name)
        self.version = Version(hash_string=hash_string)

    def _calculate_key(self, version):
        return self.name

    def __str__(self):
        return str((self.name, self.version))

    def __eq__(self, other):
        return repr(self) == repr(other)

    def __repr__(self):
        return '{0}(name="{1}", hash_string="{2}")'.format(
            type(self).__name__, self.name, self.version.hash())


class PassTask(Task):
    def __call__(self, *args, **kwargs):
        pass


class IntegrationTest(unittest.TestCase):
    def setUp(self):
        self._graph_builder = TaskGraphBuilder()
        self._saved = NameResource('outcome_name', 'some_hash')

        database = mongomock.MongoClient(tz_aware=True).db
        self._storage = ResourceStorage(
            database,
            environment_settings_provider=EnvironmentSettingsProviderSimple({}),
        )
        self._storage.save(self._saved)

    def _fill_graph(self):
        self._income = NameResource('income_name', 'hash_in')
        self._outcome_res = NameResource('outcome_name', 'hash_out')
        self.assertEqual(self._saved.key, self._outcome_res.key)
        self._storage.save(self._income)
        self._graph_builder.add_resource(self._income)
        self._graph_builder.add_resource(self._outcome_res)

        self._graph_builder.add_task(Demands(self._income.name),
                                     Creates(self._outcome_res.name),
                                     PassTask())

    def test_key_replacement(self):
        self._fill_graph()
        input_name_to_version = {self._income.name: self._income.version}
        utils.execute_graph(
            self._graph_builder,
            storage=self._storage,
            input_name_to_version=input_name_to_version,
            target_names=[self._outcome_res.name]
        )
        assert self._saved.version != self._storage[self._saved.key].version


class KeyIntegrationTest(unittest.TestCase):
    def setUp(self):
        self._graph_builder = TaskGraphBuilder()

        database = mongomock.MongoClient(tz_aware=True).db
        self._storage = ResourceStorage(
            database,
            environment_settings_provider=EnvironmentSettingsProviderSimple({}),
        )

    def _fill_graph(self):
        def add_resource(resource):
            self._graph_builder.add_resource(resource)

        self._source = PythonResource('source')
        self._first_mutable = NameResource('first', 'hash_first')
        self._second_mutable = NameResource('second', 'hash_second')
        self._flow = PythonResource('flow')

        add_resource(self._source)
        add_resource(self._first_mutable)
        add_resource(self._second_mutable)
        add_resource(self._flow)

        self._graph_builder.add_task(Demands(self._source.name),
                                     Creates(self._first_mutable.name),
                                     PassTask())
        self._graph_builder.add_task(Demands(self._first_mutable.name),
                                     Creates(self._second_mutable.name),
                                     PassTask())
        self._graph_builder.add_task(Demands(self._second_mutable.name),
                                     Creates(self._flow.name),
                                     PassTask())

    def _one_run(self, source_version):
        input_name_to_version = {'source': source_version}
        utils.execute_graph(
            self._graph_builder,
            storage=self._storage,
            input_name_to_version=input_name_to_version,
            target_names=[self._first_mutable.name, self._second_mutable.name, self._flow.name]
        )

    def test_multi_launch(self):
        self._fill_graph()
        REQUEST_COUNT = 4
        hashes = ['hash_{0}'.format(index) for index in range(REQUEST_COUNT)]
        for hash_string in hashes:
            version = Version(hash_string=hash_string)
            version.key = self._source.calculate_key(version)

            self._source.version = version
            self._storage.save(self._source)
            self._one_run(version)

            # Let's assert that the mutable resource version has changed
            new_second_mutable = self._storage[self._second_mutable.key]
            assert new_second_mutable.version != self._second_mutable.version
            self._second_mutable = new_second_mutable

        flow_versions = self._storage.find_versions(
            name_pattern=self._flow.name)
        assert len(flow_versions) == REQUEST_COUNT
