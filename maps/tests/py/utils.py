import sys
import time
import itertools
from contextlib import contextmanager
import mongomock

from maps.garden.sdk.core import Version
from maps.garden.sdk.utils.contour import default_contour_name

from maps.garden.libs_server.graph.task_queue import TaskQueue
from maps.garden.libs_server.resource_storage.storage import ResourceStorage
from maps.garden.libs_server.graph.request_handler import RequestHandler
from maps.garden.libs_server.graph.request_storage import RequestStatusString, RAMRequestStorage
from maps.garden.libs_server.test_utils.task_handler import UnittestTaskHandler
from maps.garden.libs_server.test_utils.task_handler_stubs import ModuleGraphManager, EnvironmentSettingsProviderSimple


class StubMonitor:
    """
    TODO: this function is only used in tests. Move it to libs/test_utils.
    """
    def notify_waiting(*args, **kwargs):
        pass

    def notify_running(*args, **kwargs):
        pass

    def notify_finished(*args, **kwargs):
        pass

    def notify_pending(*args, **kwargs):
        pass


def build_resources_nodaemon(request_handler_context, task_handler, *args, **kwargs):
    """
    Similar to build_resources, but revokes all unfinished tasks on exit
    TODO: this function is only used in tests. Move it to libs/test_utils.
    """
    with request_handler_context as request_handler:
        try:
            return build_resources(*args, request_handler=request_handler, **kwargs)
        finally:
            task_handler.remove_all_tasks()


def build_resources(
        input_name_to_version,
        target_names, storage, request_handler):
    """
    Convenient function for executing graph with any request_handler
    TODO: this function is only used in tests. Move it to libs/test_utils.
    """
    PROGRESS_PRINT_INTERVAL_SEC = 10
    ITERATION_SLEEP_TIME_SEC = 0.1
    progress_print_interval = int(PROGRESS_PRINT_INTERVAL_SEC / ITERATION_SLEEP_TIME_SEC)

    request_handler.enable_logging = True
    request_id = request_handler.handle(input_name_to_version, target_names)
    previous_remaining_tasks_number = None
    for iteration_number in itertools.count():
        status = request_handler.status(request_id)
        if (iteration_number % progress_print_interval == 0 and
                previous_remaining_tasks_number != status.remaining_tasks_number):
            print(
                "Progress: {0:.4}% ({1} tasks remaining)".format(
                    status.progress,
                    status.remaining_tasks_number),
                file=sys.stderr)
            previous_remaining_tasks_number = status.remaining_tasks_number

        if status.failed_tasks:
            # TODO: improve error delivery to the users
            raise RuntimeError(status.failed_tasks[0].exception.message)

        if status.string != RequestStatusString.IN_PROGRESS:
            break

        time.sleep(ITERATION_SLEEP_TIME_SEC)

    return dict(
        (resource_meta, storage.get(resource_meta.key))
        for resource_meta in request_handler.target_resources_specifiers(request_id))


@contextmanager
def create_request_handler(graph_builder,
                           task_handler,
                           task_queue=None,
                           request_storage=None,
                           contour_name=None):
    """ Creates an instance of request handler setup with task
        handler and default task queue and scheduler and RAM request storage.
        TODO: this function is only used in tests. Move it to libs/test_utils.
    """
    with create_request_handler_from_graph_manager(
            ModuleGraphManager(graph_builder, contour_name or default_contour_name()),
            task_handler,
            StubMonitor(),
            task_queue=task_queue,
            request_storage=request_storage) as request_handler:
        yield request_handler


@contextmanager
def create_request_handler_from_graph_manager(
        graph_manager,
        task_handler,
        task_monitor=None,
        task_queue=None,
        request_storage=None):
    """
    Creates an instance of request handler setup with task
    handler and default task queue and scheduler and RAM request storage.
    TODO: this function is (almost) only used in tests. Move it to libs/test_utils.
    """
    if request_storage is None:
        request_storage = RAMRequestStorage()
    if task_queue is None:
        task_queue = TaskQueue()
    if task_monitor is None:
        task_monitor = StubMonitor()

    with RequestHandler(task_handler, task_queue, graph_manager, request_storage, task_monitor) as request_handler:
        yield request_handler


def execute_graph(
    graph_builder,
    storage=None,
    request_storage=None,
    input_name_to_version=None,
    target_names=None,
):
    if not storage:
        database = mongomock.MongoClient(tz_aware=True).db
        storage = ResourceStorage(
            database,
            environment_settings_provider=EnvironmentSettingsProviderSimple({}),
        )

    target_names = target_names or graph_builder.output_resources()

    if not input_name_to_version:
        input_name_to_version = {
            r.name: r.version
            for r in storage.find_versions()
        }

    with UnittestTaskHandler.create_from_graph_builder(graph_builder, storage) as task_handler:
        request_handler = create_request_handler(
            graph_builder,
            task_handler,
            request_storage=request_storage
        )

        return build_resources_nodaemon(
            request_handler,
            task_handler,
            input_name_to_version=input_name_to_version,
            target_names=target_names,
            storage=storage
        )


def check_result(result):
    """
    Args:
        result (dict): Mapping from a resource meta to a resource.
    Raises RuntimeError if some resources are empty.
    """
    missing_resources = [
        meta
        for meta, resource in result.items()
        if resource is None and not (
            isinstance(meta.version, Version) and
            "is_empty" in meta.version.properties
        )
    ]

    if missing_resources:
        raise RuntimeError("Some output resources missing: {0}".format(missing_resources))
