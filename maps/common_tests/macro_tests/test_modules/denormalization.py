from maps.garden.sdk import core
from maps.garden.sdk.resources.python import PythonResource

from . import common


def fill_graph_au(graph_builder):
    australia = PythonResource(
        name="denormalized_australia",
        value={"area": 0, "region_name": None}
    )
    graph_builder.add_resource(australia)

    graph_builder.add_task(
        core.Demands("australia_shrinked"),
        core.Creates("denormalized_australia"),
        common.Denormalize())


def fill_graph_eu(graph_builder):
    australia = PythonResource(
        name="denormalized_europe",
        value={"area": 0, "region_name": None}
    )
    graph_builder.add_resource(australia)

    graph_builder.add_task(
        core.Demands("europe"),
        core.Creates("denormalized_europe"),
        common.Denormalize())


def fill_graph(graph_builder):
    fill_graph_au(graph_builder)
    fill_graph_eu(graph_builder)


modules = [{
    "name": "denormalization",
    "fill_graph": fill_graph
}]
