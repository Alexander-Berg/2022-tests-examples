from . import common
from maps.garden.sdk import core, resources


def fill_graph(gb):
    gb.add_resource(resources.PythonResource("target"))
    gb.add_task(
        core.Demands("source"),
        core.Creates("target"),
        common.IdentityMap()
    )


modules = [{
    "name": "signals_process",
    "fill_graph": fill_graph
}]
