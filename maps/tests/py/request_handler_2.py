import time

from maps.garden.sdk.core import (
    MutagenGraphBuilder, TaskGraphBuilder, Task, Demands, Creates, Version
)
from maps.garden.sdk.resources.python import PythonResource

from maps.garden.libs_server.graph.request_storage import MongoRequestStorage, RequestStatusString

from .barrier import Barrier
from . import utils


class AddTask(Task):
    def propagate_properties(self, demands, creates):
        target_addition = sum(source["addition"]
                              for source in demands.values())
        for target in creates.values():
            target["addition"] = target_addition

    def __call__(self, source, target):
        target.value = source.value + source.version.properties["addition"]
        time.sleep(0.1)


class StartupTask(AddTask):
    barrier = Barrier(3, timeout=2)

    def __call__(self, source, target):
        StartupTask.barrier.wait()
        super(StartupTask, self).__call__(source, target)


def fill_graph(graph_builder):
    chain_size = 4
    for i in range(1, chain_size + 1):
        graph_builder.add_resource(PythonResource(i))

    graph_builder.add_task(
        Demands(1),
        Creates(2),
        StartupTask())

    for i in range(2, chain_size):
        graph_builder.add_task(
            Demands(i),
            Creates(i + 1),
            AddTask())

    return graph_builder


def test_request_handler(db, resource_storage):
    graph_builder = fill_graph(
        MutagenGraphBuilder(TaskGraphBuilder(), transform=str))

    request_storage = MongoRequestStorage(db)

    def get_request_handler():
        return utils.create_request_handler(
            graph_builder,
            utils.UnittestTaskHandler.create_from_graph_builder(graph_builder, resource_storage),
            request_storage=request_storage
        )

    input_resources = []
    for start_value, addition in [(0, 3), (1, 1)]:
        resource = graph_builder.make_resource(name="1")
        resource.version = Version(properties={"addition": addition})
        resource.version.key = resource.calculate_key()
        resource.value = start_value
        input_resources.append(resource)
        resource_storage.save(resource)

    with get_request_handler() as request_handler:
        normal_id, cancelled_id = [
            request_handler.handle(
                input_name_to_version={"1": input_resource.version},
                target_names=["2", "3", "4"])
            for input_resource in input_resources]
        StartupTask.barrier.wait()
        request_handler.cancel(cancelled_id)

    with get_request_handler() as request_handler:
        def check_condition(condition, error_text):
            if not condition:
                raise AssertionError(error_text)

        status = request_handler.status(normal_id)
        check_condition(
            status.string == RequestStatusString.IN_PROGRESS,
            "Request is not is progress; its status: \n{0}".format(status))

        status = request_handler.status(cancelled_id)
        check_condition(
            status.string == RequestStatusString.CANCELLED,
            "Request did not finish successfully; "
            "its status: \n{0}".format(status))

        target_resource_metas = \
            list(resource_storage.find_versions(name_pattern="4"))
        check_condition(
            len(target_resource_metas) == 0,
            "The number of calculated resources is not 0; got {0}".format(
                target_resource_metas))
