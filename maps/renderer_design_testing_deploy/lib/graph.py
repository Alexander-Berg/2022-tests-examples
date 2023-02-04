from maps.garden.modules.renderer_design_testing.defs import resource_names as design_testing_rn

from maps.garden.sdk import ecstatic
from maps.garden.sdk.resources import PythonResource

from maps.garden.sdk.extensions import easy_builder
from maps.garden.sdk.extensions import mutagen

from .tasks import NotifyTask


@mutagen.propagate_properties('release_name')
def fill_graph(graph_builder, regions):
    builder = easy_builder.EasyBuilder(graph_builder)
    release_activated_res_name = 'designtesting_release_activated'
    builder.task(
        ecstatic.ActivateTask(branch=ecstatic.STABLE_BRANCH)
    ).creates(
        release_activated=PythonResource(name=release_activated_res_name)
    ).demands(
        design_testing_rn.COMPILED_MAP_ECSTATIC_DATASET
    )

    builder.task(
        NotifyTask(),
    ).creates(
        notify_res=PythonResource(name='designtesting_notification')
    ).demands(
        design_testing_rn.COMPILED_MAP_ECSTATIC_DATASET,
        release_activated_marker=release_activated_res_name,
    )
