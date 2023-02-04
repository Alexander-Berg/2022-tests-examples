from maps.garden.sdk.core import Demands, Creates
from maps.garden.sdk.resources import PythonResource
from maps.garden.sdk.extensions import mutagen
from maps.garden.modules.transit_indexer import defs

from . import transit_testing_task


@mutagen.propagate_properties("release")
def fill_graph(graph_builder, regions=None):
    graph_builder.add_resource(
        PythonResource(defs.TRANSIT_INDEX_TESTED,
                       doc="All transit tests passed."))

    graph_builder.add_task(
        Demands(stand_is_ready=defs.TRANSIT_INDEX_DEPLOYED.format(stage="validation_stage")),
        Creates(test_results=defs.TRANSIT_INDEX_TESTED),
        transit_testing_task.TransitIndexTesterTask())
