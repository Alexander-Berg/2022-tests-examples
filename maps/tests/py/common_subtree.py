''' This test checks that common task subtree for two requests
    launched simultaneously would be executed only once '''

import time

from maps.garden.sdk.core import Demands, Creates, TaskGraphBuilder, Task
from maps.garden.sdk.resources import PythonResource
from maps.garden.libs_server.graph.request_storage import RequestStatusString

from .atomic_counter import AtomicCounter
from .barrier import Barrier
from . import utils

FAILED = RequestStatusString.FAILED
IN_PROGRESS = RequestStatusString.IN_PROGRESS

first_request_started = Barrier(2, 10)
second_request_started = Barrier(2, 10)


class Wait(Task):
    def __call__(self, *args):
        first_request_started.wait()
        second_request_started.wait()


class Counter(Task):
    count = AtomicCounter()

    def __call__(self, *args):
        Counter.count.inc()


def fill_graph(graph_builder):
    """ Task graph that prepares data for single region """
    graph_builder.add_resource(
        PythonResource("wait_finished"),
    )
    graph_builder.add_resource(
        PythonResource("count_finished"),
    )

    graph_builder.add_task(
        Demands(),
        Creates("wait_finished"),
        Wait())
    graph_builder.add_task(
        Demands("wait_finished"),
        Creates("count_finished"),
        Counter())


def run_test(request_handler):
    input_name_to_version = {}
    target_names = ["wait_finished", "count_finished"]

    request_id1 = request_handler.handle(
        input_name_to_version, target_names)
    first_request_started.wait()

    request_id2 = request_handler.handle(
        input_name_to_version, target_names)
    second_request_started.wait()

    def running(request_id):
        return request_handler.status(request_id).string == IN_PROGRESS
    while running(request_id1) or running(request_id2):
        time.sleep(0.1)

    return request_id1, request_id2


def test_common_subtree(resource_storage):
    graph_builder = TaskGraphBuilder()
    fill_graph(graph_builder)

    task_handler = utils.UnittestTaskHandler.create_from_graph_builder(graph_builder, resource_storage)
    with utils.create_request_handler(graph_builder, task_handler) as request_handler:
        request_handler.enable_logging = True

        request_id1, request_id2 = run_test(request_handler)

        if request_handler.status(request_id1).string == FAILED or \
                request_handler.status(request_id2).string == FAILED:
            raise RuntimeError("One of requests failed")

    if Counter.count.value() != 1:
        msg = "Task was executed incorrect number of times.\n"
        msg += "Expected single execution, got {0}.".format(
            Counter.count.value())
        raise RuntimeError(msg)
