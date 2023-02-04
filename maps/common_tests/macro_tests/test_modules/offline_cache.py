from maps.garden.sdk import core
from maps.garden.sdk.resources.python import PythonResource

from . import common


def fill_graph(gb):
    world = PythonResource(
        name="cache",
        value={"area": 0, "region_names": []}
    )
    gb.add_resource(world)

    gb.add_task(
        core.Demands("world"),
        core.Creates(cache="cache"),
        common.CreateCache()
        )

modules = [{
    "name": "offline_cache",
    "fill_graph": fill_graph
    }]
