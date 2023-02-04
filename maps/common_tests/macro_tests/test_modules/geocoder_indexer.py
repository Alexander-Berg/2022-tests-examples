from maps.garden.sdk import core
from maps.garden.sdk.resources.python import PythonResource

from . import common


def fill_graph(gb):
    index = PythonResource(
        name="index",
        value={"area": 0, "region_names": []}
    )
    gb.add_resource(index)

    gb.add_task(
        core.Demands("australia_shrinked", "europe_expanded"),
        core.Creates(index="index"),
        common.CreateIndex(interval=2.0)
        )

modules = [{
    "name": "geocoder_indexer",
    "fill_graph": fill_graph
    }]
