import time
import unittest

import mongomock

from maps.pylibs.utils.lib.common import wait_until

from maps.garden.sdk.core import (
    TaskGraphBuilder, Task, Demands, Creates, Version, GardenError
)
from maps.garden.sdk.resources import PythonResource
from maps.garden.sdk.utils.contour import default_contour_name

from maps.garden.libs_server.resource_storage.resource_meta import ResourceMeta
from maps.garden.libs_server.graph.request_storage import RequestStatusString
from maps.garden.libs_server.resource_storage.storage import ResourceStorage
from maps.garden.libs_server.test_utils.task_handler_stubs import EnvironmentSettingsProviderSimple

from . import utils

TASK_DURATION = 0.5


class ThrowableTask(Task):
    def predict_consumption(self, demands, creates):
        return {}

    def propagate_properties(self, demands, creates):
        creates[0]["number"] = 6 / demands[0]["number"]

    def __call__(self, source, target):
        time.sleep(TASK_DURATION)
        target.value = source.value * target.properties["number"]


class WaitingTask(ThrowableTask):
    BLOCK_TASKS = False

    def __call__(self, source, target):
        target.value = source.value * target.properties["number"]
        if not wait_until(lambda: not WaitingTask.BLOCK_TASKS):
            raise RuntimeError("Task waited for too long")


class FailingTask(Task):
    def predict_consumption(self, demands, creates):
        return {}

    def __call__(self, target):
        raise RuntimeError("Oops Привет, мир!")


class NonRemovableResource(PythonResource):
    def remove(self):
        raise RuntimeError("Sorry, I cannot be removed")


class DummyTask(Task):
    EXECUTED = False

    def predict_consumption(self, demands, creates):
        return {}

    def __call__(self, *args, **kwargs):
        global EXECUTED
        EXECUTED = True


