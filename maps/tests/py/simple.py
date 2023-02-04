from maps.garden.sdk.resources.python import PythonResource
from maps.garden.sdk.core import TaskGraphBuilder, Task, Demands, Creates
from maps.garden.sdk.extensions.property_propagators import EnsureEqualProperties

from . import utils


def create_resource(name):
    return PythonResource(name=name, value=name)


class CreatorTask(Task):
    def propagate_properties(self, demands, creates):
        super(CreatorTask, self).propagate_properties(demands, creates)
        for properties in creates.values():
            properties.update({'maps_graph': {'version': '5.7.0'}})

    def __call__(self, output):
        # nothing to do now with the resource
        pass


class PrinterTask(Task):
    def propagate_properties(self, demands, creates):
        super(PrinterTask, self).propagate_properties(demands, creates)
        EnsureEqualProperties(['maps_graph'])(demands, creates)

    def __call__(self, *args, **kwargs):
        # TODO thegeorg@: it is better to convert this task to SummationTask
        print("I am the printer")


def fill_graph(graph_builder):
    for name in ["input", "inter1", "inter2", "inter3", "unused", "output", "output2"]:
        graph_builder.add_resource(create_resource(name))

    graph_builder.add_task(
        Demands(),
        Creates("input"),
        CreatorTask())
    graph_builder.add_task(
        Demands("input"),
        Creates("inter1", "inter2"),
        PrinterTask())
    graph_builder.add_task(
        Demands("input"),
        Creates("inter3", "output2"),
        PrinterTask())
    graph_builder.add_task(
        Demands("input", "inter2"),
        Creates("unused"),
        PrinterTask())
    graph_builder.add_task(
        Demands("inter1", arg2="inter2", arg3="inter3"),
        Creates("output"),
        PrinterTask()
    )


def test_simple():
    graph_builder = TaskGraphBuilder()
    fill_graph(graph_builder)

    result = utils.execute_graph(graph_builder)
    utils.check_result(result)
