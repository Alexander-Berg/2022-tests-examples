import time
import unittest
import threading
from functools import wraps

import mongomock

from maps.garden.sdk.core import (
    TaskGraphBuilder, Task, Demands, Creates, Version, Resource,
    GardenError)
from maps.garden.libs_server.test_utils.task_handler_stubs import EnvironmentSettingsProviderSimple
from maps.garden.sdk.core.resource import proto_serializable
from maps.garden.libs_server.resource_storage.storage import ResourceStorage

from .atomic_counter import AtomicCounter
from . import utils


class RaisingThread(threading.Thread):
    def __init__(self, group=None, target=None, name=None, args=(), kwargs={}):
        super(RaisingThread, self).__init__(
            group=group, name=name)
        self._target = target
        self._args = args
        self._kwargs = kwargs
        self.error = None
        self.value = None

    def run(self):
        try:
            self.value = self._target(*self._args, **self._kwargs)
        except Exception as e:
            self.error = e


def timeout(seconds=1):
    class TimeoutError(Exception):
        pass

    def decorator(func):
        def wrapper(*args, **kwargs):
            thread = RaisingThread(target=func, args=args, kwargs=kwargs)
            thread.daemon = True
            thread.start()
            thread.join(timeout=seconds)
            if thread.is_alive():
                raise TimeoutError()
            if thread.error is not None:
                raise thread.error
            return thread.value

        return wraps(func)(wrapper)
    return decorator


class DummyResource(Resource):
    def remove(self):
        pass

    def _commit(self):
        pass

    def clean(self):
        pass


@proto_serializable()
class SingletonDummyResource(DummyResource):
    TYPE = "core:singleton"

    def _calculate_key(self, version):
        return self.name

    @classmethod
    def _from_proto(cls, proto_resource):
        return SingletonDummyResource(proto_resource.name)


class NamedTask(Task):
    def predict_consumption(self, demands, creates):
        return {"cpu": 1}

    def __init__(self, name):
        super(NamedTask, self).__init__()
        self.name = name


class LocksTest(unittest.TestCase):
    def setUp(self):
        database = mongomock.MongoClient(tz_aware=True).db
        self._storage = ResourceStorage(
            database,
            environment_settings_provider=EnvironmentSettingsProviderSimple({}),
        )
        self._graph_builder = TaskGraphBuilder()

    def _build_simple_linear_graph(self, graph_builder, task_type, name):
        graph_builder.add_resource(DummyResource("input"))
        graph_builder.add_resource(DummyResource("inter"))
        graph_builder.add_resource(DummyResource("final"))
        graph_builder.add_task(Demands("input"), Creates("inter"),
                               task_type(name + str(1)))
        graph_builder.add_task(Demands("inter"), Creates("final"),
                               task_type(name + str(2)))

    def _build_simple_linear_graph_with_singleton(
            self, graph_builder, first_task_type, second_task_type):
        graph_builder.add_resource(DummyResource("input"))
        graph_builder.add_resource(SingletonDummyResource("inter"))
        graph_builder.add_resource(DummyResource("final"))
        graph_builder.add_task(Demands("input"), Creates("inter"),
                               first_task_type())
        graph_builder.add_task(Demands("inter"), Creates("final"),
                               second_task_type())

    def _build_fork_graph(self, graph_builder, task_type, name, size):
        graph_builder.add_resource(DummyResource("input"))
        graph_builder.add_resource(SingletonDummyResource("inter"))
        for i in range(size):
            graph_builder.add_resource(DummyResource("final" + str(i)))

        graph_builder.add_task(Demands("input"), Creates("inter"),
                               task_type(name + str(0)))
        for i in range(size):
            graph_builder.add_task(
                Demands("inter"),
                Creates("final" + str(i)),
                task_type(name + str(i + 1)))

    def _save(self, resources):
        for resource in resources:
            self._storage.save(resource)

    def _test_requests(self, target_names=["inter", "final"], times=1, shift=0):
        task_handler = utils.UnittestTaskHandler.create_from_graph_builder(self._graph_builder, self._storage)
        with utils.create_request_handler(self._graph_builder, task_handler) as request_handler:
            threads = []
            for i in range(times):
                input_resource = self._graph_builder.make_resource("input")
                input_resource.version = Version(str(shift + i))
                input_resource.version.key = input_resource.calculate_key()
                self._storage.save(input_resource)
                input_name_to_version = {
                    input_resource.name: input_resource.version}
                thread = RaisingThread(
                    target=self._check_result,
                    args=(request_handler, input_name_to_version,
                          target_names))
                thread.start()
                threads.append(thread)
            for thread in threads:
                thread.join()
                if thread.error is not None:
                    raise thread.error

    @timeout(10)
    def _check_result(self, request_handler, input_name_to_version,
                      target_names):
        result = utils.build_resources(
            input_name_to_version,
            target_names,
            self._storage,
            request_handler,
        )
        self.assertEqual(set(target_names),
                         set([r.name for _, r in result.items()]))
        for resource_meta, resource in result.items():
            key = resource_meta.key
            self.assertTrue(
                resource is not None,
                f"There is no output resource with key {key} in the storage"
            )
            self.assertEqual(
                Version._encode(resource.calculate_key()), key,
                f"Output resource key {Version._encode(resource.calculate_key())} does not equal to "
                f"resource key {key} saved to storage."
            )

    def test_simple(self):
        class PassTask(NamedTask):
            def __call__(self, *args, **kwargs):
                pass
        self._build_simple_linear_graph(self._graph_builder, PassTask, "task")
        self._test_requests(times=10)

    def test_one_singleton(self):
        counter = AtomicCounter()

        class FirstSleepTask(Task):
            def __call__(self, *args, **kwargs):
                if not counter.increment_if(lambda x: x == 0):
                    raise GardenError("Cannot run first task when other "
                                      "tasks are running")
                time.sleep(0.001)
                counter.dec()

        class SecondSleepTask(Task):
            def __call__(self, *args, **kwargs):
                if not counter.increment_if(lambda x: True, value=-1):
                    raise GardenError("Cannot run second task when "
                                      "first task is running")
                time.sleep(0.001)
                counter.inc()

        self._build_simple_linear_graph_with_singleton(
            self._graph_builder, FirstSleepTask, SecondSleepTask)
        self._test_requests(times=10)
        self._test_requests(times=10, shift=10)
