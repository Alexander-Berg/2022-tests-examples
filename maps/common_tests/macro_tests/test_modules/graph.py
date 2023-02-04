from maps.garden.sdk import core
from maps.garden.sdk.resources.python import PythonResource

from . import common


def fill_graph(gb):
    world = PythonResource(
        name="graph",
        value={"area": 0, "region_names": []}
    )
    gb.add_resource(world)

    gb.add_task(
        core.Demands("australia_shrinked", "europe_expanded"),
        core.Creates(world="graph"),
        common.CreateWorld())


def fill_deployment_graph(gb):
    world = PythonResource(
        name="graph_deployment",
        value={"area": 0, "region_names": []}
    )
    gb.add_resource(world)

    gb.add_task(
        core.Demands("graph"),
        core.Creates("graph_deployment"),
        common.CreateDeploymentWorld())


modules = [
    {
        "name": "graph_build",
        "fill_graph": fill_graph
    },
    {
        "name": "graph_deployment",
        "fill_graph": fill_deployment_graph
    }
]
