import unittest
from contextlib import contextmanager
import threading
import copy

import mongomock

from maps.pylibs.utils.lib.common import wait_until

from maps.garden.sdk.core import (
    TaskGraphBuilder, Task, Demands, Creates, Version
)
from maps.garden.sdk.resources import PythonResource
from maps.garden.sdk.utils.contour import default_contour_name
from maps.garden.sdk.extensions.property_propagators import EnsureEqualProperties

from maps.garden.libs_server.graph.graph_utils import ResourceVersionsPropagator
from maps.garden.libs_server.graph.task_queue import TaskQueue
from maps.garden.libs_server.resource_storage.storage import ResourceStorage
from maps.garden.libs_server.graph.request_handler import TaskDB
from maps.garden.libs_server.test_utils.task_handler_stubs import EnvironmentSettingsProviderSimple

from . import utils


class StorageDeleting(ResourceStorage):

    def __init__(self, collection):
        super().__init__(
            collection,
            environment_settings_provider=EnvironmentSettingsProviderSimple({}),
        )
        self.deleting_keys = set()

    def resources_status(self, keys):
        keys = set(keys)
        actual_keys = list(keys - self.deleting_keys)

        metas, removing = super().resources_status(actual_keys)
        removing.update(k for k in self.deleting_keys if k in keys)

        return metas, removing


class ResourceForTest(PythonResource):
    pass


class SomeTask(Task):
    def predict_consumption(self, demands, creates):
        return {}

    propagate_properties = EnsureEqualProperties(["region", "counter"])

    def __init__(self, code):
        Task.__init__(self)
        self.code = code

    def __call__(self, *args):
        return


