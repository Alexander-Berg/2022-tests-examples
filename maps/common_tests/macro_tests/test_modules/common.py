import time
import shutil
import os.path

from maps.pylibs.utils.lib.common import require
from maps.garden.sdk.core import Task
from maps.garden.sdk.core import MutagenGraphBuilder
from maps.garden.sdk.extensions.property_propagators import EnsureEqualProperties

from maps.garden.common_tests.test_utils.constants import ROAD_GRAPH_DATA


class SleepyTask(Task):
    def __init__(self, interval=0.2):
        super(SleepyTask, self).__init__()
        self._interval = interval

    def __call__(self, *args, **kwargs):
        self._call(*args, **kwargs)
        time.sleep(self._interval)


class UniteSource(SleepyTask):
    def _call(self, source, continent):
        continent.value = {
            "area": sum(source.value["areas"]),
            "region_name": continent.version.properties["region"],
        }

    propagate_properties = EnsureEqualProperties("region")


class Multiply(SleepyTask):
    def __init__(self, ratio, interval=0.0):
        super(Multiply, self).__init__(interval)
        self.ratio = ratio

    def _call(self, continent, multiplied_continent):
        multiplied_continent.value = {
            "area": self.ratio * continent.value["area"],
            "region_name": continent.value["region_name"],
        }


class CreateWorld(Task):
    def __call__(self, *continents, **kwargs):
        world = kwargs["world"]
        continents = [
            c for c in continents
            if not c.version.properties.get("is_empty")
        ]
        world.value = {
            "area": sum(c.value["area"] for c in continents),
            "region_name": [c.value["region_name"] for c in continents],
        }


class CreateWorldWithParams(CreateWorld):
    def __call__(self, build_params, *continents, **kwargs):
        p = build_params.properties
        require('release_name' in p, AssertionError())
        require('autostarted' in p, AssertionError())

        CreateWorld.__call__(self, *continents, **kwargs)


class CreateDeploymentWorld(Task):
    def __call__(self, *args, **kwargs):
        pass


class CreateIndex(SleepyTask):
    def _call(self, *worlds, **kwargs):
        pass


class CreateCache(SleepyTask):
    def _call(self, *worlds, **kwargs):
        pass


class Denormalize(Task):
    def __call__(self, *args, **kwarts):
        pass


class IdentityMap(Task):
    def __call__(self, source, target):
        target.value = source.value

    propagate_properties = EnsureEqualProperties("type")


class CreateRoadGraphTask(Task):
    def __call__(self, _src, road_graph):
        with open(road_graph.path(), "wb") as f:
            f.write(ROAD_GRAPH_DATA)


class CreateRoadGraphDirTask(Task):
    def __call__(self, road_graph, road_graph_dir):
        dirpath = road_graph_dir.path()
        road_graph_name = os.path.basename(road_graph.path())

        require(road_graph_name, AssertionError())

        shutil.copy(road_graph.path(), dirpath)
        os.mkdir(os.path.join(dirpath, "empty_dir"))


def propagate(graph_filler):
    def fill_graph(gb):
        graph_filler(
            MutagenGraphBuilder(
                gb, property_propagator=EnsureEqualProperties(
                    ["region", "vendor", "shipping_date"])))
    return fill_graph
