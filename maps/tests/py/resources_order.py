from maps.garden.sdk.core import TaskGraphBuilder, Task, Demands, Creates, Version
from maps.garden.sdk.resources.python import PythonResource

from . import utils


class CheckResourceOrderTask(Task):
    def predict_consumption(self, demands, creates):
        assert list(demands) == [0, 1]
        assert demands[0].name == "input_resource1"
        assert demands[1].name == "input_resource2"
        return {"cpu": 1, "ram": 1}

    def propagate_properties(self, demands, creates):
        assert list(demands) == [0, 1]

    def __call__(self, *resources):
        assert resources[0].name == "input_resource1"
        assert resources[1].name == "input_resource2"
        assert resources[2].name == "output_resource"


class CheckInverseResourceOrderTask(Task):
    def predict_consumption(self, demands, creates):
        assert list(demands) == [0, 1]
        assert demands[0].name == "input_resource2"
        assert demands[1].name == "input_resource1"
        return {"cpu": 1, "ram": 1}

    def propagate_properties(self, demands, creates):
        assert list(demands) == [0, 1]

    def __call__(self, *resources):
        assert resources[0].name == "input_resource2"
        assert resources[1].name == "input_resource1"
        assert resources[2].name == "output_resource_inverse"


def test_resources_order(resource_storage):
    graph_builder = TaskGraphBuilder()
    graph_builder.add_resource(PythonResource("input_resource1"))
    graph_builder.add_resource(PythonResource("input_resource2"))
    graph_builder.add_resource(PythonResource("output_resource"))
    graph_builder.add_resource(PythonResource("output_resource_inverse"))

    graph_builder.add_task(
        Demands("input_resource1", "input_resource2"),
        Creates("output_resource"),
        CheckResourceOrderTask())

    # Try different lexicographic order of demands resources
    graph_builder.add_task(
        Demands("input_resource2", "input_resource1"),
        Creates("output_resource_inverse"),
        CheckInverseResourceOrderTask())

    input_resource1 = graph_builder.make_resource("input_resource1")
    input_resource1.version = Version(properties={"key": 1})
    resource_storage.save(input_resource1)

    input_resource2 = graph_builder.make_resource("input_resource2")
    input_resource2.version = Version(properties={"key": 2})
    resource_storage.save(input_resource2)

    result = utils.execute_graph(graph_builder, storage=resource_storage)
    utils.check_result(result)
