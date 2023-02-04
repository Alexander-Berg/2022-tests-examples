""" This test checks, that if task was not completed during scheduling
    (so it is present in request) but was accomplished before merge of
    the request in-to task queue, it must not be executed twice """

import threading
import time

from maps.garden.sdk.core import Demands, Creates, TaskGraphBuilder, Task
from maps.garden.sdk.resources import PythonResource
from maps.garden.sdk.utils.contour import default_contour_name

from maps.garden.libs_server.graph.request_storage import RequestStatusString
from maps.garden.libs_server.graph.graph_manager import BaseGraphManager, GraphInfo
from maps.garden.libs_server.graph.resources_transitions import resources_transitions
from maps.garden.libs_server.graph.graph_utils import ResourceVersionsPropagator

from .atomic_counter import AtomicCounter
from .barrier import Barrier
from . import utils

FAILED = RequestStatusString.FAILED
IN_PROGRESS = RequestStatusString.IN_PROGRESS

request_scheduled = Barrier(2, 10)
request_finished = Barrier(2, 10)


class VersionPropagatorForTest(ResourceVersionsPropagator):
    def __init__(self, *args):
        super().__init__(*args)
        self._request = 0

    def __getitem__(self, _):
        return self

    def propagate(self, initial_name_to_version, target_resources):
        result = super().propagate(initial_name_to_version, target_resources)
        if self._request > 0:
            # Notify other request, that scheduling phase is done
            request_scheduled.wait()
            # Wait until other request is finished
            request_finished.wait()
        self._request += 1
        return result


class GraphManager(BaseGraphManager):
    def __init__(self, graph_builder):
        self._graph_info = GraphInfo(
            graph_builder=graph_builder,
            resource_transitions=resources_transitions(graph_builder),
            resource_version_propagator=VersionPropagatorForTest(
                graph_builder,
                default_contour_name(),
            ),
        )

    def cleanup(self):
        pass

    def graph_info(self, *_args, **_kwargs):
        return self._graph_info


class Wait(Task):
    count = AtomicCounter()

    def __call__(self, *args):
        if Wait.count.value() == 0:
            request_scheduled.wait()
        Wait.count.inc()


class Counter(Task):
    count = AtomicCounter()

    def __call__(self, *args):
        Counter.count.inc()


def fill_graph(graph_builder):
    """ Task graph that prepares data for single region """
    graph_builder.add_resource(
        PythonResource("wait_finished"))
    graph_builder.add_resource(
        PythonResource("count_finished"))

    graph_builder.add_task(
        Demands(),
        Creates("wait_finished"),
        Wait())
    graph_builder.add_task(
        Demands("wait_finished"),
        Creates("count_finished"),
        Counter())


def is_running(request_handler, request_id):
    return request_handler.status(request_id).string == IN_PROGRESS


def wait_request(request_handler, request_id):
    while is_running(request_handler, request_id):
        time.sleep(0.1)
    request_finished.wait()


def run_test(request_handler):
    input_name_to_version = {}
    target_names = ["wait_finished", "count_finished"]

    request_id1 = request_handler.handle(
        input_name_to_version, target_names)
    # request_handler.ready()
    t = threading.Thread(target=wait_request, args=(request_handler, request_id1))
    t.start()
    request_id2 = request_handler.handle(
        input_name_to_version, target_names)

    while is_running(request_handler, request_id2):
        time.sleep(0.1)

    return request_id1, request_id2


def test_task_queue(resource_storage):
    graph_builder = TaskGraphBuilder()
    fill_graph(graph_builder)

    task_handler = utils.UnittestTaskHandler.create_from_graph_builder(graph_builder, resource_storage)
    with utils.create_request_handler_from_graph_manager(
        GraphManager(graph_builder),
        task_handler,
        utils.StubMonitor()
    ) as request_handler:
        request_handler.enable_logging = True

        request_id1, request_id2 = run_test(request_handler)

        assert request_handler.status(request_id1).string != FAILED
        assert request_handler.status(request_id2).string != FAILED

    assert Counter.count.value() == 1, """
        Task was executed incorrect number of times.
        Expected single execution, got {0}.""".format(Counter.count.value())
