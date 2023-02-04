from maps.garden.sdk.core import Demands, Creates
from maps.garden.sdk.ecstatic import DatasetResource  # noqa
from maps.garden.sdk.extensions import mutagen
from maps.garden.sdk.resources import PythonResource

from maps.garden.libs.masstransit_validation.lib.router_validation import ValidatingRoutesTask
from maps.masstransit.tools.compare_routers.lib.ammo import read_masstransit_validation_config


MASSTRANSIT_ROUTES_VALIDATION_CONFIG = 'masstransit_tester/config.json'
MASSTRANSIT_HANDLE = '/masstransit/v2/route'
MASSTRANSIT_RPS = 10


class ValidatingMasstransitRoutesTask(ValidatingRoutesTask):
    def __init__(self):
        self._name = 'masstransit'
        self._rps = MASSTRANSIT_RPS
        self._handle = MASSTRANSIT_HANDLE
        self._requests_sets = read_masstransit_validation_config(MASSTRANSIT_ROUTES_VALIDATION_CONFIG)
        super().__init__()

    def __call__(self, masstransit_data, masstransit_result):
        masstransit_result.value = self.validation()


@mutagen.propagate_properties("shipping_date")
def fill_graph(graph_builder, regions=None):
    graph_builder.add_resource(
        PythonResource("masstransit_routes_validation_completed")
    )
    graph_builder.add_task(
        Demands(masstransit_data="yandex-maps-masstransit-data"),
        Creates(masstransit_result="masstransit_routes_validation_completed"),
        ValidatingMasstransitRoutesTask()
    )