class TaskHandlerTest(utils.UnittestTaskHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.reset()

    def reset(self):
        self.started_tasks = []
        self.finished_tasks = []

    def execute_if_has_slot(self, versioned_task, input_resources):
        code = versioned_task.task.code
        assert code not in self.started_tasks
        self.started_tasks.append(code)
        return super().execute_if_has_slot(versioned_task, input_resources)

    def pop_finished(self):
        ret = super().pop_finished()

        for versioned_task, err in ret:
            if err:
                raise RuntimeError("Task failed: {0}".format(err))

            code = versioned_task.task.code
            assert code in self.started_tasks
            assert code not in self.finished_tasks
            self.finished_tasks.append(code)

        return ret


class EmptyTaskHandler:
    def __init__(self, resoure_storage: ResourceStorage):
        self.finished_condition = threading.Condition()
        self.resource_storage = resoure_storage

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        pass

    def execute_if_has_slot(self, versioned_task, input_resources):
        return True

    def pop_finished(self):
        return []


def get_versioned_tasks(tasks):
    ret = dict()

    for task in tasks:
        code = task.task.code
        assert code not in ret
        ret[code] = task

    return ret


class TaskStatusTest(unittest.TestCase):
    def setUp(self):
        gb = self._gb = TaskGraphBuilder()
        database = mongomock.MongoClient(tz_aware=True).db
        self._storage = StorageDeleting(database["resources"])

        resources = [
            "res_initial", "res0", "res1", "res21", "res22", "res23", "res31", "res32"
        ]
        for name in resources:
            res = ResourceForTest(name)
            gb.add_resource(res)
            setattr(self, "_" + name, res)

        self._tasks = [None] * 6
        for i in range(len(self._tasks)):
            self._tasks[i] = SomeTask(i)

        gb.add_task(Demands("res_initial"), Creates("res0"), self._tasks[0])
        gb.add_task(Demands("res0"), Creates("res1"), self._tasks[1])
        gb.add_task(Demands("res1"), Creates("res21", "res22"), self._tasks[2])
        gb.add_task(Demands("res21"), Creates("res31"), self._tasks[3])
        gb.add_task(Demands("res22"), Creates("res32"), self._tasks[4])
        gb.add_task(Demands("res1"), Creates("res23"), self._tasks[5])

        self._task_handler = TaskHandlerTest.create_from_graph_builder(self._gb, self._storage)

        self._empty_task_handler = EmptyTaskHandler(self._storage)

        self._taskdb = TaskDB(TaskQueue(), utils.StubMonitor())

    @contextmanager
    def _create_request_handler(self, task_handler):
        with utils.create_request_handler(self._gb, task_handler) as request_handler:
            request_handler.enable_logging = True
            yield request_handler

    def _save(self, res, counter=1):
        res.version = Version(
            properties={
                "region": "abc",
                "counter": counter
            }
        )
        self._storage.save(res)

    def _build_targets(self, request_handler, target_names):
        self._task_handler.reset()

        request_id = request_handler.handle(
            input_name_to_version={
                "res_initial": self._storage.meta(self._res_initial.key).version
            },
            target_names=target_names
        )

        return request_id

    def _task_status(self, task_codes):
        with self._taskdb._lock:
            task_info = copy.deepcopy(self._taskdb._task_info)
        statuses = set(
            info.state for task, info in task_info.items()
            if task.task.code in task_codes
        )

        assert len(statuses) == 1, "{0}\n{1}".format(task_info, statuses)
        return statuses.pop()

    def _storage_res(self, name):
        resources = self._storage.find_resources(name_pattern=name)
        assert len(resources) == 1

        return resources[0]

    def test_tasks_getter(self):
        self._save(self._res_initial)

        with self._create_request_handler(self._empty_task_handler) as request_handler:
            self._build_targets(request_handler, ["res0", "res1", "res21", "res22"])
            self.assertEqual(len(
                request_handler.tasks(
                    filter=lambda tasks: True,
                    limit=100)),
                3)

            self.assertEqual(len(
                request_handler.tasks(
                    filter=lambda tasks: True,
                    limit=2)),
                2)

            filtered_tasks = request_handler.tasks(
                filter=lambda task: task.task.code == 1,
                limit=2)
            self.assertEqual(len(filtered_tasks), 1)
            self.assertEqual(filtered_tasks[0].task.code, 1)

    def test_task_db(self):
        self._save(self._res_initial)

        propagator = ResourceVersionsPropagator(
            self._gb,
            default_contour_name()
        )

        input_name_to_version = {
            "res_initial": self._storage.meta(self._res_initial.key).version
        }

        for i, target in enumerate(["res31", "res32", "res23"]):
            tasks_graph, _, _ = propagator.propagate(
                input_name_to_version,
                [target, "res21", "res22", "res1", "res0"]
            )

            self._taskdb.add_request(i, tasks_graph)

        tasks = self._taskdb.tasks(lambda task: True)
        vtasks = get_versioned_tasks(tasks)
        self.assertEqual(len(tasks), 6)
        self.assertTrue(self._task_status(range(6)), "waiting")

        ready = self._taskdb.get_ready_tasks()
        self.assertEqual(len(ready), 1)
        self.assertEqual(ready[0].task.code, 0)
        self.assertEqual(self._task_status([0]), "ready")
        self.assertEqual(self._task_status(range(1, 6)), "waiting")

        self.assertFalse(self._taskdb.finish_task_success(vtasks[3]))
        self.assertEqual(self._task_status([3]), "waiting")

        self._taskdb.cancel_request(0, lambda x: True)
        self.assertEqual(len(self._taskdb.tasks(lambda task: True)), 5)
        self.assertEqual(self._task_status([0]), "ready")
        self.assertEqual(self._task_status(range(1, 5)), "waiting")
        self.assertEqual(self._taskdb.request_task_count(0), 0)
        self.assertEqual(self._taskdb.request_task_count(1), 4)

        self.assertTrue(self._taskdb.finish_task_success(vtasks[0]))
        self.assertEqual(len(self._taskdb.tasks(lambda task: True)), 4)

        ready = self._taskdb.get_ready_tasks()
        self.assertEqual(len(ready), 1)
        self.assertEqual(self._task_status([1]), "ready")

        run_cnt = [0]

        def mk_run_callback(ret):
            def f(task):
                run_cnt[0] += 1
                return ret

            return f

        self._taskdb.run_task(vtasks[1], mk_run_callback(False))
        self.assertEqual(run_cnt, [1])
        self.assertEqual(self._task_status([1]), "ready")

        self._taskdb.run_task(vtasks[1], mk_run_callback(True))
        self.assertEqual(run_cnt, [2])
        self.assertEqual(self._task_status([1]), "running")

        self.assertTrue(self._taskdb.finish_task_success(vtasks[1]))

        ready = self._taskdb.get_ready_tasks()
        self.assertEqual(len(ready), 2)
        self.assertEqual(len(self._taskdb.tasks(lambda task: True)), 3)
        self.assertEqual(self._task_status([2, 5]), "ready")
        self.assertEqual(self._task_status([4]), "waiting")

        self.assertTrue(self._taskdb.finish_task_error(vtasks[2]))
        self.assertEqual(len(self._taskdb.tasks(lambda task: True)), 1)
        self.assertEqual(self._task_status([5]), "ready")
        self.assertEqual(self._taskdb.request_task_count(1), 0)
        self.assertEqual(self._taskdb.request_task_count(2), 1)

        self.assertTrue(self._taskdb.finish_task_success(vtasks[5]))
        self.assertEqual(self._taskdb.request_task_count(2), 0)
        self.assertEqual(len(self._taskdb.tasks(lambda task: True)), 0)

    def test_check_tasks_status(self):
        self._save(self._res_initial)

        with self._create_request_handler(self._task_handler) as request_handler:
            # First request
            self._build_targets(request_handler, ["res0", "res1", "res21", "res22"])

            wait_until(lambda: 2 in self._task_handler.finished_tasks)
            self.assertTrue(0 in self._task_handler.finished_tasks)
            self.assertTrue(1 in self._task_handler.finished_tasks)
            self.assertTrue(2 in self._task_handler.finished_tasks)

            self._storage.remove_by_key(self._storage_res("res1").key)

            # Second request

            key21 = self._storage_res("res21").key
            self._storage.deleting_keys.add(key21)

            _, deleting = self._storage.resources_status([key21])
            self.assertTrue(key21 in deleting)

            self._build_targets(request_handler, ["res0", "res1", "res21", "res31", "res32", "res23"])

            wait_until(lambda: 5 in self._task_handler.finished_tasks)

            self.assertEqual(self._task_handler.finished_tasks, [1, 5])

            self._storage.deleting_keys.remove(key21)

            wait_until(lambda: 3 in self._task_handler.finished_tasks)
            wait_until(lambda: 4 in self._task_handler.finished_tasks)

            self.assertTrue(0 not in self._task_handler.started_tasks)
            self.assertTrue(2 not in self._task_handler.started_tasks)


if __name__ == "__main__":
    unittest.main()
