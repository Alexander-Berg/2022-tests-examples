from pymongo.errors import InvalidDocument

from maps.garden.sdk.utils import KB
from maps.garden.sdk.core import (
    TaskGraphBuilder, Task, Demands, Creates, Version)
from maps.garden.sdk.resources.python import PythonResource
from maps.garden.libs_server.graph.request_storage import RAMRequestStorage

from . import utils

BIG_DOCUMENT_SIZE = 1 * KB
TASK_FAIL_SIZE = 2 * BIG_DOCUMENT_SIZE


class BigFailTask(Task):
    def predict_consumption(self, demands, creates):
        return {}

    def __call__(self, *args, **kwargs):
        raise Exception(u"я" * TASK_FAIL_SIZE)


class RequestStorage(RAMRequestStorage):
    def update_status(self, request):
        serialized_request = str(request)
        if len(serialized_request) > BIG_DOCUMENT_SIZE:
            raise InvalidDocument()
        return super().update_status(request)


# TODO: Use mock to replace RequestStorage instead of adding a parameter to execute_graph()
# TODO: Add extra test with many small failure tasks instead of one with huge failure messages
# TODO: Check if exception text is partially retrievable from execution result
def test_big_fail(resource_storage):
    # This test checks that request with large amounts of failed tasks
    # does not crash garden because of mongo record size limit
    graph_builder = TaskGraphBuilder()
    graph_builder.add_resource(PythonResource("source"))
    graph_builder.add_resource(PythonResource("target"))
    graph_builder.add_task(
        Demands("source"),
        Creates("target"),
        BigFailTask()
    )

    source_resource = PythonResource("source")
    source_resource.version = Version()
    resource_storage.save(source_resource)

    result = utils.execute_graph(
        graph_builder,
        request_storage=RequestStorage(),
        storage=resource_storage,
        target_names=["target"],
    )
    # resource named target must present result, but its value must be None
    assert len(result) == 1
    for value in result.values():
        assert value is None
