from . import common
from maps.garden.sdk import core, resources


def fill_graph(gb):
    gb.add_resource(
        resources.FileResource("road_graph", "road_graph.dat"))

    gb.add_resource(
        resources.DirResource("road_graph_dir", "road_graph.dir"))

    gb.add_task(
        core.Demands("europe_src"),
        core.Creates("road_graph"),
        common.CreateRoadGraphTask())

    gb.add_task(
        core.Demands("road_graph"),
        core.Creates("road_graph_dir"),
        common.CreateRoadGraphDirTask())


modules = [{
    "name": "road_graph",
    "fill_graph": fill_graph
}]
