from maps.garden.sdk.core import Demands, Creates
from maps.garden.sdk.ecstatic import DatasetResource  # noqa
from maps.garden.sdk.extensions import mutagen
from maps.garden.sdk.resources import PythonResource

from maps.garden.libs.masstransit_validation.lib.router_validation import ValidatingRoutesTask
from maps.masstransit.tools.compare_routers.lib.ammo import read_pedestrian_validation_config
from maps.garden.libs.pedestrian_graph.common import PEDESTRIAN_GRAPH_ECSTATIC_DATASET


PEDESTRIAN_ROUTES_VALIDATION_CONFIG = 'pedestrian_tester/pedestrian_config.json'
PEDESTRIAN_HANDLE = '/pedestrian/v2/route'
PEDESTRIAN_RPS = 50


class ValidatingPedestrianRoutesTask(ValidatingRoutesTask):
    def __init__(self):
        self._name = 'pedestrian'
        self._rps = PEDESTRIAN_RPS
        self._handle = PEDESTRIAN_HANDLE
        self._requests_sets = read_pedestrian_validation_config(PEDESTRIAN_ROUTES_VALIDATION_CONFIG)
        super().__init__()

    def __call__(self, pedestrian_graph, pedestrian_result):
        pedestrian_result.value = self.validation()


@mutagen.propagate_properties("shipping_date")
def fill_graph(graph_builder, regions=None):
    graph_builder.add_resource(
        PythonResource("pedestrian_routes_validation_completed")
    )
    graph_builder.add_task(
        Demands(pedestrian_graph=PEDESTRIAN_GRAPH_ECSTATIC_DATASET),
        Creates(pedestrian_result="pedestrian_routes_validation_completed"),
        ValidatingPedestrianRoutesTask()
    )
