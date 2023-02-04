from maps.garden.sdk import ecstatic
from maps.garden.sdk.resources import PythonResource

from maps.garden.sdk.extensions import easy_builder
from maps.garden.sdk.extensions import mutagen
from maps.garden.libs.publication import resource_names as publication_lib_rn
from maps.garden.modules.renderer_publication.defs import resource_names as publication_rn

from . import resource_names as rn


INDOOR_DATA_DESIGN_NAME = 'indoor-meta'
INDOOR_DATA_REGION = 'all'

MOBILE_META_DATAPRESTABLE_DATASET = 'yandex-maps-mobile-coverage-vec-dataprestable'


def _add_activate_maps_task(graph_builder, branch):
    graph_builder.task(
        ecstatic.ActivateTask(branch=branch)
    ).creates(
        dataset_deployed=PythonResource(
            name=rn.maps_activated_marker(branch)
        )
    ).demands(
        publication_rn.COMPILED_MAP_ECSTATIC_DATASET
    )


def _add_activate_mobile_meta_task(graph_builder, maps_activated_marker, branch):
    meta_dataset_res_name = publication_lib_rn.mobile_layers_meta_dataset()

    graph_builder.task(
        ecstatic.ActivateTask(branch=branch)
    ).creates(
        dataset_deployed=PythonResource(
            name=rn.mobile_meta_activated(branch)
        )
    ).demands(
        ecstatic_dataset=meta_dataset_res_name,
        dependency_marker=maps_activated_marker
    )


def _add_activate_coverage_task(graph_builder, maps_activated_marker, branch):
    graph_builder.task(
        ecstatic.ActivateTask(branch=branch)
    ).creates(
        dataset_deployed=PythonResource(
            name=rn.coverage_activated(branch)
        )
    ).demands(
        ecstatic_dataset=publication_lib_rn.coverage_dataset(),
        dependency_marker=maps_activated_marker
    )


@mutagen.propagate_properties('release')
def add_activate_release_tasks(graph_builder, branch):
    graph_builder = easy_builder.EasyBuilder(graph_builder)
    _add_activate_maps_task(graph_builder, branch)
    _add_activate_mobile_meta_task(graph_builder, rn.maps_activated_marker(branch), branch)
    _add_activate_coverage_task(graph_builder, rn.maps_activated_marker(branch), branch)


def fill_graph(graph_builder, regions):
    add_activate_release_tasks(graph_builder,
                               ecstatic.TESTING_BRANCH)