class RequestHandlerTest(unittest.TestCase):
    def setUp(self):
        database = mongomock.MongoClient(tz_aware=True).db
        self._storage = ResourceStorage(
            database,
            environment_settings_provider=EnvironmentSettingsProviderSimple({}),
        )
        WaitingTask.BLOCK_TASKS = False
        DummyTask.EXECUTED = False

    def _fill_graph(self, graph_builder, resource_class=PythonResource, task_class=ThrowableTask):
        for name in ["source", "target", "next_target"]:
            graph_builder.add_resource(resource_class(name))

        graph_builder.add_task(Demands("source"), Creates("target"), task_class())
        graph_builder.add_task(Demands("target"), Creates("next_target"), DummyTask())

    def _fill_storage(self, graph_builder, numbers, name="source"):
        r = graph_builder.make_resource(name)
        r.value = 5

        for number in numbers:
            r.version = Version(properties={"number": number})
            self._storage.save(r)

    def _create_request_handler(self, graph_builder, contour_name=None):
        return utils.create_request_handler(
            graph_builder,
            utils.UnittestTaskHandler.create_from_graph_builder(graph_builder, self._storage),
            contour_name=contour_name
        )

    def _wait_until_request_status(self, request_handler, request_id, expected_status):
        def check():
            status = request_handler.status(request_id)
            if status.string == expected_status:
                return True

            self.assertEqual(status.string, RequestStatusString.IN_PROGRESS)
            return False

        self.assertTrue(wait_until(check))

    def test_async_working(self):
        graph_builder = TaskGraphBuilder()
        self._fill_graph(graph_builder, task_class=WaitingTask)
        self._fill_storage(graph_builder, [1, 2, 3])

        WaitingTask.BLOCK_TASKS = True

        with self._create_request_handler(graph_builder) as request_handler:
            request_ids = []
            for meta in self._storage.find_versions(name_pattern="source"):
                request_id = request_handler.handle(
                    input_name_to_version={meta.name: meta.version},
                    target_names=["target"]
                    )
                request_ids.append(request_id)

            def wait_for(status):
                return wait_until(
                    lambda: all(
                        request_handler.status(rid).string == status
                        for rid in request_ids
                    )
                )

            self.assertTrue(wait_for(RequestStatusString.IN_PROGRESS))
            WaitingTask.BLOCK_TASKS = False
            self.assertTrue(wait_for(RequestStatusString.COMPLETED))

        self.assertEqual(
            set(r.value for r in self._storage.find_resources(
                name_pattern="target")),
            set([5 * 6/1, 5 * 6/2, 5 * 6/3])
            )

    def test_scheduling_exception(self):
        graph_builder = TaskGraphBuilder()
        self._fill_graph(graph_builder)
        self._fill_storage(graph_builder, [0, 6])

        with self._create_request_handler(graph_builder) as request_handler:
            request_ids = []
            for meta in self._storage.find_versions(name_pattern="source"):
                try:
                    request_id = request_handler.handle(
                        input_name_to_version={meta.name: meta.version},
                        target_names=["target"]
                        )
                    request_ids.append(request_id)
                except GardenError:
                    pass
            self.assertEqual(len(request_ids), 1)

            wait_until(
                lambda:
                request_handler.status(request_ids[0]).string in
                [RequestStatusString.COMPLETED, RequestStatusString.FAILED])

            self.assertEqual(
                request_handler.status(request_ids[0]).string,
                RequestStatusString.COMPLETED
            )
            self.assertEqual(
                [r.value for r in self._storage.find_resources(
                    name_pattern="target")],
                [5 * 6/6]
            )

    def test_remove_request(self):
        graph_builder = TaskGraphBuilder()
        self._fill_graph(graph_builder)
        self._fill_storage(graph_builder, [1])

        with self._create_request_handler(graph_builder) as request_handler:
            request_id = []
            resource = next(iter(self._storage))[1]
            meta = ResourceMeta(resource.name, resource.version)
            request_id = request_handler.handle(
                input_name_to_version={meta.name: meta.version},
                target_names=["target"]
                )
            self.assertNotEqual(request_handler.status(request_id), None)
            request_handler.forget(request_id)
            self.assertRaises(KeyError, request_handler.status, request_id)

    def test_request_metas(self):
        graph_builder = TaskGraphBuilder()
        self._fill_graph(graph_builder)
        self._fill_storage(graph_builder, [1, 2, 3])
        sources = self._storage.find_versions(name_pattern="source")
        source = sources[0]

        with self._create_request_handler(graph_builder) as request_handler:
            output_resources_keys = dict()

            def lock_callback(req_id, keys):
                assert req_id not in output_resources_keys
                output_resources_keys[req_id] = keys.copy()

            def profile(req_id):
                """ Returns pairs with numbers of target and output
                    resources metas. """
                return [
                    len(request_handler.target_resources_specifiers(req_id)),
                    len(output_resources_keys[req_id])
                ]

            for correct_profile in [[2, 2], [2, 2]]:
                # During the first and second request two tasks are executed,
                request_id = request_handler.handle(
                    input_name_to_version={source.name: source.version},
                    target_names=["target", "next_target"],
                    lock_callback=lock_callback
                )
                self.assertEqual(profile(request_id), correct_profile)

                wait_until(lambda:
                           request_handler.status(request_id).string
                           != RequestStatusString.IN_PROGRESS)

    def test_failing_task(self):
        gb = TaskGraphBuilder()
        gb.add_resource(PythonResource("target"))
        gb.add_task(Demands(), Creates("target"), FailingTask())

        with self._create_request_handler(gb) as request_handler:
            error_callback_called = []

            def on_status_changed(request):
                if request.status.string == RequestStatusString.COMPLETED:
                    self.assertTrue(False)
                elif len(request.status.failed_tasks) > 0 or \
                        request.status.string == RequestStatusString.FAILED:
                    error_callback_called.append(request.id)
            request_handler.on_request_status_changed.add_callback(on_status_changed)

            request_id = request_handler.handle(
                input_name_to_version={},
                target_names=["target"])

            wait_until(lambda:
                       request_handler.status(request_id).string
                       != RequestStatusString.IN_PROGRESS)
            self.assertEqual(request_handler.status(request_id).string,
                             RequestStatusString.FAILED)

            self.assertTrue(request_id in error_callback_called)

    def test_on_completed_with_no_tasks(self):
        with self._create_request_handler(TaskGraphBuilder()) as request_handler:
            callback_called = []

            def on_completed(request):
                if request.status.string == RequestStatusString.COMPLETED:
                    callback_called.append(request.id)
            request_handler.on_request_status_changed.add_callback(on_completed)

            request_id = request_handler.handle(
                input_name_to_version={},
                target_names=[])

            self._wait_until_request_status(
                request_handler, request_id, RequestStatusString.COMPLETED)
            self.assertTrue(request_id in callback_called)

    def test_on_completed(self):
        gb = TaskGraphBuilder()
        gb.add_resource(PythonResource("target"))
        gb.add_task(Demands(), Creates("target"), DummyTask())

        with self._create_request_handler(gb) as request_handler:
            callback_called = []

            def on_completed(request):
                if request.status.string == RequestStatusString.COMPLETED:
                    callback_called.append(request.id)
            request_handler.on_request_status_changed.add_callback(on_completed)

            request_id = request_handler.handle(
                input_name_to_version={},
                target_names=["target"])

            self._wait_until_request_status(
                request_handler, request_id, RequestStatusString.COMPLETED)
            self.assertTrue(request_id in callback_called)

    def test_input_resource_error(self):
        graph_builder = TaskGraphBuilder()
        self._fill_graph(graph_builder, task_class=WaitingTask)
        for name in ["source2", "target2"]:
            graph_builder.add_resource(PythonResource(name))
        graph_builder.add_task(
            Demands("target", "source2"), Creates("target2"), DummyTask()
        )

        self._fill_storage(graph_builder, [1])
        self._fill_storage(graph_builder, [2], name="source2")

        WaitingTask.BLOCK_TASKS = True

        sources = self._storage.find_versions(name_pattern="source")
        self.assertEqual(len(sources), 1)
        source = sources[0]

        sources = self._storage.find_versions(name_pattern="source2")
        self.assertEqual(len(sources), 1)
        source2 = sources[0]

        with self._create_request_handler(graph_builder) as request_handler:
            request_id = request_handler.handle(
                input_name_to_version={
                    source.name: source.version,
                    source2.name: source2.version,
                },
                target_names=["target", "target2"]
            )

            self.assertTrue(
                wait_until(
                    lambda: request_handler.status(request_id).string == RequestStatusString.IN_PROGRESS
                )
            )

            self._storage.remove(metas=[source2])
            WaitingTask.BLOCK_TASKS = False

            self.assertTrue(
                wait_until(
                    lambda: request_handler.status(request_id).string == RequestStatusString.FAILED
                )
            )

            status = request_handler.status(request_id)
            self.assertEqual(len(status.failed_tasks), 1)
            self.assertTrue(
                "Error while loading demands for task" in status.failed_tasks[0].exception.message
            )
            self.assertTrue("DummyTask" in status.failed_tasks[0].task_name)

            assert not DummyTask.EXECUTED

    def test_handle_contour_build(self):
        resource_sets = []

        def on_completed(request):
            if request.status.string == RequestStatusString.COMPLETED:
                resource_sets.append({
                    specifier.resource_key
                    for specifier in request.target_specifiers
                })

        for contour_name in ("test_contour", default_contour_name()):
            gb = TaskGraphBuilder()
            gb.add_resource(PythonResource("target"))
            gb.add_task(Demands(), Creates("target"), DummyTask())

            with self._create_request_handler(gb, contour_name=contour_name) as request_handler:
                request_handler.on_request_status_changed.add_callback(on_completed)
                request_id = request_handler.handle(
                    input_name_to_version={},
                    target_names=["target"],
                    contour_name=contour_name)
                self._wait_until_request_status(
                    request_handler, request_id, RequestStatusString.COMPLETED)

        self.assertEqual(len(resource_sets), 2)
        different_keys = set()
        for resource_keys in resource_sets:
            self.assertEqual(len(resource_keys), 1)
            different_keys |= resource_keys
        self.assertEqual(len(different_keys), 2)
